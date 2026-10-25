"""Build the separate test application; no GitHub publication or release signing.

Requires JDK 17 and ANDROID_HOME (API 36, build-tools 35.0.0).
Set GRADLE_BIN to a locally installed Gradle 8.13 executable if needed.
Keep ~/.android/debug.keystore private and backed up for compatible test updates.
"""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import time
from urllib.parse import urlparse

root = Path(__file__).resolve().parents[1]
env = os.environ.copy()
proxy = urlparse(env.get('HTTPS_PROXY', ''))
options = env.get('GRADLE_OPTS', '')
if proxy.hostname and proxy.port and not proxy.username:
    options += f' -Dhttps.proxyHost={proxy.hostname} -Dhttps.proxyPort={proxy.port}'
    options += f' -Dhttp.proxyHost={proxy.hostname} -Dhttp.proxyPort={proxy.port}'
env['GRADLE_OPTS'] = options
started = time.monotonic()
subprocess.run([env.get('GRADLE_BIN', str(root/'gradlew')), '--no-daemon',
                '--console=plain', 'testGithubDebugUnitTest', 'assembleGithubDebug'],
               cwd=root, env=env, check=True)
apk = root/'app/build/outputs/apk/github/debug/app-github-debug.apk'
out = root/'build/local-test'
out.mkdir(parents=True, exist_ok=True)
target = out/'Duo-Fold-Live-Test.apk'
shutil.copy2(apk, target)
(out/'SHA256SUMS.txt').write_text(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n')
print(f'Local test build: {time.monotonic()-started:.1f}s; {target}')
