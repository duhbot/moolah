package org.duh102.duhbot.moolah;

import java.sql.*;
import java.util.*;

public class BankDB {
  private static String dbfile = "moolah.db";
  private static BankDB instance;
  static {
    instance = new BankDB();
  }

  private BankDB(){
    createTables();
  }

  public static BankDB getInstance(){
    return instance;
  }

  public static void createTables()
  {
    Connection conn = getDBConnection();
    if(conn != null)
    {
      try
      {
        Statement stat = conn.createStatement();
        //This table is not safe for networks with no nickserv or usage on several
        //  networks (with different nickserv databases)! We only store the registered nick
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankAccount (uid INTEGER PRIMARY KEY, user TEXT UNIQUE NOT NULL, balance INTEGER NOT NULL DEFAULT 0, lastMined INTEGER NOT NULL DEFAULT (strftime('%s', datetime('now', '-1 day'))) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS slotOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, slotImages TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS hiLoOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERNECES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, resultInt INTEGER NOT NULL, hiLo TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS mineOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, mineFractions INTEGER NOT NULL, richness REAL NOT NULL, yield INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        conn.close();
      }
      catch(java.sql.SQLException sqle)
      {
        sqle.printStackTrace();
      }
    }
  }
  
  public static Connection getDBConnection()
  {
    Connection conn = null;
    try
    {
      try
      {
        Class.forName("org.sqlite.JDBC");
      }
      catch(java.lang.ClassNotFoundException cnfe)
      {
        cnfe.printStackTrace();
      }
      conn = DriverManager.getConnection("jdbc:sqlite:" + dbfile);
    }
    catch(java.sql.SQLException sqle)
    {
      sqle.printStackTrace();
    }
    return conn;
  }
}
