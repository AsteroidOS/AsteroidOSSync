/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.asteroidos.sync.dataobjects;

import java.nio.charset.StandardCharsets;

public class Notification {
    String packageName, appName, appIcon, summary, body, vibration = "";
    final MsgType msgType;
    final int id;

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
