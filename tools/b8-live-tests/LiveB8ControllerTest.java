package org.duofold.live.experimental.b8;
import java.io.File;
import java.lang.reflect.Method;
import android.os.SystemClock;
public final class LiveB8ControllerTest {
 static Method tick;static File root;static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static LiveB8Controller fresh(){LiveB8Power.reset();LiveB8Task.moves=LiveB8Task.retargets=0;LiveB8Task.selected="test.app";LiveB8Brightness.begun=0;LiveB8Brightness.failPrepared=false;LiveB8Brightness.onBegin=null;LiveB8Bridge.arms=0;LiveB8Bridge.allowRemoval=true;LiveB8Bridge.allowCommit=true;LiveB8Bridge.failArm=false;android.view.IWindowManager.locked=false;SystemClock.now+=10000;return new LiveB8Controller(root,false);}
 static void step(LiveB8Controller c,float a)throws Exception{SystemClock.now+=25;c.update(true,a,SystemClock.now,172,85);tick.invoke(c);}
 static void run(LiveB8Controller c,float a,int ms)throws Exception{for(int n=0;n<ms;n+=25)step(c,a);}
 static void stop(LiveB8Controller c)throws Exception{c.close();tick.invoke(c);check(!c.ownsRoute(),"cleanup releases ownership");}
 public static void main(String[] args)throws Exception{
  root=new File(args[0]);tick=LiveB8Controller.class.getDeclaredMethod("tick");tick.setAccessible(true);
  LiveB8Controller c=fresh();run(c,179,200);check(!c.ownsRoute()&&LiveB8Power.requests==0,"idle endpoint never starts timed test");
  run(c,140,150);check(c.ownsRoute()&&LiveB8Task.moves==0,"prewarm from live motion without moving early");
  run(c,80,1700);check(LiveB8Task.moves==1&&LiveB8Power.panel==2&&c.report().contains("HANDOFF_COMPLETE primary=2"),"hinge triggers cover move and verified primary");
  run(c,83,700);check(LiveB8Task.moves==1,"four degree hysteresis holds cover");
  run(c,100,3000);check(LiveB8Task.moves==2&&LiveB8Power.panel==1&&c.report().contains("HANDOFF_COMPLETE primary=1"),"reverse hinge moves back without manual countdown");
  run(c,180,2500);check(!c.ownsRoute()&&c.report().contains("NATIVE_COMPLETE"),"endpoint protected native release completes");stop(c);
  c=fresh();LiveB8Bridge.allowCommit=false;run(c,140,100);run(c,80,1200);check(LiveB8Power.requests==1&&LiveB8Brightness.begun==0,"never promote before shield commit");run(c,80,6000);check(!c.ownsRoute()&&LiveB8Power.requests==1,"deadline abort does not retry mid-fold");stop(c);
  c=fresh();LiveB8Bridge.failArm=true;run(c,140,100);run(c,80,1500);check(LiveB8Bridge.arms==1&&!c.ownsRoute(),"arm rejection aborts once rather than repeatedly creating mirrors");stop(c);
  c=fresh();run(c,140,100);SystemClock.now+=2100;tick.invoke(c);check(!c.ownsRoute(),"stale angle releases session without polling renewal");stop(c);
  c=fresh();run(c,140,100);LiveB8Power.cleanupFailures=1;c.update(false,140,SystemClock.now,172,85);tick.invoke(c);check(c.ownsRoute(),"failed cleanup retains ownership against legacy controller");tick.invoke(c);check(!c.ownsRoute(),"cleanup retried and ownership released");stop(c);
  c=fresh();run(c,140,100);android.view.IWindowManager.locked=true;step(c,120);check(!c.ownsRoute(),"lock aborts owned session");stop(c);
  c=fresh();LiveB8Task.selected=null;run(c,140,100);run(c,80,1800);check(LiveB8Task.moves==0&&LiveB8Bridge.arms==0&&!c.ownsRoute(),"unselected app never freezes wallpaper or promotes");stop(c);
  c=fresh();run(c,140,100);LiveB8Bridge.allowCommit=false;run(c,80,500);run(c,120,100);LiveB8Bridge.allowCommit=true;
  run(c,120,5000);check(LiveB8Task.moves==2&&LiveB8Power.panel==1,"rapid reversal serializes pending handoff then follows latest hinge");stop(c);
  c=fresh();run(c,140,100);final LiveB8Controller disabling=c;LiveB8Brightness.onBegin=()->disabling.update(false,80,SystemClock.now,172,85);
  run(c,80,450);check(LiveB8Power.requests==1&&!c.ownsRoute(),"disable during brightness sampling prevents primary request");stop(c);
  c=fresh();LiveB8Brightness.failPrepared=true;run(c,80,1000);check(LiveB8Bridge.arms==0&&!c.ownsRoute(),"brightness failure occurs before any screenshot/shield");stop(c);
  c=fresh();run(c,140,100);run(c,80,1000);int before=LiveB8Bridge.arms;run(c,100,500);run(c,180,1500);check(LiveB8Bridge.arms==before+1&&c.report().contains("ENDPOINT_REUSE")&&!c.ownsRoute(),"endpoint reuses in-flight shield without a second capture");stop(c);
  c=fresh();run(c,140,100);LiveB8Bridge.allowRemoval=false;run(c,80,1000);before=LiveB8Bridge.arms;run(c,120,1000);check(LiveB8Bridge.arms==before&&c.ownsRoute(),"no second shield before removal commit");LiveB8Bridge.last.closed=true;LiveB8Bridge.allowRemoval=true;step(c,120);run(c,120,1200);check(LiveB8Power.panel==1,"reversal resumes after confirmed removal");stop(c);
  c=fresh();run(c,140,100);c.thermal(3);step(c,120);check(!c.ownsRoute()&&c.report().contains("Thermal pause"),"severe thermal state cleans up");stop(c);
  check(LiveB8Policy.target(81,1,85,172)==2&&LiveB8Policy.target(82,1,85,172)==1,"closing threshold");
  check(LiveB8Policy.target(85,2,85,172)==1&&LiveB8Policy.target(84,2,85,172)==2,"opening threshold");
  check(!LiveB8Policy.usable(true,10000,7000,9999,90),"missing app heartbeat expires even with a fresh sample");
  check(!LiveB8Policy.usable(true,10000,9999,9999,Float.NaN),"NaN never routes");
  System.out.println("LiveB8ControllerTest: "+checks+" lifecycle/readiness checks passed (fake platform, not handset proof)");
 }
}
