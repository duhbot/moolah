package org.duh102.duhbot.moolah.parsing;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.duh102.duhbot.moolah.parsing.exceptions.BadValueException;
import org.duh102.duhbot.moolah.parsing.exceptions.InvalidModifierNameException;
import org.duh102.duhbot.moolah.parsing.exceptions.LeadingZeroesValueException;
import org.duh102.duhbot.moolah.parsing.exceptions.ThousandsSeparatorValueException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShortcutParser {
    public static final ImmutableMap<String, SuffixRepresentation> lookupMap
            = new ImmutableMap.Builder<String, SuffixRepresentation>()
            .putAll(
                    Stream.of(SuffixRepresentation.values())
                    .collect(Collectors.toMap(
                            SuffixRepresentation::getSuffix,
                            Functions.identity()
                    ))
            ).putAll(
                    Stream.of(SuffixRepresentation.values())
                            .collect(Collectors.toMap(
                                    SuffixRepresentation::getFull,
                                    Functions.identity()
                            ))
            ).build();
    public static final Pattern PARSER_PATTERN = Pattern.compile(
                      "(?<neg>-)?"
                    + "(?<value>([1-9][0-9]{0,2}((,[0-9]{3})*|([0-9]{3})*)|0)"
                    + "(\\.[0-9]+)?)"
                    + "([ \t]*(?<modifier>[a-zA-Z]+))?");
    public static final Pattern PROBLEM_LEADING_ZEROES = Pattern.compile(
            "^-?0[^.]");
    public static final Pattern PROBLEM_THOUSANDS_INTERMEDIATE =
            Pattern.compile("(,([0-9]{0,2}|[0-9]{4})|[0-9]{4},)");
    public static BigInteger parseValue(String input) throws BadValueException {
        Matcher match = PARSER_PATTERN.matcher(input);
        if( !match.matches() ) {
            if( PROBLEM_LEADING_ZEROES.matcher(input).find()) {
                throw new LeadingZeroesValueException(input);
            } else if ( PROBLEM_THOUSANDS_INTERMEDIATE.matcher(input).find()) {
                throw new ThousandsSeparatorValueException(input);
            } else {
                throw new BadValueException(input);
            }
        }
        boolean negative = "-".equals(match.group("neg"));
        String rawVal = match.group("value").replaceAll(",", "");
        String lookup = match.group("modifier");
        BigDecimal multiple = new BigDecimal(rawVal);
        BigDecimal multiplier = BigDecimal.ONE;
        if( lookup != null) {
            lookup = lookup.toLowerCase();
            if( !lookupMap.containsKey(lookup) ) {
                throw new InvalidModifierNameException(lookup);
            }
            multiplier = new BigDecimal(lookupMap.get(lookup).getMultiplier());
        }
        if(negative) {
            multiplier = multiplier.negate();
        }
        BigDecimal result =
                multiplier.multiply(multiple);
        return result.toBigInteger();
    }
}
