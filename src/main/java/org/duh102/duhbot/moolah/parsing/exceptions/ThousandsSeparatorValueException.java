package org.duh102.duhbot.moolah.parsing.exceptions;

public class ThousandsSeparatorValueException extends BadValueException {
    public ThousandsSeparatorValueException() {
        super();
    }
    public ThousandsSeparatorValueException(String value) {
        super(value);
    }
    public ThousandsSeparatorValueException(String value, Throwable cause) {
        super(value, cause);
    }
}
