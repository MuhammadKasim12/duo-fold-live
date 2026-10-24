package org.duofold.powerlab;
import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import android.graphics.Color;
import java.io.*;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
 private TextView state;private final Handler h=new Handler(Looper.getMainLooper());
 private final Runnable update=new Runnable(){public void run(){state.setText(Bridge.state+"\n\n"+RecorderService.status);h.postDelayed(this,1000);}};
 public void onCreate(Bundle b){super.onCreate(b);
  ScrollView scroll=new ScrollView(this);LinearLayout col=new LinearLayout(this);col.setOrientation(1);int p=(int)(20*getResources().getDisplayMetrics().density);col.setPadding(p,p,p,p);scroll.addView(col);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{col.setPadding(p,p+insets.getSystemWindowInsetTop(),p,p+insets.getSystemWindowInsetBottom());return insets;});
  text(col,"Fold Power Lab",28);text(col,"Display power investigation • 0.2.0",15);
  text(col,"1. Turn Duo Fold Live OFF. Keep Shizuku running.\n2. Authorize and connect below.\n3. Start a test. Go Home or open your usual app. Wait until the notification says RECORDING, then perform the selected gesture. For shade tests, pull down slowly, hold fully open for 4 seconds, then dismiss.\n4. Return after 10 seconds and save the report ZIP.",17);
  text(col,"Baseline is read-only. Override tests last at most 10 seconds; Stop releases them. Disable Duo before override tests. Detailed blur tracing streams data to preserve the full gesture; it adds overhead and may be restricted by Samsung. Reports contain app/window names and system logs; no screenshots or network uploads.",14);
  button(col,"Authorize Shizuku",()->{try{if(!Shizuku.pingBinder())throw new IllegalStateException("Start Shizuku and reopen this app");if(Shizuku.checkSelfPermission()!=0)Shizuku.requestPermission(7);else Bridge.state="Authorized — tap Connect";}catch(Exception e){message(e.toString());}});
  button(col,"Connect",()->{try{if(Shizuku.checkSelfPermission()!=0)throw new IllegalStateException("Authorize first");Bridge.connect(this);}catch(Exception e){message(e.toString());}});
  button(col,"Record handoff · 10 seconds",()->start(0));button(col,"Record Recents blur · 10 seconds",()->start(1));
  button(col,"Record notification shade blur · 10 seconds",()->start(2));
  button(col,"Record Quick Settings blur · 10 seconds",()->start(3));
  button(col,"Record Duo compositor blur · 10 seconds",()->start(4));
  button(col,"Power baseline + API inventory · 10 seconds",()->start(5));
  button(col,"Samsung ON override · D0 · 10 seconds",()->start(6));
  button(col,"Samsung ON override · D1 · 10 seconds",()->start(7));
  button(col,"Samsung ON override · both · 10 seconds",()->start(8));
  button(col,"Bright wake lock · 10 seconds",()->start(9));
  button(col,"Concurrent inner primary · 10 seconds",()->start(10));
  button(col,"Concurrent cover primary · 10 seconds",()->start(11));
  button(col,"Concurrent inner + both ON · 10 seconds",()->start(12));
  button(col,"Concurrent cover + both ON · 10 seconds",()->start(13));
  button(col,"Stop recording",()->{if(RecorderService.active)startService(new Intent(this,RecorderService.class).setAction("stop"));});
  button(col,"Save report ZIP",()->{if(RecorderService.active){message("Wait for recording to finish");return;}File f=latest();if(f==null){message("No completed report yet");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,f.getName());startActivityForResult(i,9);});
  state=text(col,"Idle",16);
  text(col,"For handoffs: use the same app and orientation; open, pause, close, repeat. For blur: open Recents, pause, return to the app, repeat. Record each mode separately and save its ZIP before starting another. For notification shade, use the notifications side of your top swipe; Quick Settings may use the other side on Samsung. Keep the same wallpaper/app, display and theme. Enable Duo only for the Duo compositor test, then fold slowly to show its blur. The diagnostic does not open panels or change settings for you. Software events cannot prove when pixels became visible. If a trace source is blocked, the report retains the error and other available observations.",14);
 }
 private File latest(){String name=getSharedPreferences("reports",0).getString("latest","");File f=new File(getFilesDir(),name);return f.isFile()?f:null;}
 private void start(int mode){if(RecorderService.active){message("A recording is already running");return;}if(Bridge.remote==null){message("Connect Shizuku first");return;}if(checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0){requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},8);message("Allow notifications, then tap Record again");return;}startForegroundService(new Intent(this,RecorderService.class).putExtra("mode",mode));}
 public void onResume(){super.onResume();h.post(update);}public void onPause(){h.removeCallbacks(update);super.onPause();}
 protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==9&&result==RESULT_OK&&data!=null){File f=latest();if(f==null)return;new Thread(()->{try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(data.getData())){byte[] b=new byte[16384];int n;while((n=in.read(b))>=0)out.write(b,0,n);runOnUiThread(()->message("Saved. Attach the ZIP in our chat."));}catch(Exception e){runOnUiThread(()->message("Save failed: "+e));}}).start();}}
 private TextView text(LinearLayout l,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(225,232,246));t.setPadding(0,12,0,16);l.addView(t);return t;}
 private void button(LinearLayout l,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);l.addView(b);b.setOnClickListener(v->r.run());}
 private void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
