// parental.js
// Pantallas: Canjear estrellas (niño) y Control parental (admin con PIN).
// Depende de app-blocker.js (window.AppBlocker) y de window.LC expuesto por app.js.

const PIN_KEY = 'lc_admin_pin';
let appsSeleccionadasParaGuardar = []; // se arma al renderizar el panel admin

function pinGuardado() {
    return localStorage.getItem(PIN_KEY);
}

/* ================= CANJE (niño) ================= */

async function abrirRedeem() {
    LC.showScreen('redeem');
    const list = document.getElementById('redeem-app-list');
    list.innerHTML = '<p class="text-xs text-slate-400">Cargando...</p>';

    const { apps } = await AppBlocker.getBlockedApps();

    if (!apps || apps.length === 0) {
        list.innerHTML = '<p class="text-sm text-slate-400 text-center py-6">Todavía no hay apps configuradas por un adulto en "Control parental".</p>';
        return;
    }

    list.innerHTML = '';
    apps.forEach(app => {
        const wrapper = document.createElement('div');
        wrapper.className = 'border border-slate-200 rounded-xl overflow-hidden';
        wrapper.dataset.packageName = app.packageName;

        const btn = document.createElement('button');
        btn.className = 'w-full flex items-center justify-between p-3 bg-slate-50 hover:bg-slate-100 transition';
        btn.innerHTML = `<span class="font-semibold text-slate-700 text-sm">📱 ${app.label}</span><span class="chevron-canje text-xs text-indigo-600 font-bold whitespace-nowrap">Ver opciones ›</span>`;

        const detalle = document.createElement('div');
        detalle.className = 'detalle-canje hidden p-3 border-t border-slate-200 space-y-2';

        btn.addEventListener('click', () => {
            const estaAbierto = !detalle.classList.contains('hidden');

            // Acordeón: se cierra cualquier otro que haya quedado abierto.
            document.querySelectorAll('#redeem-app-list .detalle-canje').forEach(d => d.classList.add('hidden'));
            document.querySelectorAll('#redeem-app-list .chevron-canje').forEach(c => c.innerText = 'Ver opciones ›');

            if (!estaAbierto) {
                if (!detalle.dataset.armado) {
                    armarOpcionesDeCanje(app, detalle);
                    detalle.dataset.armado = '1';
                }
                detalle.classList.remove('hidden');
                btn.querySelector('.chevron-canje').innerText = 'Ocultar ▲';
            }
        });

        wrapper.appendChild(btn);
        wrapper.appendChild(detalle);
        list.appendChild(wrapper);
    });
}

