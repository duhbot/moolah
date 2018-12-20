package org.duh102.duhbot.moolah.db.migration;

import com.google.common.collect.ImmutableList;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.dao.DatabaseVersionDAO;
import org.duh102.duhbot.moolah.exceptions.InvalidDBConfiguration;
import org.duh102.duhbot.moolah.exceptions.InvalidEnvironment;
import org.duh102.duhbot.moolah.db.migration.migrations.*;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MigrationManager {
    public static final ImmutableList<Migrator>  ALL_MIGRATORS =
            new ImmutableList.Builder<Migrator>().addAll(List.of(
                    new Version0_0_0(),
                    new Version0_0_1()
            )).build();
    public static final DatabaseVersion HIGHEST_VERSION =
            ALL_MIGRATORS.stream().map(Migrator::getVersion).reduce((a, b) -> a.compareTo(b) > 0 ? a : b).get();

    BankDB database;
    DatabaseVersionDAO databaseVersionDAO;
    public MigrationManager(BankDB database) {
        this.database = database;
        databaseVersionDAO = new DatabaseVersionDAO(database);
    }
    public MigrationManager(String databaseFile) {
        // we're going to manage the database state ourselves, so don't
        // create the tables by default, and use our intended database file
        this(BankDB.getDBInstance(databaseFile));
    }

    public List<Migrator> getQuickestMigrationPath(DatabaseVersion targetVersion) {
        List<Migrator> plan = new ArrayList<>();
        DatabaseVersion currentVersion = getCurrentVersion();
        return plan;
    }

    public void createOrUpgradeToLatest() throws RecordFailure {
        Connection conn = database.getDBConnection();
    }

    public DatabaseVersion getCurrentVersion() {
        DatabaseVersion version = null;
        try {
            version = databaseVersionDAO.getVersion();
        } catch (RecordFailure recordFailure) {
            Throwable cause = recordFailure.getCause();
            if( cause instanceof SQLException && ((SQLException)cause).getErrorCode() ==  942) {
                version = DatabaseVersion.UNVERSIONED;
            } else {
                recordFailure.printStackTrace();
            }
        }
        return version;
    }
}
