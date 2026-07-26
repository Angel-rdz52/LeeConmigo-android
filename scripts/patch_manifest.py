import sys

path = sys.argv[1]
with open(path) as f:
    content = f.read()

if 'PACKAGE_USAGE_STATS' in content:
    print("Manifest ya parcheado, no se hace nada.")
    sys.exit(0)

if 'xmlns:tools=' not in content:
    content = content.replace('<manifest ', '<manifest xmlns:tools="http://schemas.android.com/tools" ', 1)

permisos = """
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
"""
content = content.replace('<application', permisos + '\n    <application', 1)

componentes = """
        <service
            android:name=".MonitorService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Control parental LeeConmigo" />
        </service>

        <activity
            android:name=".LockActivity"
            android:exported="false"
            android:launchMode="singleTask"
            android:theme="@style/Theme.AppCompat.NoActionBar"
            android:excludeFromRecents="true" />

        <receiver
            android:name=".BootReceiver"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".WatchdogReceiver"
            android:exported="false" />

        <receiver
            android:name=".LeeConmigoDeviceAdminReceiver"
            android:permission="android.permission.BIND_DEVICE_ADMIN"
            android:exported="true">
            <meta-data
                android:name="android.app.device_admin"
                android:resource="@xml/device_admin" />
            <intent-filter>
                <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
            </intent-filter>
        </receiver>

        <service
            android:name=".ChildModeTileService"
            android:label="Modo niño"
            android:icon="@drawable/ic_child_mode"
            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.quicksettings.action.QS_TILE" />
            </intent-filter>
            <meta-data
                android:name="android.service.quicksettings.ACTIVE_TILE"
                android:value="true" />
        </service>

        <activity
            android:name=".ChildModePinActivity"
            android:exported="false"
            android:theme="@style/Theme.AppCompat.NoActionBar" />
"""
content = content.replace('</application>', componentes + '    </application>', 1)

with open(path, 'w') as f:
    f.write(content)

print("AndroidManifest.xml parcheado correctamente.")
