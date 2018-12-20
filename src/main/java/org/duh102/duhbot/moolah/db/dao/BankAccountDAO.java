package org.duh102.duhbot.moolah.db.dao;

import org.duh102.duhbot.moolah.BankAccount;
import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.exceptions.*;

import java.sql.*;
import java.text.ParseException;

public class BankAccountDAO {
    private BankDB database;
    public BankAccountDAO(BankDB database) {
        this.database = database;
    }

    public BankAccount openAccount(String user) throws AccountAlreadyExists, RecordFailure {
        return openAccount(user, 0l);
    }

    public BankAccount openAccount(String user, long balance) throws AccountAlreadyExists, RecordFailure {
        Connection conn = database.getDBConnection();
        BankAccount account = null;
        account = getAccount(user);
        if( account != null )
            throw new AccountAlreadyExists(String.format("User %s registered as %d", user, account.uid));

        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO bankAccount (user, balance) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
            stat.setString(1, user.toLowerCase());
            stat.setLong(2, balance);
            stat.executeUpdate();
            ResultSet rs = stat.getGeneratedKeys();
            if (rs.next()) {
                long genUID = rs.getLong(1);
                account = getAccount(genUID);
            }
            else {
                try {
                    account = getAccountExcept(user);
                } catch( AccountDoesNotExist adne ) {
                    throw new RecordFailure(adne);
                }
            }
            rs.close();
            return account;
        } catch( SQLException sqle ) {
            throw new RecordFailure(sqle);
        }
    }

    public BankAccount getAccountExcept(String user) throws AccountDoesNotExist, RecordFailure {
        Connection conn = database.getDBConnection();
        BankAccount account = getAccount(user);
        if( account == null)
            throw new AccountDoesNotExist(user);
        return account;
    }
    public BankAccount getAccount(String user) throws RecordFailure {
        Connection conn = database.getDBConnection();
        BankAccount account = null;
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT uid, balance, lastMined FROM bankAccount where user = ? LIMIT 1;");
            stat.setString(1, user.toLowerCase());
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                long uid = rs.getLong("uid");
                long balance = rs.getLong("balance");
                Timestamp lastMined = null;
                try {
                    lastMined = LocalTimestamp.parse(rs.getString("lastMined"));
                } catch( ParseException pe ) {
                    //If we can't read the last mined timestamp, just set it to now
                    lastMined = LocalTimestamp.now();
                }
                try {
                    account = new BankAccount(uid, user.toLowerCase(), balance, lastMined);
                } catch( ImproperBalanceAmount iba ) {
                    //this should never happen, since we're grabbing from the database
                    throw new RecordFailure(iba);
                }
            }
            rs.close();
            return account;
        } catch( SQLException sqle ) {
            //may want to check for SQLITE_CONSTRAINT(19) here
            throw new RecordFailure(sqle);
        }
    }

    public BankAccount getAccountExcept(long uid) throws AccountDoesNotExist, RecordFailure {
        Connection conn = database.getDBConnection();
        BankAccount account = getAccount(uid);
        if( account == null)
            throw new AccountDoesNotExist(String.format("%d", uid));
        return account;
    }
    public BankAccount getAccount(long uid) throws RecordFailure {
        Connection conn = database.getDBConnection();
        BankAccount account = null;
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT user, balance, lastMined FROM bankAccount where uid = ? LIMIT 1;");
            stat.setLong(1, uid);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                String user = rs.getString("user");
                long balance = rs.getLong("balance");
                Timestamp lastMined = null;
                try {
                    lastMined = LocalTimestamp.parse(rs.getString("lastMined"));
                } catch( ParseException pe ) {
                    //If we can't read the last mined timestamp, just set it to now
                    lastMined = LocalTimestamp.now();
                }
                try {
                    account = new BankAccount(uid, user, balance, lastMined);
                } catch( ImproperBalanceAmount iba ) {
                    //this should never happen, since we're grabbing from the database
                    throw new RecordFailure(iba);
                }
            }
            rs.close();
            return account;
        } catch( SQLException sqle ) {
            //may want to check for SQLITE_CONSTRAINT(19) here
            throw new RecordFailure(sqle);
        }
    }

    // Sync an account to the database
    public BankAccount pushAccount(BankAccount account) throws RecordFailure, AccountDoesNotExist {
        Connection conn = database.getDBConnection();
        BankAccount dbAccount = getAccountExcept(account.uid);
        try {
            PreparedStatement stat = conn.prepareStatement("UPDATE bankAccount SET user = ?, balance = ?, lastMined = ? WHERE uid = ?;");
            stat.setString(1, account.user);
            stat.setLong(2, account.balance);
            stat.setString(3, LocalTimestamp.format(account.lastMined));
            stat.setLong(4, account.uid);
            stat.executeUpdate();
        } catch( SQLException sqle ) {
            throw new RecordFailure(sqle);
        }
        return account;
    }


    // Returns the sum total of all accounts on record
    public long getAccountTotal() throws RecordFailure {
        Connection conn = database.getDBConnection();
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT sum(balance) AS total FROM bankAccount;");
            ResultSet rs = stat.executeQuery();
            try {
                if (rs.next()) {
                    return rs.getLong("total");
                } else
                    throw new RecordFailure("Could not get total");
            } finally {
                rs.close();
            }
        } catch( SQLException sqle ) {
            throw new RecordFailure(sqle);
        }
    }
}
