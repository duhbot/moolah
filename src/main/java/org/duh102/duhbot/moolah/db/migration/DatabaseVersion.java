package org.duh102.duhbot.moolah.db.migration;

import org.duh102.duhbot.moolah.Pair;
import org.duh102.duhbot.moolah.exceptions.InvalidInputError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseVersion implements Comparable<DatabaseVersion> {
    public final int major, minor, patch;
    public static final Pattern VERSION_PATTERN = Pattern.compile(
            "(?<major>[1-9][0-9]*|0)\\." +
            "(?<minor>[1-9][0-9]*|0)\\." +
            "(?<patch>[1-9][0-9]*|0)"
    );

    public DatabaseVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    public DatabaseVersion(String versionString) throws InvalidInputError {
        Matcher match = VERSION_PATTERN.matcher(versionString);
        if(!match.matches()) {
            throw new InvalidInputError(String.format("Version string %s " +
                    "invalid, must be (int).(int).(int)", versionString));
        }
        String major = match.group("major"), minor = match.group("minor"),
                patch = match.group("patch");
        this.major = Integer.parseInt(major);
        this.minor = Integer.parseInt(minor);
        this.patch = Integer.parseInt(patch);
    }

    @Override
    public int compareTo(DatabaseVersion other) {
        return 0;
    }

    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
