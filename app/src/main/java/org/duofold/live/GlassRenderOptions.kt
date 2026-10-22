package org.duofold.live
import android.content.Context
internal object GlassRenderOptions {
 val ids=listOf("normal","light","projection","system","system_projected")
 val labels=listOf("Normal glass (default)","Lightweight glass","Projection only (no blur)","Samsung notification background blur (experimental)","Samsung notification background blur + stretch (experimental)")
 fun mode(context:Context):String {
  val p=context.getSharedPreferences("standalone",0)
  if(p.getBoolean("dual",false))return "normal"
  return p.getString("glass_render_mode","normal")?.takeIf{it in ids} ?: "normal"
 }
 var systemStatus="System blur: not requested"
 fun report(context:Context)="Rendering: "+labels[ids.indexOf(mode(context))]+"; "+systemStatus
}
