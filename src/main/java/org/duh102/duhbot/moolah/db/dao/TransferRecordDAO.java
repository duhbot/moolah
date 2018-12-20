package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.TransferRecord;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;

import java.sql.*;

public class TransferRecordDAO {
    private BankDB database;
    public TransferRecordDAO(BankDB database) {
        this.database = database;
    }

    public TransferRecord recordTransfer(TransferRecord record) throws RecordFailure {
        return recordTransfer(record.uidSource, record.uidDestination, record.amount, record.timestamp);
    }
    public TransferRecord recordTransfer(long uidSource, long uidDestination, long amount, Timestamp timestamp) throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO transferRecord (uidSource, uidDest, amount, timestamp) values (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            stat.setLong(1, uidSource);
            stat.setLong(2, uidDestination);
            stat.setLong(3, amount);
            stat.setString(4, LocalTimestamp.format(timestamp));
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            try {
                if (rs.next()) {
                    long genOID = rs.getLong(1);
                    TransferRecord outcome = new TransferRecord(genOID, uidSource, uidDestination, amount, timestamp);
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
