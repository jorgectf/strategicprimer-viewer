import strategicprimer.model.idreg {
    DuplicateIDException
}

"A slightly-customizable warning-handling interface."
shared interface Warning of ignore|warn|die|CustomWarningHandler {
    "Handle a warning, e.g. if a particular map-format construct is deprecated."
    shared formal void handle(Throwable warning);
}
shared object warningLevels {
    shared Warning ignore => package.ignore;
    shared Warning warn => package.warn;
    shared Warning die => package.die;
    shared Warning custom(Anything(Throwable)|Anything(String) handler =
            process.writeLine) => CustomWarningHandler(handler);
    "The default warning handler. This is provided so that it can in theory be changed
     later in one place rather than everywhere."
    shared Warning default => warn;
}
"Don't do anything with warnings."
shared object ignore satisfies Warning {
    shared actual void handle(Throwable warning) {}
}
"Log each warning, but let them pass."
shared object warn satisfies Warning {
    shared actual void handle(Throwable warning) {
        if (is SPFormatException|DuplicateIDException warning) {
            log.warn(warning.message);
        } else {
            log.warn("Warning: ", warning);
        }
    }
}
"Treat warnings as errors."
shared object die satisfies Warning {
    shared actual void handle(Throwable warning) {
        throw warning;
    }
}
"A warning handler that takes a user-provided handler."
shared class CustomWarningHandler(
        Anything(Throwable)|Anything(String) handler = process.writeLine)
        satisfies Warning {
    shared actual void handle(Throwable warning);
    if (is Anything(Throwable) handler) {
        handle = handler;
    } else {
        handle = (Throwable warning) { // TODO: Convert from lambda to named member function
            if (is SPFormatException warning) {
                handler("SP format warning: ``warning.message``");
            } else {
                handler("Warning: ``warning.message``");
            }
        };
    }
}
