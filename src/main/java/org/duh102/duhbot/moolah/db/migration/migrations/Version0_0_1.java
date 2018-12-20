package org.duh102.duhbot.moolah.db.migration.migrations;

import org.duh102.duhbot.moolah.db.migration.DatabaseVersion;
import org.duh102.duhbot.moolah.db.migration.Migrator;

import java.sql.*;
import java.util.List;

public class Version0_0_1 implements Migrator {
    public static final DatabaseVersion VERSION = new DatabaseVersion(0,0,1);
    @Override
    public DatabaseVersion getVersion() {
        return VERSION;
    }

    @Override
    public List<DatabaseVersion> getCompatibleBaseVersions() {
        return List.of(DatabaseVersion.UNVERSIONED);
    }

    @Override
    public void upgrade(Connection connection) {
        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS databaseVersion " +
                    "(id INTEGER PRIMARY KEY, major INTEGER, minor INTEGER, " +
                    "patch INTEGER);");
            PreparedStatement stat2 = connection.prepareStatement(
                    "INSERT INTO databaseVersion" +
                            " (major, minor, patch) values (?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            stat2.setInt(1, VERSION.major);
            stat2.setInt(2, VERSION.minor);
            stat2.setInt(3, VERSION.patch);
            stat2.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (!rs.next()) {
                    System.err.println("Wasn't able to insert the version");
                }
            } finally {
                rs.close();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    @Override
    public void downgrade(DatabaseVersion downgradeTo, Connection connection) {

    }

    @Override
    public boolean downgradeCausesDataLoss() {
        return true;
    }
}
