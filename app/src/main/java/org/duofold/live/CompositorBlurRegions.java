package org.duofold.live;

import java.util.ArrayList;

/** Physical-panel mask, independent of texture stretch and color. Rotation follows
 * the existing renderer's canonical hinge axis. Regions never cross the inner seam. */
final class CompositorBlurRegions {
 static float radius(float value){return Float.isFinite(value)?Math.max(0,Math.min(500,value)):250;}
 static float[][] build(int width,int height,boolean inner,int rotation,float progress,float requested,float strength){
  if(width<=0 || height<=0 || !Float.isFinite(progress) || !Float.isFinite(strength))return new float[0][];
  float p=Math.max(0,Math.min(1,progress));
  if(p<=.003f || strength<=0)return new float[0][];
  float motion=p*p*(3-2*p);
  // Reach leads strength: spread farther at the same hinge angle, without crossing the seam.
  float sweep=Math.min(1,p*1.25f);
  float reach=(inner?.5f:1f)*sweep*sweep*(3-2*sweep);
  float peak=Math.min(500,radius(requested)*Math.min(3,strength/.4f))*motion;
  boolean horizontal=rotation==1 || rotation==3,reverse=rotation==1 || rotation==2;
  ArrayList<float[]> regions=new ArrayList<>();
  int blur=Math.round(peak);
  if(blur<1)return new float[0][];
  // One full-strength core, then 64 non-overlapping alpha strips. All share a
  // radius: AOSP SkiaRenderEngine caches the blurred image by radius per layer.
  // Alpha mixes original and blurred content, never a white/black tint. Keeping
  // the core solid avoids washing out the entire blur while softening its edge.
  // This remains a fine stepped approximation, not a continuous pixel mask.
  final int featherSteps=64;
  final float core=.35f;
  for(int i=0;i<=featherSteps;i++){
   float low=i==0?0:reach*(core+(1-core)*(i-1)/featherSteps);
   float high=i==0?reach*core:reach*(core+(1-core)*i/featherSteps);
   float depth=i==0?1:1-(i-.5f)/featherSteps;
   float alpha=depth*depth*(3-2*depth);
   float from=inner?low:1-high,to=inner?high:1-low;
   if(reverse){float old=from;from=1-to;to=1-old;}
   // Radius, alpha, left, top, right, bottom, four corner radii (AOSP JNI layout).
   regions.add(new float[]{blur,alpha,horizontal?0:from*width,horizontal?from*height:0,
     horizontal?width:to*width,horizontal?to*height:height,0,0,0,0});
  }
  return regions.toArray(new float[0][]);
 }
}
