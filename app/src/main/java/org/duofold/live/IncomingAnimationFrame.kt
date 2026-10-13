package org.duofold.live

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.runtime.*

/** Verify the exact incoming app once, then feed fresh excluded-overlay captures to its animation. */
internal object IncomingAnimationFrame {
 var frame by mutableStateOf<GlassFrame?>(null);private set
 var status="Incoming animation idle";private set
 private var session=0L
 private var generation=0
 private var pending=false
 private var retryAt=0L
 private var physical=""
 private var cleanStamp=0L
 private var capturing=false
 private var updates=0
 private var lastStamp=0L
 private var lastSession="No incoming live session completed"
 @JvmStatic fun report()="$status\nLast incoming session: $lastSession"
 @JvmStatic fun clear(){
  if(session>0L)lastSession="session=$session; physical=$physical; live updates=$updates; exact app verified=${cleanStamp>0L}; last status=$status"
  generation++;session=0L;physical="";frame=null;pending=false;retryAt=0L;cleanStamp=0L;updates=0;lastStamp=0L;releaseCapture();status="Incoming animation idle"
 }
 private fun releaseCapture(){if(capturing){capturing=false;GlassFrames.release()}}
 @JvmStatic fun update(context:Context,info:Bundle?,fresh:Boolean,allowed:Boolean){
  val prefs=context.getSharedPreferences("standalone",0)
  val next=info?.getLong("session")?:0L
  val valid=fresh&&allowed&&prefs.getBoolean("enabled",false)&&prefs.getBoolean("dual",false)&&prefs.getBoolean("screenshot_camera_startup",false)&&info?.getBoolean("active")==true&&next>0L&&context.getSystemService(PowerManager::class.java).isInteractive&&!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
  val target=info?.getString("physical")?:""
  if(!valid||next!=session||target!=physical){
   clear()
   session=if(valid)next else 0L;physical=if(valid)target else ""
   status=if(valid)"Waiting for exact incoming app route" else "Incoming animation idle"
  }
  if(!valid||target.isEmpty())return
  if(info?.getBoolean("routed")!=true){
   if(capturing||pending||cleanStamp>0L){generation++;pending=false;cleanStamp=0L;frame=null;releaseCapture()}
   status="Incoming animation waiting for fullscreen app route";return
  }
  val now=SystemClock.elapsedRealtime()
  if(cleanStamp>0L){
   val live=GlassFrames.frame
   if(live!=null&&ScreenshotLiveFramePolicy.accepts(physical,live.physicalId,live.displayId,cleanStamp,live.stamp,now,info.getInt("width"),info.getInt("height"),live.width,live.height)){
    frame=live
    if(lastStamp!=live.stamp){lastStamp=live.stamp;updates++}
    status="Incoming live animation: D1; updates=$updates; frame age=${now-live.stamp} ms; physical=$physical"
   }else{
    if(frame!=null&&now-frame!!.stamp>350L)frame=null
    status="Incoming live animation waiting for fresh destination frame; updates=$updates; ${GlassFrames.status}"
   }
   return
  }
  if(pending||now<retryAt)return
  val task=info.getInt("task",-1)
  if(task<0)return
  pending=true;val epoch=generation
  GlassFrames.incoming(task,target){captured,note->
   if(epoch!=generation)return@incoming
   pending=false
   if(captured!=null&&captured.physicalId==physical){
    cleanStamp=captured.stamp;frame=captured
    GlassFrames.configure(context)
    if(!capturing){capturing=true;GlassFrames.acquire()}
    GlassFrames.requestFreshCapture()
    status="Exact incoming app verified; starting live animation captures; task=$task; D1"
    RecoveryLog.add(status)
   }else{status="Incoming capture waiting: $note";retryAt=SystemClock.elapsedRealtime()+150L}
  }
 }
}
