package org.duofold.live

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.*
import android.view.*
import android.widget.ImageView

/** Independent from the main overlay rebuild: follows one physical panel through D1 -> D0. */
internal object IncomingHold {
 private val main=Handler(Looper.getMainLooper())
 private val events=ArrayDeque<String>()
 private val windows=ArrayList<HoldWindow>()
 private var generation=0
 private var session=0L
 private var attempted=false
 private var engaged=false
 private var frame:GlassFrame?=null
 private var deadline=0L
 private var context:Context?=null
 private var lastUpdate=0L
 private var task=-1
 private var physical=""
 private var closing=false
 private var captureAngle=0f
 private var pendingReady=false
 private var readyCount=0
 private var checkAt=0L
 private var lastNote=""
 private fun log(s:String){events.addLast("${SystemClock.elapsedRealtime()}: $s");while(events.size>30)events.removeFirst();RecoveryLog.add("Incoming hold: $s")}
 @JvmStatic fun report()="Incoming app screenshot hold (commit/readiness are software evidence):\n"+events.joinToString("\n")
 @JvmStatic fun clear(reason:String){
  generation++;main.removeCallbacks(tick);windows.forEach{it.close()};windows.clear();frame=null;pendingReady=false;readyCount=0
  if(engaged)log("RELEASE $reason; session=$session")
  engaged=false
 }
 @JvmStatic fun update(ctx:Context,info:Bundle?,angle:Float,fresh:Boolean,allowed:Boolean){
  val now=SystemClock.elapsedRealtime();lastUpdate=now;context=ctx.applicationContext
  val prefs=ctx.getSharedPreferences("standalone",0)
  if(!allowed||!fresh||!prefs.getBoolean("enabled",false)||!prefs.getBoolean("dual",false)||!prefs.getBoolean("screenshot_camera_startup",false)||!prefs.getBoolean("incoming_app_hold",true)||ctx.getSystemService(KeyguardManager::class.java).isKeyguardLocked||!ctx.getSystemService(PowerManager::class.java).isInteractive){clear("disabled, stale, or locked");return}
  val next=info?.getLong("session")?:0L
  if(next!=session){clear("new session");session=next;attempted=false}
  if(engaged&&((closing&&angle>captureAngle+10f)||(!closing&&angle<captureAngle-10f))){clear("direction reversed");return}
  if(attempted||next==0L||info==null||!info.getBoolean("active"))return
  val close=info.getBoolean("closing")
  if(!IncomingHoldPolicy.capture(close,angle,prefs.getFloat("open_threshold",172f)))return
  val selected=info.getInt("task",-1);val target=info.getString("physical")?:""
  attempted=true
  if(selected<0||target.isEmpty()){log("SKIP no exact app/physical target");return}
  task=selected;physical=target;closing=close;captureAngle=angle;engaged=true;deadline=now+1800;checkAt=0;lastNote=""
  val epoch=++generation;log("CAPTURE session=$session task=$task physical=$physical angle=$angle; maximum hold=1800 ms")
  main.post(tick)
  GlassFrames.incoming(task,physical){captured,note->
   if(epoch!=generation)return@incoming
   if(captured==null||captured.physicalId!=physical||SystemClock.elapsedRealtime()>=deadline){log("SKIP $note");clear("capture unavailable/late");return@incoming}
   frame=captured
   val dm=ctx.getSystemService(DisplayManager::class.java)
   try{
    for(id in 0..1){val d=dm.getDisplay(id)?:continue;windows.add(HoldWindow(ctx,d,captured));windows.last().show()}
    log("ATTACHED two logical hosts; capture age=${SystemClock.elapsedRealtime()-captured.stamp} ms")
   }catch(e:Exception){log("ATTACH failed ${e.javaClass.simpleName}");clear("attachment failure")}
  }
 }
 private val tick=object:Runnable{override fun run(){
  if(!engaged)return
  val ctx=context?:return;val now=SystemClock.elapsedRealtime()
  if(now>=deadline||now-lastUpdate>750||ctx.getSystemService(KeyguardManager::class.java).isKeyguardLocked||!ctx.getSystemService(PowerManager::class.java).isInteractive){clear("deadline, connection loss, or lock");return}
  windows.forEach{it.refresh()}
  val primary=ctx.getSystemService(DisplayManager::class.java).getDisplay(0)
  val f=frame
  if(f!=null&&primary!=null&&identity(primary)==physical&&primary.state==Display.STATE_ON&& !pendingReady && now>=checkAt){
   pendingReady=true;checkAt=now+150;val epoch=generation
   GlassFrames.incomingReady(task,physical,f.width,f.height){ready,note->
    if(epoch!=generation)return@incomingReady
    pendingReady=false
    if(note!=lastNote){lastNote=note;log("D0 readiness: $note")}
    readyCount=if(ready)readyCount+1 else 0
    if(readyCount>=2){log("Exact app HAS_DRAWN twice; releasing after 64 ms settle")
     main.postDelayed({if(epoch==generation)clear("app drawn on incoming D0")},64)
    }
   }
  }
  main.postDelayed(this,16)
 }}
 private fun identity(d:Display?):String=if(d==null)"" else runCatching{Display::class.java.getMethod("getUniqueId").invoke(d).toString()}.getOrDefault("")
 private class HoldWindow(ctx:Context,d:Display,val captured:GlassFrame){
  private val id=d.displayId
  private val dm=ctx.getSystemService(DisplayManager::class.java)
  private val wc=ctx.createDisplayContext(d).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null)
  private val dialog=Dialog(wc)
  private val image=ImageView(wc)
  private var wasVisible=false
  private var commitPending=false
  private var alive=true
  init{
   image.setImageBitmap(captured.bitmap);image.scaleType=ImageView.ScaleType.FIT_CENTER;image.alpha=0f
   image.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
   image.viewTreeObserver.addOnPreDrawListener{refresh();true}
   image.viewTreeObserver.addOnDrawListener{
    if(wasVisible&&!commitPending){commitPending=true;image.viewTreeObserver.registerFrameCommitCallback{if(alive&&wasVisible)log("COMMIT host D$id physical=${captured.physicalId}; age=${SystemClock.elapsedRealtime()-captured.stamp} ms")}}
   }
   dialog.window?.apply{
    setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);setFormat(PixelFormat.TRANSLUCENT);setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    attributes=attributes.apply{dimAmount=0f;setFitInsetsTypes(0);layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;title="Duo incoming app endpoint hold"}
   }
   dialog.setContentView(image)
  }
  fun show(){dialog.show();dialog.window?.setLayout(-1,-1);refresh()}
  fun refresh(){
   val d=dm.getDisplay(id)
   val visible=identity(d)==captured.physicalId
   if(visible!=wasVisible){wasVisible=visible;commitPending=false;image.alpha=if(visible)1f else 0f;log("HOST D$id visible=$visible physical=${identity(d)} state=${d?.state}");image.invalidate()}
  }
  fun close(){alive=false;image.alpha=0f;image.setImageDrawable(null);runCatching{dialog.dismiss()}}
 }
}
