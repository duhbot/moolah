package org.duh102.duhbot.moolah;

import java.util.*;

import org.pircbotx.hooks.*;
import org.pircbotx.hooks.events.*;

import org.duh102.duhbot.functions.*;

public class MoolahPlugin extends ListenerAdapter implements DuhbotFunction
{
  public MoolahPlugin() {
  }
  
  static String message;
  public void onMessage(MessageEvent event) {
    message = event.getMessage();
  }

  public String getUserReg (User user) {
		try {
			user.send().whoisDetail();
			WaitForQueue waitForQueue = new WaitForQueue(getBot());
			while (true) {
				WhoisEvent event = waitForQueue.waitFor(WhoisEvent.class);
				if (!event.getNick().equals(getNick()))
					continue;

				//Got our event
				waitForQueue.close();
				return event.getRegisteredAs();
			}
		} catch (InterruptedException ex) {
			(new RuntimeException("Couldn't finish querying user for verified status", ex)).printStackTrace();
		} 
  }

  public void replyBalance(MessageEvent event, long balance, byte fractional) {
    event.reply(String.format("You have $%,d.%02d", balance, fractional));
  }

  public void noAccountMessage(MessageEvent event, User user) {
    event.reply("You don't have a registered bank account!");
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
