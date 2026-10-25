package org.duofold.live.experimental.b8;

/** Pure live-hinge decisions. No lab countdown or simulated timeline. */
final class LiveB8Policy {
 static final long STALE_MS=2000, HEARTBEAT_MS=2500, TRANSITION_MS=6000;
 static boolean usable(boolean enabled,long now,long heartbeat,long sample,float angle){
  return enabled&&Float.isFinite(angle)&&sample>0&&now-sample<=STALE_MS&&now-heartbeat<=HEARTBEAT_MS;
 }
 static int target(float angle,int current,float handoff,float open){
  if(angle<=0)return 2;
  if(angle>=open)return 1;
  if(current==1)return angle<=handoff-4?2:1;
  return angle>=handoff?1:2;
 }
 static boolean endpoint(float angle,float open){return angle<=0||angle>=open;}
 static boolean stable(long now,long since){return since>=0&&now-since>=300;}
 static boolean reveal(long now,long verified){return verified>=0&&now-verified>=150;}
}
