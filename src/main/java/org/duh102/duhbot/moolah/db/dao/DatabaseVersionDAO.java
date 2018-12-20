package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.migration.DatabaseVersion;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.sql.*;

public class DatabaseVersionDAO {
    private BankDB database;
    public DatabaseVersionDAO(BankDB database) {
        this.database = database;
    }

    public DatabaseVersion getVersion() throws RecordFailure {
        Connection conn = database.getDBConnection();
        DatabaseVersion version = null;
        try {
            PreparedStatement stat = conn.prepareStatement(
                    "SELECT major, minor, patch " +
                        "FROM databaseVersion ORDER BY id LIMIT 1;");
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                int major = rs.getInt("major");
                int minor = rs.getInt("minor");
                int patch = rs.getInt("patch");
                version = new DatabaseVersion(major, minor, patch);
            }
            rs.close();
            return version;
        } catch( SQLException sqle ) {
            //may want to check for SQLITE_CONSTRAINT(19) here
            throw new RecordFailure(sqle);
        }
    }

    public DatabaseVersion setVersion(DatabaseVersion toSet) throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement(
                    "INSERT INTO databaseVersion" +
                    " (major, minor, patch) values (?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            stat.setInt(1, toSet.major);
            stat.setInt(2, toSet.minor);
            stat.setInt(3, toSet.patch);
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (rs.next()) {
                    return toSet;
                }
                else {
                    throw new RecordFailure("No generated key");
                }
            } finally {
                rs.close();
            }
        } catch( SQLException sqle ) {
            //may want to check for SQLITE_CONSTRAINT(19) here
            throw new RecordFailure(sqle);
        }
    }
}
