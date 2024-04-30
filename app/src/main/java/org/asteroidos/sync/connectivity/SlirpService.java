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

package org.asteroidos.sync.connectivity;

import android.content.Context;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

public class SlirpService implements IConnectivityService {

    private final IAsteroidDevice mDevice;

    private final Context mCtx;

    private final Thread slirpThread;

    private volatile int mtu;

    public SlirpService(Context ctx, IAsteroidDevice device) {
        mDevice = device;
        mCtx = ctx;

        slirpThread = new Thread(() -> {
            FileDescriptor fd = getVdeFd();
            StructPollfd pollfd = new StructPollfd();
            pollfd.fd = fd;
            pollfd.events = (short) OsConstants.POLLIN;
            StructPollfd[] pollfds = new StructPollfd[] { pollfd };
            while (true) {
                try {
                    if (Os.poll(pollfds, 1500) == 0) {
                        continue;
                    }

                    ByteBuffer rx = ByteBuffer.allocateDirect(mtu);
                    long read = vdeRecv(rx, 0, mtu);
                    byte[] data = new byte[(int) read];
                    rx.get(data);
                    mDevice.send(AsteroidUUIDS.SLIRP_OUTGOING_CHAR, data, SlirpService.this);

//                    resetMtu();
                } catch (Exception e) {
                    Log.e("SlirpService", e.toString());
                }
            }
        });

        mtu = mDevice.getMtu();

        initNative(mtu - 14);

        mDevice.registerCallback(AsteroidUUIDS.SLIRP_INCOMING_CHAR, data -> {
            resetMtu();

            ByteBuffer tx = ByteBuffer.allocateDirect(data.length);
            tx.put(data);
            vdeSend(tx, 0, data.length);
        });

        slirpThread.start();
    }

    private void resetMtu() {
        int newMtu = mDevice.getMtu();
        if (mtu != newMtu) {
            mtu = newMtu;

            finalizeNative();
            initNative(mtu - 14);
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
        chars.put(AsteroidUUIDS.SLIRP_OUTGOING_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.SLIRP_INCOMING_CHAR, Direction.FROM_WATCH);
        return chars;
    }

    @Override
    protected void finalize() throws Throwable {
        finalizeNative();
        super.finalize();
    }

    static {
        System.loadLibrary("sync");
    }

    @Override
    public UUID getServiceUUID() {
        return AsteroidUUIDS.SLIRP_SERVICE_UUID;
    }

    private long mySlirp = 0;

    private native void initNative(int mtu);

    private native void finalizeNative();

    private native long vdeRecv(ByteBuffer buffer, long offset, long count);

    private native long vdeSend(ByteBuffer buffer, long offset, long count);

    private native FileDescriptor getVdeFd();
}
