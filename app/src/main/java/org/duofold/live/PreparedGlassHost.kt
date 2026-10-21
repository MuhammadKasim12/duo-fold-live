package org.duofold.live

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.ViewGroup

/** Two persistent buffers; standby pixels are never destination-readiness evidence. */
internal class PreparedGlassHost(context:Context):ViewGroup(context) {
 private val panels= arrayOf(FrostSurface(context),FrostSurface(context))
 private val sizes= arrayOf(Point(),Point())
 private var selected=-1
 private var warmAt=0L
 private var warmStamp=-1L
 init {
  clipChildren=true
  panels.forEach { it.preparedStandby=true;it.alpha=0f;addView(it,LayoutParams(1,1)) }
 }
 fun configure(frame:GlassFrame?,amount:Float,intensity:Float,inner:Boolean,rotation:Int,angle:Float){
  val target=if(inner)1 else 0
  val dm=context.getSystemService(DisplayManager::class.java)
  // Query physical mode sizes, not stale bounds inherited by D0 during a remap.
  dm.displays.forEach { d ->
   val mode=d.mode
   val slot=if(minOf(mode.physicalWidth,mode.physicalHeight).toFloat()/maxOf(mode.physicalWidth,mode.physicalHeight)>.7f)1 else 0
   val geometry=LivePanelGeometry.resolve(mode.physicalWidth,mode.physicalHeight,rotation,0,0)
   if(sizes[slot].x!=geometry[0] || sizes[slot].y!=geometry[1]){
    sizes[slot].set(geometry[0],geometry[1]);requestLayout()
    RecoveryLog.add("Prepared glass geometry ${if(slot==1) "inner" else "cover"}: ${geometry[0]}x${geometry[1]}")
   }
  }
  if(selected!=target){
   panels.forEachIndexed { i,p -> p.preparedStandby=i!=target }
   selected=target
   HandoffStartupTrace.event("PREPARED_RENDERER_SELECT","inner=$inner; geometry=${sizes[target].x}x${sizes[target].y}; surfaceValid=${panels[target].holder.surface.isValid}; not content readiness",false)
  }
  panels[target].configure(frame,amount,intensity,inner,rotation,angle=angle)
  val other=1-target
  val now=SystemClock.elapsedRealtime()
  if(frame!=null && sizes[other].x>0 && frame.stamp!=warmStamp && now-warmAt>=180 && LiveAngles.fresh() && LiveAngles.effectAllowed){
   warmAt=now;warmStamp=frame.stamp
   // Exercise the destination shader with bounded, hidden outgoing content. A switch
   // reconfigures this surface with live validation before revealing new draws.
   panels[other].configure(frame,DuoShadeCurve.progress(angle,other==1,context.getSharedPreferences("standalone",0).getFloat("open_threshold",172f)),intensity,other==1,rotation,frozen=true,angle=angle)
  }
 }
 override fun onMeasure(w:Int,h:Int){
  setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h))
  panels.forEachIndexed { i,p ->
   val s=sizes[i]
   p.measure(MeasureSpec.makeMeasureSpec(maxOf(1,s.x),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(maxOf(1,s.y),MeasureSpec.EXACTLY))
  }
 }
 override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int){
  panels.forEach { it.layout(0,0,it.measuredWidth,it.measuredHeight) }
 }
}
