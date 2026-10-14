package org.duofold.live.wallpaperlayer;
/** Pure crop math shared by the editor and every wallpaper renderer. */
public final class WallpaperCrop {
 private WallpaperCrop() {}
 public static float bounded(float value,float low,float high,float fallback){return Float.isFinite(value)?Math.max(low,Math.min(high,value)):fallback;}
 public static int[] rect(int width,int height,int targetWidth,int targetHeight,float zoom,float x,float y){
  if(width<1||height<1||targetWidth<1||targetHeight<1)throw new IllegalArgumentException("Empty wallpaper dimensions");
  zoom=bounded(zoom,1,4,1);x=bounded(x,0,1,.5f);y=bounded(y,0,1,.5f);
  float scale=Math.max((float)targetWidth/width,(float)targetHeight/height)*zoom;
  int w=Math.max(1,Math.min(width,Math.round(targetWidth/scale))),h=Math.max(1,Math.min(height,Math.round(targetHeight/scale)));
  int left=Math.round((width-w)*x),top=Math.round((height-h)*y);
  return new int[]{left,top,left+w,top+h};
 }
 public static boolean cover(int width,int height){return (double)Math.min(width,height)/Math.max(width,height)<.72;}
}
