package org.duofold.live

/** Inverse geometry warp, shared by both glass shaders. Zero tilt is identity. */
internal object MotionGlassShader {
 val source="""
 float motionAxis(float x,float gain,float span,float pinnedSeam){
  if(pinnedSeam>0.5){
   if(x>=span)return x;
   return x/(gain+(1.0-gain)*x/span);
  }
  return x/gain;
 }
 float motionLever(float x,float span,float pinnedSeam){
  if(pinnedSeam>0.5)return x<span ? x*(1.0-x/span) : 0.0;
  return x;
 }
 float2 motionWarp(float2 uv){
  float fade=smoothstep(0.0,0.15,progress);
  float2 tilt=motionOffset*fade;
  if(tilt.x==0.0 && tilt.y==0.0)return uv;
  // The reflected preview is already flipped by its canvas. Undo that flip
  // for the anchor calculation, then restore it for the existing projection.
  float2 screen=uv;
  if(reflectedCover>0.5)screen.x=1.0-screen.x;
  float x=mix(screen.x,screen.y,horizontal);
  x=mix(x,1.0-x,reverse);
  float y=mix(screen.y,screen.x,horizontal);
  float along=mix(tilt.x,tilt.y,horizontal)*(1.0-2.0*reverse);
  float across=mix(tilt.y,tilt.x,horizontal);
  float pinnedSeam=inner>0.5 && fallback<0.5 ? 1.0 : 0.0;
  float gain=exp2(clamp(along,-2.0,2.0));
  float warped=motionAxis(x,gain,0.5,pinnedSeam);
  y-=0.25*across*motionLever(x,0.5,pinnedSeam);
  warped=mix(warped,1.0-warped,reverse);
  screen=horizontal>0.5 ? float2(y,warped) : float2(warped,y);
  if(reflectedCover>0.5)screen.x=1.0-screen.x;
  return screen;
 }
 """.trimIndent()
}
