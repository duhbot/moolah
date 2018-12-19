package org.duh102.duhbot.moolah.parsing.exceptions;

public class InvalidModifierNameException extends BadValueException {
    public InvalidModifierNameException() {
        super();
    }
    public InvalidModifierNameException(String value) {
        super(value);
    }
    public InvalidModifierNameException(String value, Throwable cause) {
        super(value, cause);
    }
}
