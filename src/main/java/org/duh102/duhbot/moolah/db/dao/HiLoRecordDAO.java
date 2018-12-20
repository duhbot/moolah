package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.HiLoBetType;
import org.duh102.duhbot.moolah.db.HiLoRecord;
import org.duh102.duhbot.moolah.exceptions.InvalidDBConfiguration;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.sql.*;

public class HiLoRecordDAO {
    private BankDB database;
    public HiLoRecordDAO(BankDB database) {
        this.database = database;
    }

    public HiLoRecord recordHiLoRecord(HiLoRecord record) throws RecordFailure {
        return recordHiLoRecord(record.uid, record.resultInt, record.hiLo, record.wager, record.payout, record.multiplier, record.timestamp);
    }
    public HiLoRecord recordHiLoRecord(long uid, int resultInt, HiLoBetType hiLo, long wager, long payout, double multiplier, Timestamp timestamp) throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO hiLoOutcome (uid, resultInt, hiLo, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            stat.setLong(1, uid);
            stat.setInt(2, resultInt);
            stat.setString(3, hiLo.toString());
            stat.setLong(4, wager);
            stat.setLong(5, payout);
            stat.setDouble(6, multiplier);
            stat.setString(7, LocalTimestamp.format(timestamp));
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (rs.next()) {
                    long genOID = rs.getLong(1);
                    HiLoRecord outcome = new HiLoRecord(genOID, uid, resultInt, hiLo, wager, payout, multiplier, timestamp);
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
