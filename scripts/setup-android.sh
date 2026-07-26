#!/bin/bash
# scripts/setup-android.sh
# Copia el plugin de control parental dentro del proyecto android/ generado
# por `npx cap add android`, y parchea Manifest + MainActivity automáticamente.
set -e

if [ ! -d "android" ]; then
  echo "ERROR: no existe la carpeta android/. Corre primero: npx cap add android"
  exit 1
fi

APPID=$(node -e "console.log(require('./capacitor.config.json').appId)")
PKG_PATH="android/app/src/main/java/$(echo "$APPID" | tr '.' '/')"
echo "appId: $APPID"
echo "Copiando plugin a: $PKG_PATH"

mkdir -p "$PKG_PATH"
cp native-plugin/android/*.kt "$PKG_PATH/"

# Ajusta la declaración "package" si tu appId es distinto de com.leeconmigo.app
sed -i "s/^package com\.leeconmigo\.app/package $APPID/" "$PKG_PATH"/*.kt

# Recursos (por ahora solo device_admin.xml e ic_child_mode.xml)
if [ -d native-plugin/android/res ]; then
  mkdir -p android/app/src/main/res
  cp -r native-plugin/android/res/* android/app/src/main/res/
  echo "Recursos copiados. Contenido de res/xml y res/drawable ahora:"
  find android/app/src/main/res/xml android/app/src/main/res/drawable -type f 2>/dev/null
else
  echo "ADVERTENCIA: no existe native-plugin/android/res en el repo — device_admin.xml e ic_child_mode.xml no se van a copiar, y el build de Gradle va a fallar buscándolos."
fi

MAIN_ACTIVITY=$(find android/app/src/main/java -name "MainActivity.java" | head -n1)
if [ -z "$MAIN_ACTIVITY" ]; then
  echo "ERROR: no se encontró MainActivity.java"
  exit 1
fi
python3 scripts/patch_main_activity.py "$MAIN_ACTIVITY"

MANIFEST="android/app/src/main/AndroidManifest.xml"
python3 scripts/patch_manifest.py "$MANIFEST"

# El proyecto que genera Capacitor por defecto es Java puro: sin esto,
# Gradle ignora silenciosamente los .kt del plugin y falla al compilar
# MainActivity con "cannot find symbol".
python3 scripts/patch_gradle_kotlin.py

echo ""
echo "Listo. Ahora podés compilar con:"
echo "  cd android && ./gradlew assembleDebug"
