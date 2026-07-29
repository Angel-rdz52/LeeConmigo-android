// app-blocker.js
// Puente entre la web app (WebView) y el plugin nativo de Android "AppBlocker".
// Si se ejecuta fuera de Android (navegador normal), usa un fallback en memoria
// para que puedas seguir probando la interfaz sin dispositivo.

const NATIVE_DISPONIBLE = () => !!(window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.AppBlocker);

const AppBlockerFallback = (() => {
  let apps = JSON.parse(localStorage.getItem('lc_fallback_blocklist') || '[]');
  let unlocks = JSON.parse(localStorage.getItem('lc_fallback_unlocks') || '{}');
  return {
    async listInstalledApps() {
      return { apps: [
        { packageName: 'com.mojang.minecraftpe', label: 'Minecraft' },
        { packageName: 'com.roblox.client', label: 'Roblox' },
        { packageName: 'com.google.android.youtube', label: 'YouTube' },
        { packageName: 'com.supercell.clashofclans', label: 'Clash of Clans' },
      ] };
    },
    async getBlockedApps() { return { apps }; },
    async setBlockedApps({ apps: nuevas }) {
      apps = nuevas;
      localStorage.setItem('lc_fallback_blocklist', JSON.stringify(apps));
      return { ok: true };
    },
    async unlockApp({ packageName, minutes }) {
      unlocks[packageName] = Date.now() + minutes * 60000;
      localStorage.setItem('lc_fallback_unlocks', JSON.stringify(unlocks));
      return { ok: true };
    },
    async hasUsageAccessPermission() { return { granted: true }; },
    async hasOverlayPermission() { return { granted: true }; },
    async requestUsageAccessPermission() { return { granted: true }; },
    async requestOverlayPermission() { return { granted: true }; },
    async startMonitorService() { return { ok: true }; },
    async getTestMode() { return { enabled: !!JSON.parse(localStorage.getItem('lc_fallback_test_mode') || 'false') }; },
    async setTestMode(enabled) { localStorage.setItem('lc_fallback_test_mode', JSON.stringify(!!enabled)); return { ok: true }; },
    async getPendingRedeem() { return { packageName: null }; },
    async hasBatteryOptimizationExemption() { return { granted: true }; },
    async requestBatteryOptimizationExemption() { return { ok: true }; },
    async cancelAllUnlocks() { unlocks = {}; localStorage.setItem('lc_fallback_unlocks', JSON.stringify(unlocks)); return { ok: true }; },
    async isDeviceAdminActive() { return { active: true }; },
    async requestDeviceAdmin() { return { ok: true }; },
    async setAdminPin() { return { ok: true }; },
    async isChildModeActive() { return { active: !!JSON.parse(localStorage.getItem('lc_fallback_child_mode') || 'false') }; },
    async setChildMode(active) { localStorage.setItem('lc_fallback_child_mode', JSON.stringify(!!active)); return { ok: true }; },
    async requestAddQuickSettingsTile() { return { supported: false }; },
  };
})();

const AppBlocker = {
  async listInstalledApps() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.listInstalledApps();
    return AppBlockerFallback.listInstalledApps();
  },
  async getBlockedApps() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.getBlockedApps();
    return AppBlockerFallback.getBlockedApps();
  },
  async setBlockedApps(apps) {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.setBlockedApps({ apps });
    return AppBlockerFallback.setBlockedApps({ apps });
  },
  async unlockApp(packageName, minutes) {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.unlockApp({ packageName, minutes });
    return AppBlockerFallback.unlockApp({ packageName, minutes });
  },
  async hasPermissions() {
    if (NATIVE_DISPONIBLE()) {
      const usage = await window.Capacitor.Plugins.AppBlocker.hasUsageAccessPermission();
      const overlay = await window.Capacitor.Plugins.AppBlocker.hasOverlayPermission();
      return usage.granted && overlay.granted;
    }
    return true;
  },
  async requestPermissions() {
    if (NATIVE_DISPONIBLE()) {
      // Se piden de a uno: lanzar los dos Intents casi al mismo tiempo hacía
      // que el usuario solo llegara a ver (y conceder) uno de los dos.
      const usage = await window.Capacitor.Plugins.AppBlocker.hasUsageAccessPermission();
      if (!usage.granted) {
        return window.Capacitor.Plugins.AppBlocker.requestUsageAccessPermission();
      }
      const overlay = await window.Capacitor.Plugins.AppBlocker.hasOverlayPermission();
      if (!overlay.granted) {
        return window.Capacitor.Plugins.AppBlocker.requestOverlayPermission();
      }
    }
  },
  async startMonitor() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.startMonitorService();
  },
  async getTestMode() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.getTestMode();
    return AppBlockerFallback.getTestMode();
  },
  async setTestMode(enabled) {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.setTestMode({ enabled });
    return AppBlockerFallback.setTestMode(enabled);
  },
  async getPendingRedeem() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.getPendingRedeem();
    return AppBlockerFallback.getPendingRedeem();
  },
  async hasBatteryExemption() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.hasBatteryOptimizationExemption();
    return AppBlockerFallback.hasBatteryOptimizationExemption();
  },
  async requestBatteryExemption() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.requestBatteryOptimizationExemption();
    return AppBlockerFallback.requestBatteryOptimizationExemption();
  },
  async cancelAllUnlocks() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.cancelAllUnlocks();
    return AppBlockerFallback.cancelAllUnlocks();
  },
  async isDeviceAdminActive() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.isDeviceAdminActive();
    return AppBlockerFallback.isDeviceAdminActive();
  },
  async requestDeviceAdmin() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.requestDeviceAdmin();
    return AppBlockerFallback.requestDeviceAdmin();
  },
  async setAdminPin(pin) {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.setAdminPin({ pin });
    return AppBlockerFallback.setAdminPin();
  },
  async isChildModeActive() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.isChildModeActive();
    return AppBlockerFallback.isChildModeActive();
  },
  async setChildMode(active) {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.setChildMode({ active });
    return AppBlockerFallback.setChildMode(active);
  },
  async requestAddQuickSettingsTile() {
    if (NATIVE_DISPONIBLE()) return window.Capacitor.Plugins.AppBlocker.requestAddQuickSettingsTile();
    return AppBlockerFallback.requestAddQuickSettingsTile();
  },
};

window.AppBlocker = AppBlocker;
