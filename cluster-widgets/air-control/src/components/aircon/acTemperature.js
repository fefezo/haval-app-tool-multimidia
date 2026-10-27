import { stateManager, subscribe } from '../../state.js';
import { createFocusElementWithChildren } from './focusElement.js';
import { div } from '../../utils/createElement.js';
import { updateProgressRings } from './mainAcControl.js'
import { bumpValue, createRockerHint } from './rocker.js';

export function createTemperatureElement() {

    var tempDisplay = div({
        className: 'temp-display-label font-bold',
        children: [
            stateManager.get('temp') + '°C',
        ],
    });

    // Mesma pista do fan: a temperatura sobe/desce com a tecla do volante, entao
    // os chevrons mostram a direcao e o valor entra deslizando do lado certo.
    var rocker = createRockerHint();

    var focusArea = createFocusElementWithChildren({
        className: 'ac-temp-control-area',
        focused: stateManager.get('focusArea') === 'temp',
        children: [
            div({
                className: 'text-white text-center',
                children: [div({
                    className: 'text-20 text-gray',
                    children: [
                        'Temp',
                    ],
                }), tempDisplay],
            }),
            rocker,
        ]
    });

    var unsubscribeFocus = subscribe('focusArea', function(newFocusArea) {
        var isFocused = newFocusArea === 'temp';

        // Apply base styles with browser prefixes
        //focusArea.className = 'focus-area focus-area-small';

        if (isFocused) {
            focusArea.classList.add('transition-all', 'focus-active');
            focusArea.classList.remove('focus-inactive');
        } else {
            focusArea.classList.add('focus-inactive');
            focusArea.classList.remove('transition-all', 'focus-active');
        }
    });

    var previousTemp = stateManager.get('temp');
    var unsubscribeTemp = subscribe('temp', function(newTemp) {
        var direction = Number(newTemp) >= Number(previousTemp) ? 'up' : 'down';
        previousTemp = newTemp;
        if (newTemp == 16) {
            tempDisplay.textContent = 'Low';
        } else if (newTemp == 32) {
            tempDisplay.textContent = 'High';
        } else {
            tempDisplay.textContent = newTemp + '°C';
        }
        updateProgressRings();
        rocker.flash(direction);
        bumpValue(tempDisplay, direction);
    });

    // Add cleanup method to the element
    focusArea.cleanup = function() {
        unsubscribeFocus();
        unsubscribeTemp();
        rocker.cleanup();
    };

    return focusArea;
}
