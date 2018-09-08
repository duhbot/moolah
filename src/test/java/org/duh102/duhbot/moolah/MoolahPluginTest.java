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
      MineRecord record = plugin.doMine(event);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "mined");
      assertContains(response, String.format("%,d %s", record.yield, MoolahPlugin.currFull));
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
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, transfer));
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

  /*
   * Slot machine
   */
  @Test
  public void testSlots() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      SlotRecord record = plugin.doSlots(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response, record.getImagesString());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsNoWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l;
    String[] arguments = new String[]{};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.doSlots(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, MoolahPlugin.commandPrefix);
      assertContains(response, MoolahPlugin.slotsComm);
      assertContains(response, "[wager]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsNegativeWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = -100l;
    String[] arguments = new String[]{String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.doSlots(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsTextWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l;
    String[] arguments = new String[]{"asdf"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.doSlots(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsInsufficientFunds() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal+2;
    String[] arguments = new String[]{String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.doSlots(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient funds");
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, wager));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsNoAccount() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doSlots(event, arguments);
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
   * HiLo betting
   */
  @Test
  public void testHiLoLow() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response.toLowerCase(), type.toLowerCase());
      assertContains(response, String.format("%d", record.resultInt));
      assertContains(response, String.format("%d", HiLoRecord.MID));
      assertContains(response, (record.payout > 0)?"<":">=");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoEqual() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "equal";
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response.toLowerCase(), type.toLowerCase());
      assertContains(response, String.format("%d", record.resultInt));
      assertContains(response, String.format("%d", HiLoRecord.MID));
      assertContains(response, (record.payout > 0)?"==":"!=");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoHigh() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response.toLowerCase(), type.toLowerCase());
      assertContains(response, String.format("%d", record.resultInt));
      assertContains(response, String.format("%d", HiLoRecord.MID));
      assertContains(response, (record.payout > 0)?">":"<=");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoNoWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l;
    String[] arguments = new String[]{type};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, MoolahPlugin.commandPrefix);
      assertContains(response, MoolahPlugin.hiLoComm);
      assertContains(response, "[h(igh)|l(ow)|e(qual)]");
      assertContains(response, "[wager]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoNoType() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, "[h(igh)|l(ow)|e(qual)]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoNoArgs() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l;
    String[] arguments = new String[]{};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, MoolahPlugin.commandPrefix);
      assertContains(response, MoolahPlugin.hiLoComm);
      assertContains(response, "[h(igh)|l(ow)|e(qual)]");
      assertContains(response, "[wager]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoNegativeWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    long initialBal = 123484l, wager = -10l;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoTextWager() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    long initialBal = 123484l;
    String[] arguments = new String[]{type, "asdf"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoInvalidType() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "notatype";
    long initialBal = 123484l, wager = initialBal/4;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "invalid");
      assertContains(response, type);
      assertContains(response, "[h(igh)|l(ow)|e(qual)]");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoInsufficientFunds() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    long initialBal = 123484l, wager = initialBal+2;
    String[] arguments = new String[]{type, String.format("%d", wager)};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient funds");
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, wager));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoNoAccount() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    String[] arguments = new String[]{type, "10"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      HiLoRecord record = plugin.doHiLo(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response, username);
    } finally {
      conn.rollback();
    }
  }

  /*
   * onMessage tests
   */
  @Test
  public void testBalanceOnMessage() throws Exception {
    String username = fakeUser1.getNick();
    long bal = 102314l;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s", MoolahPlugin.commandPrefix, MoolahPlugin.balanceComm)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.onMessage(event);
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
  public void testMineOnMessage() throws Exception {
    String username = fakeUser1.getNick();
    long initialBal = 101l;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s", MoolahPlugin.commandPrefix, MoolahPlugin.mineComm)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onMessage(event);
      MineRecord record = (MineRecord)plugin.lastHandled;
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "mined");
      assertContains(response, String.format("%,d %s", record.yield, MoolahPlugin.currFull));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testOpenOnMessage() throws Exception {
    String username = fakeUser1.getNick();
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s", MoolahPlugin.commandPrefix, MoolahPlugin.openComm)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.onMessage(event);
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
  public void testTransferOnMessage() throws Exception {
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = initialBal1/4;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.transferComm, username2, transfer
        )
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount(username1, initialBal1);
      BankAccount dest = db.openAccount(username2, initialBal2);
      plugin.onMessage(event);
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
  public void testSlotsOnMessage() throws Exception {
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/4;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.slotsComm, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onMessage(event);
      SlotRecord record = (SlotRecord)plugin.lastHandled;
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response, record.getImagesString());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoLowOnMessage() throws Exception {
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l, wager = initialBal/4;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.hiLoComm, type, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db, true);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onMessage(event);
      HiLoRecord record = (HiLoRecord)plugin.lastHandled;
      acct = db.getAccountExcept(acct.uid);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, acct.balance));
      assertContains(response.toLowerCase(), String.format("%.2fx payout", record.multiplier));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, record.payout));
      assertContains(response.toLowerCase(), type.toLowerCase());
      assertContains(response, String.format("%d", record.resultInt));
      assertContains(response, String.format("%d", HiLoRecord.MID));
      assertContains(response, (record.payout > 0)?"<":">=");
    } finally {
      conn.rollback();
    }
  }
}
