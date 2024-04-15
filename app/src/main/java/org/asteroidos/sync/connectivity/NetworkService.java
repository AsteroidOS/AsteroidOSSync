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

package org.asteroidos.sync.connectivity;

import android.content.Context;
import android.util.Log;

import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.utils.AsteroidUUIDS;
import org.asteroidos.sync.utils.UnixSocketManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.UUID;
import java.util.Arrays;
import java.nio.ByteBuffer;

public class NetworkService implements IConnectivityService {
    private final Context mCtx;
    private final IAsteroidDevice mDevice;

    private UnixSocketManager mSocket;

    private byte[] mAccumulatedData;
    private int mLastSeqNumber = -1;

    public static byte[] concatenate(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public NetworkService(Context ctx, IAsteroidDevice device) {
        mDevice = device;
        mCtx = ctx;
        mAccumulatedData = new byte[]{};

        runPasst();

        device.registerCallback(AsteroidUUIDS.NETWORK_TX_CHAR, data -> {
            byte header = data[0];
            boolean hasMore = (header & 0x80) != 0;
            int seqNumber = (header & 0x7F);

            if (seqNumber != mLastSeqNumber + 1) {
                mAccumulatedData = new byte[]{};
                mLastSeqNumber = -1;
            }

            mLastSeqNumber = seqNumber;
            mAccumulatedData = concatenate(mAccumulatedData, Arrays.copyOfRange(data, 1, data.length));

            if (!hasMore) {
                ByteBuffer qemuHeader = ByteBuffer.allocate(4);
                qemuHeader.order(ByteOrder.BIG_ENDIAN);
                qemuHeader.putInt(mAccumulatedData.length);

                Log.d("NetworkService", "Forwarding message from BLE to passt");
                mSocket.sendMessage(concatenate(qemuHeader.array(), mAccumulatedData));
                mAccumulatedData = new byte[]{};
                mLastSeqNumber = -1;
            }
        });
    }

    private static void streamInputStream(InputStream inputStream, String streamType) {
        new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    Log.i(streamType, line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void runPasst(){
        try {
            String socketName = mCtx.getFilesDir() + "/passt-" + UUID.randomUUID().toString();

            Log.i("NetworkService", "Running passt");
            String passt = mCtx.getApplicationInfo().nativeLibraryDir + "/libpasst.so";
            Process p = Runtime.getRuntime().exec(passt + " -e -s " + socketName);
            streamInputStream(p.getInputStream(), "NetworkService");
            streamInputStream(p.getErrorStream(), "NetworkService");
            p.waitFor();

            mSocket = new UnixSocketManager(socketName, new UnixSocketManager.SocketListener() {
                @Override
                public void onMessageReceived(byte[] data) {
                    Log.d("NetworkService", "Forwarding message from passt to BLE");
                    // Trim the first uint32_t transmitted by QEMU containing the full message length
                    byte[] dataTrimmed = Arrays.copyOfRange(data, 4, data.length);
                    int seqNumber = 0;

                    int totalLength = dataTrimmed.length;
                    int currentIndex = 0;

                    while (currentIndex < totalLength) {
                        final int maxChunkLen = 240 - 1;
                        int chunkLength = Math.min(maxChunkLen, totalLength - currentIndex);
                        byte[] chunk = new byte[chunkLength];
                        System.arraycopy(dataTrimmed, currentIndex, chunk, 0, chunkLength);

                        boolean hasMore = currentIndex + chunkLength < totalLength;
                        byte[] header = { (byte) ((seqNumber & 0x7F) | (hasMore ? 0x80 : 0)) };

                        mDevice.send(AsteroidUUIDS.NETWORK_RX_CHAR, concatenate(header, chunk), NetworkService.this);
                        currentIndex += chunkLength;

                        seqNumber++;
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sync() {
    }

    @Override
    public void unsync() {
    }

    @Override
    public HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.NETWORK_RX_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.NETWORK_TX_CHAR, Direction.FROM_WATCH);
        return chars;
    }

    @Override
    public UUID getServiceUUID() {
        return AsteroidUUIDS.NETWORK_SERVICE_UUID;
    }
}
