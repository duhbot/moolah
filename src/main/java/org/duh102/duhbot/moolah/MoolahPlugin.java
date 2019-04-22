package org.duh102.duhbot.moolah;

import java.math.BigInteger;
import java.util.*;

import com.google.common.collect.ImmutableSortedSet;

import org.duh102.duhbot.moolah.BankAccount;
import org.duh102.duhbot.moolah.Pair;
import org.duh102.duhbot.moolah.SlotReelImage;
import org.duh102.duhbot.moolah.db.*;
import org.duh102.duhbot.moolah.db.dao.BankAccountDAO;
import org.duh102.duhbot.moolah.parsing.ShortcutParser;
import org.duh102.duhbot.moolah.parsing.exceptions.BadValueException;
import org.pircbotx.Colors;
import org.pircbotx.User;
import org.pircbotx.Channel;
import org.pircbotx.hooks.*;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.events.*;

import org.duh102.duhbot.functions.*;

import org.duh102.duhbot.moolah.exceptions.*;

public class MoolahPlugin extends ListenerAdapter implements ListeningPlugin
{
  public static final String currSymbol = "🞛", currFull = "gemeralds", currAbrv = "gm";
  public static final String messagePrefix = String.format("[%s] ", currSymbol);
  public static final String commandPrefix = ".bank", balanceComm = "balance",
         transferComm = "transfer", slotsComm = "slots", hiLoComm = "hilo",
         mineComm = "mine", openComm = "open";
  public static final String strRateLimit = "5 minutes";
  public static final long rateLimit = 5*60*1000;

  BankDB db;
  public MoolahPlugin() {
    try {
      db = BankDB.getDBInstance();
    } catch( Exception idc ) {
      idc.printStackTrace();
      db = null;
    }
  }
  public MoolahPlugin(BankDB db) {
    this.db = db;
  }

  public static String[] parseCommand(String input) {
    return input.split("\\s+");
  }

  public static void replyGenericError(GenericMessageEvent event) {
    respondEvent(event, "An unknown error occurred, please notify maintainers");
  }

  public static void replyDBError(GenericMessageEvent event) {
    respondEvent(event, "An unknown database error occurred, please notify maintainers");
  }

  public static void replyDBUnreachableError(GenericMessageEvent event) {
    respondEvent(event, "The database is unreachable, please notify maintainers");
  }

  public static void replyUseHelp(GenericMessageEvent event) {
    respondEvent(event, String.format("%s cannot be used alone, see help for valid subcommands", commandPrefix));
  }

  public static void replyTooSoon(GenericMessageEvent event, String command) {
    respondEvent(event, String.format("Unable to use %s so soon, please wait at least %s", command, strRateLimit));
  }

  public static void replyUserPermissionsError(GenericMessageEvent event) {
    respondEvent(event, String.format("User %s has insufficient permissions, must have vop+ at least", event.getUser().getNick()));
  }

  public static void replyUsePrivateMessage(GenericMessageEvent event) {
    respondEvent(event, "Unable to use this command in a channel, send it to this bot in private");
  }

  public static void replyUsePublicMessage(GenericMessageEvent event) {
    respondEvent(event, "Unable to use this command in a private message, send it to this bot in public");
  }

  public static void replyUnknownCommand(GenericMessageEvent event, String command) {
    respondEvent(event, String.format("Unknown command '%s', see help for valid subcommands", command));
  }

  public static void replyBalance(GenericMessageEvent event, BankAccount acct) {
    respondEvent(event, String.format("%s has %s%,d", acct.user, currSymbol, acct.balance));
  }

  public static void replyInsufficientFunds(GenericMessageEvent event, BankAccount acct, long required) {
    respondEvent(event, String.format("You have insufficient funds to complete that transaction; you have %1$s%2$,d, you need %1$s%3$,d", currSymbol, acct.balance, required));
  }

  public static void replyNoAccount(GenericMessageEvent event, String username) {
    respondEvent(event, String.format("User '%s' does not have a registered bank account", username));
  }

  public static void replyAccountAlreadyExists(GenericMessageEvent event, String username) {
    respondEvent(event, String.format("A registered bank account for user '%s' already exists", username));
  }

  public static void replyMineTooSoon(GenericMessageEvent event) {
    respondEvent(event, String.format("Mine attempt too soon, wait at least half an hour between attempts"));
  }

  public static void replyMineAttempt(GenericMessageEvent event, BankAccount account, MineRecord record) {
    respondEvent(event, String.format("You mined %,d %s, you now have %s%,d", record.yield, currFull, currSymbol, account.balance));
  }

