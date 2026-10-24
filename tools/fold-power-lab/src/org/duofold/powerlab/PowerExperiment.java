package org.duofold.powerlab;
import android.os.*;
import android.content.Context;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** No untyped Binder transactions or persistent settings. Token leases only. */
final class PowerExperiment {
 private final File dir;
 private final ScheduledExecutorService timers=Executors.newScheduledThreadPool(3);
 private final IBinder[] tokens={new Binder(),new Binder()};
 private final boolean[] pending={false,false};
 private volatile boolean stopped,cleaned=true,override;
 private Object display,stateManager; private Method setter,stateCancel; private boolean stateOwned; private PowerManager.WakeLock wake;
 private long start;
 PowerExperiment(File dir){this.dir=dir;}
 static Object service(String name,String api)throws Exception{
  IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,name);
  return Class.forName(api+"$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
 }
 private void log(String s){try(FileWriter f=new FileWriter(new File(dir,"power-experiment.txt"),true)){f.write(SystemClock.elapsedRealtime()+" "+s+"\n");}catch(IOException ignored){}}
 void start(int mode){
  start=SystemClock.elapsedRealtime();override=mode>=6;
  log("START mode="+mode+" UID="+android.os.Process.myUid()+"; accepted calls are not effective panel power or photon proof");
  // Cleanup and final process-death watchdog use separate workers from sampling.
  timers.schedule(this::close,10,TimeUnit.SECONDS);
  timers.schedule(()->{if(!cleaned){log("CLEANUP FAILED/BLOCKED: terminate shell worker to release Binder tokens; reconnect required");android.os.Process.killProcess(android.os.Process.myPid());}},15,TimeUnit.SECONDS);
  timers.schedule(()->timers.shutdown(),16,TimeUnit.SECONDS);
  new Thread(this::sample,"power-samples").start();
  if(!override)return;
  try{
   Object states=service("device_state","android.hardware.devicestate.IDeviceStateManager");
   Object info=Class.forName("android.hardware.devicestate.IDeviceStateManager").getMethod("getDeviceStateInfo").invoke(states);
   Object current=info.getClass().getField("currentState").get(info),base=info.getClass().getField("baseState").get(info);
   if(!current.getClass().getMethod("getIdentifier").invoke(current).equals(base.getClass().getMethod("getIdentifier").invoke(base)))throw new IllegalStateException("Another device-state override is active. Disable Duo and retry.");
   if(mode>=10){
    Class<?> managerClass=Class.forName("android.hardware.devicestate.DeviceStateManager");
    Object manager=managerClass.getConstructor().newInstance();int requested=-1;
    String target=(mode==10||mode==12)?"CONCURRENT_INNER_DEFAULT":"CONCURRENT_OUTER_DEFAULT";
    for(Object state:(List<?>)managerClass.getMethod("getSupportedDeviceStates").invoke(manager))
     if(target.equals(state.getClass().getMethod("getName").invoke(state)))requested=(int)state.getClass().getMethod("getIdentifier").invoke(state);
    if(requested<0)throw new IllegalStateException("Unsupported named state "+target);
    Class<?> request=Class.forName("android.hardware.devicestate.DeviceStateRequest"),callback=Class.forName("android.hardware.devicestate.DeviceStateRequest$Callback");
    Object builder=request.getMethod("newBuilder",int.class).invoke(null,requested),built=builder.getClass().getMethod("build").invoke(builder);
    Object cb=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(proxy,m,args)->{
     if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);
     if(m.getName().equals("equals"))return proxy==args[0];
     if(m.getName().equals("toString"))return "FoldPowerLabCallback";
     log("DEVICE_STATE callback "+m.getName());return null;
    });
    synchronized(this){if(stopped)return;stateManager=manager;stateCancel=managerClass.getMethod("cancelStateRequest");stateOwned=true;cleaned=false;
     managerClass.getMethod("requestState",request,Executor.class,callback).invoke(manager,built,(Executor)Runnable::run,cb);
    }
    log("DEVICE_STATE accepted "+target+" id="+requested+"; no reassertion after Android cancellation");
   }
   if((mode>=6&&mode<=8)||mode>=12){
    display=service("display","android.hardware.display.IDisplayManager");
    setter=Class.forName("android.hardware.display.IDisplayManager").getMethod("setDisplayStateOverrideWithDisplayId",IBinder.class,int.class,int.class,int.class);
    for(int id=0;id<2;id++)if(mode==8 || mode>=12 || id==mode-6){
     synchronized(this){if(stopped)return;cleaned=false;pending[id]=true;
     setter.invoke(display,tokens[id],2,id,10000);}
     log("ON accepted D"+id+"; logical display id can remap physical panel; explicit cleanup at 10 seconds");
    }
   }else if(mode==9){
    Class<?> at=Class.forName("android.app.ActivityThread");
    Object thread=at.getMethod("currentActivityThread").invoke(null);
    if(thread==null)thread=at.getMethod("systemMain").invoke(null);
    Context context=(Context)at.getMethod("getSystemContext").invoke(thread);
    PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
    synchronized(this){if(stopped)return;wake=pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"FoldPowerLab:10s");cleaned=false;wake.acquire(10000);}
    log("SCREEN_BRIGHT_WAKE_LOCK accepted; does not prove per-panel ON");
   }
  }catch(Throwable e){log("OVERRIDE REJECTED/UNAVAILABLE "+root(e));close();}
 }
 void close(){
  stopped=true;
  synchronized(this){
   try{
    if(wake!=null && wake.isHeld())wake.release();
    for(int id=0;id<2;id++)if(pending[id]){
     try{setter.invoke(display,tokens[id],0,id,-1);pending[id]=false;log("RELEASE accepted D"+id);}
     catch(Throwable e){log("RELEASE pending D"+id+" "+root(e));}
    }
    if(stateOwned){try{stateCancel.invoke(stateManager);stateOwned=false;log("DEVICE_STATE released");}catch(Throwable e){log("DEVICE_STATE release pending "+root(e));}}
    cleaned=!stateOwned&&!pending[0]&&!pending[1]&&(wake==null||!wake.isHeld());
    log("STOP elapsed="+(SystemClock.elapsedRealtime()-start)+"ms cleanupComplete="+cleaned);
    if(!cleaned&&!timers.isShutdown())timers.schedule(this::close,250,TimeUnit.MILLISECONDS);
   }catch(Throwable e){log("CLEANUP error "+root(e));}
  }
  // Keep the 15-second death watchdog alive even after Stop.
 }
 private void sample(){
  long previous=SystemClock.elapsedRealtime(),maxGap=0;int count=0;
  try(BufferedWriter out=new BufferedWriter(new FileWriter(new File(dir,"power-samples.txt")))){
   Object dm=service("display","android.hardware.display.IDisplayManager");Class<?> dc=Class.forName("android.hardware.display.IDisplayManager");
   Method ids=dc.getMethod("getDisplayIds",boolean.class),di=dc.getMethod("getDisplayInfo",int.class);
   Object pm=service("power","android.os.IPowerManager");Method interactive=Class.forName("android.os.IPowerManager").getMethod("isInteractive");
   Object sm=service("device_state","android.hardware.devicestate.IDeviceStateManager");Method si=Class.forName("android.hardware.devicestate.IDeviceStateManager").getMethod("getDeviceStateInfo");
   while(!stopped&&SystemClock.elapsedRealtime()-start<10000){
    long begin=SystemClock.elapsedRealtime(),gap=begin-previous;previous=begin;maxGap=Math.max(maxGap,gap);
    StringBuilder line=new StringBuilder("t="+begin+" gapMs="+gap+" interactive="+interactive.invoke(pm));
    Object info=si.invoke(sm);
    for(String field:new String[]{"baseState","currentState"})try{line.append(" ").append(field).append("=").append(info.getClass().getField(field).get(info));}catch(Throwable e){line.append(" ").append(field).append(" unavailable");}
    for(int id:(int[])ids.invoke(dm,false)){
     Object d=di.invoke(dm,id);line.append(" D").append(id);
     if(d==null){line.append(" missing");continue;}
     for(String field:new String[]{"uniqueId","state","committedState","logicalWidth","logicalHeight","rotation","layerStack"})try{line.append(" ").append(field).append("=").append(d.getClass().getField(field).get(d));}catch(Throwable ignored){}
    }
    line.append(" queryMs=").append(SystemClock.elapsedRealtime()-begin);out.write(line.toString());out.newLine();out.flush();count++;
    Thread.sleep(Math.max(1,25-(SystemClock.elapsedRealtime()-begin)));
   }
   out.write("END samples="+count+" maximumGapMs="+maxGap+"; sampled software states, not optical blackout\n");
  }catch(Throwable e){log("SAMPLER unavailable/error "+root(e));}
 }
 static String inventory(){
  StringBuilder b=new StringBuilder("Discovery only. Methods listed here are NOT automatically invoked. Permission and effect unverified.\n");
  for(String name:new String[]{"android.hardware.display.IDisplayManager","android.os.IPowerManager","android.hardware.devicestate.IDeviceStateManager","android.view.IWindowManager","android.view.SurfaceControl","android.hardware.SensorManager"}){
   b.append("\nCLASS ").append(name).append('\n');
   try{for(Method m:Class.forName(name).getDeclaredMethods())b.append(m).append('\n');}catch(Throwable e){b.append(root(e)).append('\n');}
  }return b.toString();
 }
 private static Throwable root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e;}
}
