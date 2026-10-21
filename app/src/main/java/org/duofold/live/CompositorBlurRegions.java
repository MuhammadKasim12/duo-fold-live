package org.duofold.live;

import java.util.ArrayList;

/** Physical-panel mask, independent of texture stretch and color. Rotation follows
 * the existing renderer's canonical hinge axis. Regions never cross the inner seam. */
final class CompositorBlurRegions {
 static float radius(float value){return Float.isFinite(value)?Math.max(0,Math.min(240,value)):120;}
 static float[][] build(int width,int height,boolean inner,int rotation,float progress,float requested,float strength){
  if(width<=0 || height<=0 || !Float.isFinite(progress) || !Float.isFinite(strength))return new float[0][];
  float p=Math.max(0,Math.min(1,progress));
  if(p<=.003f || strength<=0)return new float[0][];
  float motion=p*p*(3-2*p);
  float reach=(inner?.5f:1f)*motion;
  float peak=Math.min(240,radius(requested)*Math.min(3,strength/.4f))*motion;
  boolean horizontal=rotation==1 || rotation==3,reverse=rotation==1 || rotation==2;
  ArrayList<float[]> regions=new ArrayList<>();
  // Non-overlapping bands approximate a spatial radius gradient without tint or
  // alpha modulation. Every frame moves the leading boundary in panel space.
  for(int i=0;i<24;i++){
   float low=reach*i/24,high=reach*(i+1)/24;
   float depth=1-(i+.5f)/24;
   int blur=Math.round(peak*depth*depth*(3-2*depth));
   if(blur<1)continue;
   float from=inner?low:1-high,to=inner?high:1-low;
   if(reverse){float old=from;from=1-to;to=1-old;}
   // Radius, alpha, left, top, right, bottom, four corner radii (AOSP JNI layout).
   regions.add(new float[]{blur,1,horizontal?0:from*width,horizontal?from*height:0,
     horizontal?width:to*width,horizontal?to*height:height,0,0,0,0});
  }
  return regions.toArray(new float[0][]);
 }
}
