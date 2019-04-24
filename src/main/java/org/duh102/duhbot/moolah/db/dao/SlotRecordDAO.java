package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.SlotReelImage;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.SlotRecord;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.math.BigInteger;
import java.sql.*;

public class SlotRecordDAO {
    private BankDB database;
    public SlotRecordDAO(BankDB database) {
        this.database = database;
    }

    public SlotRecord recordSlotRecord(SlotRecord record) throws RecordFailure {
        return recordSlotRecord(record.uid, record.slotImages, record.wager, record.payout, record.multiplier, record.timestamp);
    }
    public SlotRecord recordSlotRecord(long uid, SlotReelImage[] slotState,
                                       BigInteger wager, BigInteger payout,
                                       double multiplier, Timestamp timestamp) throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO slotOutcome (uid, slotImages, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            stat.setLong(1, uid);
            stat.setString(2, SlotRecord.getRegexString(slotState));
            stat.setString(3, wager.toString());
            stat.setString(4, payout.toString());
            stat.setDouble(5, multiplier);
            stat.setString(6, LocalTimestamp.format(timestamp));
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (rs.next()) {
                    long genOID = rs.getLong(1);
                    SlotRecord outcome = new SlotRecord(genOID, uid, slotState, wager, payout, multiplier, timestamp);
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
