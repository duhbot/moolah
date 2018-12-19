package org.duh102.duhbot.moolah.parsing.exceptions;

public class LeadingZeroesValueException extends BadValueException {
    public LeadingZeroesValueException() {
        super();
    }
    public LeadingZeroesValueException(String value) {
        super(value);
    }
    public LeadingZeroesValueException(String value, Throwable cause) {
        super(value, cause);
    }
}
