package org.asteroidos.sync.dataobjects;

import java.nio.charset.StandardCharsets;

public class Notification {
    String packageName, appName, appIcon, summary, body, vibration = "";
    MsgType msgType = null;
    int id = 0;

    public Notification(MsgType msgType, String packageName, int id, String appName, String appIcon, String summary, String body, String vibration) {
        this.msgType = msgType;
        this.packageName = packageName;
        this.id = id;
        this.appName = appName;
        this.appIcon = appIcon;
        this.summary = summary;
        this.body = body;
        this.vibration = vibration;
    }

    public Notification(MsgType msgType, int id) {
        this.msgType = msgType;
        this.id = id;
    }

    /***
     * @return XML serialized {@link Notification}
     */
    public final String toXML() {
        String xmlRequest = "";

        if (msgType == MsgType.POSTED) {
            xmlRequest = "<insert><id>" + id + "</id>";
            if (!packageName.isEmpty())
                xmlRequest += "<pn>" + packageName + "</pn>";
            if (!vibration.isEmpty())
                xmlRequest += "<vb>" + vibration + "</vb>";
            if (!appName.isEmpty())
                xmlRequest += "<an>" + appName + "</an>";
            if (!appIcon.isEmpty())
                xmlRequest += "<ai>" + appIcon + "</ai>";
            if (!summary.isEmpty())
                xmlRequest += "<su>" + summary + "</su>";
            if (!body.isEmpty())
                xmlRequest += "<bo>" + body + "</bo>";
            xmlRequest += "</insert>";
        } else if (msgType == MsgType.REMOVED) {
            xmlRequest = "<removed>" +
                    "<id>" + id + "</id>" +
                    "</removed>";
        }

        return xmlRequest;
    }

    /***
     * @return Returns {@link Notification#toXML()} as byte[] for BLE transmission
     */
    public final byte[] toBytes() {
        return this.toXML().getBytes(StandardCharsets.UTF_8);
    }

    public enum MsgType {
        POSTED, REMOVED
    }
}
