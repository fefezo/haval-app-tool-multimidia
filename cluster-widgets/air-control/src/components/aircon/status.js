import { stateManager, subscribe } from '../../state.js';
import { div, img, span } from '../../utils/createElement.js';

var dryIcon = "data:image/svg+xml;base64," + btoa('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><path fill="#eaa33e" d="M32 4C32 4 12 28 12 42c0 11 9 18 20 18s20-7 20-18C52 28 32 4 32 4z"/></svg>');

var autoModeIcon = function (fill) {
    return 'data:image/svg+xml;base64,' + btoa('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640"><path fill="' + fill + '" d="M552 256L408 256C398.3 256 389.5 250.2 385.8 241.2C382.1 232.2 384.1 221.9 391 215L437.7 168.3C362.4 109.7 253.4 115 184.2 184.2C109.2 259.2 109.2 380.7 184.2 455.7C259.2 530.7 380.7 530.7 455.7 455.7C463.9 447.5 471.2 438.8 477.6 429.6C487.7 415.1 507.7 411.6 522.2 421.7C536.7 431.8 540.2 451.8 530.1 466.3C521.6 478.5 511.9 490.1 501 501C401 601 238.9 601 139 501C39.1 401 39 239 139 139C233.3 44.7 382.7 39.4 483.3 122.8L535 71C541.9 64.1 552.2 62.1 561.2 65.8C570.2 69.5 576 78.3 576 88L576 232C576 245.3 565.3 256 552 256z"/></svg>');
}

var internalIcon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8BAMAAADI0sRBAAAAJFBMVEV5pf57o/57nvd4ke1bjPJpfupvbH9HcExnlfhejfJhkfU9/+4C6j87AAAADHRSTlMiFQwG/gMBAFrOlQHp+XlJAAAC1ElEQVQ4y32VsU4jMRCGx9IVC5VtURA6777BrvIAJ02RpI3gDUhDfxQ0QUoBVEFKcXRBShHuAWh4uftnbK8dTrqRQGt/nvHMZGZM90l+3a0g11luZPV5T4k+yPL2uhIcuPtK+NdXVh1UpvKJrYQ/75Quh1GmyiN+iHQ4EeERx3t1s2vbth85FbqMsGt7/Osjp1q3B8jSdcLpflWo7oeQD2CLYjKWSTUAeue8T5zGa5UCAboQueCKeqUqwSmnRGVRoJoQ85RoP1JrbTqAC6jcCwV3vmPmt7c/RwP72KRiOYBylvkj9IFTHgOga5hPuBfcZZ/tgedrImqeXj6UB0/RtOi6s0hVcM3cuJYkWUHTQTveElV84TyN6bBnvCiUDPjW0ZisS+Y11fyZ54IV2ubAM9l9UpGD8PSRNKbmA+kQv5pdiktOTgTD7IfubA3RgQu254LDlWq8HSViXhTjdAYcnNg9rska2aid+8GGAjw+GusMWWwsTrEjN+G9NVFoM/sHb9gYq1K0z9fZuDssBMF4dXejyRfXLM+cg13r4Nvo+U74Fb/SJb9agrpql7g1SfwTR95NliprBa9FV6mVUijGGxifABdtql2b8Dv+UlgptiqwDTy/4Kxr6Zsc8Htf8JGcuG4r4/EGhAzXUEMIvPjMvBf6zPwKPSwRmt3UNa66KFVPqBSeWUdcy14yyntUKip8h+sPKEutOQR/xYs1lNFmLfmA8heRpoIPkn91YyY9iloLVpbb1Nf4aWInxh5rtRbnv23pfGNedvO9djBK8XQqJBPSztLfbeYezVKf9DodpAPRKRhYQb/aTIOOniFOOt+maefT0AtxcJ1MyiReDbdDN6TpkGWovnVo6qRVA32c01083+nMp/gA9In1yocO87iTF4HKA9EN378wz08eEChM69fkRmZqPDAdn6jlNC1vVlQ9bfIy1Uu8ZJR39NlLcjuepdV/5S/ikFxpmirRgwAAAABJRU5ErkJggg==";
var externalIcon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8BAMAAADI0sRBAAAAJ1BMVEV6pf97nfh7o/12kO5bjPJlf+leVZZlk/ZHcExejvJgj/P//+0A/9FP0Or/AAAADXRSTlMeDBMG/QMBVQDLlQEBuyPuOwAAAtFJREFUOMt9Vc1q20AQHpaAcU6VdOpttQRMchPCL1AWjHw18RtYl/ZgjCEkTxBoDyEITG+mGIzzALkk51z6Uv1m9kerFvfDyDv7zcyORrMz9Ojw4+21BW4Dlix9PNLj93dZ/cZjdZuAFT7ovX1tv7bRsnaY8hp79NE6rBISWEyFp7a3XNQD8AGgl6uULASBp8GZVeZRVY6ntrcsiqwqooLwBM8LYSORKpAzraLbsiyNccuCaWY9lefG4AeFsoTI1otgCiOQeSkKePBeTT5e2UuRs4OCxJa9emitw1JoPlgkpUhddtba5ufLURk5H9ZZTkIq0pc2oFmzf9BZ9snu4ZJIjWyPZgP7jGCsoUuMg1uM7h5OnicO+t7OmR0HNXjCMQ3OJ45LH+wTjDp+BoCfm5xgrDWYNW2dDw8F/gnWiJp1G8S1jqTGS97bRmiOeotgZ9HSqRzshtg3jU78LsFY+/9r0Mi0I1PfTmsM2uibTtJw6iM7uj+hDdjmZc1JmXv77U4iuLCKDCLeud0OaneMrdu5sIbMtd17n6OuT/ku0FsbgwkxRhrOuzRXo9Q5h6b7bES40OjGPtMk/Q7DFyP7BSp7Ogeh1+fYEZxfn6VReb/+Q+MrbujKxi80hKIDvveVPZ49emboM2oKJT7IKL8LisU+G6SFRSXlEmvc17xCzpHoGemJTbHnjNo9qhDXp8PxKGUtkBqer3HZlDYZas2MxWLj7iFfUgljhksGOnfijinlLvClV8cd44s/OTU7lVx99dA1ezZmGrw2/yCXBkRJa8jzPDNeKtkW3aGSZmXEVexdxvWuoqbat6w8k2bFbMaauXQuik1tANHkvlYXKZ+0x6KoxLryW9yFpdGGjomhQK4ZV9KhK9eXnVTwyHA9ldd1kY6C0M//mhHTab/kabAK3X4aR9Ri6sVlS8lou20Hk44nGYWdpWchuNkjgh9UPb6lwtsfbTVCnXvwUeQAAAAASUVORK5CYII=";

