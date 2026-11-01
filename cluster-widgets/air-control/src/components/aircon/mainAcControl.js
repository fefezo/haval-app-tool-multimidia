import { stateManager, subscribe } from '../../state.js';
import { div, span } from '../../utils/createElement.js';
import { bumpValue } from './rocker.js';

// Tela do A/C no cluster — layout "trilho" (variante B aprovada).
//
// Quatro cartoes lado a lado na ordem em que o OK avanca:
// ventilador -> temperatura -> circulacao -> modo padrao.
// O cartao em foco cresce e ganha contorno branco; o que esta ligado fica azul.
// As temperaturas externa/interna sao leituras passivas na base, fora do ciclo.
//
// O estado continua vindo do Kotlin pelos mesmos canais (control/focus):
// - fan: 0..7          - temp: 16..32 (passo 0.5)
// - power/auto/aion: 0|1
// - recycle: 0 = interna, 1 = externa (o H6 inverte o cycle_mode)
// - outside_temp/inside_temp: inteiros ja arredondados no app
var STROKE = 'stroke="currentColor" stroke-width="4.6" stroke-linecap="round" stroke-linejoin="round"';

var ICONS = {
    fan: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><path d="M10 20h27a9 9 0 1 0-9-9"/><path d="M10 32h35a9 9 0 1 1-9 9"/><path d="M10 44h22a8 8 0 1 1-8 8"/></g></svg>',
    thermo: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><rect x="26" y="6" width="12" height="31" rx="6"/><circle cx="32" cy="46" r="10"/><path d="M32 20v22"/></g></svg>',
    recirc: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><path d="M50 25a20 20 0 1 0 2 17"/><path d="M50 11v14H36"/></g></svg>',
    fresh: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><path d="M34 14h15a8 8 0 0 1 8 8v20a8 8 0 0 1-8 8H34"/><path d="M4 32h26"/><path d="M22 22l10 10-10 10"/></g></svg>',
    car: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><path d="M10 44V35l5-10c1.2-2.5 3.5-4 6.2-4h21.6c2.7 0 5 1.5 6.2 4l5 10v9"/><path d="M10 44h44"/><circle cx="20" cy="44" r="3.2"/><circle cx="44" cy="44" r="3.2"/></g></svg>',
    check: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><circle cx="32" cy="32" r="22"/><path d="M21 32l7.5 8L47 21"/></g></svg>',
    power: '<svg viewBox="0 0 64 64" fill="none"><g ' + STROKE + '><path d="M32 8v20"/><path d="M19 16a19 19 0 1 0 26 0"/></g></svg>',
    auto: '<svg viewBox="0 0 640 640"><path fill="currentColor" d="M552 256L408 256C398.3 256 389.5 250.2 385.8 241.2C382.1 232.2 384.1 221.9 391 215L437.7 168.3C362.4 109.7 253.4 115 184.2 184.2C109.2 259.2 109.2 380.7 184.2 455.7C259.2 530.7 380.7 530.7 455.7 455.7C463.9 447.5 471.2 438.8 477.6 429.6C487.7 415.1 507.7 411.6 522.2 421.7C536.7 431.8 540.2 451.8 530.1 466.3C521.6 478.5 511.9 490.1 501 501C401 601 238.9 601 139 501C39.1 401 39 239 139 139C233.3 44.7 382.7 39.4 483.3 122.8L535 71C541.9 64.1 552.2 62.1 561.2 65.8C570.2 69.5 576 78.3 576 88L576 232C576 245.3 565.3 256 552 256z"/></svg>',
    drop: '<svg viewBox="0 0 64 64"><path fill="currentColor" d="M32 6C32 6 14 27 14 40a18 18 0 0 0 36 0C50 27 32 6 32 6z"/></svg>',
    sun: '<svg viewBox="0 0 64 64" fill="none"><circle cx="32" cy="32" r="11" stroke="currentColor" stroke-width="4"/><g stroke="currentColor" stroke-width="4" stroke-linecap="round"><path d="M32 8v8"/><path d="M32 48v8"/><path d="M8 32h8"/><path d="M48 32h8"/><path d="M15 15l5.7 5.7"/><path d="M43.3 43.3L49 49"/><path d="M49 15l-5.7 5.7"/><path d="M20.7 43.3L15 49"/></g></svg>',
    chev: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 15l6-6 6 6" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>',
};

