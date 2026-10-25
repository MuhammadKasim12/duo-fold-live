"""Execute the real live controller against a deterministic fake Android platform."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/org/duofold/live/experimental/b8'
with tempfile.TemporaryDirectory(prefix='live-b8-test-') as tmp:
 classes=Path(tmp)/'classes'; reports=Path(tmp)/'reports';classes.mkdir();reports.mkdir()
 files=list((root/'tools/b8-live-tests').rglob('*.java'))+[source/'LiveB8Controller.java',source/'LiveB8Policy.java']
 subprocess.run(['javac','-source','8','-target','8','-d',str(classes),*map(str,files)],check=True)
 subprocess.run(['java','-cp',str(classes),'org.duofold.live.experimental.b8.LiveB8ControllerTest',str(reports)],check=True)