var formatSeconds = function (seconds) {
    if (!seconds || seconds <= 0) return '';
    var m = Math.floor(seconds / 60);
    var s = seconds % 60;
    return m + ':' + (s < 10 ? '0' + s : s);
};

export function createStatusElement() {
    const isAutoOn = stateManager.get('auto') === 1;
    // H6: car.hvac.cycle_mode é INVERTIDO vs AOSP — 1 = ar externo (fresh air), 0 = interna
    // Swap v1.8: cycle=0 (interna) mostra internalIcon (loop dentro da carroceria);
    // cycle=1 (externa) mostra externalIcon (seta entrando de fora).
    const isRecycleIn = stateManager.get('recycle') !== 1;
    const isMaxAuto = stateManager.get('maxauto') === 1;

    var autoModeIconElement = img({
        src: autoModeIcon(isAutoOn ? '#4d9eff' : '#9ca3af'),
        className: 'w-32 h-32',
    });
    var recycleIconElement = img({
        src: isRecycleIn ? internalIcon : externalIcon,
        className: 'w-54 h-54',
    });

    var autoModeLabel = span({
        src: autoModeIcon(isAutoOn ? '#4d9eff' : '#9ca3af'),
        children: ['AUTO'],
    });

    var maxAutoIconElement = span({
        children: ['MAX AUTO ON']
    });

    var statusElement = div({
        className: 'status-element',
        children: [
            div({
                className: 'auto-mode-container',
                children: [
                    autoModeIconElement,
                    autoModeLabel,
                ],
            }),
            div({
                className: 'recycle-mode-container',
                children: [
                    recycleIconElement
                ],
            }),
            div({
                className: `maxauto-icon-container ${isMaxAuto ? '' : 'hidden'}`,
                children: [
                    maxAutoIconElement
                ],
            }),
            div({
                className: 'dry-icon-container hidden',
                children: [
                    span({className: 'dry-badge-icon'}),
                    span({className: 'dry-badge-label', children: ['SECAGEM']}),
                    span({className: 'dry-badge-countdown'})
                ],
            }),
        ],
    })

    var unsubscribeAuto = subscribe('auto', function(newAuto) {
        const isAutoOn = newAuto === 1;
        autoModeIconElement.src = autoModeIcon(isAutoOn ? '#4d9eff' : '#3a3f4c');
        autoModeLabel.className = isAutoOn ? 'text-white' : 'text-gray';
    });

    var unsubscribeRecycle = subscribe('recycle', function(newRecycle) {
        // H6: cycle_mode 1 = ar externo (fresh air), 0 = interna
        const isRecycleIn = newRecycle !== 1;
        recycleIconElement.src = isRecycleIn ? internalIcon : externalIcon;
    });

    // Circulation is a focusable control (OK cycles fan -> temp -> circulation):
    // highlight the recycle icon the same way the fan/temp areas highlight.
    const recycleContainer = statusElement.querySelector('.recycle-mode-container');
    const isRecycleFocused = stateManager.get('focusArea') === 'recycle';
    if (isRecycleFocused) {
        recycleContainer.classList.add('transition-all', 'focus-active');
    }
    var unsubscribeRecycleFocus = subscribe('focusArea', function(newFocusArea) {
        const isFocused = newFocusArea === 'recycle';
        if (isFocused) {
            recycleContainer.classList.add('transition-all', 'focus-active');
            recycleContainer.classList.remove('focus-inactive');
        } else {
            recycleContainer.classList.add('focus-inactive');
            recycleContainer.classList.remove('transition-all', 'focus-active');
        }
    });

    const maxAutoContainer = statusElement.querySelector('.maxauto-icon-container');
    var unsubscribeMaxAuto = subscribe('maxauto', function(newMaxAuto) {
        const isMaxAuto = newMaxAuto === 1;
        maxAutoContainer.classList.toggle('hidden', !isMaxAuto);
    });

    const dryBadgeElement = statusElement.querySelector('.dry-icon-container');
    const dryCountdownElement = statusElement.querySelector('.dry-badge-countdown');
    var unsubscribeDry = subscribe('dry', function(newDry) {
        const active = newDry > 0;
        dryBadgeElement.classList.toggle('hidden', !active);
        dryCountdownElement.textContent = active ? formatSeconds(newDry) : '';
    });

    statusElement.cleanup = function() {
        unsubscribeAuto();
        unsubscribeRecycle();
        unsubscribeRecycleFocus();
        unsubscribeMaxAuto();
        unsubscribeDry();
    };

    return statusElement;
}