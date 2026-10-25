package org.duofold.live.experimental.b8;

import android.os.SystemClock;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Hinge-driven B8, serialized independently of the angle-reader Binder loop. */
public final class LiveB8Controller {
 private static final class Input {
  final boolean enabled;final float angle,open,handoff;final long sample,heartbeat;
  Input(boolean e,float a,long s,float o,float h){enabled=e;angle=a;sample=s;open=o;handoff=h;heartbeat=SystemClock.elapsedRealtime();}
 }
 private volatile Input input=new Input(false,Float.NaN,0,172,85);
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"duo-live-b8");t.setDaemon(true);return t;});
 private volatile boolean active,closing;
 private volatile long sceneSession,sceneAckSession,sceneCommitAt,sceneAckAt;
 private volatile boolean scenePrepared,capturePaused,capturePauseRequested;
 public boolean capturePauseRequested(){return capturePauseRequested;}
 private volatile String sceneSource="",scenePrimary="";
 private volatile int sceneTask=-1;
 public long sceneSession(){return sceneSession;}
 public int sceneTask(){return sceneTask;}
 public String sceneSource(){return sceneSource;}
 public void presentation(long session,boolean prepared,String physical,long commitAt,boolean paused){capturePaused=paused;scenePrepared=prepared;scenePrimary=physical==null?"":physical;sceneCommitAt=commitAt;sceneAckAt=SystemClock.elapsedRealtime();sceneAckSession=session;}
 private boolean presentationReady(long now){return sceneSession>0&&sceneAckSession==sceneSession&&scenePrepared&&now-sceneAckAt<=250;}
 private boolean presentationAlive(long now){return sceneSession>0&&sceneAckSession==sceneSession&&now-sceneAckAt<=750;}
 private boolean primaryPresented(long now){return presentationAlive(now)&&expected.equals(scenePrimary)&&sceneCommitAt>=holdStarted&&sceneCommitAt<=now&&now-sceneCommitAt<=250;}

 private volatile String status="Live B8 idle";
 private final ArrayDeque<String> history=new ArrayDeque<>();
 private LiveB8Power power;private LiveB8Brightness brightness;private LiveB8Bridge shield;private LiveB8Task task;
 private File dir;private final LiveB8Evidence evidence;private volatile int thermal;private long shieldStarted;private volatile String lastResources="No shield allocated";
 private final File diagnosticRoot;
 private int phase,current,target;
 private long started,renewed,deadline,stableSince=-1,verified=-1,preparedAt,holdStarted;
 private String candidate="",expected="",inner="",cover="";
 private boolean failed,nativeReturn;private volatile boolean cleaning;
 public LiveB8Controller(){this(new File("/data/local/tmp"),true);}
 LiveB8Controller(File root,boolean scheduled){diagnosticRoot=root;evidence=new LiveB8Evidence(root,scheduled);if(scheduled)worker.scheduleWithFixedDelay(this::tick,0,25,TimeUnit.MILLISECONDS);}
 public void update(boolean enabled,float angle,long sample,float open,float handoff){input=new Input(enabled,angle,sample,open,handoff);}
 public void thermal(int value){thermal=value;}
 public boolean ownsRoute(){return active||cleaning;}
 public boolean finished(){return worker.isShutdown();}
 public String status(){return status;}
 public synchronized String report(){return status+"\nHinge-driven; commits and mapping checks are not visible-pixel proof.\n"+String.join("\n",history)+"\nResources: "+lastResources+"\n"+evidence.report();}
 private synchronized void log(String text){status="Live B8: "+text;history.addLast(SystemClock.elapsedRealtime()+" "+text);while(history.size()>80)history.removeFirst();evidence.record(dir,text);}
 private void unlocked()throws Exception{
  Object wm=PowerExperiment.service("window","android.view.IWindowManager"),pm=PowerExperiment.service("power","android.os.IPowerManager");
  if((boolean)Class.forName("android.view.IWindowManager").getMethod("isKeyguardLocked").invoke(wm)||!(boolean)Class.forName("android.os.IPowerManager").getMethod("isInteractive").invoke(pm))throw new IllegalStateException("Locked or noninteractive");
 }
 private void createFolder()throws Exception{
  File base=diagnosticRoot;String prefix="duo-live-b8-"+android.os.Process.myUid()+"-";
  File[] old=base.listFiles(f->f.isDirectory()&&f.getName().startsWith(prefix));
  if(old!=null){Arrays.sort(old,Comparator.comparingLong(File::lastModified));for(int n=0;n<old.length-5;n++)delete(old[n]);}
  dir=new File(base,prefix+SystemClock.elapsedRealtime());if(!dir.mkdir())throw new IOException("B8 diagnostic folder unavailable");android.system.Os.chmod(dir.getPath(),0700);
 }
 private static void delete(File f){File[] children=f.listFiles();if(children!=null)for(File c:children)if(c.isFile())c.delete();f.delete();}
 private void begin()throws Exception{
  active=true;createFolder();power=new LiveB8Power();current=power.primary();started=SystemClock.elapsedRealtime();
  if(power.state(false)!=power.state(true))throw new IllegalStateException("Another device-state owner is active");
  power.request(current);power.on();renewed=SystemClock.elapsedRealtime();phase=1;deadline=renewed+6000;
  log("PREWARM primary="+current+"; hinge starts session; both ON requested");
 }
 private void identities()throws Exception{
  Object a=power.info(0),b=power.info(1);String x=LiveB8Power.physical(a),y=LiveB8Power.physical(b);
  long aa=(long)LiveB8Power.number(a,"logicalWidth")*LiveB8Power.number(a,"logicalHeight"),bb=(long)LiveB8Power.number(b,"logicalWidth")*LiveB8Power.number(b,"logicalHeight");
  if(aa==bb||x.equals(y))throw new IllegalStateException("Ambiguous physical panels");inner=aa>bb?x:y;cover=aa>bb?y:x;
 }
 private void prepareBrightness()throws Exception{if(brightness!=null)brightness.close();brightness=new LiveB8Brightness(dir);brightness.prepare();brightness.requirePrepared();preparedAt=SystemClock.elapsedRealtime();}
 private String primaryGeometry()throws Exception{
  Object d=power.info(0);if(!expected.equals(LiveB8Power.physical(d))||LiveB8Power.number(d,"state")!=2||LiveB8Power.number(d,"committedState")!=2)throw new IllegalStateException("Destination primary not ON/committed");
  if(task!=null)return task.verifyPrimary(expected);
  return expected+":"+LiveB8Power.number(d,"logicalWidth")+"x"+LiveB8Power.number(d,"logicalHeight")+":"+LiveB8Power.number(d,"rotation");
 }
 private boolean settle(String value,long now){if(!value.equals(candidate)){candidate=value;stableSince=now;verified=-1;}return LiveB8Policy.stable(now,stableSince);}
 private void resetGeometry(){candidate="";stableSince=-1;verified=-1;}
 private void startTransfer(int wanted,boolean release)throws Exception{
  target=wanted;nativeReturn=release;expected=target==1?inner:cover;task=null;resetGeometry();deadline=SystemClock.elapsedRealtime()+LiveB8Policy.TRANSITION_MS;
  if(SystemClock.elapsedRealtime()-preparedAt>30000)prepareBrightness();
  if(!release){String pkg=LiveB8Task.focusedPackage();
   if(pkg!=null){task=new LiveB8Task(dir,pkg);task.selectOnly();if(task.taskId()!=sceneTask)throw new IllegalStateException("Focused app changed during presentation");log("TRANSFER_PREPARE target="+target+" package="+pkg+"; presentation precedes task move");capturePauseRequested=true;phase=8;return;}
   throw new IllegalStateException("No routable fullscreen app; refusing to freeze destination wallpaper");
  }else{
   String pkg=LiveB8Task.focusedPackage();if(pkg==null)throw new IllegalStateException("No fullscreen app for endpoint shield");task=new LiveB8Task(dir,pkg);task.selectOnly();if(task.taskId()!=sceneTask)throw new IllegalStateException("Focused app changed before endpoint");
   log("ENDPOINT_RELEASE target="+target+"; native mapping will be verified");
  }
  capturePauseRequested=true;phase=8;
 }
 private void arm()throws Exception{brightness.requirePrepared();shieldStarted=SystemClock.elapsedRealtime();shield=new LiveB8Bridge(dir,true);shield.arm();lastResources=shield.resources();phase=4;log("SHIELD_SUBMITTED target="+target+" nativeRelease="+nativeReturn);}
 private void tick(){
  try{
   long now=SystemClock.elapsedRealtime();Input i=input;
   if(!LiveB8Policy.usable(i.enabled,now,i.heartbeat,i.sample,i.angle)||closing){
    if(active||power!=null){log("STOP enabled="+i.enabled+" thermal="+thermal+" sampleAge="+(now-i.sample));cleanup();}
    if(!i.enabled)failed=false;
    if(closing&&power==null)worker.shutdown();return;
   }
   if(cleaning){cleanup();return;}
   if(failed){if(LiveB8Policy.endpoint(i.angle,i.open))failed=false;else return;}
   if(!active){if(LiveB8Policy.endpoint(i.angle,i.open))return;unlocked();if(thermal>=3){log("THERMAL_PAUSE status="+thermal);failed=true;return;}begin();return;}
   unlocked();
   if(thermal>=3)throw new IllegalStateException("Thermal pause status="+thermal);
   // An Android cancellation at full close is expected. Do not reassert it.
   if(power.canceled){log("ANDROID_CANCELED; no automatic retry");cleanup();failed=!LiveB8Policy.endpoint(i.angle,i.open);return;}
   if(now-renewed>=500){power.on();renewed=now;}
   if(phase!=2&&now>=deadline)throw new IllegalStateException("Transition readiness timeout phase="+phase);
   if(now-started>=30000)throw new IllegalStateException("Presentation session reached 30 second bound");
   if(phase==1){if(!power.ready())return;identities();
    String pkg=LiveB8Task.focusedPackage();if(pkg==null)throw new IllegalStateException("No fullscreen app for presentation");
    task=new LiveB8Task(dir,pkg);task.selectOnly();sceneTask=task.taskId();sceneSource=current==1?inner:cover;sceneSession=started;
    phase=7;deadline=now+6000;log("PRESENTATION_PREPARE task="+sceneTask+" source="+sceneSource+"; waiting for both committed app composites");return;}
   if(phase==7){if(!presentationReady(now))return;prepareBrightness();phase=2;log("PRESENTATION_READY both panels; task has not moved");return;}
   if(!presentationAlive(now))throw new IllegalStateException("Presentation lease lost");
   if(phase==8){if(presentationReady(now)&&capturePaused)arm();return;}
   if(phase==2){
    if(!presentationReady(now))return;
    int wanted=LiveB8Policy.target(i.angle,current,i.handoff,i.open);
    if(LiveB8Policy.endpoint(i.angle,i.open)){
     int base=power.state(true),nativePanel=base==3?1:base==0?2:0;
     if(nativePanel==wanted){startTransfer(wanted,true);return;}
    }
    if(wanted!=current)startTransfer(wanted,false);
    return;
   }
   if(phase==3){
    if(shield==null||shield.isClosed())throw new IllegalStateException("Animated shield expired before promotion");
    shield.followPhysicalStacks();
    String g;try{g=task.destinationGeometry();}catch(Exception pending){resetGeometry();return;}
    if(settle(g,now)){promote();}return;
   }
   if(phase==6)return;
   if(shield==null||shield.isClosed())throw new IllegalStateException("Shield no longer active");
   shield.followPhysicalStacks();lastResources=shield.resources();
   if(phase==4){
    if(!shield.ready()||!presentationReady(now))return;
    // Keep the proven WMS mirror/brightness route, but uncover its live animated
    // window subtree once both opaque composites are committed. No frozen wall.
    if(!shield.animatedReady()){shield.animate();return;}
    if(!nativeReturn){task.moveSelected();phase=3;resetGeometry();log("HINGE_MOVE target="+target+"; animated shield already committed");return;}
    promote();return;
   }
   if(phase==5){
    int base=power.state(true);
    if(!nativeReturn&&LiveB8Policy.endpoint(i.angle,i.open)&&((target==1&&i.angle>=i.open&&base==3)||(target==2&&i.angle<=0&&base==0))&&expected.equals(LiveB8Power.physical(power.info(0)))&&now-shieldStarted<3000){
     brightness.begin("live-endpoint-reuse");power.nativeRelease();nativeReturn=true;resetGeometry();log("ENDPOINT_REUSE animated shield; no new captures");return;
    }
    try{
     if(nativeReturn&&power.state(false)!=power.state(true))throw new IllegalStateException("Native state pending");
     if(settle(primaryGeometry(),now)&&verified<0){verified=now;log("PRIMARY_VERIFIED target="+target+"; geometry stable 300ms");if(task!=null)task.focus();}
     if(verified>=0&&primaryPresented(now)){shield.reveal();phase=6;log("REVEAL target="+target+"; matching animated primary commit; outgoing presentation retained");}
    }catch(Exception pending){resetGeometry();}
    return;
   }
  }catch(Throwable e){while(e.getCause()!=null)e=e.getCause();log("ABORT "+e);failed=true;cleanup();}
  finally{
   if(phase==6&&shield!=null&&shield.isClosed()){
    lastResources=shield.resources();log("SHIELD_REMOVED "+lastResources);current=target;shield=null;task=null;capturePauseRequested=false;
    if(nativeReturn){log("NATIVE_COMPLETE primary="+current);cleanup();}
    else{phase=2;log("HANDOFF_COMPLETE primary="+current+"; outgoing presentation retained until endpoint");}
   }
  }
 }
 private void promote()throws Exception{
    power.on();brightness.begin(nativeReturn?"live-endpoint":"live-hinge");holdStarted=SystemClock.elapsedRealtime();
    Input latest=input;long at=SystemClock.elapsedRealtime();
    if(!LiveB8Policy.usable(latest.enabled,at,latest.heartbeat,latest.sample,latest.angle)||closing||!presentationReady(at))throw new IllegalStateException("Input/presentation expired before primary request");
    if(nativeReturn)power.nativeRelease();else power.request(target);
    phase=5;resetGeometry();log("PRIMARY_REQUEST target="+target+" nativeRelease="+nativeReturn);
 }
 private void cleanup(){
  cleaning=true;sceneSession=0;sceneTask=-1;scenePrepared=false;
  if(shield!=null){shield.close();lastResources=shield.resources();if(!shield.isClosed())return;log("SHIELD_REMOVED "+lastResources);shield=null;}
  capturePauseRequested=false;
  if(task!=null){task.restore();task=null;}
  if(brightness!=null){brightness.close();brightness=null;}
  if(power!=null){if(!power.close()){status="Live B8 cleanup pending; legacy handoff remains suspended";return;}power=null;}
  active=false;phase=0;cleaning=false;log("CLEANUP_COMPLETE; owned power and layer handles released");
 }
 public void close(){closing=true;input=new Input(false,Float.NaN,0,172,85);}
}
