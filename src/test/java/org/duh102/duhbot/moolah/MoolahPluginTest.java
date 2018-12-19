package org.duh102.duhbot.moolah;

import java.util.HashMap;
import java.sql.Connection;
import java.io.IOException;

import org.duh102.duhbot.moolah.db.*;
import org.duh102.duhbot.moolah.exceptions.*;

import org.pircbotx.PircBotX;
import org.pircbotx.UserChannelDao;
import org.pircbotx.Channel;
import org.pircbotx.InputParser;
import org.pircbotx.UserHostmask;
import org.pircbotx.User;
import org.pircbotx.Configuration;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.hooks.managers.GenericListenerManager;
import org.pircbotx.hooks.events.*;
import org.pircbotx.exception.IrcException;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoolahPluginTest {
  /*
   * Mock stuff
   */
  String[] fakeRawLines = new String[] {
    ":irc.testserv.net 332 TestBot #achannel :This is a motd!",
    ":irc.testserv.net 333 TestBot #achannel theboss!~theboss@boss.place 1531359304",
    ":irc.testserv.net 353 TestBot @ #achannel :NoPerms!~NoPerms@noplace.net @FakeUser1!~FakeUser1@aplace.net @FakeUser2!~FakeUser2@someplace.net",
    ":irc.testserv.net 366 TestBot #achannel :End of /NAMES list.",
    ":irc.testserv.net 324 TestBot #achannel +npstz ",
    ":irc.testserv.net 329 TestBot #achannel 1325392953",
    ":irc.testserv.net 352 TestBot #achannel ~NoPerms noplace.net * NoPerms H :0 NoPermsReal",
    ":irc.testserv.net 352 TestBot #achannel ~FakeUser1 aplace.net * FakeUser1 Hr@ :0 FakeUser1Real",
    ":irc.testserv.net 352 TestBot #achannel ~FakeUser2 someplace.net * FakeUser2 Hr@ :0 FakeUser2Real",
    ":irc.testserv.net 315 TestBot #achannel :End of /WHO list."
  };

  Configuration.Builder config = new Configuration.Builder()
				.addServer("127.1.1.1")
				.setListenerManager(new GenericListenerManager())
				.setName("TestBot")
				.setMessageDelay(new StaticDelay(0))
				.setShutdownHookEnabled(false)
				.setAutoReconnect(false)
				.setCapEnabled(false);
  PircBotX fakeBot = new PircBotX(config.buildConfiguration());

  UserHostmask fakeUserHost1 = null,
                   fakeUserHost2 = null,
                   noPermsUserHost = null;
  User fakeUser1 = null,
           fakeUser2 = null,
           noPermsUser = null;
  Channel fakeChannel = null;
  @BeforeAll
  void initAll() {
    UserChannelDao fakeDao = fakeBot.getUserChannelDao();
    fakeDao.createChannel("#achannel");
    fakeDao.createChannel("#bchannel");
    InputParser inputParser = fakeBot.getInputParser();
    for( String line : fakeRawLines ) {
      try {
        inputParser.handleLine(line);
      } catch( IOException | IrcException ioe ) {
        ioe.printStackTrace();
      }
    }
    fakeUser1 = fakeDao.getUser("FakeUser1");
    fakeUser2 = fakeDao.getUser("FakeUser2");
    noPermsUser = fakeDao.getUser("NoPerms");
    fakeUserHost1 = (UserHostmask)fakeUser1;
    fakeUserHost2 = (UserHostmask)fakeUser2;
    noPermsUserHost = (UserHostmask)noPermsUser;
    fakeChannel = fakeDao.getChannel("#achannel");
  }
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
  private class FakePrivateMessageEvent extends PrivateMessageEvent {
    private String response = "";
    public FakePrivateMessageEvent(User user, UserHostmask userMask, String message) {
      super(fakeBot, userMask, user, message, null);
    }
    public void respond(String response) {
      this.response = response;
    }
    public void respondWith(String response) {
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
   * User permissions levels
   */
  @Test
  public void testGetUserReg() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    assertEquals(fakeUser1.getNick().toLowerCase(), plugin.getUserReg(fakeChannel, fakeUser1));
    assertEquals(fakeUser2.getNick().toLowerCase(), plugin.getUserReg(fakeChannel, fakeUser2));
    assertThrows(InsufficientPrivilegesException.class, () -> {
      plugin.getUserReg(fakeChannel, noPermsUser);
    });
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
    assertContains(response.toLowerCase(), username.toLowerCase());
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
    assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username.toLowerCase());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceSameUserNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String[] arguments = new String[]{};
    String username = noPermsUser.getNick();
    long bal = 102314l;

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
      assertContains(response, "insufficient permissions");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceOtherUserNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username = fakeUser2.getNick();
    String[] arguments = new String[]{username};
    long bal = 12387l;

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, bal));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testBalanceNoOtherUserNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username = fakeUser2.getNick();
    String[] arguments = new String[]{username};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doBalance(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response.toLowerCase(), username.toLowerCase());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testOpenDouble() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username);
      plugin.doOpen(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
      assertContains(response.toLowerCase(), "already exists");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testOpenNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username = noPermsUser.getNick();

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doOpen(event);
      assertThrows(AccountDoesNotExist.class, () -> {
        db.getAccountExcept(username);
      });
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient permissions");
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response, String.format("%s transferred", username1.toLowerCase()));
      assertEquals(initialBal1 - transfer, source.balance);
      assertEquals(initialBal2 + transfer, dest.balance);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, transfer));
      assertContains(response, String.format("to %s", username2.toLowerCase()));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal1-transfer));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal2+transfer));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testTransferParsed() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l, transfer = 1100l;
    String[] arguments = new String[]{username2, "1.1k"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response, String.format("%s transferred", username1.toLowerCase()));
      assertEquals(initialBal1 - transfer, source.balance);
      assertEquals(initialBal2 + transfer, dest.balance);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, transfer));
      assertContains(response, String.format("to %s", username2.toLowerCase()));
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  public void testTransferBadAmountParsedWayTooBig() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
    long initialBal1 = 123484l, initialBal2 = 24587l;
    String[] arguments = new String[]{username2, "1 tredecil"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    public void testTransferBadAmountZero() throws Exception {
        FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
        String username1 = fakeUser1.getNick(), username2 = fakeUser2.getNick();
        long initialBal1 = 123484l, initialBal2 = 24587l, transfer = 0l;
        String[] arguments = new String[]{username2, String.format("%d", transfer)};

        BankDB db = BankDB.getMemoryInstance();
        MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response.toLowerCase(), username2.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response.toLowerCase(), username1.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username1.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  @Test
  public void testTransferNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username1 = noPermsUser.getNick(), username2 = fakeUser2.getNick();
    String[] arguments = new String[]{username2, "1"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doTransfer(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient permissions");
      assertContains(response.toLowerCase(), username1.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  public void testSlotsParsed() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = 1100l;
    String[] arguments = new String[]{"1.1k"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      SlotRecord record = plugin.doSlots(event, arguments);
      assertEquals(wager, record.wager);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    public void testSlotsZeroWager() throws Exception {
        FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
        String username = fakeUser1.getNick();
        long initialBal = 123484l, wager = 0l;
        String[] arguments = new String[]{String.format("%d", wager)};

        BankDB db = BankDB.getMemoryInstance();
        MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  public void testSlotsParsedWagerWayTooBig() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick();
    long initialBal = 123484l;
    String[] arguments = new String[]{"1 tredecil"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doSlots(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username.toLowerCase());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username = noPermsUser.getNick();
    String[] arguments = new String[]{"1"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doSlots(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient permissions");
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  public void testHiLoLowParsed() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l, wager = 1100l;
    String[] arguments = new String[]{type, "1.1k"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      HiLoRecord record = plugin.doHiLo(event, arguments);
      assertEquals(wager, record.wager);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    public void testHiLoZeroWager() throws Exception {
        FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
        String username = fakeUser1.getNick(), type = "high";
        long initialBal = 123484l, wager = 0l;
        String[] arguments = new String[]{type, String.format("%d", wager)};

        BankDB db = BankDB.getMemoryInstance();
        MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
  public void testHiLoParsedWagerWayTooBig() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1, "amessage");
    String username = fakeUser1.getNick(), type = "high";
    long initialBal = 123484l;
    String[] arguments = new String[]{type, "1 tredecil"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      HiLoRecord record = plugin.doHiLo(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response.toLowerCase(), "does not have");
      assertContains(response.toLowerCase(), "bank account");
      assertContains(response.toLowerCase(), username.toLowerCase());
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoLowNoPermissions() throws Exception {
    FakeMessageEvent event = new FakeMessageEvent(noPermsUser, noPermsUserHost, "amessage");
    String username = noPermsUser.getNick();
    String[] arguments = new String[]{"low", "1"};

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      plugin.doHiLo(event, arguments);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "insufficient permissions");
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount(username, bal);
      plugin.onMessage(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), username.toLowerCase());
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, bal));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testMineOnPrivateMessage() throws Exception {
    String username = fakeUser1.getNick();
    long initialBal = 101l;
    FakePrivateMessageEvent event = new FakePrivateMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s", MoolahPlugin.commandPrefix, MoolahPlugin.mineComm)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onPrivateMessage(event);
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response.toLowerCase(), username.toLowerCase());
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
    MoolahPlugin plugin = new MoolahPlugin(db);
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
      assertContains(response, String.format("%s transferred", username1.toLowerCase()));
      assertEquals(initialBal1 - transfer, source.balance);
      assertEquals(initialBal2 + transfer, dest.balance);
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, transfer));
      assertContains(response, String.format("to %s", username2.toLowerCase()));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal1-transfer));
      assertContains(response, String.format("%s%,d", MoolahPlugin.currSymbol, initialBal2+transfer));
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testSlotsOnPrivateMessage() throws Exception {
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/10;
    FakePrivateMessageEvent event = new FakePrivateMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.slotsComm, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onPrivateMessage(event);
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
  public void testSlotsPrivateMessage() throws Exception {
    String username = fakeUser1.getNick();
    long initialBal = 123484l, wager = initialBal/4;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.slotsComm, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onMessage(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "private");
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testHiLoLowOnPrivateMessage() throws Exception {
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l, wager = initialBal/10;
    FakePrivateMessageEvent event = new FakePrivateMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.hiLoComm, type, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onPrivateMessage(event);
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
  @Test
  public void testHiLoLowPrivateMessage() throws Exception {
    String username = fakeUser1.getNick(), type = "low";
    long initialBal = 123484l, wager = initialBal/4;
    FakeMessageEvent event = new FakeMessageEvent(fakeUser1, fakeUserHost1,
        String.format("%s %s %s %d", MoolahPlugin.commandPrefix, MoolahPlugin.hiLoComm, type, wager)
    );

    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(username, initialBal);
      plugin.onMessage(event);
      String response = event.getResponse();
      assertTrue(response.length() > 0);
      assertContains(response, MoolahPlugin.messagePrefix);
      assertContains(response.toLowerCase(), "private");
    } finally {
      conn.rollback();
    }
  }

  /*
   * Help
   */
  @Test
  public void testHelp() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    MoolahPlugin plugin = new MoolahPlugin(db);
    HashMap<String,String> helpHash = plugin.getHelpFunctions();
  }
}
