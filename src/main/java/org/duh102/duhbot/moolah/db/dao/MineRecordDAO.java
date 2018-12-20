package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.MineRecord;
import org.duh102.duhbot.moolah.exceptions.InvalidDBConfiguration;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.sql.*;

public class MineRecordDAO {
    private BankDB database;
    public MineRecordDAO(BankDB database) {
        this.database = database;
    }

    public MineRecord recordMineOutcome(MineRecord record) throws RecordFailure {
        return recordMineOutcome(record.uid, record.mineFractions, record.richness, record.yield, record.timestamp);
    }
    public MineRecord recordMineOutcome(long uid, int mineFractions, double richness, long yield, Timestamp timestamp) throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO mineOutcome (uid, mineFractions, richness, yield, timestamp) values (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            stat.setLong(1, uid);
            stat.setInt(2, mineFractions);
            stat.setDouble(3, richness);
            stat.setLong(4, yield);
            stat.setString(5, LocalTimestamp.format(timestamp));
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (rs.next()) {
                    long genOID = rs.getLong(1);
                    MineRecord outcome = new MineRecord(genOID, uid, mineFractions, richness, yield, timestamp);
                    return outcome;
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