function armarOpcionesDeCanje(app, contenedor) {
    contenedor.innerHTML = `<p class="text-xs font-bold text-slate-500 mb-1">Arma tu tiempo:</p>`;

    (app.options || []).forEach((op) => {
        let cantidad = 1;
        const user = LC.getUser();
        const maxPorEstrellas = Math.max(1, Math.floor((user.total_stars || 0) / op.cost));
        const TOPE_UNIDADES = 10; // evita desbloqueos absurdamente largos por error de toque
        const maxCantidad = Math.max(1, Math.min(TOPE_UNIDADES, maxPorEstrellas));

        const card = document.createElement('div');
        card.className = 'border border-slate-200 rounded-xl p-3 space-y-2';
        card.innerHTML = `
            <div class="flex items-center justify-between">
                <span class="text-xs text-slate-500">Bloque base: ${op.minutes} min por ⭐${op.cost}</span>
            </div>
            <div class="flex items-center justify-between">
                <div class="flex items-center space-x-3">
                    <button class="btn-menos w-9 h-9 rounded-full bg-slate-100 font-bold text-lg">−</button>
                    <span class="cantidad-label font-bold text-lg w-6 text-center">1</span>
                    <button class="btn-mas w-9 h-9 rounded-full bg-slate-100 font-bold text-lg">+</button>
                </div>
                <div class="text-right">
                    <p class="total-tiempo font-bold text-slate-800 text-sm"></p>
                    <p class="total-costo text-amber-700 text-xs font-bold"></p>
                </div>
            </div>
            <button class="btn-confirmar-canje w-full py-2.5 rounded-xl font-bold text-sm"></button>
        `;

        const cantidadLabel = card.querySelector('.cantidad-label');
        const totalTiempo = card.querySelector('.total-tiempo');
        const totalCosto = card.querySelector('.total-costo');
        const btnConfirmar = card.querySelector('.btn-confirmar-canje');

        function formatearMinutos(mins) {
            if (mins < 60) return `${mins} min`;
            const h = Math.floor(mins / 60);
            const m = mins % 60;
            return m === 0 ? `${h} h` : `${h} h ${m} min`;
        }

        function refrescar() {
            const minutosTotal = op.minutes * cantidad;
            const costoTotal = op.cost * cantidad;
            const alcanza = (LC.getUser().total_stars || 0) >= costoTotal;

            cantidadLabel.innerText = cantidad;
            totalTiempo.innerText = formatearMinutos(minutosTotal);
            totalCosto.innerText = `⭐ ${costoTotal}`;

            btnConfirmar.disabled = !alcanza;
            btnConfirmar.innerText = alcanza ? `Canjear ${formatearMinutos(minutosTotal)}` : 'Te faltan estrellas';
            btnConfirmar.className = `btn-confirmar-canje w-full py-2.5 rounded-xl font-bold text-sm ${alcanza ? 'bg-amber-500 hover:bg-amber-600 text-white' : 'bg-slate-100 text-slate-400 cursor-not-allowed'}`;
        }

        card.querySelector('.btn-menos').addEventListener('click', () => {
            cantidad = Math.max(1, cantidad - 1);
            refrescar();
        });
        card.querySelector('.btn-mas').addEventListener('click', () => {
            cantidad = Math.min(maxCantidad, cantidad + 1);
            refrescar();
        });
        btnConfirmar.addEventListener('click', () => canjear(app, { minutes: op.minutes * cantidad, cost: op.cost * cantidad }));

        refrescar();
        contenedor.appendChild(card);
    });
}

async function canjear(app, opcion) {
    const user = LC.getUser();
    if ((user.total_stars || 0) < opcion.cost) return;

    user.total_stars -= opcion.cost;
    LC.saveUser();
    LC.refreshDashboard();

    await AppBlocker.unlockApp(app.packageName, opcion.minutes);
    alert(`🎉 ¡${app.label} desbloqueada por ${opcion.minutes} minutos!`);
    LC.showScreen('dashboard');
}

/* ================= CONTROL PARENTAL (admin) ================= */

function abrirAdminPin() {
    document.getElementById('admin-pin-input').value = '';
    LC.showScreen('adminPin');
}

function validarPin() {
    const ingresado = document.getElementById('admin-pin-input').value.trim();
    if (!ingresado) return;
    const guardado = pinGuardado();

    if (!guardado) {
        // Primera vez: el PIN ingresado se convierte en el PIN de control parental.
        localStorage.setItem(PIN_KEY, ingresado);
        AppBlocker.setAdminPin(ingresado);
        abrirAdminPanel();
        return;
    }
    if (ingresado === guardado) {
        // Se re-sincroniza en cada entrada correcta — así, si el PIN nativo
        // alguna vez quedó desactualizado (por ejemplo, instalaciones
        // anteriores a que existiera el Modo Niño), se autocorrige solo.
        AppBlocker.setAdminPin(ingresado);
        abrirAdminPanel();
    } else {
        alert('PIN incorrecto.');
    }
}

async function abrirAdminPanel() {
    LC.showScreen('admin');
    const searchInput = document.getElementById('admin-app-search');
    if (searchInput) searchInput.value = '';
    await refrescarEstadoPermisos();
    await cargarModoPrueba();
    await refrescarEstadoModoNino();
    await renderAdminAppList();
}

