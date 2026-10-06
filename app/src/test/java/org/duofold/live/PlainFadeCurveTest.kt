package org.duofold.live
import org.junit.Assert.*
import org.junit.Test
class PlainFadeCurveTest {
 @Test fun directionalWipeStartsEarlyAndPreservesDimming(){
  for(inner in listOf(false,true)){
   val angle=if(inner)167f else 5f
   val dim=PlainFadeCurve.alpha(angle,inner,172f,85f)
   val leading=PlainFadeCurve.wipeAlpha(dim,1f)
   val trailing=PlainFadeCurve.wipeAlpha(dim,0f)
   assertTrue(leading>.8f)
   assertEquals(0f,trailing,0f)
   assertTrue(leading>DuoShadeCurve.alpha(DuoShadeCurve.progress(angle,inner,172f),1f))
   assertTrue(dim+(1f-dim)*leading>dim)
  }
  assertEquals(0f,PlainFadeCurve.wipeAlpha(0f,1f),0f)
 }
 @Test fun endpointsAndEarlyOnset(){
  assertEquals(0f,PlainFadeCurve.alpha(0f,false,172f,85f),0f)
  assertEquals(0f,PlainFadeCurve.alpha(172f,true,172f,85f),0f)
  assertTrue(PlainFadeCurve.alpha(5f,false,172f,85f)>.2f)
  assertTrue(PlainFadeCurve.alpha(167f,true,172f,85f)>.2f)
  for(a in 0..85)assertEquals(1f,PlainFadeCurve.alpha(a.toFloat(),true,172f,85f),0f)
  assertEquals(1f,PlainFadeCurve.alpha(85f,false,172f,85f),0f)
 }
}
