package org.asteroidos.sync.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

// Originally from https://github.com/matejdro/PebbleNotificationCenter-Android written by Matej Drobnič under the terms of the GPLv3

public class NotificationParser {
    public String summary;
    public String body;

    public NotificationParser(Notification notification) {
        this.summary = null;
        this.body = "";

        if (tryParseNatively(notification))
            return;

        getExtraBigData(notification);
    }

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    private boolean tryParseNatively(Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null)
            return false;

        if (parseMessageStyleNotification(notification, extras))
            return true;

        CharSequence[] textLinesSequence = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (textLinesSequence != null && textLinesSequence.length > 0) {
            if (parseInboxNotification(extras))
                return true;
        }

        if (extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null && extras.get(Notification.EXTRA_BIG_TEXT) == null)
            return false;

        CharSequence bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);

        if (bigTitle != null && (bigTitle.length() < 40 || extras.get(Notification.EXTRA_TITLE) == null))
            summary = bigTitle.toString();
        else if (title != null)
            summary = title.toString();

        if (extras.get(Notification.EXTRA_TEXT_LINES) != null) {
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

            StringBuilder sb = new StringBuilder();
            sb.append(body);
            if (lines != null) {
                for (CharSequence line : lines) {
                    sb.append(formatCharSequence(line));
                    sb.append("\n\n");
                }
            }

            body = sb.toString().trim();
        } else if (extras.get(Notification.EXTRA_BIG_TEXT) != null)
            body = formatCharSequence(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        else
            body = formatCharSequence(extras.getCharSequence(Notification.EXTRA_TEXT));

        return true;
    }

    private boolean parseMessageStyleNotification(Notification notification, Bundle extras) {
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
        messagesDescending.sort((m1, m2) -> (int) (m2.getTimestamp() - m1.getTimestamp()));

        StringBuilder sb = new StringBuilder();
        body = "";

        for (NotificationCompat.MessagingStyle.Message message : messagesDescending) {
            String sender;
            if (message.getPerson() == null || message.getPerson().getName() == null)
                sender = formatCharSequence(messagingStyle.getUser().getName());
            else
                sender = formatCharSequence(message.getPerson().getName());

            sb.append(sender);
            sb.append(": ");
            sb.append(message.getText());
            sb.append("\n");
        }

        body = sb.toString().trim();

        return true;
    }

    @TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
    private boolean parseInboxNotification(Bundle extras) {
        CharSequence summaryTextSequence = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        CharSequence subTextSequence = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        CharSequence titleSequence = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);

        if (summaryTextSequence != null)
            summary = summaryTextSequence.toString();
        else if (subTextSequence != null)
            summary = subTextSequence.toString();
        else if (titleSequence != null)
            summary = titleSequence.toString();
        else
            return false;

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

        StringBuilder sb = new StringBuilder();
        sb.append(body);
        if (lines != null) {
            for (CharSequence line : lines) {
                sb.append(formatCharSequence(line));
                sb.append("\n\n");
            }
        }

        body = sb.toString().trim();

        return true;
    }

    private String formatCharSequence(CharSequence sequence) {
        if (sequence == null)
            return "";

        if (!(sequence instanceof SpannableString))
            return sequence.toString();

        SpannableString spannableString = (SpannableString) sequence;
        String text = spannableString.toString();

        StyleSpan[] spans = spannableString.getSpans(0, spannableString.length(), StyleSpan.class);


        int amountOfBoldspans = 0;

        for (int i = spans.length - 1; i >= 0; i--) {
            StyleSpan span = spans[i];
            if (span.getStyle() == Typeface.BOLD)
                amountOfBoldspans++;
        }

        if (amountOfBoldspans == 1) {
            for (int i = spans.length - 1; i >= 0; i--) {
                StyleSpan span = spans[i];
                if (span.getStyle() == Typeface.BOLD) {
                    text = insertString(text, spannableString.getSpanEnd(span));
                    break;
                }
            }
        }

        return text;
    }

    private static String insertString(String text, int pos) {
        return text.substring(0, pos).trim().concat("\n").trim().concat(text.substring(pos)).trim();
    }

    private void getExtraData(Notification notification) {
        // TODO fix this deprecation ASAP
        //noinspection deprecation
        RemoteViews views = notification.contentView;
        if (views == null)
            return;

        parseRemoteView(views);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getExtraBigData(Notification notification) {
        RemoteViews views;
        try {
            // TODO fix this deprecation ASAP
            //noinspection deprecation
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

    /**
     * TODO Figure out better way to parseRemoteView without using private APIs
     */
    @SuppressWarnings({"unchecked", "rawtypes", "JavaReflectionMemberAccess"})
    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    private void parseRemoteView(RemoteViews views) {
        try {
            Class remoteViewsClass = RemoteViews.class;
            Class baseActionClass = Class.forName("android.widget.RemoteViews$Action");

            Field actionsField = remoteViewsClass.getDeclaredField("mActions");

            actionsField.setAccessible(true);

            ArrayList<Object> actions = (ArrayList<Object>) actionsField.get(views);

            StringBuilder sb = new StringBuilder();
            sb.append(body);

            if (actions != null) {
                for (Object action : actions) {
                    if (!action.getClass().getName().contains("$ReflectionAction"))
                        continue;

                    Field typeField = action.getClass().getDeclaredField("type");
                    typeField.setAccessible(true);
                    int type = typeField.getInt(action);
                    if (type != 9 && type != 10)
                        continue;


                    int viewId = -1;
                    try {
                        Field idField = baseActionClass.getDeclaredField("viewId");
                        idField.setAccessible(true);
                        viewId = idField.getInt(action);
                    } catch (NoSuchFieldException ignored) {
                    }

                    Field valueField = action.getClass().getDeclaredField("value");
                    valueField.setAccessible(true);
                    CharSequence value = (CharSequence) valueField.get(action);

                    if (value == null ||
                            value.equals("...") ||
                            isInteger(value.toString()) ||
                            body.contains(value)) {
                        continue;
                    }

                    if (viewId == android.R.id.title) {
                        if (summary == null || summary.length() < value.length())
                            summary = value.toString();
                    } else {
                        sb.append(formatCharSequence(value));
                        sb.append("\n\n");
                    }
                }
            }

            body = sb.toString().trim();
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
