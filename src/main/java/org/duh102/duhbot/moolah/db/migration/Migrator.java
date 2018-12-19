package org.duh102.duhbot.moolah.db.migration;

public interface Migrator {
    DatabaseVersion getVersion();
}
