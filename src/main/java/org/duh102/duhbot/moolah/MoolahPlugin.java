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
         mineComm = "mine";

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
    respondEvent(event, String.format("You have insufficient funds to complete that transaction; you have %1$s%,d, you need %1$s%,d", currSymbol, acct.balance, required));
  }

  public static void replyNoAccount(MessageEvent event, String username) {
    respondEvent(event, String.format("User '%s' does not have a registered bank account", username));
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

  public void doMine(MessageEvent event) {
  }
  public void doBalance(MessageEvent event, String[] arguments) {
    String username;
    if( arguments.length > 0 )
      username = arguments[1];
    else {
      try {
        username = getUserReg(event.getUser());
      } catch( RuntimeException re ) {
        re.printStackTrace();
        replyGenericError(event);
        return;
      }
    }
    BankAccount account;
    try {
      account = db.getAccountExcept(username);
    } catch( AccountDoesNotExist adne ) {
      replyNoAccount(event, username);
      return;
    } catch( RecordFailure rf ) {
      replyDBError(event);
      rf.printStackTrace();
      return;
    }
    replyBalance(event, account);
  }
  public void doTransfer(MessageEvent event, String[] arguments) {
  }
  public void doSlots(MessageEvent event, String[] arguments) {
  }
  public void doHiLo(MessageEvent event, String[] arguments) {
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
