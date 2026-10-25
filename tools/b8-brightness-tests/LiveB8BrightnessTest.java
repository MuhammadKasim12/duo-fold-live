package org.duofold.live.experimental.b8;
import java.io.File;import java.lang.reflect.Field;import java.util.*;import android.view.SurfaceControl;
final class BrightnessRefresh {BrightnessRefresh(File d){}void refresh(Set<Long> ids){}long clearedAt(long id){return 0;}void close(){}}
public class LiveB8BrightnessTest {
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  LiveB8Brightness hold=new LiveB8Brightness(new File(args[0]));boolean rejected=false;
  try{hold.requirePrepared();}catch(Exception expected){rejected=true;}check(rejected,"missing samples rejected before shielding");
  Map<Long,BrightnessSamples.Sample> samples=(Map<Long,BrightnessSamples.Sample>)field(hold,"prepared");
  for(long id:new long[]{1,2}){BrightnessSamples.Sample s=new BrightnessSamples.Sample();s.id=id;s.ms=9999;s.value=.5f;s.nits=s.sdr=100;s.ratio=1;samples.put(id,s);}
  hold.requirePrepared();hold.begin("hinge");check(SurfaceControl.writes>=2,"first brightness writes accepted without launching logcat");Thread worker=(Thread)field(hold,"worker");
  hold.begin("endpoint");check(worker==field(hold,"worker"),"endpoint extends same writer");hold.close();worker.join(300);check(!worker.isAlive(),"stop terminates bounded writer");
  System.out.println("LiveB8BrightnessTest: missing-sample rejection, cached-only begin, one writer and stop passed");
 }
}
