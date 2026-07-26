import re
import sys

ROOT_GRADLE = "android/build.gradle"
APP_GRADLE = "android/app/build.gradle"
KOTLIN_VERSION = "1.9.24"

# --- android/build.gradle: agrega el classpath del plugin de Kotlin ---
with open(ROOT_GRADLE) as f:
    root = f.read()

if "kotlin-gradle-plugin" not in root:
    nuevo, n = re.subn(
        r"(classpath ['\"]com\.android\.tools\.build:gradle:[^'\"]+['\"])",
        r"\1\n        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:%s'" % KOTLIN_VERSION,
        root,
        count=1,
    )
    if n == 0:
        print("ADVERTENCIA: no se encontró la línea 'classpath com.android.tools.build:gradle' en android/build.gradle. Agregá manualmente el classpath de Kotlin.")
        sys.exit(1)
    with open(ROOT_GRADLE, "w") as f:
        f.write(nuevo)
    print("android/build.gradle: classpath de Kotlin agregado.")
else:
    print("android/build.gradle: ya tenía Kotlin configurado.")

# --- android/app/build.gradle: aplica el plugin kotlin-android + stdlib ---
with open(APP_GRADLE) as f:
    app = f.read()

cambiado = False

if "kotlin-android" not in app:
    app, n = re.subn(
        r"apply plugin: ['\"]com\.android\.application['\"]",
        "apply plugin: 'com.android.application'\napply plugin: 'kotlin-android'",
        app,
        count=1,
    )
    if n == 0:
        print("ADVERTENCIA: no se encontró \"apply plugin: 'com.android.application'\" en android/app/build.gradle. Agregá manualmente 'apply plugin: kotlin-android'.")
        sys.exit(1)
    cambiado = True

if "kotlin-stdlib" not in app:
    app, n = re.subn(
        r"(dependencies\s*\{)",
        r'\1\n    implementation "org.jetbrains.kotlin:kotlin-stdlib:%s"' % KOTLIN_VERSION,
        app,
        count=1,
    )
    if n == 0:
        print("ADVERTENCIA: no se encontró el bloque dependencies{} en android/app/build.gradle. Agregá manualmente la dependencia de kotlin-stdlib.")
        sys.exit(1)
    cambiado = True

if cambiado:
    with open(APP_GRADLE, "w") as f:
        f.write(app)
    print("android/app/build.gradle: plugin kotlin-android + kotlin-stdlib agregados.")
else:
    print("android/app/build.gradle: ya tenía Kotlin configurado.")
