package org.duofold.live

import android.graphics.Rect
import android.view.SurfaceControl

/** An owned effect child ABOVE the projected buffer. Its parent is already excluded
 * from captures, so neither projection nor blur is fed back into the next capture.
 * Never changes another app's layer. Android may suppress accepted blur requests. */
internal class ProjectedSystemGlassBlur {
 private var parent:SurfaceControl?=null
 private var layer:SurfaceControl?=null
 private var radius=-1
 private var width=0
 private var height=0
 private var failed=false
 fun apply(sc:SurfaceControl,w:Int,h:Int,requested:Int,submit:(SurfaceControl.Transaction)->Unit):Boolean {
  if(!sc.isValid || w<=0 || h<=0){clear();return false}
  if(parent!==sc){clear();parent=sc}
  if(failed)return false
  if(requested==0 && layer==null)return true
  if(radius==requested && width==w && height==h)return true
  return try {
   val effect=layer ?: run {
    val builder=SurfaceControl.Builder().setName("Duo projected compositor blur").setParent(sc)
    SurfaceControl.Builder::class.java.getMethod("setEffectLayer").invoke(builder)
    builder.build().also{layer=it}
   }
   SurfaceControl.Transaction().use { t ->
    // SurfaceView's child coordinates follow the buffer size and parent transform.
    t.setLayer(effect,1).setCrop(effect,Rect(0,0,w,h)).setAlpha(effect,1f)
    SurfaceControl.Transaction::class.java.getMethod("setBackgroundBlurRadius",SurfaceControl::class.java,Int::class.javaPrimitiveType).invoke(t,effect,requested)
    t.setVisibility(effect,requested>0)
    submit(t)
   }
   radius=requested;width=w;height=h
   if(requested>0)GlassRenderOptions.systemStatus="System blur + stretch: projected buffer + owned blur child submitted; Android may suppress blur; not optical proof"
   true
  } catch(e:Exception){
   removeLayer();failed=true
   GlassRenderOptions.systemStatus="System blur + stretch unavailable: ${e.javaClass.simpleName}; using projection without blur"
   RecoveryLog.add(GlassRenderOptions.systemStatus)
   false
  }
 }
 private fun removeLayer(){
  layer?.let { child ->
   runCatching{SurfaceControl.Transaction().use{it.setVisibility(child,false).reparent(child,null).apply()}}
   child.release()
  }
  layer=null;radius=-1;width=0;height=0
 }
 fun clear(){removeLayer();parent=null;failed=false}
}
