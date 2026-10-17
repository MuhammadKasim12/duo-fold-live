package org.duofold.live

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderNode
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.SurfaceHolder

/** One producer per live surface. Caller keeps the recording immutable until completion. */
internal class LiveSurfaceSubmitter(private val holder:SurfaceHolder) {
 private val thread=HandlerThread("DuoLiveSurface").apply{start()}
 private val handler=Handler(thread.looper)
 fun submit(node:RenderNode,width:Int,height:Int,done:(Long,Long,Long,Throwable?)->Unit){
  check(handler.post{
   var locked=SystemClock.elapsedRealtimeNanos()
   var drawn=locked
   var error:Throwable?=null
   try{
    val canvas=holder.lockHardwareCanvas()
    locked=SystemClock.elapsedRealtimeNanos()
    try{
     canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
     canvas.scale(canvas.width.toFloat()/width,canvas.height.toFloat()/height)
     canvas.drawRenderNode(node)
    }finally{
     drawn=SystemClock.elapsedRealtimeNanos()
     holder.unlockCanvasAndPost(canvas)
    }
   }catch(failure:Exception){error=failure}
   done(locked,drawn,SystemClock.elapsedRealtimeNanos(),error)
  })
 }
 fun close(){
  thread.quitSafely()
  // No worker operation waits on main. Keep waiting if interrupted: returning
  // early would permit native canvas access after SurfaceHolder destruction.
  var interrupted=false
  while(thread.isAlive){try{thread.join()}catch(_:InterruptedException){interrupted=true}}
  if(interrupted)Thread.currentThread().interrupt()
 }
}
