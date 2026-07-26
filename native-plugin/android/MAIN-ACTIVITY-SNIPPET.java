// android/app/src/main/java/com/leeconmigo/app/MainActivity.java
// Este archivo lo genera `npx cap add android`. Edítalo así (Capacitor 6):

package com.leeconmigo.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppBlockerPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
