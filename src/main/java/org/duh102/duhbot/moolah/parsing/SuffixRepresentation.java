package org.duh102.duhbot.moolah.parsing;

import java.math.BigInteger;

public enum SuffixRepresentation {
    THOUSAND(1, "thousand", "k"),
    MILLION(2, "million", "mil"),
    BILLION(3, "billion", "bil"),
    TRILLION(4, "trillion", "tril"),
    QUADRILLION(5, "quadrillion", "quadril"),
    QUINTILLION(6, "quintillion", "quintil"),
    SEXTILLION(7, "sextillion", "sextil"),
    SEPTILLION(8, "septillion", "septil"),
    OCTILLION(9, "octillion", "octil"),
    NONILLION(10, "nonillion", "nonil"),
    DECILLION(11, "decillion", "decil"),
    UNDECILLION(12, "undecillion", "undecil"),
    DUODECILLION(13, "duodecillion", "duodecil"),
    TREDECILLION(14, "tredecillion", "tredecil");

    private int magnitude;
    private BigInteger multiplier;
    private String full, suffix;

    SuffixRepresentation(int magnitude, String full, String suffix) {
        this.magnitude = magnitude;
        this.multiplier =
                (new BigInteger("1000")).pow(magnitude);
        this.full = full;
        this.suffix = suffix;
    }

    public String toString() {
        return full.substring(0,1).toUpperCase()+full.substring(1).toLowerCase();
    }
    public int getMagnitude() {
        return magnitude;
    }
    public BigInteger getMultiplier() {
        return multiplier;
    }
    public String getFull() {
        return full;
    }
    public String getSuffix() {
        return suffix;
    }
}
