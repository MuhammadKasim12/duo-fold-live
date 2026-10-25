package org.duofold.live

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.*
import android.view.*

/** One bounded scene owns both logical windows across a physical primary exchange.
 * Its outgoing image is captured before moving the task, never from the vacated Home.
 * Frame-commit acknowledgements are software evidence, not visible-pixel proof. */
internal object B8Presentation {
 private val main=Handler(Looper.getMainLooper())
 private var session=0L
 private var blocked=0L
 private var lastUpdate=0L
 private var started=0L
 private var source=""
 private var task=-1
 private var pending=false
 private var retryAt=0L
 private var original:GlassFrame?=null
 private val heldPanels=HashMap<String,GlassFrame>()
 private val windows=ArrayList<PanelWindow>()
 @Volatile private var acknowledgement=Bundle()
 private var status="Coordinated B8 presentation idle"
 @JvmStatic fun acknowledgement():Bundle=Bundle(acknowledgement)
 @JvmStatic fun report()=status
 private val guard=object:Runnable{override fun run(){
  if(session==0L)return
  val host=StandaloneService.instance
  val now=SystemClock.elapsedRealtime()
  if(host==null||!allowed(host)||now-lastUpdate>750||now-started>=30000){blocked=session;clear("lease/lock/timeout");return}
  main.postDelayed(this,100)
 }}
 private fun allowed(context:Context):Boolean {
  val prefs=context.getSharedPreferences("standalone",0)
  return prefs.getBoolean("enabled",false)&&prefs.getBoolean("live_b8_enabled",false)&&!prefs.getBoolean("dual",false)&&LiveAngles.effectAllowed&&LiveAngles.fresh()&&context.getSystemService(PowerManager::class.java).isInteractive&&!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
 }
 @JvmStatic fun clear(reason:String){
  val was=session
  session=0;pending=false;acknowledgement=Bundle();main.removeCallbacks(guard)
  windows.forEach{it.close()};windows.clear();original=null;heldPanels.clear();GlassFrames.b8CapturePause(false)
  status="Coordinated B8 presentation released: $reason"
  if(was!=0L)RecoveryLog.add("B8 PRESENTATION_RELEASE session=$was reason=$reason")
 }
 @JvmStatic fun update(context:Context,info:Bundle?){
  val next=info?.getLong("session")?:0L
  val host=StandaloneService.instance
  if(next==0L||host==null||!allowed(context)) {if(session!=0L)clear("controller/setting ended");GlassFrames.b8CapturePause(info?.getBoolean("pauseCapture")==true);return}
  if(next==blocked)return
  if(next!=session){
   clear("new session");session=next;started=SystemClock.elapsedRealtime();source=info!!.getString("source","");task=info.getInt("task",-1);retryAt=0
   main.postDelayed(guard,100)
  }
  lastUpdate=SystemClock.elapsedRealtime()
  GlassFrames.b8CapturePause(info?.getBoolean("pauseCapture")==true)
  if(original==null&&!pending&&lastUpdate>=retryAt&&task>=0){
   pending=true;val token=session
   GlassFrames.incoming(task,source,0){frame,note->
    if(token!=session)return@incoming
    pending=false
    if(frame==null||frame.physicalId!=source){status="B8 presentation waiting for exact outgoing app: $note";retryAt=SystemClock.elapsedRealtime()+100;return@incoming}
    original=frame;heldPanels[source]=frame
    try{
     val dm=host.getSystemService(DisplayManager::class.java)
     for(id in 0..1){val d=dm.getDisplay(id)?:error("Missing built-in D$id");check(BuiltInPanel.accepts(d));val w=PanelWindow(host,d,token,frame);windows.add(w);w.show()}
     status="B8 app composites attached; waiting for both commits; source=$source task=$task"
     RecoveryLog.add(status)
    }catch(e:Exception){blocked=token;clear("attachment failed: ${e.message}")}
   }
  }
  publish()
 }
 private fun publish(){
  val now=SystemClock.elapsedRealtime()
  val both=windows.size==2&&windows.all{it.validCommit(now)}
  val primary=windows.firstOrNull{it.id==0&&it.validCommit(now)}
  acknowledgement=Bundle().apply{putLong("session",session);putBoolean("capturePaused",GlassFrames.b8CaptureQuiescent());putBoolean("prepared",both);putString("primary",primary?.committedPhysical?:"");putLong("commitAt",primary?.committedAt?:0)}
  if(both)status="B8 coordinated app + glass committed on both panels; outgoing held to endpoint; session=$session"
 }
 private class PanelWindow(host:StandaloneService,display:Display,private val token:Long,private val outgoing:GlassFrame){
  val id=display.displayId
  private val ctx=host.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,null)
  private val wm=ctx.getSystemService(WindowManager::class.java)
  private val dm=host.getSystemService(DisplayManager::class.java)
  private val prefs=host.getSharedPreferences("standalone",0)
  private val painter=FrostSurface(ctx)
  private var attached=false
  private var physical=""
  var committedPhysical="";private set
  var committedAt=0L;private set
  private var committedWidth=0;private var committedHeight=0;private var committedRotation=-1
  private var commitPending=false
  private var visual=Float.NaN
  private var drawnAt=0L
  private var held=outgoing
  private val view=object:View(ctx){
   override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){painter.layout(0,0,w,h);committedAt=0}
   override fun onDraw(canvas:Canvas){
    val d=dm.getDisplay(id)?:return
    val identity=PanelIdentity.identity(d)
    val size=Point();d.getRealSize(size)
    // Always paint an opaque app backdrop, including transient identity/geometry gaps.
    // Live updates are accepted only for the verified physical primary; the other
    // panel keeps its last app image even after Android puts Home on that stack.
    if(PanelIdentityPolicy.known(identity)&&identity!=physical){physical=identity;held=heldPanels[identity]?:outgoing}
    val live=GlassFrames.frame
    if(id==0&&live!=null&&live.physicalId==identity&&live.displayId==0&&live.rotation==d.rotation&&live.width==size.x&&live.height==size.y&&SystemClock.elapsedRealtime()-live.stamp in 0..250){held=live;heldPanels[identity]=live}
    drawScreenshotBackdrop(canvas,held,width,height)
    val now=SystemClock.elapsedRealtime()
    val angle=LiveAngles.angle
    val dt=if(drawnAt==0L)8.33f else (now-drawnAt).toFloat().coerceIn(1f,50f)
    visual=FrameSmoothing.step(visual,angle,dt,prefs.getFloat("smoothing_ms",FrameSmoothing.DEFAULT_MS));drawnAt=now
    val inner=minOf(size.x,size.y).toFloat()/maxOf(size.x,size.y)>.7f
    val amount=DuoShadeCurve.progress(visual,inner,prefs.getFloat("open_threshold",172f))
    painter.configure(held,amount,prefs.getFloat("intensity",.4f),inner,d.rotation,true,visual,true)
    painter.drawEmbedded(canvas)
    if(!commitPending&&PanelIdentityPolicy.known(identity)&&width==size.x&&height==size.y){
     commitPending=true;val w=width;val h=height;val rotation=d.rotation
     viewTreeObserver.registerFrameCommitCallback{
      commitPending=false
      if(session==token&&attached&&PanelIdentity.identity(dm.getDisplay(id))==identity&&width==w&&height==h){
       committedPhysical=identity;committedWidth=w;committedHeight=h;committedRotation=rotation;committedAt=SystemClock.elapsedRealtime();publish()
      }
     }
    }
    if(session==token&&attached)postInvalidateOnAnimation()
   }
  }
  fun validCommit(now:Long):Boolean {
   val d=dm.getDisplay(id)?:return false
   val size=Point();d.getRealSize(size)
   return attached&&committedAt>0&&now-committedAt<=250&&PanelIdentity.identity(d)==committedPhysical&&committedWidth==size.x&&committedHeight==size.y&&committedRotation==d.rotation
  }
  fun show(){
   val lp=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.OPAQUE)
   lp.alpha=1f;lp.dimAmount=0f;lp.windowAnimations=0;lp.gravity=Gravity.TOP or Gravity.LEFT;lp.setFitInsetsTypes(0);lp.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;lp.title="Duo B8 coordinated app scene"
   wm.addView(view,lp);attached=true
   view.post{
    if(session!=token||!attached)return@post
    try{
     val root=View::class.java.getDeclaredMethod("getViewRootImpl").invoke(view)
     val sc=root.javaClass.getMethod("getSurfaceControl").invoke(root) as SurfaceControl
     check(sc.isValid);GlassFrames.surface(view,sc)
     // Capture exclusion only. The B8 WMS mirror must include this animated window.
    }catch(e:Exception){blocked=token;clear("capture exclusion unavailable")}
   }
  }
  fun close(){attached=false;committedAt=0;GlassFrames.surface(view,null);runCatching{wm.removeViewImmediate(view)}}
 }
}
