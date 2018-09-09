package org.duh102.duhbot.moolah;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.pircbotx.Colors;
import org.pircbotx.User;
import org.pircbotx.Channel;
import org.pircbotx.hooks.*;
import org.pircbotx.hooks.events.*;

import org.duh102.duhbot.functions.*;

import org.duh102.duhbot.moolah.exceptions.*;

public class MoolahPlugin extends ListenerAdapter implements DuhbotFunction
{
  public static final String currSymbol = "🞛", currFull = "gemeralds", currAbrv = "gm";
  public static final String messagePrefix = String.format("[%s] ", currSymbol);
  public static final String commandPrefix = ".bank", balanceComm = "balance",
         transferComm = "transfer", slotsComm = "slots", hiLoComm = "hilo",
         mineComm = "mine", openComm = "open";
  public static final String strRateLimit = "5 minutes";
  public static final long rateLimit = 5*60*1000;
  public static ConcurrentHashMap<String, Long> antiSpamMapSlots = new ConcurrentHashMap<String, Long>();
  public static ConcurrentHashMap<String, Long> antiSpamMapHiLo = new ConcurrentHashMap<String, Long>();

  BankDB db;
  boolean disconnected = false; //if disconnected we should assume we're testing
  public MoolahPlugin() {
    try {
      db = BankDB.getDBInstance();
    } catch( InvalidDBConfiguration | InvalidEnvironment idc ) {
      idc.printStackTrace();
      db = null;
    }
  }
  public MoolahPlugin(BankDB db, boolean disconnected) {
    this.db = db;
    this.disconnected = disconnected;
  }

  public static String[] parseCommand(String input) {
    return input.split("\\s+");
  }

  public static void replyGenericError(MessageEvent event) {
    respondEvent(event, "An unknown error occurred, please notify maintainers");
  }

  public static void replyDBError(MessageEvent event) {
    respondEvent(event, "An unknown database error occurred, please notify maintainers");
  }

  public static void replyDBUnreachableError(MessageEvent event) {
    respondEvent(event, "The database is unreachable, please notify maintainers");
  }

  public static void replyUseHelp(MessageEvent event) {
    respondEvent(event, String.format("%s cannot be used alone, see help for valid subcommands", commandPrefix));
  }

  public static void replyTooSoon(MessageEvent event, String command) {
    respondEvent(event, String.format("Unable to use %s so soon, please wait at least %s", command, strRateLimit));
  }

  public static void replyUserPermissionsError(MessageEvent event) {
    respondEvent(event, String.format("User %s has insufficient permissions, must have vop+ at least", event.getUser().getNick()));
  }

  public static void replyUnknownCommand(MessageEvent event, String command) {
    respondEvent(event, String.format("Unknown command '%s', see help for valid subcommands", command));
  }

  public static void replyBalance(MessageEvent event, BankAccount acct) {
    respondEvent(event, String.format("%s has %s%,d", acct.user, currSymbol, acct.balance));
  }

  public static void replyInsufficientFunds(MessageEvent event, BankAccount acct, long required) {
    respondEvent(event, String.format("You have insufficient funds to complete that transaction; you have %1$s%2$,d, you need %1$s%3$,d", currSymbol, acct.balance, required));
  }

  public static void replyNoAccount(MessageEvent event, String username) {
    respondEvent(event, String.format("User '%s' does not have a registered bank account", username));
  }

  public static void replyAccountAlreadyExists(MessageEvent event, String username) {
    respondEvent(event, String.format("A registered bank account for user '%s' already exists", username));
  }

  public static void replyMineTooSoon(MessageEvent event) {
    respondEvent(event, String.format("Mine attempt too soon, wait at least half an hour between attempts"));
  }

  public static void replyMineAttempt(MessageEvent event, BankAccount account, MineRecord record) {
    respondEvent(event, String.format("You mined %,d %s, you now have %s%,d", record.yield, currFull, currSymbol, account.balance));
  }

  public static void replyAccountOpened(MessageEvent event, BankAccount acct) {
    respondEvent(event, String.format("Bank account opened for user '%s'", acct.user));
  }

  public static void replyTransfer(MessageEvent event, BankAccount source, BankAccount dest, TransferRecord record) {
    respondEvent(event,
        String.format("Transfer successful: %2$s transferred %1$s%4$,d to %3$s, %2$s now has %1$s%5$,d and %3$s now has %1$s%6$,d",
          currSymbol, source.user, dest.user, record.amount, source.balance, dest.balance)
    );
  }

  public static void replyTransferArgumentError(MessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [destination] [amount]", commandPrefix, transferComm));
  }

