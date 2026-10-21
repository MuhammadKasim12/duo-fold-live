package org.duofold.live
import android.content.Context
internal object GlassRenderOptions {
 val ids=listOf("normal","light","projection","system")
 val labels=listOf("Normal glass (default)","Lightweight glass","Projection only (no blur)","System compositor blur (experimental)")
 fun mode(context:Context):String {
  val p=context.getSharedPreferences("standalone",0)
  if(p.getBoolean("dual",false))return "normal"
  return p.getString("glass_render_mode","normal")?.takeIf{it in ids} ?: "normal"
 }
 var systemStatus="System blur: not requested"
 fun report(context:Context)="Rendering: "+labels[ids.indexOf(mode(context))]+"; "+systemStatus
}
