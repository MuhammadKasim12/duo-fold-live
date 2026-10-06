package org.duofold.live
import kotlin.math.sqrt
/** Full-panel black fade: quick onset at either endpoint, opaque through handoff. */
internal object PlainFadeCurve {
 // Retain the original spatial wipe and direction, but advance its onset.
 // Draw over the unchanged full-panel dimming so handoff remains opaque.
 fun wipeAlpha(dimming:Float,edge:Float,intensity:Float=1f):Float =
  DuoShadeCurve.alpha(sqrt(dimming.coerceIn(0f,1f)),edge,intensity)
 fun alpha(angle:Float,inner:Boolean,open:Float,handoff:Float):Float {
  if(!angle.isFinite())return 0f
  val end=FoldThreshold.sanitize(open)
  val split=handoff.coerceIn(1f,end-1f)
  val progress=if(inner)(end-angle)/(end-split) else angle/split
  return sqrt(progress.coerceIn(0f,1f))
 }
}