  public static void replyAccountOpened(GenericMessageEvent event, BankAccount acct) {
    respondEvent(event, String.format("Bank account opened for user '%s'", acct.user));
  }

  public static void replyTransfer(GenericMessageEvent event, BankAccount source, BankAccount dest, TransferRecord record) {
    respondEvent(event,
        String.format("Transfer successful: %2$s transferred %1$s%4$,d to %3$s, %2$s now has %1$s%5$,d and %3$s now has %1$s%6$,d",
          currSymbol, source.user, dest.user, record.amount, source.balance, dest.balance)
    );
  }

  public static void replyTransferArgumentError(GenericMessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [destination] [amount]", commandPrefix, transferComm));
  }

  public static void replyInvalidAmount(GenericMessageEvent event, String amountStr) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %s", amountStr));
  }

  public static void replyInvalidAmount(GenericMessageEvent event, long amount) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %,d", amount));
  }

  public static void replyTransferSameAccount(GenericMessageEvent event, BankAccount account) {
    respondEvent(event, String.format("Cannot transfer money between the same account: %s", account.user));
  }

  public static void replySlots(GenericMessageEvent event, BankAccount acct, SlotRecord record) {
    respondEvent(event,
        String.format("Slots outcome: %2$s, %3$.2fx payout (%1$s%4$,d), your balance now %1$s%5$,d",
          currSymbol, record.getImagesString(), record.multiplier, record.payout, acct.balance
        )
    );
  }

  public static void replySlotsArgumentError(GenericMessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [wager]", commandPrefix, slotsComm));
  }

  public static void replyHiLo(GenericMessageEvent event, BankAccount acct, HiLoRecord record) {
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

  public static void replyHiLoArgumentError(GenericMessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [h(igh)|l(ow)|e(qual)] [wager]", commandPrefix, hiLoComm));
  }

  public static void replyHiLoTypeError(GenericMessageEvent event, String type) {
    respondEvent(event, String.format("Invalid HiLo type '%s', must be [h(igh)|l(ow)|e(qual)]", type));
  }

  public static void respondEvent(GenericMessageEvent event, String message) {
    String response = messagePrefix + message;
    // We only need the nick prefix if we're responding to a channel
    if( event instanceof MessageEvent )
      event.respond(response);
    else
      event.respondWith(response);
  }

  public void announceHiLoBust(GenericMessageEvent event, BankAccount account, HiLoRecord record) {
    sendAnnouncement(event,
        String.format("%s just bet %s%,d on %s and lost it all, leaving them with %s%,d",
          account.user, currSymbol, record.wager, record.hiLo.toString().toLowerCase(), currSymbol, account.balance)
    );
  }
  public void announceSlotsBust(GenericMessageEvent event, BankAccount account, SlotRecord record) {
    sendAnnouncement(event,
        String.format("%s just bet %s%,d on the slots and lost it all, leaving them with %s%,d",
          account.user, currSymbol, record.wager, currSymbol, account.balance)
    );
  }
  public void announceHiLoJackpot(GenericMessageEvent event, BankAccount account, HiLoRecord record) {
    sendAnnouncement(event,
        String.format("%2$s just bet %1$s%3$,d on %4$s and hit the jackpot, netting a huge payout of %1$s%5$,d (%$6.2f), leaving them with %1$s%7$,d",
          currSymbol, account.user, record.wager, record.hiLo.toString().toLowerCase(), record.payout, record.multiplier, account.balance)
    );
  }
  public void announceSlotsJackpot(GenericMessageEvent event, BankAccount account, SlotRecord record) {
    sendAnnouncement(event,
        String.format("%2$s just bet %1$s%3$,d on the slots and hit the jackpot, netting a huge payout of %1$s%4$,d (%5$.2f), leaving them with %1$s%6$,d",
          currSymbol, account.user, record.wager, record.payout, record.multiplier, account.balance)
    );
  }

  public void sendAnnouncement(GenericMessageEvent event, String message) {
    ImmutableSortedSet<Channel> channels = event.getBot().getUserChannelDao().getAllChannels();
    for( Channel chan : channels ) {
      chan.send().message(messagePrefix + message);
    }
  }

  public String getUserReg(Channel channel, User user) throws InsufficientPrivilegesException {
    if( channel.getNormalUsers().contains(user) )
      throw new InsufficientPrivilegesException();
    return user.getNick().toLowerCase();
  }
  //less efficient
  public String getUserReg(GenericMessageEvent event, User user) throws InsufficientPrivilegesException {
    for( Channel channel : event.getBot().getUserChannelDao().getAllChannels() ) {
      if( !channel.getNormalUsers().contains(user) )
        return user.getNick().toLowerCase();
    }
    throw new InsufficientPrivilegesException();
  }

  public boolean isSignificantWager(BankAccount account, long wager) {
    if( wager < 5000l )
      return false;
    if( ((double)wager) / account.balance < 0.9 )
      return false;
    try {
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      if( ((double)wager) / accountDAO.getAccountTotal() < 0.4 )
        return false;
    } catch( RecordFailure rf ) {
      rf.printStackTrace();
      return false;
    }
    return true;
  }

  static String message, subCommand;
  static String[] commandParts, arguments;
  public static Object lastHandled = null;
  static Pair<String, String[]> doNotRespond = new Pair<String, String[]>(null, null);
  public Pair<String, String[]> parseHandleCommand(GenericMessageEvent event) {
    message = Colors.removeFormattingAndColors(event.getMessage()).trim();
    if( !message.startsWith(commandPrefix) )
      return doNotRespond;
    if( db == null ) {
      replyDBUnreachableError(event);
      return doNotRespond;
    }
    commandParts = parseCommand(message);
    arguments = Arrays.copyOfRange(commandParts, Math.min(2, commandParts.length), commandParts.length);
    if( commandParts.length < 2 ) {
      replyUseHelp(event);
      return doNotRespond;
    }
    subCommand = commandParts[1];
    return new Pair<String, String[]>(subCommand, arguments);
  }

  public void onMessage(MessageEvent event) {
    lastHandled = null;
    Pair<String, String[]> messageParts = parseHandleCommand(event);
    if( messageParts == doNotRespond )
      return;
    subCommand = messageParts.first;
    arguments = messageParts.second;
    switch(subCommand) {
      case openComm:
        lastHandled = (Object)doOpen(event);
        return;
      case mineComm:
        replyUsePrivateMessage(event);
        return;
      case balanceComm:
        doBalance(event, arguments);
        return;
      case transferComm:
        lastHandled = (Object)doTransfer(event, arguments);
        return;
      case slotsComm:
        replyUsePrivateMessage(event);
        return;
      case hiLoComm:
        replyUsePrivateMessage(event);
        return;
      default:
        replyUnknownCommand(event, subCommand);
    }
  }
  public void onPrivateMessage(PrivateMessageEvent event) {
    lastHandled = null;
    Pair<String, String[]> messageParts = parseHandleCommand(event);
    if( messageParts == doNotRespond )
      return;
    subCommand = messageParts.first;
    arguments = messageParts.second;
    switch(subCommand) {
      case openComm:
        replyUsePublicMessage(event);
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




  public BankAccount doOpen(GenericMessageEvent event) {
    String username = null;
    BankAccount acct = null;
    try {
      User user = event.getUser();
      if( event instanceof MessageEvent )
        username = getUserReg(((MessageEvent)event).getChannel(), user);
      else
        username = getUserReg(event, user);
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      acct = accountDAO.openAccount(username);
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



  public MineRecord doMine(GenericMessageEvent event) {
    String username = null;
    MineRecord record = null;
    try {
      User user = event.getUser();
      if( event instanceof MessageEvent )
        username = getUserReg(((MessageEvent)event).getChannel(), user);
      else
        username = getUserReg(event, user);
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      BankAccount account = accountDAO.getAccountExcept(username);
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



  public void doBalance(GenericMessageEvent event, String[] arguments) {
    String username = null;
    try {
      if( arguments.length > 0 )
        username = arguments[0];
      else {
        User user = event.getUser();
        if( event instanceof MessageEvent )
          username = getUserReg(((MessageEvent)event).getChannel(), user);
        else
          username = getUserReg(event, user);
      }
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      BankAccount account = accountDAO.getAccountExcept(username);
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



  public TransferRecord doTransfer(GenericMessageEvent event, String[] arguments) {
    String destName = null;
    long transferAmount = 0;
    String sourceName = null;
    BankAccount source = null, dest = null;
    TransferRecord record = null;
    try {
      destName = arguments[0];
      String[] amountArgs = unprotectedCopyOfRange(arguments, 1,
              arguments.length);
      BigInteger temp = ShortcutParser.parseValue(String.join(" ", amountArgs));
      transferAmount = temp.longValue();
      if( transferAmount == 0 ) {
        throw new ImproperBalanceAmount(transferAmount);
      }
      User user = event.getUser();
      if( event instanceof MessageEvent )
        sourceName = getUserReg(((MessageEvent)event).getChannel(), user);
      else
        sourceName = getUserReg(event, user);
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      source = accountDAO.getAccountExcept(sourceName);
      dest = accountDAO.getAccountExcept(destName);
      record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      replyTransfer(event, source, dest, record);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replyTransferArgumentError(event);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[1]);
    } catch( BadValueException bve ) {
      replyInvalidAmount(event, bve.getMessage());
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



  public SlotRecord doSlots(GenericMessageEvent event, String[] arguments) {
    long wager = 0;
    String username = null;
    BankAccount acct = null;
    SlotRecord record = null;
    try {
      String[] wagerArgs = unprotectedCopyOfRange(arguments, 0,
              arguments.length);
      BigInteger temp = ShortcutParser.parseValue(String.join(" ", wagerArgs));
      wager = temp.longValue();
      if( wager == 0 ) {
        throw new ImproperBalanceAmount(wager);
      }
      User user = event.getUser();
      if( event instanceof MessageEvent )
        username = getUserReg(((MessageEvent)event).getChannel(), user);
      else
        username = getUserReg(event, user);
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      acct = accountDAO.getAccountExcept(username);
      record = SlotRecord.recordSlotAttempt(db, acct, wager);
      if( (record.multiplier == 0 || SlotRecord.isJackpot(record.multiplier)) && isSignificantWager(acct, wager) ) {
        if( record.multiplier == 0 )
          announceSlotsBust(event, acct, record);
        else 
          announceSlotsJackpot(event, acct, record);
      }
      replySlots(event, acct, record);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replySlotsArgumentError(event);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[0]);
    } catch( BadValueException bve ) {
      replyInvalidAmount(event, bve.getMessage());
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




  public HiLoRecord doHiLo(GenericMessageEvent event, String[] arguments) {
    long wager = 0;
    HiLoBetType type = null;
    String username = null;
    BankAccount acct = null;
    HiLoRecord record = null;
    try {
      type = HiLoBetType.fromString(arguments[0]);
      String[] wagerArgs = unprotectedCopyOfRange(arguments, 1,
              arguments.length);
      BigInteger temp = ShortcutParser.parseValue(String.join(" ", wagerArgs));
      wager = temp.longValue();
      if( wager == 0 ) {
        throw new ImproperBalanceAmount(wager);
      }
      User user = event.getUser();
      if( event instanceof MessageEvent )
        username = getUserReg(((MessageEvent)event).getChannel(), user);
      else
        username = getUserReg(event, user);
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      acct = accountDAO.getAccountExcept(username);
      record = HiLoRecord.recordBetHiLo(db, acct, type, wager);
      if( (record.multiplier == 0 || HiLoRecord.isJackpot(record.multiplier)) && isSignificantWager(acct, wager) ) {
        if( record.multiplier == 0 )
          announceHiLoBust(event, acct, record);
        else 
          announceHiLoJackpot(event, acct, record);
      }
      replyHiLo(event, acct, record);
    } catch( InsufficientPrivilegesException ipe ) {
      replyUserPermissionsError(event);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replyHiLoArgumentError(event);
    } catch( InvalidInputError iie ) {
      replyHiLoTypeError(event, arguments[0]);
    } catch( NumberFormatException nfe ) {
      replyInvalidAmount(event, arguments[1]);
    } catch( BadValueException bve ) {
      replyInvalidAmount(event, bve.getMessage());
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

  public String[] unprotectedCopyOfRange(String[] original, int from, int to) {
    String[] toRet = new String[to - from];
    if(toRet.length > 1) {
      for (int i = 0; i < toRet.length; i++) {
        toRet[i] = original[from + i];
      }
    } else {
      toRet[0] = original[from];
    }
    return toRet;
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
        String.format("High: %.2fx | Equal: %.2fx | Low: %.2fx",
          HiLoBetType.HIGH.getMultiplier(), HiLoBetType.EQUAL.getMultiplier(), HiLoBetType.LOW.getMultiplier())
    );
    helpFunctions.put("(slots payouts)",
        String.format("Any bars: %.2fx | All bars: %.2fx | Per-7 bonus: %.2fx | All 7s: %.2fx | Two symbols: %.2fx | Three symbols: %.2fx | Symbol bonuses: $5 %.2fx, %s%s %.2fx, %s %.2fx",
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
