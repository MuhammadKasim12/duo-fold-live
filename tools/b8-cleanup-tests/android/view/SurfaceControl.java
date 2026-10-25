package android.view;
import java.util.concurrent.Executor;
import android.hardware.HardwareBuffer;
public class SurfaceControl {
 public int stack=-1;public boolean released;public boolean isValid(){return !released;}public void release(){released=true;}
 public interface TransactionCommittedListener {void onTransactionCommitted();}
 public static class Transaction {
  public static Runnable commit;public static int removals,clears;public static boolean reject;
  public Transaction(){} public Transaction setAlpha(SurfaceControl s,float a){return this;}public Transaction hide(SurfaceControl s){return this;}
  public Transaction setLayerStack(SurfaceControl s,int stack){s.stack=stack;return this;}
  public Transaction remove(SurfaceControl s){removals++;return this;}
  public Transaction setBuffer(SurfaceControl s,HardwareBuffer b){if(b==null)clears++;return this;}
  public Transaction addTransactionCommittedListener(Executor e,TransactionCommittedListener l){commit=()->e.execute(l::onTransactionCommitted);return this;}
  public void apply(){if(reject)throw new IllegalStateException("test rejected");}public void close(){}
 }
}
