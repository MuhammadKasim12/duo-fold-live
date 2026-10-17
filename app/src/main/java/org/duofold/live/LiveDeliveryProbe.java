package org.duofold.live;

import android.os.Looper;
import android.os.SystemClock;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Diagnostic only: sample a delayed main callback once, without moving UI work. */
final class LiveDeliveryProbe {
 private static final ScheduledThreadPoolExecutor WATCHER = new ScheduledThreadPoolExecutor(1, task -> {
  Thread thread = new Thread(task, "duo-delivery-probe");
  thread.setDaemon(true);
  return thread;
 });
 static { WATCHER.setRemoveOnCancelPolicy(true); }
 private static long nextProbeAt;
 static synchronized Ticket begin(String label) {
  long now = SystemClock.elapsedRealtime();
  if (now < nextProbeAt) return null;
  nextProbeAt = now + 1000;
  Ticket ticket = new Ticket(label, now);
  ticket.future = WATCHER.schedule(ticket::sample, 64, TimeUnit.MILLISECONDS);
  return ticket;
 }
 static final class Ticket {
  private final AtomicBoolean finished = new AtomicBoolean();
  private final String label;
  private final long queued;
  private ScheduledFuture<?> future;
  Ticket(String label, long queued) { this.label=label; this.queued=queued; }
  void finish() {
   finished.set(true);
   if (future != null) future.cancel(false);
  }
  private void sample() {
   if (finished.get()) return;
   Thread main = Looper.getMainLooper().getThread();
   Thread.State state = main.getState();
   StackTraceElement[] stack = main.getStackTrace();
   long sampled = SystemClock.elapsedRealtime();
   if (finished.get()) return;
   StringBuilder note = new StringBuilder("Live delivery stall: ").append(label)
     .append("; waited=").append(sampled-queued).append(" ms; main=").append(state)
     .append("; sampled stack (not a duration measurement)=");
   for(int i=0;i<Math.min(18,stack.length);i++){
    if(i>0)note.append(" <- ");
    note.append(stack[i]);
   }
   RecoveryLog.add(note.toString());
  }
 }
}