async function refrescarEstadoModoNino() {
    const label = document.getElementById('child-mode-status');
    if (!label) return;
    const { active } = await AppBlocker.isChildModeActive();
    label.innerText = active ? '🟢 Activo (todo bloqueado)' : '⚪ Apagado';
}

async function refrescarEstadoPermisos() {
    const warning = document.getElementById('admin-permission-warning');
    if (warning) {
        const ok = await AppBlocker.hasPermissions();
        warning.classList.toggle('hidden', ok);

        // Estado individual, para que se vea claro cuál falta todavía.
        if (window.Capacitor?.Plugins?.AppBlocker) {
            const usage = await window.Capacitor.Plugins.AppBlocker.hasUsageAccessPermission();
            const overlay = await window.Capacitor.Plugins.AppBlocker.hasOverlayPermission();
            const elUsage = document.getElementById('status-usage');
            const elOverlay = document.getElementById('status-overlay');
            if (elUsage) elUsage.innerText = usage.granted ? '✅' : '❌';
            if (elOverlay) elOverlay.innerText = overlay.granted ? '✅' : '❌';
        }
    }
    const bateria = document.getElementById('admin-battery-warning');
    if (bateria) {
        const { granted } = await AppBlocker.hasBatteryExemption();
        bateria.classList.toggle('hidden', !!granted);
    }
    const desinstalacion = document.getElementById('admin-uninstall-warning');
    if (desinstalacion) {
        const { active } = await AppBlocker.isDeviceAdminActive();
        desinstalacion.classList.toggle('hidden', !!active);
    }
}

function pantallaAdminVisible() {
    const el = document.getElementById('screen-admin');
    return el && el.classList.contains('screen-active');
}

// Cuando volvés de Ajustes del sistema (a conceder permisos), Android no recarga
// la app, solo la reanuda — sin esto, seguía mostrando el aviso de "faltan
// permisos" hasta forzar detención.
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        if (pantallaAdminVisible()) refrescarEstadoPermisos();
        revisarCanjeDirigido();
    }
});
window.addEventListener('focus', () => {
    if (pantallaAdminVisible()) refrescarEstadoPermisos();
    revisarCanjeDirigido();
});

/* ================= Canje dirigido desde la pantalla de bloqueo ================= */
// Cuando el niño toca "Comprar tiempo" en la pantalla de bloqueo nativa,
// esta función lo manda directo a esa app en Canjear estrellas — sin pasar
// por el dashboard ni por control parental.
async function revisarCanjeDirigido() {
    let pendiente;
    try {
        pendiente = await AppBlocker.getPendingRedeem();
    } catch (e) {
        return;
    }
    const packageName = pendiente && pendiente.packageName;
    if (!packageName || !LC.getUser()) return;

    await abrirRedeem();
    const { apps } = await AppBlocker.getBlockedApps();
    const app = (apps || []).find(a => a.packageName === packageName);
    if (!app) return;

    const fila = document.querySelector(`#redeem-app-list [data-package-name="${packageName}"] > button`);
    if (fila) fila.click();
}

window.addEventListener('DOMContentLoaded', () => {
    // Pequeño margen para que app.js cargue el perfil guardado primero.
    setTimeout(() => {
        forzarConfiguracionInicial();
        revisarCanjeDirigido();
    }, 200);
});

/* ================= Modo prueba (unidades en segundos) ================= */

async function cargarModoPrueba() {
    const checkbox = document.getElementById('admin-test-mode');
    if (!checkbox) return;
    const { enabled } = await AppBlocker.getTestMode();
    checkbox.checked = !!enabled;
}

