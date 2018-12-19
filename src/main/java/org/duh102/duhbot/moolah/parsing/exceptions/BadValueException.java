package org.duh102.duhbot.moolah.parsing.exceptions;

public class BadValueException extends Exception {
    public BadValueException() {
        super();
    }
    public BadValueException(String value) {
        super(value);
    }
    public BadValueException(String value, Throwable source) {
        super(value, source);
    }
}
