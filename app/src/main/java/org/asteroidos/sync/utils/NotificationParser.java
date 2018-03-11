package org.asteroidos.sync.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Originally from https://github.com/matejdro/PebbleNotificationCenter-Android written by Matej Drobnič under the terms of the GPLv3

public class NotificationParser {
    public String summary;
    public String body;

    public NotificationParser(Notification notification)
    {
        this.summary = null;
        this.body = "";

        if (tryParseNatively(notification))
            return;

        getExtraBigData(notification);
    }

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    private boolean tryParseNatively(Notification notification)
    {
        Bundle extras = notification.extras;
        if (extras == null)
            return false;

        if (parseMessageStyleNotification(notification, extras))
            return true;

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null && extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).length > 0)
        {
            if (parseInboxNotification(extras))
                return true;
        }

        if (extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null && extras.get(Notification.EXTRA_BIG_TEXT) == null)
            return false;

        if (extras.get(Notification.EXTRA_TITLE_BIG) != null)
        {
            CharSequence bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
            if (bigTitle.length() < 40 || extras.get(Notification.EXTRA_TITLE) == null)
                summary = bigTitle.toString();
            else
                summary = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        }
        else if (extras.get(Notification.EXTRA_TITLE) != null)
            summary = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null)
        {
            for (CharSequence line : extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
                body += formatCharSequence(line) + "\n\n";

            body = body.trim();
        }
        else if (extras.get(Notification.EXTRA_BIG_TEXT) != null)
            body = formatCharSequence(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        else
            body = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TEXT));

        if (extras.get(Notification.EXTRA_SUB_TEXT) != null)
        {
            body = body.trim();
            body = body + "\n\n" + formatCharSequence(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        }

        return true;
    }

    private boolean parseMessageStyleNotification(Notification notification, Bundle extras)
    {
        NotificationCompat.MessagingStyle messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        if (messagingStyle == null)
            return false;

        summary = formatCharSequence(messagingStyle.getConversationTitle());
        if (TextUtils.isEmpty(summary))
            summary = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        if (TextUtils.isEmpty(summary))
            summary = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TITLE));
        if (summary == null)
            summary = "";

        List<NotificationCompat.MessagingStyle.Message> messagesDescending = new ArrayList<>(messagingStyle.getMessages());
        Collections.sort(messagesDescending, new Comparator<NotificationCompat.MessagingStyle.Message>() {
            @Override
            public int compare(NotificationCompat.MessagingStyle.Message m1, NotificationCompat.MessagingStyle.Message m2) {
                return (int) (m2.getTimestamp() - m1.getTimestamp());
            }
        });

        body = "";
        for (NotificationCompat.MessagingStyle.Message message : messagesDescending)
        {
            String sender;
            if (message.getSender() == null)
                sender = formatCharSequence(messagingStyle.getUserDisplayName());
            else
                sender = formatCharSequence(message.getSender());

            body += sender + ": " + message.getText() + "\n";
        }

        return true;
    }

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    public boolean parseInboxNotification(Bundle extras)
    {
        if (extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) != null)
            summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString();
        else if (extras.getCharSequence(Notification.EXTRA_SUB_TEXT) != null)
            summary = extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString();
        else if (extras.getCharSequence(Notification.EXTRA_TITLE) != null)
            summary = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        else
            return false;

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

        int i = 0;
        while (true)
        {
            body += formatCharSequence(lines[i]) + "\n\n";

            i++;
            if (i >= lines.length)
                break;
        }

        return true;
    }

    private String formatCharSequence(CharSequence sequence)
    {
        if (sequence == null)
            return "";

        if (!(sequence instanceof SpannableString))
            return sequence.toString();

        SpannableString spannableString = (SpannableString) sequence;
        String text = spannableString.toString();

        StyleSpan[] spans = spannableString.getSpans(0, spannableString.length(), StyleSpan.class);


        int amountOfBoldspans = 0;

        for (int i = spans.length - 1; i >= 0; i--)
        {
            StyleSpan span = spans[i];
            if (span.getStyle() == Typeface.BOLD)
                amountOfBoldspans++;
        }

        if (amountOfBoldspans == 1)
        {
            for (int i = spans.length - 1; i >= 0; i--)
            {
                StyleSpan span = spans[i];
                if (span.getStyle() == Typeface.BOLD)
                {
                    text = insertString(text, spannableString.getSpanEnd(span));
                    break;
                }
            }
        }

        return text;
    }

    private static String insertString(String text, int pos)
    {
        return text.substring(0, pos).trim().concat("\n").trim().concat(text.substring(pos)).trim();
    }

    private void getExtraData(Notification notification) {
        RemoteViews views = notification.contentView;
        if (views == null)
            return;

        parseRemoteView(views);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getExtraBigData(Notification notification) {
        RemoteViews views;
        try {
            views = notification.bigContentView;
        } catch (NoSuchFieldError e) {
            getExtraData(notification);
            return;
        }
        if (views == null) {
            getExtraData(notification);
            return;
        }

        parseRemoteView(views);
    }

    @SuppressLint("PrivateApi")
    private void parseRemoteView(RemoteViews views)
    {
        try {
            Class remoteViewsClass = RemoteViews.class;
            Class baseActionClass = Class.forName("android.widget.RemoteViews$Action");

            Field actionsField = remoteViewsClass.getDeclaredField("mActions");

            actionsField.setAccessible(true);

            ArrayList<Object> actions = (ArrayList<Object>) actionsField.get(views);
            for (Object action : actions) {
                if (!action.getClass().getName().contains("$ReflectionAction"))
                    continue;

                Field typeField = action.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                int type = typeField.getInt(action);
                if (type != 9 && type != 10)
                    continue;


                int viewId = -1;
                try
                {
                    Field idField = baseActionClass.getDeclaredField("viewId");
                    idField.setAccessible(true);
                    viewId = idField.getInt(action);
                }
                catch (NoSuchFieldException ignored) {}

                Field valueField = action.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                CharSequence value = (CharSequence) valueField.get(action);

                if (value == null ||
                        value.equals("...") ||
                        isInteger(value.toString()) ||
                        body.contains(value))
                {
                    continue;
                }

                if (viewId == android.R.id.title)
                {
                    if (summary == null || summary.length() < value.length())
                        summary = value.toString();
                }
                else
                    body += formatCharSequence(value) + "\n\n";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
