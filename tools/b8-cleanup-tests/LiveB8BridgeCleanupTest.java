package org.duofold.live.experimental.b8;
import java.io.File;import java.lang.reflect.Field;import java.util.List;import android.view.SurfaceControl;
final class PowerExperiment {static Object service(String n,String c){return null;}}
public class LiveB8BridgeCleanupTest {
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static List<Object> list(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return (List<Object>)f.get(o);}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  LiveB8Bridge bridge=new LiveB8Bridge(new File(args[0]),true);set(bridge,"sc",SurfaceControl.class);set(bridge,"tc",SurfaceControl.Transaction.class);
  SurfaceControl layer=new SurfaceControl(),container=new SurfaceControl();list(bridge,"surfaces").add(layer);list(bridge,"surfaces").add(container);list(bridge,"bufferLayers").add(layer);
  bridge.close();check(!bridge.isClosed(),"must wait for removal commit");check(!layer.released,"keep handle until commit");check(SurfaceControl.Transaction.removals==2&&SurfaceControl.Transaction.clears==1,"remove both and detach frozen buffer");
  bridge.close();check(SurfaceControl.Transaction.removals==2,"pending removal is not submitted repeatedly");
  SurfaceControl.Transaction.commit.run();check(bridge.isClosed()&&layer.released&&container.released,"commit releases handles");check(list(bridge,"surfaces").isEmpty()&&list(bridge,"bufferLayers").isEmpty(),"clear strong references");check(bridge.resources().contains("removeCommitted=true"),"report confirmation");
  System.out.println("LiveB8BridgeCleanupTest: delayed commit, buffer detach, idempotency, handle release and reference cleanup passed");
 }
}
