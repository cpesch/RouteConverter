const ecmaScriptInfo = (function() {
    function getESEdition() {
        const array = [];
        switch (true) {
            case !Array.isArray:
                return 3;
            case !window.Promise:
                return 5;
            case !array.includes:
                return 6;
            case !''.padStart:
                return 7;
            case !Promise.prototype.finally:
                return 8;
            case !window.BigInt:
                return 9;
            case !Promise.allSettled:
                return 10;
            case !''.replaceAll:
                return 11;
            case !array.at:
                return 12;
            default:
                return 13;
        }
    }

    function getESYear(edition) {
        return {
            3: 1999,
            5: 2009
        }[edition] || (2009 + edition);
    }

    const edition = getESEdition();
    const year = getESYear(edition);

    return {
        edition: edition, // usually shortened [edition,]
        year: year,       // usually shortened [year,]
        text: 'ECMAScript Edition: '+ edition +', Year: '+ year
    }
})();

// since there is no console in JavaFX, redirect output to #error
if (typeof console != "undefined")
    if (typeof console.log != 'undefined')
        console.olog = console.log;
    else
        console.olog = function () {};
console.log = function (message) {
    console.olog(message);
    alert(message);
    $("#error").append('<p>' + message + '</p>');
};
console.error = console.debug = console.info = console.log;

function isInitialized() {
    return true;
}

function resize(width, height) {
    var div = document.getElementById("map");
    div.style.width = width + "px";
    div.style.height = height + "px";
}

var callbackQueue = [];

function getCallbacks() {
    var callbacks = callbackQueue.join("--");
    callbackQueue = [];
    return callbacks;
}

