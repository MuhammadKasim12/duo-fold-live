package org.duofold.live.experimental.b8;
import java.io.File;
final class PowerExperiment {static Object service(String n,String c){return n.equals("window")?new android.view.IWindowManager():new android.os.IPowerManager();}}
final class LiveB8Power {
 static int panel=1,base=3,current=3,requests,releases,cleanupFailures;static boolean allowReady=true;boolean canceled;
 static void reset(){panel=1;base=current=3;requests=releases=cleanupFailures=0;allowReady=true;}
 static final class Info {String id;int w,h;Info(int p){id=p==1?"inner":"cover";w=p==1?2448:1248;h=p==1?1848:1972;}}
 Object info(int id){return new Info(id==0?panel:3-panel);}static String physical(Object o){return ((Info)o).id;}
 static int number(Object o,String f){Info i=(Info)o;return f.equals("logicalWidth")?i.w:f.equals("logicalHeight")?i.h:f.equals("rotation")?0:2;}
 int primary(){return panel;}void on(){}void request(int p){panel=p;current=p==1?4:5;requests++;}int state(boolean b){return b?base:current;}boolean ready(){return allowReady;}
 void nativeRelease(){releases++;current=base;panel=base==3?1:2;}
 boolean close(){if(cleanupFailures>0){cleanupFailures--;return false;}nativeRelease();return true;}
}
final class LiveB8Brightness {static int begun;static boolean failPrepared;static Runnable onBegin;LiveB8Brightness(File f){}void prepare(){}void requirePrepared(){if(failPrepared)throw new IllegalStateException("test brightness unavailable");}void begin(String p){begun++;if(onBegin!=null)onBegin.run();}void close(){}}
final class LiveB8Bridge {
 static boolean allowCommit=true,allowAnimation=true,failArm,allowRemoval=true;static LiveB8Bridge last;static int arms;boolean closed;
 LiveB8Bridge(File f,boolean freeze){last=this;}void arm(){arms++;if(failArm)throw new IllegalStateException("test arm failure");}void animate(){}boolean animatedReady(){return allowAnimation&&allowCommit;}boolean ready(){return allowCommit;}boolean isClosed(){return closed;}void followPhysicalStacks(){}String resources(){return "removeCommitted="+closed;}void reveal(){closed=allowRemoval;}void close(){closed=allowRemoval;}
}
final class LiveB8Task {
 static int moves,retargets;static String selected="test.app";
 static String focusedPackage(){return selected;}LiveB8Task(File d,String p){}int taskId(){return 42;}void selectOnly(){}void moveSelected(){if(!LiveB8Bridge.last.animatedReady())throw new AssertionError("move before animated shield commit");moves++;}void selectAndMove(){moves++;}
 String destinationGeometry(){return "destination";}String verifyPrimary(String p){retargets++;return p;}void focus(){}void restore(){}
}

final class LiveB8Evidence {LiveB8Evidence(File f,boolean c){}String report(){return "test evidence";}void record(File f,String s){}}
