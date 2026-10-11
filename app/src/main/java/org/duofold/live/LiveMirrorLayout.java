package org.duofold.live;
final class LiveMirrorLayout {
 static float[] fit(int sw,int sh,int dw,int dh){
  if(sw<=0||sh<=0||dw<=0||dh<=0)throw new IllegalArgumentException("Invalid mirror dimensions");
  float scale=Math.min(dw/(float)sw,dh/(float)sh);
  return new float[]{scale,dw-sw*scale,(dh-sh*scale)/2f};
 }
 // sx, sy, x, y, source crop left/top/right/bottom. Portrait is the original fit.
 static float[] preview(int sw,int sh,int dw,int dh){
  float[] original=fit(sw,sh,dw,dh);
  if(sw<=sh)return new float[]{original[0],original[0],original[1],original[2],0,0,sw,sh};
  // Fill the right half at the destination orientation, center-cropping instead of
  // letterboxing or squeezing a landscape frame into a narrow column.
  float half=dw/2f,scale=Math.max(half/sw,dh/(float)sh);
  int cw=Math.max(1,Math.min(sw,Math.round(half/scale)));
  int ch=Math.max(1,Math.min(sh,Math.round(dh/scale)));
  int left=(sw-cw)/2,top=(sh-ch)/2;
  float sx=half/cw,sy=dh/(float)ch;
  return new float[]{sx,sy,half-left*sx,-top*sy,left,top,left+cw,top+ch};
 }
 static float[] fill(int sw,int sh,int dw,int dh){
  if(sw<=0||sh<=0||dw<=0||dh<=0)throw new IllegalArgumentException("Invalid mirror dimensions");
  float scale=Math.max(dw/(float)sw,dh/(float)sh);
  return new float[]{scale,(dw-sw*scale)/2f,(dh-sh*scale)/2f};
 }
}