function icon(name, className) {
    var element = span({ className: 'b-ic ' + (className || '') });
    element.setAttribute('aria-hidden', 'true');
    element.innerHTML = ICONS[name];
    return element;
}

function toNumber(value, fallback) {
    var parsed = Number(value);
    return isNaN(parsed) ? fallback : parsed;
}

function tempText(value) {
    var temp = toNumber(value, NaN);
    if (isNaN(temp)) {
        return '--';
    }
    if (temp >= 32) {
        return 'HI';
    }
    if (temp <= 16) {
        return 'LO';
    }
    return temp.toFixed(1) + '°';
}

function sensorText(value) {
    if (value === '-' || value === '' || value === null || value === undefined) {
        return '--';
    }
    var temp = toNumber(value, NaN);
    return isNaN(temp) ? '--' : Math.round(temp) + '°C';
}

function formatSeconds(seconds) {
    if (!seconds || seconds <= 0) {
        return '';
    }
    var minutes = Math.floor(seconds / 60);
    var rest = seconds % 60;
    return minutes + ':' + (rest < 10 ? '0' + rest : rest);
}

// Chevrons de cima/baixo. O volante manda o valor, nao a tecla: a direcao vem
// da comparacao entre o valor anterior e o novo. So aparecem no cartao em foco.
function createChevrons() {
    var root = span({ className: 'b-chev' });
    root.setAttribute('aria-hidden', 'true');

    var up = span({ className: 'b-chev-ico b-chev-up' });
    var down = span({ className: 'b-chev-ico b-chev-down' });
    up.innerHTML = ICONS.chev;
    down.innerHTML = ICONS.chev;
    root.appendChild(up);
    root.appendChild(down);

    var upTimer = 0;
    var downTimer = 0;

    root.flash = function (direction) {
        var element = direction === 'up' ? up : down;
        var timer = direction === 'up' ? upTimer : downTimer;
        if (timer) {
            clearTimeout(timer);
        }
        element.classList.remove('pressed');
        // Reflow: duas mudancas seguidas no mesmo sentido precisam reiniciar
        // a animacao, senao o segundo toque passa despercebido.
        void element.offsetWidth;
        element.classList.add('pressed');
        var newTimer = setTimeout(function () {
            element.classList.remove('pressed');
        }, 320);
        if (direction === 'up') {
            upTimer = newTimer;
        } else {
            downTimer = newTimer;
        }
    };

    root.cleanup = function () {
        if (upTimer) {
            clearTimeout(upTimer);
        }
        if (downTimer) {
            clearTimeout(downTimer);
        }
    };

    return root;
}

