package hello;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatGetPermalinkRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.MessageEvent;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MyApp {
    public static void main(String[] args) throws Exception {
        // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
        App app = new App();

        String bugURLBase = "https://bugs.schedmd.com/show_bug.cgi?id=";
        app.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();
            String text = event.getText();
            MethodsClient client = ctx.client();

            Matcher bugMatcher = Pattern.compile("bug[\\s]*[0-9]+", Pattern.CASE_INSENSITIVE).matcher(text);
            if (bugMatcher.find()) {
                bugMatcher.reset();
                Set<String> bugNums = new TreeSet<>();
                while (bugMatcher.find()) {
                    String bugMention = bugMatcher.group();
                    Matcher numMatcher = Pattern.compile("[0-9]+").matcher(bugMention);
                    String bugNum = "";
                    while (numMatcher.find()) {
                        bugNum = numMatcher.group();
                    }
                    bugNums.add(bugNum);
                }

                StringBuilder bugPrint = new StringBuilder();
                for (String bugNum : bugNums) {
                    bugPrint.append("<");
                    bugPrint.append(bugURLBase);
                    bugPrint.append(bugNum);
                    bugPrint.append("|Bug ");
                    bugPrint.append(bugNum);
                    bugPrint.append(">\n");
                }
                ChatPostMessageResponse message = client.chatPostMessage(r -> r
                        .channel(event.getChannel())
                        .text(bugPrint.toString()));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed {}", message.getError());
                }
            }
            return ctx.ack();
        });

        SlackAppServer server = new SlackAppServer(app);
        server.start(); // http://localhost:3000/slack/events
    }
}