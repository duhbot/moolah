package org.duh102.duhbot.moolah;

import com.google.common.collect.ImmutableSortedSet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserChannelDao;
import org.pircbotx.hooks.events.*;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class ManualTestClient {
    @Mock PrivateMessageEvent privateEvent;
    @Mock MessageEvent messageEvent;
    @Mock User user;
    @Mock Channel channel;
    @Mock PircBotX bot;
    @Mock UserChannelDao userChannelDao;
    Pattern commandPattern = Pattern.compile("^(?<command>\\\\q|\\\\m|\\\\p)[ \t]+(?<message>.+)$");

    public ManualTestClient() {
        MockitoAnnotations.initMocks(this);
        setupMocks();
        Scanner sc = new Scanner(System.in);
        MoolahPlugin plugin = new MoolahPlugin();
        String command = null, message, fullLine;
        while(!"\\q".equals(command)) {
            fullLine = sc.nextLine();
            try {
                Matcher match = commandPattern.matcher(fullLine);
                if (!match.matches()) {
                    throw new Exception(String.format("String does not match pattern: %s", fullLine));
                }
                command = match.group("command");
                message = match.group("message");
                switch (command) {
                    case "\\q":
                        continue;
                    case "\\m":
                        when(messageEvent.getMessage()).thenReturn(message);
                        plugin.onMessage(messageEvent);
                        break;
                    case "\\p":
                        when(privateEvent.getMessage()).thenReturn(message);
                        plugin.onPrivateMessage(privateEvent);
                        break;
                    default:
                        throw new Exception(String.format("Unknown command %s", command));
                }
            } catch(Exception e) {
                errorMessage(e);
            }
        }
    }

    public void privateMessage(String response) {
        System.out.printf("[p] %s\n", response);
    }
    public void channelMessage(String response) {
        System.out.printf("[m] %s\n", response);
    }
    public void errorMessage(Exception e) {
        System.out.printf("[e] %s\n", e.getMessage());
        e.printStackTrace();
    }

    public static void main(String args[]) {
        new ManualTestClient();
    }

    private void setupMocks() {
        doAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            privateMessage(arg);
            return null;
        }).when(messageEvent).respond(anyString());
        doAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            channelMessage(arg);
            return null;
        }).when(privateEvent).respondWith(anyString());
        when(messageEvent.getUser()).thenReturn(user);
        when(privateEvent.getUser()).thenReturn(user);
        when(messageEvent.getChannel()).thenReturn(channel);
        when(channel.getNormalUsers()).thenReturn(ImmutableSortedSet.of());
        when(channel.getOps()).thenReturn(ImmutableSortedSet.of(user));
        when(messageEvent.getBot()).thenReturn(bot);
        when(privateEvent.getBot()).thenReturn(bot);
        when(bot.getUserChannelDao()).thenReturn(userChannelDao);
        when(userChannelDao.getAllChannels()).thenReturn(ImmutableSortedSet.of(channel));
        when(user.getNick()).thenReturn("test");
    }
}
