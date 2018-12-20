package org.duh102.duhbot.moolah.db.migration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseVersionTest {
    private static Stream<Arguments> goodParseSource() {
        return Stream.of(
                Arguments.of("1.1.1", new DatabaseVersion(1,1,1)),
                Arguments.of("0.0.0", new DatabaseVersion(0,0,0)),
                Arguments.of("1.2.3", new DatabaseVersion(1,2,3)),
                Arguments.of("10.20.30", new DatabaseVersion(10,20,30)),
                Arguments.of("1.20.3", new DatabaseVersion(1,20,3)),
                Arguments.of("1.2.30", new DatabaseVersion(1,2,30)),
                Arguments.of("10.2.3", new DatabaseVersion(10,2,3)),
                Arguments.of("0.2.3", new DatabaseVersion(0,2,3))
        );
    }
    @ParameterizedTest(name = "{index} parse({0}) == {1}")
    @MethodSource("goodParseSource")
    public void testGoodParse(String input, DatabaseVersion expOutput) throws Exception {
        assertEquals(expOutput, new DatabaseVersion(input));
    }

    private static Stream<Arguments> orderingSource() {
        return Stream.of(
                Arguments.of("1.1.1", "1.1.1", 0),
                Arguments.of("1.2.3", "1.2.3", 0),
                Arguments.of("3.2.1", "3.2.1", 0),
                Arguments.of("1.1.2", "1.1.1", 1),
                Arguments.of("1.1.1", "1.1.2", -1),
                Arguments.of("1.2.1", "1.1.1", 1),
                Arguments.of("1.1.1", "1.2.1", -1),
                Arguments.of("2.1.1", "1.1.1", 1),
                Arguments.of("1.1.1", "2.1.1", -1),
                Arguments.of("1.2.1", "1.1.2", 1),
                Arguments.of("1.1.2", "1.2.1", -1),
                Arguments.of("2.1.1", "1.2.3", 1),
                Arguments.of("1.2.3", "2.1.1", -1),
                Arguments.of("2.0.0", "1.200.300", 1),
                Arguments.of("1.200.300", "2.0.0", -1)
        );
    }
    @ParameterizedTest(name = "{index} {0}.compareTo({1}) == {2}")
    @MethodSource("orderingSource")
    public void testOrdering(String input1,
                             String input2, int expOutput) throws Exception {
        assertEquals(expOutput,
                new DatabaseVersion(input1).compareTo(new DatabaseVersion(input2)));
    }
}
