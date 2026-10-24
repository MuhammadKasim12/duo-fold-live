#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
platform="$ANDROID_HOME/platforms/android-36/android.jar"
bt="$ANDROID_HOME/build-tools/35.0.0"
mkdir -p build/classes
javac -source 8 -target 8 -proc:none -bootclasspath "$platform:$bt/core-lambda-stubs.jar" -cp '../../app/libs/*' -d build/classes src/org/duofold/powerlab/*.java
jar cf build/app-classes.jar -C build/classes .
"$bt/d8" --min-api 34 --lib "$platform" --output build build/app-classes.jar ../../app/libs/*.jar
"$bt/aapt2" link -o build/unsigned.apk -I "$platform" --manifest AndroidManifest.xml
python3 - <<'END'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 z.write('build/classes.dex','classes.dex')
 z.write('LICENSE-Shizuku.txt','assets/LICENSE-Shizuku.txt')
END