async function renderAdminAppList() {
    const list = document.getElementById('admin-app-list');
    list.innerHTML = '<p class="text-xs text-slate-400">Cargando apps instaladas...</p>';

    const { apps: instaladas } = await AppBlocker.listInstalledApps();
    const { apps: yaBloqueadas } = await AppBlocker.getBlockedApps();
    const bloqueadasPorPaquete = Object.fromEntries((yaBloqueadas || []).map(a => [a.packageName, a]));

    list.innerHTML = '';
    appsSeleccionadasParaGuardar = [];

    instaladas.forEach(app => {
        const existente = bloqueadasPorPaquete[app.packageName];
        const activo = !!existente;
        const op1 = existente?.options?.[0] || { minutes: 30, cost: 15 };
        const op2 = existente?.options?.[1] || { minutes: 60, cost: 25 };

        const row = document.createElement('div');
        row.className = 'border border-slate-200 rounded-xl p-3';
        row.innerHTML = `
            <label class="flex items-center space-x-2 mb-2">
                <input type="checkbox" class="admin-app-check w-4 h-4" ${activo ? 'checked' : ''}>
                <span class="font-semibold text-sm text-slate-700">${app.label}</span>
            </label>
            <div class="grid grid-cols-2 gap-2 admin-app-options ${activo ? '' : 'hidden'}">
                <div class="flex items-center space-x-1">
                    <input type="number" min="5" class="op1-min w-14 px-2 py-1 border rounded text-xs" value="${op1.minutes}"> min /
                    <input type="number" min="1" class="op1-cost w-12 px-2 py-1 border rounded text-xs" value="${op1.cost}"> ⭐
                </div>
                <div class="flex items-center space-x-1">
                    <input type="number" min="5" class="op2-min w-14 px-2 py-1 border rounded text-xs" value="${op2.minutes}"> min /
                    <input type="number" min="1" class="op2-cost w-12 px-2 py-1 border rounded text-xs" value="${op2.cost}"> ⭐
                </div>
            </div>
        `;

        const checkbox = row.querySelector('.admin-app-check');
        const optionsBox = row.querySelector('.admin-app-options');
        checkbox.addEventListener('change', () => {
            optionsBox.classList.toggle('hidden', !checkbox.checked);
            actualizarContadorSeleccionadas();
        });

        row.dataset.packageName = app.packageName;
        row.dataset.label = app.label;
        list.appendChild(row);
    });

    actualizarContadorSeleccionadas();
    filtrarListaAdmin();
}

function actualizarContadorSeleccionadas() {
    const total = document.querySelectorAll('#admin-app-list .admin-app-check:checked').length;
    const el = document.getElementById('admin-selected-count');
    if (el) el.innerText = `${total} elegida${total === 1 ? '' : 's'}`;
}

function filtrarListaAdmin() {
    const q = (document.getElementById('admin-app-search')?.value || '').trim().toLowerCase();
    document.querySelectorAll('#admin-app-list > div').forEach(row => {
        const label = (row.dataset.label || '').toLowerCase();
        row.classList.toggle('hidden', !!q && !label.includes(q));
    });
}

async function guardarConfiguracionAdmin() {
    const rows = document.querySelectorAll('#admin-app-list > div');
    const resultado = [];

    rows.forEach(row => {
        const checked = row.querySelector('.admin-app-check').checked;
        if (!checked) return;
        resultado.push({
            packageName: row.dataset.packageName,
            label: row.dataset.label,
            options: [
                { minutes: Number(row.querySelector('.op1-min').value), cost: Number(row.querySelector('.op1-cost').value) },
                { minutes: Number(row.querySelector('.op2-min').value), cost: Number(row.querySelector('.op2-cost').value) },
            ]
        });
    });

    await AppBlocker.setBlockedApps(resultado);
    await AppBlocker.startMonitor();
    alert('✅ Lista de apps bloqueadas actualizada.');
}

/* ================= listeners ================= */

