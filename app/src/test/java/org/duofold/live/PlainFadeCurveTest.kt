package org.duofold.live
import org.junit.Assert.*
import org.junit.Test
class PlainFadeCurveTest {
 @Test fun endpointsAndEarlyOnset(){
  assertEquals(0f,PlainFadeCurve.alpha(0f,false,172f,85f),0f)
  assertEquals(0f,PlainFadeCurve.alpha(172f,true,172f,85f),0f)
  assertTrue(PlainFadeCurve.alpha(5f,false,172f,85f)>.2f)
  assertTrue(PlainFadeCurve.alpha(167f,true,172f,85f)>.2f)
  for(a in 0..85)assertEquals(1f,PlainFadeCurve.alpha(a.toFloat(),true,172f,85f),0f)
  assertEquals(1f,PlainFadeCurve.alpha(85f,false,172f,85f),0f)
 }
}
