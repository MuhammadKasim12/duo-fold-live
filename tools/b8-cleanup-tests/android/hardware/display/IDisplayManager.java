package android.hardware.display;
public class IDisplayManager {
 public static boolean swapped;public static int stack0=10,stack1=20;
 public static class Info {public String uniqueId;public int layerStack;Info(String id,int s){uniqueId=id;layerStack=s;}}
 public Info getDisplayInfo(int id){return new Info(id==0?(swapped?"inner":"cover"):(swapped?"cover":"inner"),id==0?stack0:stack1);}
}
