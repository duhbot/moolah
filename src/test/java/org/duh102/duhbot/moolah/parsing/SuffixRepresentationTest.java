package org.duh102.duhbot.moolah.parsing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuffixRepresentationTest {
    private static Stream<Arguments> expectedValueStream() {
        return Stream.of(SuffixRepresentation.values())
                .map((val) -> Arguments.of(
                        val,
                        (new BigInteger("1000")).pow(val.getMagnitude())
                ));
    }
    @ParameterizedTest(name = "{index} Multiplier of {0} == {1}")
    @MethodSource("expectedValueStream")
    public void testExpectedMultiplier(SuffixRepresentation value,
                                       BigInteger expOutput) {
        assertEquals(expOutput, value.getMultiplier());
    }
}