export function createAcControlScreen() {
    var main = document.createElement('main');
    main.className = 'main-container b-screen';

    // --- barra de status: energia e AUTO ---------------------------------
    var powerIcon = icon('power', 'b-st');
    var autoIcon = icon('auto', 'b-st');
    var statusRail = div({ className: 'b-status', children: [powerIcon, autoIcon] });
    statusRail.setAttribute('aria-hidden', 'true');

    // --- leituras passivas do carro, na base ------------------------------
    var outsideValue = span({ className: 'b-sensor-val', children: [sensorText(stateManager.get('outside_temp'))] });
    var insideValue = span({ className: 'b-sensor-val', children: [sensorText(stateManager.get('inside_temp'))] });
    var sensors = div({ className: 'b-sensors', children: [
        span({ className: 'b-sensor', children: [icon('sun', 'b-sensor-ico'), outsideValue] }),
        span({ className: 'b-sensor', children: [icon('car', 'b-sensor-ico'), insideValue] }),
    ] });
    sensors.setAttribute('aria-hidden', 'true');

    // --- cartao do ventilador ---------------------------------------------
    var fanValue = span({ className: 'b-val', children: [String(stateManager.get('fan') || 0)] });
    var fanIcon = icon('fan', 'b-ic');
    var fanChevrons = createChevrons();
    var fanCard = div({ className: 'b-card', children: [fanIcon, fanValue, fanChevrons] });

    // --- cartao da temperatura (ajuste, nao a medida da cabine) -----------
    var tempValue = span({ className: 'b-val b-temp', children: [tempText(stateManager.get('temp'))] });
    var tempIcon = icon('thermo', 'b-ic on');
    var tempChevrons = createChevrons();
    var tempCard = div({ className: 'b-card is-on', children: [tempIcon, tempValue, tempChevrons] });

    // --- cartao da circulacao: os dois icones, o ativo em azul -------------
    var insideIcon = icon('recirc', 'b-ic b-pair-ico');
    var outsideIcon = icon('fresh', 'b-ic b-pair-ico');
    var circPair = span({ className: 'b-pair', children: [insideIcon, outsideIcon] });
    var circChevrons = createChevrons();
    var circCard = div({ className: 'b-card is-on', children: [circPair, circChevrons] });

    // --- cartao do modo padrao --------------------------------------------
    var defaultChevrons = createChevrons();
    var defaultCard = div({ className: 'b-card', children: [icon('check', 'b-ic neutral'), defaultChevrons] });

    var rail = div({ className: 'b-rail', children: [fanCard, tempCard, circCard, defaultCard] });

    // --- badges de estado: MAX AUTO e secagem -----------------------------
    var maxAutoBadge = div({ className: 'b-badge b-maxauto hidden', children: [
        icon('auto', 'b-badge-ico'),
        span({ className: 'b-badge-text', children: ['MAX AUTO'] }),
    ] });
    var dryCountdown = span({ className: 'b-dry-countdown' });
    var dryBadge = div({ className: 'b-badge b-dry hidden', children: [
        icon('drop', 'b-badge-ico'),
        span({ className: 'b-badge-text', children: ['SECAGEM'] }),
        dryCountdown,
    ] });

    // --- flash verde quando o perfil padrao e aplicado --------------------
    var applyFlash = div({ className: 'b-flash-green' });
    applyFlash.setAttribute('aria-hidden', 'true');
    var applyFlashTimer = 0;

    function flashDefaultApplied() {
        if (stateManager.get('focusArea') !== 'default') {
            return;
        }
        if (applyFlashTimer) {
            clearTimeout(applyFlashTimer);
        }
        applyFlash.classList.remove('is-active');
        void applyFlash.offsetWidth;
        applyFlash.classList.add('is-active');
        applyFlashTimer = setTimeout(function () {
            applyFlash.classList.remove('is-active');
            applyFlashTimer = 0;
        }, 600);
    }

    main.appendChild(statusRail);
    main.appendChild(sensors);
    main.appendChild(rail);
    main.appendChild(maxAutoBadge);
    main.appendChild(dryBadge);
    main.appendChild(applyFlash);

    // --- foco: o cartao em foco cresce e ganha contorno branco ------------
    var focusKeyFor = function (focusArea) {
        // O Kotlin manda 'recycle'; 'circ' fica aceito para estados antigos.
        if (focusArea === 'circ') {
            return 'recycle';
        }
        return focusArea;
    };

    var cardsByKey = {
        fan: fanCard,
        temp: tempCard,
        recycle: circCard,
        default: defaultCard,
    };

    function renderFocus(focusArea) {
        var activeKey = focusKeyFor(focusArea);
        Object.keys(cardsByKey).forEach(function (key) {
            cardsByKey[key].classList.toggle('is-focused', key === activeKey);
        });
    }

    // --- ventilador -------------------------------------------------------
    var previousFan = toNumber(stateManager.get('fan'), 0);

    function renderFan() {
        var fan = toNumber(stateManager.get('fan'), 0);
        var fanOn = toNumber(stateManager.get('power'), 0) === 1 && fan > 0;
        var ionOn = fanOn && toNumber(stateManager.get('aion'), 0) === 1;
        var tone = fanOn ? (ionOn ? 'aion' : 'on') : 'off';

        fanCard.classList.toggle('is-on', fanOn);
        fanIcon.className = 'b-ic ' + tone;
        fanValue.className = 'b-val ' + tone;
        fanValue.textContent = String(fan);
    }

    // --- temperatura ------------------------------------------------------
    var previousTemp = toNumber(stateManager.get('temp'), 0);

    function renderTemp() {
        tempValue.textContent = tempText(stateManager.get('temp'));
    }

    // --- circulacao (H6: recycle 1 = externa, 0 = interna) ----------------
    var circFlipTimer = 0;

    function renderCirculation(animate) {
        var internal = toNumber(stateManager.get('recycle'), 0) !== 1;
        insideIcon.className = 'b-ic b-pair-ico ' + (internal ? 'on' : 'off');
        outsideIcon.className = 'b-ic b-pair-ico ' + (internal ? 'off' : 'on');

        if (!animate) {
            return;
        }
        if (circFlipTimer) {
            clearTimeout(circFlipTimer);
        }
        circPair.classList.remove('flip');
        void circPair.offsetWidth;
        circPair.classList.add('flip');
        circFlipTimer = setTimeout(function () {
            circPair.classList.remove('flip');
            circFlipTimer = 0;
        }, 260);
    }

    // --- status -----------------------------------------------------------
    function renderStatus() {
        var powered = toNumber(stateManager.get('power'), 0) === 1;
        var autoOn = powered && toNumber(stateManager.get('auto'), 0) === 1;
        powerIcon.classList.toggle('on', powered);
        autoIcon.classList.toggle('on', autoOn);
    }

    // --- montagem do estado inicial --------------------------------------
    renderFocus(stateManager.get('focusArea'));
    renderFan();
    renderTemp();
    renderCirculation(false);
    renderStatus();
    maxAutoBadge.classList.toggle('hidden', toNumber(stateManager.get('maxauto'), 0) !== 1);
    dryBadge.classList.toggle('hidden', !(toNumber(stateManager.get('dry'), 0) > 0));
    dryCountdown.textContent = formatSeconds(toNumber(stateManager.get('dry'), 0));

    var unsubscribe = [];

    unsubscribe.push(subscribe('focusArea', renderFocus));

    unsubscribe.push(subscribe('fan', function (value) {
        var fan = toNumber(value, previousFan);
        var direction = fan >= previousFan ? 'up' : 'down';
        previousFan = fan;
        renderFan();
        bumpValue(fanValue, direction);
        fanChevrons.flash(direction);
        flashDefaultApplied();
    }));

    unsubscribe.push(subscribe('temp', function (value) {
        var temp = toNumber(value, previousTemp);
        var direction = temp >= previousTemp ? 'up' : 'down';
        previousTemp = temp;
        renderTemp();
        bumpValue(tempValue, direction);
        tempChevrons.flash(direction);
        flashDefaultApplied();
    }));

    unsubscribe.push(subscribe('recycle', function () {
        renderCirculation(true);
        flashDefaultApplied();
    }));

    unsubscribe.push(subscribe('power', function () {
        renderFan();
        renderStatus();
        flashDefaultApplied();
    }));

    unsubscribe.push(subscribe('auto', function () {
        renderStatus();
        flashDefaultApplied();
    }));

    unsubscribe.push(subscribe('aion', renderFan));

    unsubscribe.push(subscribe('maxauto', function (value) {
        maxAutoBadge.classList.toggle('hidden', toNumber(value, 0) !== 1);
    }));

    unsubscribe.push(subscribe('dry', function (value) {
        var seconds = toNumber(value, 0);
        dryBadge.classList.toggle('hidden', !(seconds > 0));
        dryCountdown.textContent = formatSeconds(seconds);
    }));

    unsubscribe.push(subscribe('outside_temp', function (value) {
        outsideValue.textContent = sensorText(value);
    }));

    unsubscribe.push(subscribe('inside_temp', function (value) {
        insideValue.textContent = sensorText(value);
    }));

    var chevrons = [fanChevrons, tempChevrons, circChevrons, defaultChevrons];

    main.cleanup = function () {
        unsubscribe.forEach(function (fn) {
            fn();
        });
        chevrons.forEach(function (chevron) {
            if (chevron && chevron.cleanup) {
                chevron.cleanup();
            }
        });
        if (applyFlashTimer) {
            clearTimeout(applyFlashTimer);
        }
        if (circFlipTimer) {
            clearTimeout(circFlipTimer);
        }
    };

    return main;
}