  public static void replyInvalidAmount(MessageEvent event, String amountStr) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %s", amountStr));
  }

  public static void replyInvalidAmount(MessageEvent event, long amount) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %,d", amount));
  }

  public static void replyTransferSameAccount(MessageEvent event, BankAccount account) {
    respondEvent(event, String.format("Cannot transfer money between the same account: %s", account.user));
  }

  public static void replySlots(MessageEvent event, BankAccount acct, SlotRecord record) {
    respondEvent(event,
        String.format("Slots outcome: %2$s, %3$.2fx payout (%1$s%4$,d), your balance now %1$s%5$,d",
          currSymbol, record.getImagesString(), record.multiplier, record.payout, acct.balance
        )
    );
  }

  public static void replySlotsArgumentError(MessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [wager]", commandPrefix, slotsComm));
  }

  public static void replyHiLo(MessageEvent event, BankAccount acct, HiLoRecord record) {
    boolean won = record.payout > 0l;
    String comparison = "<>";
    switch(record.hiLo) {
      case HIGH:
        comparison = won?">":"<=";
        break;
      case LOW:
        comparison = won?"<":">=";
        break;
      case EQUAL:
        comparison = won?"==":"!=";
    }
    respondEvent(event,
        String.format("HiLo %8$s outcome: %2$d%3$s%4$d, %5$.2fx payout (%1$s%6$,d), your balance now %1$s%7$,d",
          currSymbol, record.resultInt, comparison, HiLoRecord.MID, record.multiplier,
          record.payout, acct.balance, record.hiLo.toString().toLowerCase()
        )
    );
  }

  public static void replyHiLoArgumentError(MessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [h(igh)|l(ow)|e(qual)] [wager]", commandPrefix, hiLoComm));
  }

  public static void replyHiLoTypeError(MessageEvent event, String type) {
    respondEvent(event, String.format("Invalid HiLo type '%s', must be [h(igh)|l(ow)|e(qual)]", type));
  }

  public static void respondEvent(MessageEvent event, String message) {
    event.respond(messagePrefix + message);
  }

  public String getUserReg (Channel channel, User user) throws InsufficientPrivilegesException {
    if( channel.getNormalUsers().contains(user) )
      throw new InsufficientPrivilegesException();
    return user.getNick().toLowerCase();
  }

  static String message, subCommand;
  static String[] commandParts, arguments;
  public static Object lastHandled = null;
  public void onMessage(MessageEvent event) {
    lastHandled = null;
    message = Colors.removeFormattingAndColors(event.getMessage()).trim();
    if( !message.startsWith(commandPrefix) )
      return;
    if( db == null ) {
      replyDBUnreachableError(event);
      return;
    }
    commandParts = parseCommand(message);
    arguments = Arrays.copyOfRange(commandParts, Math.min(2, commandParts.length), commandParts.length);
    if( commandParts.length < 2 )
      replyUseHelp(event);
    subCommand = commandParts[1];
    switch(subCommand) {
      case openComm:
        lastHandled = (Object)doOpen(event);
        return;
      case mineComm:
        lastHandled = (Object)doMine(event);
        return;
      case balanceComm:
        doBalance(event, arguments);
        return;
      case transferComm:
        lastHandled = (Object)doTransfer(event, arguments);
        return;
      case slotsComm:
        lastHandled = (Object)doSlots(event, arguments);
        return;
      case hiLoComm:
        lastHandled = (Object)doHiLo(event, arguments);
        return;
      default:
        replyUnknownCommand(event, subCommand);
    }
  }




  public BankAccount doOpen(MessageEvent event) {
    String username = null;
    BankAccount acct = null;
    try {
      username = getUserReg(event.getChannel(), event.getUser());
      acct = db.openAccount(username);
      replyAccountOpened(event, acct);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountAlreadyExists aae ) {
      replyAccountAlreadyExists(event, username);
    }
    return acct;
  }



  public MineRecord doMine(MessageEvent event) {
    String username = null;
    MineRecord record = null;
    try {
      username = getUserReg(event.getChannel(), event.getUser());
      BankAccount account = db.getAccountExcept(username);
      record = MineRecord.recordMineAttempt(db, account);
      replyMineAttempt(event, account, record);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, username);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( MineAttemptTooSoon mats ) {
      replyMineTooSoon(event);
    }
    return record;
  }



  public void doBalance(MessageEvent event, String[] arguments) {
    String username = null;
    try {
      if( arguments.length > 0 )
        username = arguments[0];
      else {
        username = getUserReg(event.getChannel(), event.getUser());
      }
      BankAccount account = db.getAccountExcept(username);
      replyBalance(event, account);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, username);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    }
  }



  public TransferRecord doTransfer(MessageEvent event, String[] arguments) {
    String destName = null;
    long transferAmount = 0;
    String sourceName = null;
    BankAccount source = null, dest = null;
    TransferRecord record = null;
    try {
      destName = arguments[0];
      transferAmount = Long.parseLong(arguments[1]);
      sourceName = getUserReg(event.getChannel(), event.getUser());
      source = db.getAccountExcept(sourceName);
      dest = db.getAccountExcept(destName);
      record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      replyTransfer(event, source, dest, record);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replyTransferArgumentError(event);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[1]);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, adne.getMessage());
    } catch( ImproperBalanceAmount iba ) {
      replyInvalidAmount(event, transferAmount);
    } catch( InsufficientFundsException ife ) {
      replyInsufficientFunds(event, source, transferAmount);
    } catch( SameAccountException sae ) {
      replyTransferSameAccount(event, source);
    }
    return record;
  }



  public SlotRecord doSlots(MessageEvent event, String[] arguments) {
    long wager = 0;
    String username = null;
    BankAccount acct = null;
    SlotRecord record = null;
    try {
      wager = Long.parseLong(arguments[0]);
      username = getUserReg(event.getChannel(), event.getUser());
      acct = db.getAccountExcept(username);
      Long lastUsed = antiSpamMapSlots.get(acct.user);
      Long now = System.currentTimeMillis();
      if( lastUsed != null && lastUsed >= now-rateLimit ) {
        replyTooSoon(event, slotsComm);
        return null;
      }
      record = SlotRecord.recordSlotAttempt(db, acct, wager);
      replySlots(event, acct, record);
      antiSpamMapSlots.put(acct.user, now);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replySlotsArgumentError(event);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[0]);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, adne.getMessage());
    } catch( ImproperBalanceAmount iba ) {
      replyInvalidAmount(event, wager);
    } catch( InsufficientFundsException ife ) {
      replyInsufficientFunds(event, acct, wager);
    }
    return record;
  }




  public HiLoRecord doHiLo(MessageEvent event, String[] arguments) {
    long wager = 0;
    HiLoBetType type = null;
    String username = null;
    BankAccount acct = null;
    HiLoRecord record = null;
    try {
      type = HiLoBetType.fromString(arguments[0]);
      wager = Long.parseLong(arguments[1]);
      username = getUserReg(event.getChannel(), event.getUser());
      acct = db.getAccountExcept(username);
      Long lastUsed = antiSpamMapHiLo.get(acct.user);
      Long now = System.currentTimeMillis();
      if( lastUsed != null && lastUsed >= now-rateLimit ) {
        replyTooSoon(event, hiLoComm);
        return null;
      }
      record = HiLoRecord.recordBetHiLo(db, acct, type, wager);
      replyHiLo(event, acct, record);
      antiSpamMapHiLo.put(acct.user, now);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replyHiLoArgumentError(event);
    } catch( InvalidInputError iie ) {
      replyHiLoTypeError(event, arguments[0]);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[1]);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, adne.getMessage());
    } catch( ImproperBalanceAmount iba ) {
      replyInvalidAmount(event, wager);
    } catch( InsufficientFundsException ife ) {
      replyInsufficientFunds(event, acct, wager);
    }
    return record;
  }






  public HashMap<String,String> getHelpFunctions() {
    HashMap<String,String> helpFunctions = new HashMap<String,String>();
    helpFunctions.put(String.format("%s %s", commandPrefix, openComm),
        "Open an account, uses your nick as the account name, max one per user (must be registered with nickserv and have vop+ in current channel)"
    );
    helpFunctions.put(String.format("%s %s", commandPrefix, balanceComm), "Check your current balance");
    helpFunctions.put(String.format("%s %s [user]", commandPrefix, balanceComm), "Check someone else's current balance (does not require an account)");
    helpFunctions.put(String.format("%s %s", commandPrefix, mineComm),
        String.format("Hit the %1$s mines and mine some free %1$s based on how long since you last mined; max 24 hours",
          currFull)
    );
    helpFunctions.put(String.format("%s %s [destination] [amount]", commandPrefix, transferComm),
        "Transfer [amount] of money from your account to [destination] account");
    helpFunctions.put(String.format("%s %s [wager]", commandPrefix, slotsComm),
        "Gamble [wager] on the outcome of a 3-reel slot machine. See https://github.com/duhbot/moolah/blob/master/doc/design.md for payout calculations"
    );
    helpFunctions.put(String.format("%s %s [h(igh)|l(ow)|e(qual)] [wager]", commandPrefix, hiLoComm),
        String.format("Gamble [wager] on the outcome of choosing a random number between %d and %d; pays based on whether the result is higher, lower, or equal to %d",
          HiLoRecord.MIN, HiLoRecord.MAX, HiLoRecord.MID)
    );
    helpFunctions.put("(hi/lo payouts)",
        String.format("High: %.2fx | Equal: %.2f | Low: %.2f",
          HiLoBetType.HIGH.getMultiplier(), HiLoBetType.EQUAL.getMultiplier(), HiLoBetType.LOW.getMultiplier())
    );
    helpFunctions.put("(slots payouts)",
        String.format("Any bars: %.2fx | All bars: %.2f | Per-7 bonus: %.2f | All 7s: %.2f | Two symbols: %.2f | Three symbols: %.2f | Symbol bonuses: $5 %.2f, %s%s %.2f, %s %.2f",
          0.0, SlotRecord.BAR_MULT, SlotRecord.SEVEN_ADD_MULT, SlotRecord.SEVEN_ALL_MULT, SlotRecord.TWO_MATCH_MULT,
          SlotRecord.THREE_MATCH_MULT, SlotRecord.DOLLAR_FIVE_BONUS, SlotReelImage.CHERRIES.toString(), SlotReelImage.LEMON.toString(), SlotRecord.FRUIT_BONUS, SlotReelImage.BELL.toString(), SlotRecord.BELL_BONUS)
    );
    return helpFunctions;
  }

  public String getPluginName() {
    return "Moolah Plugin";
  }

  public ListenerAdapter getAdapter() {
    return this;
  }
}
