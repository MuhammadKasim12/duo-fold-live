package org.duofold.live;
final class LiveMirrorLayout {
 static float[] fit(int sw,int sh,int dw,int dh){
  if(sw<=0||sh<=0||dw<=0||dh<=0)throw new IllegalArgumentException("Invalid mirror dimensions");
  // A rotated cover must stay in the right-hand preview region, rather than
  // becoming a full-width band through the middle of the inner display.
  // Keep the established portrait calculation exactly unchanged.
  float availableWidth=sw>sh?dw/2f:dw;
  float scale=Math.min(availableWidth/sw,dh/(float)sh);
  return new float[]{scale,dw-sw*scale,(dh-sh*scale)/2f};
 }
 static float[] fill(int sw,int sh,int dw,int dh){
  if(sw<=0||sh<=0||dw<=0||dh<=0)throw new IllegalArgumentException("Invalid mirror dimensions");
  float scale=Math.max(dw/(float)sw,dh/(float)sh);
  return new float[]{scale,(dw-sw*scale)/2f,(dh-sh*scale)/2f};
 }
}
