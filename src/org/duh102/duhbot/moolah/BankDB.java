package org.duh102.duhbot.moolah;

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
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankaccount (user TEXT, channel TEXT, server TEXT, balance INTEGER, balancefractional INTEGER, lastmined INTEGER);");
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
