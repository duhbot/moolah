package org.duh102.duhbot.moolah;

import java.sql.Connection;

import org.duh102.duhbot.moolah.exceptions.*;

import org.pircbotx.PircBotX;
import org.pircbotx.Channel;
import org.pircbotx.UserHostmask;
import org.pircbotx.User;
import org.pircbotx.Configuration;
import org.pircbotx.hooks.managers.GenericListenerManager;
import org.pircbotx.hooks.*;
import org.pircbotx.hooks.events.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MoolahPluginTest {
  /*
   * Mock stuff
   */

  Configuration.Builder config = new Configuration.Builder()
				.addServer("127.1.1.1")
				.setListenerManager(new GenericListenerManager())
				.setName("TestBot")
				.setMessageDelay(0)
				.setShutdownHookEnabled(false)
				.setAutoReconnect(false)
				.setCapEnabled(false);
  PircBotX fakeBot = new PircBotX(config.buildConfiguration());

  private class FakeUserHostmask extends UserHostmask {
    public FakeUserHostmask(String nick, String user, String host, String unk) {
      super(fakeBot, nick, user, host, unk);
    }
  }
  private class FakeUser extends User {
    public FakeUser(FakeUserHostmask mask) {
      super(mask);
    }
  }
  private class FakeChannel extends Channel {
    public FakeChannel(String channelName) {
      super(fakeBot, channelName);
    }
  }
  FakeUserHostmask fakeUserHost1 = new FakeUserHostmask("aFakeUser", "aRealname", "aPlace.net", ""),
                   fakeUserHost2 = new FakeUserHostmask("anotherFakeUser", "anotherRealname", "anotherPlace.net", "");
  FakeUser fakeUser1 = new FakeUser(fakeUserHost1), fakeUser2 = new FakeUser(fakeUserHost2);
  FakeChannel fakeChannel = new FakeChannel("achannel");
  private class FakeMessageEvent extends MessageEvent {
    private String response = "";
    public FakeMessageEvent(User user, UserHostmask userMask, String message) {
      super(fakeBot, fakeChannel, "channelsource", userMask, user, message, null);
    }
    public void respond(String response) {
      this.response = response;
    }
    public String getResponse() {
      return response;
    }
  }

  public static void assertContains(String container, String containee) {
    assertTrue(container.contains(containee), String.format("expected: '%s' to contain '%s' but did not", container, containee));
  }
  public static void assertContains(String container, String containee, String message) {
    assertTrue(container.contains(containee), message);
  }

  /*
   * Message responses, test they have the required info
   */

  @Test
  public void testParseCommand() throws Exception {
    String command = "a b  c\td \te\t \tf \t g";
    String[] expected = new String[] {"a","b","c","d","e","f","g"};
    assertArrayEquals(expected, MoolahPlugin.parseCommand(command));
  }
  @Test
  public void testRespondEvent() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String text = "This is a tEst Message 123456";
    MoolahPlugin.respondEvent(event, text);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, text);
    assertContains(response, MoolahPlugin.messagePrefix);
  }
  @Test
  public void testReplyGenericError() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    MoolahPlugin.replyGenericError(event);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "unknown error");
  }
  @Test
  public void testReplyDBError() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    MoolahPlugin.replyDBError(event);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "unknown database error");
  }
  @Test
  public void testReplyDBUnreachableError() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    MoolahPlugin.replyDBUnreachableError(event);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "database is unreachable");
  }
  @Test
  public void testReplyUseHelp() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    MoolahPlugin.replyUseHelp(event);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "cannot be used alone");
  }
  @Test
  public void testReplyUnknownCommand() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String fakeCommand = "abcdcommand";
    MoolahPlugin.replyUnknownCommand(event, fakeCommand);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "unknown command");
    assertContains(response.toLowerCase(), fakeCommand);
  }
  @Test
  public void testReplyBalance() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    long balance = 11298l;
    String username = "aSillyUser";
    BankAccount acct = new BankAccount(0l, username, balance, LocalTimestamp.now());
    MoolahPlugin.replyBalance(event, acct);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response, username);
    assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, balance));
  }
  @Test
  public void testReplyInsufficientFunds() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    long balance = 121350l, required = 2341235l;
    String username = "aSillyUser";
    BankAccount acct = new BankAccount(0l, username, balance, LocalTimestamp.now());
    MoolahPlugin.replyInsufficientFunds(event, acct, required);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, balance));
    assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, required));
  }
  @Test
  public void testReplyNoAccount() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = "aSillyUser";
    MoolahPlugin.replyNoAccount(event, username);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertContains(response, MoolahPlugin.messagePrefix);
    assertContains(response.toLowerCase(), "does not have");
    assertContains(response.toLowerCase(), "bank account");
    assertContains(response, username);
  }

  /*
   * Database propagation and error checking
   */
  @Test
  public void test() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
    } finally {
      conn.rollback();
    }
  }

  /*
   * Balance checking
   */
  @Test
  public void testBalanceSameUser() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String[] arguments = new String[]{};
    String username = fakeUser1.getNick();
    long bal = 102314l;

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, username);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, bal));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceOtherUser() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser2.getNick();
    String[] arguments = new String[]{username};
    long bal = 12387l;

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, username);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, bal));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceNoUser() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    String[] arguments = new String[]{username};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceNoOtherUser() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser2.getNick();
    String[] arguments = new String[]{username};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username);
    } finally {
      conn.rollback();
    }
  }

  /*
   * Mining
   */
  @Test
  public void testMine() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 101l;

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.doMine(event);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "mined");
      assertContains(response, String.format("%,d %s", acct.balance - initialBal, MoolahPlugin.currFull));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testMineTooSoon() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username);
      acct.lastMined = LocalTimestamp.now();
      db.pushAccount(acct);
      plugin.doMine(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, "too soon");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testMineNoAccount() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doMine(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
    } finally {
      conn.rollback();
    }
  }

  /*
   * Open account
   */
  @Test
  public void testOpen() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doOpen(event);
      BankAccount acct = db.getAccountExcept(username);
      assertEquals(0l, acct.balance);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "account opened");
      assertContains(response, username);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testOpenDouble() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username);
      plugin.doOpen(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, username);
      assertContains(response.toLowerCase(), "already exists");
    } finally {
      conn.rollback();
    }
  }

  /*
   * Transfers
   */
  @Test
  public void testTransfer() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1/4;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      source = db.getAccountExcept(source.uid);
      dest = db.getAccountExcept(dest.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s transferred", username1));
      assertEquals(initialBal1 - transfer, source.balance);
      assertEquals(initialBal2 + transfer, dest.balance);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, transfer));
      assertContains(response, String.format("to %s", username2));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal1-transfer));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal2+transfer));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferNoArgs() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String[] arguments = new String[]{};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, MoolahPlugin.commandPrefix);
      assertContains(response, MoolahPlugin.transferComm);
      assertContains(response, "[destination]");
      assertContains(response, "[amount]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferBadAmountNotInt() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l;
    String[] arguments = new String[]{username2, "asdf"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferBadAmountNegative() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = -100l;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferBadAmountTooBig() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1+2;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient funds");
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal1));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferMissingArg() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l;
    String[] arguments = new String[]{username2};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, MoolahPlugin.commandPrefix);
      assertContains(response, MoolahPlugin.transferComm);
      assertContains(response, "[destination]");
      assertContains(response, "[amount]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferNoDestination() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1/4;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username2);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferSource() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1/4;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username2, initialBal2);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username1);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferBoth() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1/4;
    String[] arguments = new String[]{username2, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username1);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferSameAccount() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick();
    long initialBal1 = 123484l, transfer = initialBal1/4;
    String[] arguments = new String[]{username1, String.format("%d", transfer)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "same account");
    } finally {
      conn.rollback();
    }
  }
}
