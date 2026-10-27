// Pistas visuais de "mudar com cima/baixo" para o widget do cluster.
//
// O volante nao manda a direcao da tecla para o widget: ele manda o VALOR que
// resultou dela. Por isso a direcao e derivada comparando o valor anterior com o
// novo dentro de cada componente. O que se ganha: o chevron correspondente acende
// quando o valor sobe/desce, e o numero entra deslizando do lado de onde veio.

export function createRockerHint() {
    var root = document.createElement('div');
    root.className = 'rocker-hint';
    root.setAttribute('aria-hidden', 'true');

    var up = document.createElement('span');
    up.className = 'rocker-chevron rocker-chevron-up';

    var down = document.createElement('span');
    down.className = 'rocker-chevron rocker-chevron-down';

    root.appendChild(up);
    root.appendChild(down);

    var upTimer = 0;
    var downTimer = 0;

    // `direction` e 'up' ou 'down'. O timeout existe para o brilho nao ficar
    // aceso quando o motorista segura a tecla e o valor muda varias vezes.
    root.flash = function (direction) {
        var element = direction === 'up' ? up : down;
        var timer = direction === 'up' ? upTimer : downTimer;

        if (timer) {
            clearTimeout(timer);
        }
        element.classList.remove('pressed');
        // Reflow: sem isso, duas mudancas seguidas na mesma direcao nao
        // reiniciam a transicao e o segundo toque passa despercebido.
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

// Animacao curta no proprio valor. A direcao e a mesma dos chevrons: subiu,
// entra de baixo para cima; desceu, entra de cima para baixo.
export function bumpValue(element, direction) {
    if (!element) {
        return;
    }
    var upClass = 'value-bump-up';
    var downClass = 'value-bump-down';
    var className = direction === 'up' ? upClass : downClass;

    if (element._bumpTimer) {
        clearTimeout(element._bumpTimer);
    }
    element.classList.remove(upClass, downClass);
    void element.offsetWidth;
    element.classList.add(className);

    element._bumpTimer = setTimeout(function () {
        element.classList.remove(className);
        element._bumpTimer = 0;
    }, 260);
}
