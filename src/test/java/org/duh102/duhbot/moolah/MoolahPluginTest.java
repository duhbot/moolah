package org.duh102.duhbot.moolah;

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
  FakeUserHostmask fakeUserHost = new FakeUserHostmask("nickname", "UserID", "Host", "");
  FakeUser fakeUser = new FakeUser(fakeUserHost);
  FakeChannel fakeChannel = new FakeChannel("achannel");
  private class FakeMessageEvent extends MessageEvent {
    private String response;
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

  @Test
  public void testParseCommand() throws Exception {
    String command = "a b  c\td \te\t \tf \t g";
    String[] expected = new String[] {"a","b","c","d","e","f","g"};
    assertArrayEquals(expected, MoolahPlugin.parseCommand(command));
  }
  @Test
  public void testReplyGenericError() {
    FakeMessageEvent event = new FakeMessageEvent(fakeUser, fakeUserHost, "amessage");
    MoolahPlugin.replyGenericError(event);
    String response = event.getResponse();
    assertTrue(response.length() > 0);
    assertTrue(response.contains("unknown error"));
  }
}
