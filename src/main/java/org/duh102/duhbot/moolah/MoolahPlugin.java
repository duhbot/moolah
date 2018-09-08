package org.duh102.duhbot.moolah;

import java.util.*;

import org.pircbotx.Colors;
import org.pircbotx.User;
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
        String.format("Transfer successful: %2$s transferred %1$s%4$,d to %3$s; %2$s now at %1$s%5$,d and %3$s at %1$s%6$,d",
          currSymbol, source.user, dest.user, record.amount, source.balance, dest.balance
        )
    );
  }

  public static void replyTransferArgumentError(MessageEvent event) {
    respondEvent(event, String.format("Invalid command, usage: %s %s [destination] [amount]", commandPrefix, transferComm));
  }

  public static void replyTransferInvalidAmount(MessageEvent event, String amountStr) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %s", amountStr));
  }

  public static void replyTransferInvalidAmount(MessageEvent event, long amount) {
    respondEvent(event, String.format("Invalid amount, must be a positive integer: %,d", amount));
  }

  public static void replyTransferSameAccount(MessageEvent event, BankAccount account) {
    respondEvent(event, String.format("Cannot transfer money between the same account: %s", account.user));
  }

  public static void respondEvent(MessageEvent event, String message) {
    event.respond(messagePrefix + message);
  }

  public String getUserReg (User user) {
    if( disconnected )
      return user.getNick();
		try {
			user.send().whoisDetail();
			WaitForQueue waitForQueue = new WaitForQueue(user.getBot());
			while (true) {
				WhoisEvent event = waitForQueue.waitFor(WhoisEvent.class);
				if (!event.getRegisteredAs().equals(user.getNick()))
					continue;
				//Got our event
				waitForQueue.close();
				return event.getRegisteredAs();
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException("Couldn't finish querying user for verified status", ex);
		}
  }

  static String message, subCommand;
  static String[] commandParts, arguments;
  public void onMessage(MessageEvent event) {
    message = Colors.removeFormattingAndColors(event.getMessage()).trim();
    if( !message.startsWith(commandPrefix) )
      return;
    if( db == null ) {
      replyDBUnreachableError(event);
      return;
    }
    commandParts = parseCommand(message);
    arguments = Arrays.copyOfRange(commandParts, Math.min(3, commandParts.length), commandParts.length);
    if( commandParts.length < 2 )
      replyUseHelp(event);
    subCommand = commandParts[1];
    switch(subCommand) {
      case openComm:
        doOpen(event);
        break;
      case mineComm:
        doMine(event);
        break;
      case balanceComm:
        doBalance(event, arguments);
        break;
      case transferComm:
        doTransfer(event, arguments);
        break;
      case slotsComm:
        doSlots(event, arguments);
        break;
      case hiLoComm:
        doHiLo(event, arguments);
        break;
      default:
        replyUnknownCommand(event, subCommand);
    }
  }




  public void doOpen(MessageEvent event) {
    String username = null;
    try {
      username = getUserReg(event.getUser());
      BankAccount acct = db.openAccount(username);
      replyAccountOpened(event, acct);
    } catch( RuntimeException re ) {
      re.printStackTrace();
      replyGenericError(event);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountAlreadyExists aae ) {
      replyAccountAlreadyExists(event, username);
    }
  }



  public void doMine(MessageEvent event) {
    String username = null;
    try {
      username = getUserReg(event.getUser());
      BankAccount account = db.getAccountExcept(username);
      MineRecord record = MineRecord.recordMineAttempt(db, account);
      replyMineAttempt(event, account, record);
    } catch( RuntimeException re ) {
      re.printStackTrace();
      replyGenericError(event);
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, username);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( MineAttemptTooSoon mats ) {
      replyMineTooSoon(event);
    }
  }



  public void doBalance(MessageEvent event, String[] arguments) {
    String username;
    if( arguments.length > 0 )
      username = arguments[0];
    else {
      try {
        username = getUserReg(event.getUser());
      } catch( RuntimeException re ) {
        re.printStackTrace();
        replyGenericError(event);
        return;
      }
    }
    try {
      BankAccount account = db.getAccountExcept(username);
      replyBalance(event, account);
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, username);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    }
  }



  public void doTransfer(MessageEvent event, String[] arguments) {
    String destName = null;
    long transferAmount = 0;
    String sourceName = null;
    BankAccount source = null, dest = null;
    try {
      destName = arguments[0];
      transferAmount = Long.parseLong(arguments[1]);
      sourceName = getUserReg(event.getUser());
      source = db.getAccountExcept(sourceName);
      dest = db.getAccountExcept(destName);
      TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      replyTransfer(event, source, dest, record);
    } catch( ArrayIndexOutOfBoundsException oob ) {
      replyTransferArgumentError(event);
    } catch( NumberFormatException nfe ) {
      replyTransferInvalidAmount(event, arguments[1]);
    } catch( RuntimeException re ) {
      re.printStackTrace();
      replyGenericError(event);
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, adne.getMessage());
    } catch( ImproperBalanceAmount iba ) {
      replyTransferInvalidAmount(event, transferAmount);
    } catch( InsufficientFundsException ife ) {
      replyInsufficientFunds(event, source, transferAmount);
    } catch( SameAccountException sae ) {
      replyTransferSameAccount(event, source);
    }
  }



  public void doSlots(MessageEvent event, String[] arguments) {
    String username = null;
    try {
      username = getUserReg(event.getUser());
    } catch( RuntimeException re ) {
      re.printStackTrace();
      replyGenericError(event);
      return;
    }
  }




  public void doHiLo(MessageEvent event, String[] arguments) {
    String username = null;
    try {
      username = getUserReg(event.getUser());
    } catch( RuntimeException re ) {
      re.printStackTrace();
      replyGenericError(event);
      return;
    }
  }






  public HashMap<String,String> getHelpFunctions() {
    HashMap<String,String> helpFunctions = new HashMap<String,String>();
    helpFunctions.put(".bank", "Main command");
    return helpFunctions;
  }

  public String getPluginName() {
    return "Moolah Plugin";
  }

  public ListenerAdapter getAdapter() {
    return this;
  }
}