document.getElementById('btn-open-redeem')?.addEventListener('click', abrirRedeem);
document.getElementById('btn-redeem-back')?.addEventListener('click', () => {
    LC.showScreen(LC.getUser() ? 'dashboard' : 'login');
});

document.getElementById('btn-open-admin')?.addEventListener('click', abrirAdminPin);
document.getElementById('btn-admin-pin-cancel')?.addEventListener('click', () => {
    LC.showScreen(LC.getUser() ? 'dashboard' : 'login');
});
document.getElementById('btn-admin-pin-submit')?.addEventListener('click', validarPin);

document.getElementById('btn-admin-back')?.addEventListener('click', () => {
    LC.showScreen(LC.getUser() ? 'dashboard' : 'login');
});
document.getElementById('btn-admin-save')?.addEventListener('click', guardarConfiguracionAdmin);
document.getElementById('admin-app-search')?.addEventListener('input', filtrarListaAdmin);
document.getElementById('btn-request-permissions')?.addEventListener('click', async () => {
    await AppBlocker.requestPermissions();
    const ok = await AppBlocker.hasPermissions();
    document.getElementById('admin-permission-warning').classList.toggle('hidden', ok);
});
document.getElementById('btn-admin-change-pin')?.addEventListener('click', () => {
    const nuevo = document.getElementById('admin-new-pin').value.trim();
    if (!nuevo) return;
    localStorage.setItem(PIN_KEY, nuevo);
    AppBlocker.setAdminPin(nuevo);
    document.getElementById('admin-new-pin').value = '';
    alert('PIN actualizado.');
});
document.getElementById('admin-test-mode')?.addEventListener('change', async (e) => {
    await AppBlocker.setTestMode(e.target.checked);
});
document.getElementById('btn-request-battery')?.addEventListener('click', async () => {
    await AppBlocker.requestBatteryExemption();
    // Ese diálogo del sistema a veces no hace que la app se "pause y reanude"
    // como sí pasa con las otras pantallas de permisos, así que el refresco
    // automático por visibilidad puede no dispararse — lo forzamos aparte.
    setTimeout(refrescarEstadoPermisos, 1500);
    setTimeout(refrescarEstadoPermisos, 3500);
});
document.getElementById('btn-request-device-admin')?.addEventListener('click', async () => {
    await AppBlocker.requestDeviceAdmin();
    setTimeout(refrescarEstadoPermisos, 1500);
    setTimeout(refrescarEstadoPermisos, 3500);
});
document.getElementById('btn-child-mode-toggle')?.addEventListener('click', async () => {
    const { active } = await AppBlocker.isChildModeActive();
    // Ya estamos dentro del panel admin (autenticado con PIN), así que
    // apagarlo desde acá no vuelve a pedir el PIN — para eso está el tile.
    await AppBlocker.setChildMode(!active);
    await refrescarEstadoModoNino();
});
document.getElementById('btn-add-qs-tile')?.addEventListener('click', async () => {
    const { supported } = await AppBlocker.requestAddQuickSettingsTile();
    if (!supported) {
        alert('En esta versión de Android no se puede agregar automáticamente. Abrí la barra de notificaciones, tocá "editar accesos rápidos" y arrastrá "Modo niño" desde la lista.');
    }
});
document.getElementById('btn-admin-stop-timer')?.addEventListener('click', async () => {
    if (!confirm('¿Bloquear ahora mismo cualquier app que esté desbloqueada con estrellas?')) return;
    await AppBlocker.cancelAllUnlocks();
    alert('✅ Listo, se detuvo cualquier tiempo desbloqueado.');
});

/* ================= Primera apertura: ir directo a Control Parental ================= */
// Mientras no exista un PIN configurado, consideramos que la app recién se
// instaló: en vez de mostrar el login del niño, forzamos a que un adulto
// configure el control parental primero.
function forzarConfiguracionInicial() {
    if (!pinGuardado()) {
        abrirAdminPin();
    }
}

