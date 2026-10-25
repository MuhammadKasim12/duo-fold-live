package android.view;import android.os.IBinder;
public class SurfaceControl {
 public static int writes;public static long[] getPhysicalDisplayIds(){return new long[]{1,2};}
 public static IBinder getPhysicalDisplayToken(long id){return new IBinder(){};}
 public static boolean getDisplayBrightnessSupport(IBinder b){return true;}
 public static boolean setDisplayBrightness(IBinder b,float a,float c,float d,float e){writes++;return true;}
}
