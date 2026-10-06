package org.duofold.live
import kotlin.math.sqrt
/** Full-panel black fade: quick onset at either endpoint, opaque through handoff. */
internal object PlainFadeCurve {
 fun alpha(angle:Float,inner:Boolean,open:Float,handoff:Float):Float {
  if(!angle.isFinite())return 0f
  val end=FoldThreshold.sanitize(open)
  val split=handoff.coerceIn(1f,end-1f)
  val progress=if(inner)(end-angle)/(end-split) else angle/split
  return sqrt(progress.coerceIn(0f,1f))
 }
}
