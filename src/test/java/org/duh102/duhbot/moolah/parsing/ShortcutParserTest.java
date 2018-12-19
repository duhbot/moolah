package org.duh102.duhbot.moolah.parsing;

import org.duh102.duhbot.moolah.parsing.exceptions.BadValueException;
import org.duh102.duhbot.moolah.parsing.exceptions.InvalidModifierNameException;
import org.duh102.duhbot.moolah.parsing.exceptions.LeadingZeroesValueException;
import org.duh102.duhbot.moolah.parsing.exceptions.ThousandsSeparatorValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShortcutParserTest {
    private static Stream<Arguments> goodParseSource() {
        return Stream.of(
                Arguments.of("1", new BigInteger("1")),
                Arguments.of("-1", new BigInteger("-1")),
                Arguments.of("10", new BigInteger("10")),
                Arguments.of("-10", new BigInteger("-10")),
                Arguments.of("100", new BigInteger("100")),
                Arguments.of("1000", new BigInteger("1000")),
                Arguments.of("1,000", new BigInteger("1000")),
                Arguments.of("-0.111", new BigInteger("0")),
                Arguments.of("1.111", new BigInteger("1")),
                Arguments.of("1k", new BigInteger("1000")),
                Arguments.of("1 k", new BigInteger("1000")),
                Arguments.of("1.1k", new BigInteger("1100")),
                Arguments.of("1.1 k", new BigInteger("1100")),
                Arguments.of("1.1     k", new BigInteger("1100")),
                Arguments.of("10k", new BigInteger("10000")),
                Arguments.of("11k", new BigInteger("11000")),
                Arguments.of("11.1k", new BigInteger("11100")),
                Arguments.of("1,000,000", new BigInteger("1000000")),
                Arguments.of("1,234,567", new BigInteger("1234567")),
                Arguments.of("9,876,543", new BigInteger("9876543")),
                Arguments.of("-11.1k", new BigInteger("-11100")),
                Arguments.of("1.1111111k", new BigInteger("1111")),
                Arguments.of("9.9999999k", new BigInteger("9999")),
                Arguments.of("1mil", new BigInteger("1000000")),
                Arguments.of("1MIL", new BigInteger("1000000")),
                Arguments.of("1MiL", new BigInteger("1000000")),
                Arguments.of("1million", new BigInteger("1000000")),
                Arguments.of("1MiLlIoN", new BigInteger("1000000")),
                Arguments.of("1 million", new BigInteger("1000000")),
                Arguments.of("1 bil", new BigInteger(
                        "1000000000")),
                Arguments.of("1 tril", new BigInteger(
                        "1000000000000")),
                Arguments.of("1 quadril", new BigInteger(
                        "1000000000000000")),
                Arguments.of("1 quintil", new BigInteger(
                        "1000000000000000000")),
                Arguments.of("1 sextil", new BigInteger(
                        "1000000000000000000000")),
                Arguments.of("1 septil", new BigInteger(
                        "1000000000000000000000000")),
                Arguments.of("1 octil", new BigInteger(
                        "1000000000000000000000000000")),
                Arguments.of("1 nonil", new BigInteger(
                        "1000000000000000000000000000000")),
                Arguments.of("1 decil", new BigInteger(
                        "1000000000000000000000000000000000")),
                Arguments.of("1 undecil", new BigInteger(
                        "1000000000000000000000000000000000000")),
                Arguments.of("1 duodecil", new BigInteger(
                        "1000000000000000000000000000000000000000")),
                Arguments.of("1 tredecil", new BigInteger(
                        "1000000000000000000000000000000000000000000")),
                Arguments.of("1 billion",new BigInteger(
                        "1000000000")),
                Arguments.of("1 trillion", new BigInteger(
                        "1000000000000")),
                Arguments.of("1 quadrillion", new BigInteger(
                        "1000000000000000")),
                Arguments.of("1 quintillion", new BigInteger(
                        "1000000000000000000")),
                Arguments.of("1 sextillion", new BigInteger(
                        "1000000000000000000000")),
                Arguments.of("1 septillion", new BigInteger(
                        "1000000000000000000000000")),
                Arguments.of("1 octillion", new BigInteger(
                        "1000000000000000000000000000")),
                Arguments.of("1 nonillion", new BigInteger(
                        "1000000000000000000000000000000")),
                Arguments.of("1 decillion", new BigInteger(
                        "1000000000000000000000000000000000")),
                Arguments.of("1 undecillion", new BigInteger(
                        "1000000000000000000000000000000000000")),
                Arguments.of("1 duodecillion", new BigInteger(
                        "1000000000000000000000000000000000000000")),
                Arguments.of("1 tredecillion", new BigInteger(
                        "1000000000000000000000000000000000000000000"))
        );
    }

    @ParameterizedTest(name = "{index} Value of {0} == {1}")
    @MethodSource("goodParseSource")
    public void testGoodParse(String input, BigInteger expOutput) throws Exception {
        assertEquals(expOutput, ShortcutParser.parseValue(input));
    }
    private static Stream<Arguments> badParseSource() {
        return Stream.of(
                Arguments.of("", new BadValueException()),
                Arguments.of("a", new BadValueException()),
                Arguments.of("1,0", new ThousandsSeparatorValueException()),
                Arguments.of("1,000000",
                        new ThousandsSeparatorValueException()),
                Arguments.of("1,0000",
                        new ThousandsSeparatorValueException()),
                Arguments.of("1000,000",
                        new ThousandsSeparatorValueException()),
                Arguments.of("100000,0",
                        new ThousandsSeparatorValueException()),
                Arguments.of("1 hexadecimal",
                        new InvalidModifierNameException()),
                Arguments.of("1 1",
                        new BadValueException()),
                Arguments.of("010", new LeadingZeroesValueException()),
                Arguments.of("010.0", new LeadingZeroesValueException()),
                Arguments.of("000001", new LeadingZeroesValueException())
        );
    }
    @ParameterizedTest(name = "{index} Parsing {0} should throw {1}")
    @MethodSource("badParseSource")
    public void testBadParse(String input, Exception expectedThrow) {
        assertThrows(expectedThrow.getClass(),
                () -> ShortcutParser.parseValue(input));
    }
}
