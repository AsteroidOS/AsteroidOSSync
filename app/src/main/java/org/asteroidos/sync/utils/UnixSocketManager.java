/*
 * AsteroidOSSync
 * Copyright (c) 2024 AsteroidOS
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

package org.asteroidos.sync.utils;

import android.net.LocalSocketAddress;
import android.net.LocalSocket;
import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UnixSocketManager {
    private LocalSocket clientSocket;

    private SocketListener socketListener;
    private Handler handler;

    public UnixSocketManager(String socketName, SocketListener listener) {
        try {
            this.clientSocket = new LocalSocket();
            this.clientSocket.connect(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
            this.socketListener = listener;
            this.handler = new Handler(Looper.getMainLooper());
            handleMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = UnixSocketManager.this.clientSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byte[] data = Arrays.copyOfRange(buffer, 0, bytesRead);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                socketListener.onMessageReceived(data);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendMessage(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket.getOutputStream().write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public interface SocketListener {
        void onMessageReceived(byte[] data);
    }
}