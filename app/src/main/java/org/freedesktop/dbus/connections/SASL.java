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

package org.freedesktop.dbus.connections;

import static org.freedesktop.dbus.connections.SASL.SaslCommand.*;

import org.freedesktop.dbus.config.DBusSysProps;
import org.freedesktop.dbus.connections.config.SaslConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.AbstractUnixTransport;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.SocketClosedException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.freedesktop.dbus.utils.TimeMeasure;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.*;

public class SASL {
    public static final int       AUTH_NONE                   = 0;
    public static final int       AUTH_EXTERNAL               = 1;
    public static final int       AUTH_SHA                    = 2;
    public static final int       AUTH_ANON                   = 4;

    public static final int       LOCK_TIMEOUT                = 1000;
    public static final int       NEW_KEY_TIMEOUT_SECONDS     = 60 * 5;
    public static final int       EXPIRE_KEYS_TIMEOUT_SECONDS = NEW_KEY_TIMEOUT_SECONDS + (60 * 2);
    public static final int       MAX_TIME_TRAVEL_SECONDS     = 60 * 5;
    public static final int       COOKIE_TIMEOUT              = 240;
    public static final String    COOKIE_CONTEXT              = "org_freedesktop_java";

    private static final String   AUTH_TYPE_EXTERNAL          = "EXTERNAL";
    private static final String   AUTH_TYPE_DBUS_COOKIE_SHA1  = "DBUS_COOKIE_SHA1";
    private static final String   AUTH_TYPE_ANONYMOUS         = "ANONYMOUS";

    private static final String   INVALID_CMD_ERR             = "Got invalid command";

    // stop reading when reaching ~1MByte of data
    private static final int      MAX_READ_BYTES              = 1024 * 1024;

    private static final Random   RANDOM                      = new Random();

    private static final Collator COL = Collator.getInstance();
    static {
        COL.setStrength(Collator.PRIMARY);
    }

    private static final String   SYSPROP_USER_HOME           = System.getProperty("user.home");
    private static final String   DBUS_TEST_HOME_DIR          = System.getProperty(DBusSysProps.SYSPROP_DBUS_TEST_HOME_DIR);

    private static final File     DBUS_KEYRINGS_DIR           = new File(SYSPROP_USER_HOME, ".dbus-keyrings");

    private static final Set<PosixFilePermission> BAD_FILE_PERMISSIONS =
            Collections.emptySet();

    private String challenge = "";
    private String cookie    = "";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    /** whether file descriptor passing is supported on the current connection. */
    private boolean fileDescriptorSupported;
    private final SaslConfig saslConfig;

    /**
     * Create a new SASL auth handler.
     *
     * @param _saslConfig SASL configuration
     */
    public SASL(SaslConfig _saslConfig) {
        saslConfig = Objects.requireNonNull(_saslConfig, "Sasl Configuration required");
    }

    private String findCookie(String _context, String _id) throws IOException {
        File keyringDir = DBUS_KEYRINGS_DIR;
        if (!Util.isBlank(DBUS_TEST_HOME_DIR)) {
            keyringDir = new File(DBUS_TEST_HOME_DIR);
        }

        File f = new File(keyringDir, _context);
        long currentTime = System.currentTimeMillis() / 1000;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            String s = null;
            String lCookie = null;

            while (null != (s = r.readLine())) {
                String[] line = s.split(" ");
                if (line.length != 3) {
                    continue;
                }
                long timestamp;
                try {
                    timestamp = Long.parseLong(line[1]);
                } catch (NumberFormatException _ex) {
                    continue;
                }
                if (line[0].equals(_id) && timestamp >= 0 && currentTime >= timestamp - MAX_TIME_TRAVEL_SECONDS && currentTime < timestamp + EXPIRE_KEYS_TIMEOUT_SECONDS) {
                    lCookie = line[2];
                    break;
                }
            }
            return lCookie;
        }
    }

    @SuppressWarnings("checkstyle:emptyblock")
    private void addCookie(String _context, String _id, long _timestamp, String _cookie) throws IOException {
        throw new IllegalArgumentException("Not implemented yet");
    }

    /**
     * Takes the string, encodes it as hex and then turns it into a string again.
     * No, I don't know why either.
     */
    private String stupidlyEncode(String _data) {
        return Hexdump.toHex(_data.getBytes(), false);
    }

    private String stupidlyEncode(byte[] _data) {
        return Hexdump.toHex(_data, false);
    }

    private byte getNibble(char _c) {
        return switch (_c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (byte) (_c - '0');
            case 'A', 'B', 'C', 'D', 'E', 'F' -> (byte) (_c - 'A' + 10);
            case 'a', 'b', 'c', 'd', 'e', 'f' -> (byte) (_c - 'a' + 10);
            default -> 0;
        };
    }

    private String stupidlyDecode(String _data) {
        char[] cs = new char[_data.length()];
        char[] res = new char[cs.length / 2];
        _data.getChars(0, _data.length(), cs, 0);
        for (int i = 0, j = 0; j < res.length; i += 2, j++) {
            int b = 0;
            b |= getNibble(cs[i]) << 4;
            b |= getNibble(cs[i + 1]);
            res[j] = (char) b;
        }
        return new String(res);
    }

    public SASL.Command receive(SocketChannel _sock) throws IOException {
        StringBuilder sb = new StringBuilder();
        ByteBuffer buf = ByteBuffer.allocate(1); // only read one byte at a time to avoid reading to much (which would break the next message)

        boolean runLoop = true;
        int bytesRead = 0;
        while (runLoop) {

            int read = _sock.read(buf);
            bytesRead += read;
            buf.position(0);
            if (read == -1) {
                throw new SocketClosedException("Stream unexpectedly short (broken pipe)");
            }

            for (int i = buf.position(); i < read; i++) {
                byte c = buf.get();
                if (c == 0 || c == '\r') {
                    continue;
                } else if (c == '\n') {
                    runLoop = false;
                    break;
                } else {
                    sb.append((char) c);
                }
            }
            buf.clear();

            if (bytesRead > MAX_READ_BYTES) { // safe-guard to stop reading if no \n found
                break;
            }
        }

        logger.trace("received: {}", sb);
        try {
            return new Command(sb.toString());
        } catch (Exception _ex) {
            logger.error("Cannot create command.", _ex);
            throw new AuthenticationException("Failed to authenticate.", _ex);
        }
    }

    public void send(SocketChannel _sock, SaslCommand _command, String... _data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(_command.name());

        for (String s : _data) {
            sb.append(' ');
            sb.append(s);
        }
        sb.append('\r');
        sb.append('\n');
        logger.trace("sending: {}", sb);
        _sock.write(ByteBuffer.wrap(sb.toString().getBytes()));
    }

    SaslResult doChallenge(int _auth, SASL.Command _c) throws IOException {
        switch (_auth) {
        case AUTH_SHA:
            String[] reply = stupidlyDecode(_c.getData()).split(" ");
            LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Auth data: {}", Arrays.toString(reply)));

            if (3 != reply.length) {
                logger.debug("Reply is not length 3");
                return SaslResult.ERROR;
            }

            String context = reply[0];
            String id = reply[1];
            final String serverchallenge = reply[2];

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException _ex) {
                logger.debug("Could not find SHA algorithm", _ex);
                return SaslResult.ERROR;
            }

            byte[] buf = new byte[8];

            // ensure we get a (more or less unique) positive long
            long seed = Optional.of(System.nanoTime()).map(t -> t < 0 ? t * -1 : t).get();

            Message.marshallintBig(seed, buf, 0, 8);
            String clientchallenge = stupidlyEncode(md.digest(buf));
            md.reset();

            TimeMeasure tm = new TimeMeasure();
            String lCookie = null;

            while (lCookie == null && tm.getElapsed() < LOCK_TIMEOUT) {
                lCookie = findCookie(context, id);
            }

            if (lCookie == null) {
                logger.debug("Did not find a cookie in context {}  with ID {}", context, id);
                return SaslResult.ERROR;
            }

            String response = serverchallenge + ":" + clientchallenge + ":" + lCookie;
            buf = md.digest(response.getBytes());

            if (logger.isTraceEnabled()) {
                logger.trace("Response: {} hash: {}", response, Hexdump.format(buf));
            }

            response = stupidlyEncode(buf);
            _c.setResponse(stupidlyEncode(clientchallenge + " " + response));
            return SaslResult.OK;
        case AUTH_ANON:
            // Pong back DATA if server wants it for anonymous auth
            _c.setResponse(_c.getData() == null ? "" : _c.getData());
            return SaslResult.OK;
        default:
            logger.debug("Not DBUS_COOKIE_SHA1 authtype.");
            return SaslResult.ERROR;
        }
    }

    SaslResult doResponse(int _auth, String _uid, String _kernelUid, SASL.Command _c) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException _ex) {
            logger.error("SHA hash algorithm not available", _ex);
            return SaslResult.ERROR;
        }
        switch (_auth) {
            case AUTH_NONE:
                switch (_c.getMechs()) {
                case AUTH_ANON:
                    return SaslResult.OK;
                case AUTH_EXTERNAL:
                    if (0 == COL.compare(_uid, _c.getData()) && (null == _kernelUid || 0 == COL.compare(_uid, _kernelUid))) {
                        return SaslResult.OK;
                    } else {
                        return SaslResult.REJECT;
                    }
                case AUTH_SHA:
                    String context = COOKIE_CONTEXT;
                    long id = System.currentTimeMillis();
                    byte[] buf = new byte[8];
                    Message.marshallintBig(id, buf, 0, 8);
                    challenge = stupidlyEncode(md.digest(buf));

                    RANDOM.nextBytes(buf);
                    cookie = stupidlyEncode(md.digest(buf));
                    try {
                        addCookie(context, "" + id, id / 1000, cookie);
                    } catch (IOException _ex) {
                        logger.error("Error authenticating using cookie", _ex);
                        return SaslResult.ERROR;
                    }

                    logger.debug("Sending challenge: {} {} {}", context, id, challenge);

                    _c.setResponse(stupidlyEncode(context + ' ' + id + ' ' + challenge));
                    return SaslResult.CONTINUE;
                default:
                    return SaslResult.ERROR;
                }
            case AUTH_SHA:
                String[] response = stupidlyDecode(_c.getData()).split(" ");
                if (response.length < 2) {
                    return SaslResult.ERROR;
                }
                String cchal = response[0];
                String hash = response[1];
                String prehash = challenge + ":" + cchal + ":" + cookie;
                byte[] buf = md.digest(prehash.getBytes());
                String posthash = stupidlyEncode(buf);
                logger.debug("Authenticating Hash; data={} remote-hash={} local-hash={}", prehash, hash, posthash);
                if (0 == COL.compare(posthash, hash)) {
                    return SaslResult.OK;
                } else {
                    return SaslResult.ERROR;
                }
            default:
                return SaslResult.ERROR;
            }
    }

    public String[] convertAuthTypes(int _types) {
        return switch (_types) {
            case AUTH_EXTERNAL -> new String[] {
                        AUTH_TYPE_EXTERNAL
                };
            case AUTH_SHA -> new String[] {
                        AUTH_TYPE_DBUS_COOKIE_SHA1
                };
            case AUTH_ANON -> new String[] {
                        AUTH_TYPE_ANONYMOUS
                };
            case AUTH_SHA + AUTH_EXTERNAL -> new String[] {
                        AUTH_TYPE_EXTERNAL, AUTH_TYPE_DBUS_COOKIE_SHA1
                };
            case AUTH_SHA + AUTH_ANON -> new String[] {
                        AUTH_TYPE_ANONYMOUS, AUTH_TYPE_DBUS_COOKIE_SHA1
                };
            case AUTH_EXTERNAL + AUTH_ANON -> new String[] {
                        AUTH_TYPE_ANONYMOUS, AUTH_TYPE_EXTERNAL
                };
            case AUTH_EXTERNAL + AUTH_ANON + AUTH_SHA -> new String[] {
                        AUTH_TYPE_ANONYMOUS, AUTH_TYPE_EXTERNAL, AUTH_TYPE_DBUS_COOKIE_SHA1
                };
            default -> new String[] {};
        };
    }

    /**
     * Performs SASL auth on the given socketchannel.
     * Mode selects whether to run as a SASL server or client.
     * Types is a bitmask of the available auth types.
     *
     * @param _sock socket channel
     * @param _transport transport
     *
     * @return true if the auth was successful and false if it failed.
     * @throws IOException on failure
     */
    public boolean auth(SocketChannel _sock, AbstractTransport _transport) throws IOException {
        String luid = null;
        String kernelUid = null;

        long uid = saslConfig.getSaslUid().orElse(getUserId());
        luid = stupidlyEncode("" + uid);

        SASL.Command c;
        int failed = 0;
        int current = 0;
        SaslAuthState state = SaslAuthState.INITIAL_STATE;

        while (state != SaslAuthState.FINISHED && state != SaslAuthState.FAILED) {

            logger.trace("Mode: {} AUTH state: {}", saslConfig.getMode(), state);

            switch (saslConfig.getMode()) {
            case CLIENT:
                switch (state) {
                case INITIAL_STATE:
                    _sock.write(ByteBuffer.wrap(new byte[] {0}));
                    send(_sock, AUTH);
                    state = SaslAuthState.WAIT_DATA;
                    break;
                case WAIT_DATA:
                    c = receive(_sock);
                    switch (c.getCommand()) {
                        case DATA:
                            switch (doChallenge(current, c)) {
                                case CONTINUE:
                                    send(_sock, DATA, c.getResponse());
                                    break;
                                case OK:
                                    send(_sock, DATA, c.getResponse());
                                    state = SaslAuthState.WAIT_OK;
                                    break;
                                case ERROR:
                                default:
                                    send(_sock, ERROR, c.getResponse());
                                    break;
                            }
                        break;
                        case REJECTED:
                            failed |= current;
                            int available = c.getMechs() & (~failed);
                            int retVal = handleReject(available, luid, _sock);
                            if (retVal == -1) {
                                state = SaslAuthState.FAILED;
                            } else {
                                current = retVal;
                            }
                            break;
                        case ERROR:
                            // when asking for file descriptor support, ERROR means FD support is not supported
                            if (state == SaslAuthState.NEGOTIATE_UNIX_FD) {
                                state = SaslAuthState.FINISHED;
                                logger.trace("File descriptors NOT supported by server");
                                fileDescriptorSupported = false;
                                send(_sock, BEGIN);
                            } else {
                                send(_sock, CANCEL);
                                state = SaslAuthState.WAIT_REJECT;
                            }
                            break;
                        case OK:
                            logger.trace("Authenticated");

                            if (saslConfig.isFileDescriptorSupport()) {
                                state = SaslAuthState.WAIT_DATA;
                                logger.trace("Asking for file descriptor support");
                                // if authentication was successful, ask remote end for file descriptor support
                                send(_sock, NEGOTIATE_UNIX_FD);
                            } else {
                                state = SaslAuthState.FINISHED;
                                send(_sock, BEGIN);
                            }
                            break;
                        case AGREE_UNIX_FD:
                            if (saslConfig.isFileDescriptorSupport()) {
                                state = SaslAuthState.FINISHED;
                                logger.trace("File descriptors supported by server");
                                fileDescriptorSupported = true;
                                send(_sock, BEGIN);
                            }
                            break;
                        default:
                            send(_sock, ERROR, INVALID_CMD_ERR);
                            break;
                        }
                    break;
                case WAIT_OK:
                    c = receive(_sock);
                    switch (c.getCommand()) {
                    case OK:
                        send(_sock, BEGIN);
                        state = SaslAuthState.FINISHED;
                        break;
                    case ERROR, DATA:
                        send(_sock, CANCEL);
                        state = SaslAuthState.WAIT_REJECT;
                        break;
                    case REJECTED:
                        failed |= current;
                        int available = c.getMechs() & (~failed);
                        state = SaslAuthState.WAIT_DATA;
                        if (0 != (available & AUTH_EXTERNAL)) {
                            send(_sock, AUTH, AUTH_TYPE_EXTERNAL, luid);
                            current = AUTH_EXTERNAL;
                        } else if (0 != (available & AUTH_SHA)) {
                            send(_sock, AUTH, AUTH_TYPE_DBUS_COOKIE_SHA1, luid);
                            current = AUTH_SHA;
                        } else if (0 != (available & AUTH_ANON)) {
                            send(_sock, AUTH, AUTH_TYPE_ANONYMOUS);
                            current = AUTH_ANON;
                        } else {
                            state = SaslAuthState.FAILED;
                        }
                        break;
                    default:
                        send(_sock, ERROR, INVALID_CMD_ERR);
                        break;
                    }
                    break;
                case WAIT_REJECT:
                    c = receive(_sock);
                    if (c.getCommand() == REJECTED) {
                            failed |= current;
                            int available = c.getMechs() & (~failed);
                            int retVal = handleReject(available, luid, _sock);
                            if (retVal == -1) {
                                state = SaslAuthState.FAILED;
                            } else {
                                current = retVal;
                            }
                        } else {
                            state = SaslAuthState.FAILED;
                    }
                    break;
                default:
                    state = SaslAuthState.FAILED;
                }
                break;
            case SERVER:
                switch (state) {
                    case INITIAL_STATE:
                        ByteBuffer buf = ByteBuffer.allocate(1);
                        if (_sock instanceof NetworkChannel) {
                            _sock.read(buf); // 0
                            state = SaslAuthState.WAIT_AUTH;
                        } else {
                            try {
                                int kuid = -1;
                                if (_transport instanceof AbstractUnixTransport aut) {
                                    kuid = aut.getUid(_sock);
                                }
                                if (kuid >= 0) {
                                    kernelUid = stupidlyEncode("" + kuid);
                                }
                                state = SaslAuthState.WAIT_AUTH;

                            } catch (SocketException _ex) {
                                state = SaslAuthState.FAILED;
                            }
                        }
                    break;
                    case WAIT_AUTH:
                        c = receive(_sock);
                        switch (c.getCommand()) {
                            case AUTH:
                                switch (doResponse(current, luid, kernelUid, c)) {
                                    case CONTINUE:
                                        send(_sock, DATA, c.getResponse());
                                        current = c.getMechs();
                                        state = SaslAuthState.WAIT_DATA;
                                        break;
                                    case OK:
                                        send(_sock, OK, saslConfig.getGuid());
                                        state = SaslAuthState.WAIT_BEGIN;
                                        current = 0;
                                        break;
                                    case REJECT:
                                    default:
                                        send(_sock, REJECTED, convertAuthTypes(saslConfig.getAuthMode()));
                                        current = 0;
                                        break;
                                }
                                break;
                            case ERROR:
                                send(_sock, REJECTED, convertAuthTypes(saslConfig.getAuthMode()));
                                break;
                            case BEGIN:
                                state = SaslAuthState.FAILED;
                                break;
                            default:
                                send(_sock, ERROR, INVALID_CMD_ERR);
                                break;
                            }
                    break;
                    case WAIT_DATA:
                        c = receive(_sock);
                    switch (c.getCommand()) {
                    case DATA:
                        switch (doResponse(current, luid, kernelUid, c)) {
                            case CONTINUE:
                                send(_sock, DATA, c.getResponse());
                                state = SaslAuthState.WAIT_DATA;
                                break;
                            case OK:
                                send(_sock, OK, saslConfig.getGuid());
                                state = SaslAuthState.WAIT_BEGIN;
                                current = 0;
                                break;
                            case REJECT:
                            default:
                                send(_sock, REJECTED, convertAuthTypes(saslConfig.getAuthMode()));
                                current = 0;
                                break;
                            }
                        break;
                        case ERROR, CANCEL:
                            send(_sock, REJECTED, convertAuthTypes(saslConfig.getAuthMode()));
                            state = SaslAuthState.WAIT_AUTH;
                        break;
                        case BEGIN:
                            state = SaslAuthState.FAILED;
                        break;
                        default:
                            send(_sock, ERROR, INVALID_CMD_ERR);
                        break;
                    }
                    break;
                    case WAIT_BEGIN:
                        c = receive(_sock);
                        switch (c.getCommand()) {
                            case ERROR, CANCEL:
                                send(_sock, REJECTED, convertAuthTypes(saslConfig.getAuthMode()));
                                state = SaslAuthState.WAIT_AUTH;
                            break;
                            case BEGIN:
                                    state = SaslAuthState.FINISHED;
                            break;
                            case NEGOTIATE_UNIX_FD:
                                logger.debug("File descriptor negotiation requested");
                                if (!saslConfig.isFileDescriptorSupport()) {
                                    send(_sock, ERROR);
                                } else {
                                    send(_sock, AGREE_UNIX_FD);
                                }

                            break;
                            default:
                                send(_sock, ERROR, INVALID_CMD_ERR);
                            break;
                        }
                    break;
                    default:
                        state = SaslAuthState.FAILED;
                    }
                break;
            default:
                return false;
            }
        }

        return state == SaslAuthState.FINISHED;
    }

    public boolean isFileDescriptorSupported() {
        return fileDescriptorSupported;
    }

    /**
     * Handle reject of authentication.
     *
     * @param _available
     * @param _luid
     * @param _sock socketchannel
     * @return current state or -1 if failed
     * @throws IOException when sending fails
     */
    private int handleReject(int _available, String _luid, SocketChannel _sock) throws IOException {
        int current = -1;
        if (0 != (_available & AUTH_EXTERNAL)) {
            send(_sock, AUTH, AUTH_TYPE_EXTERNAL, _luid);
            current = AUTH_EXTERNAL;
        } else if (0 != (_available & AUTH_SHA)) {
            send(_sock, AUTH, AUTH_TYPE_DBUS_COOKIE_SHA1, _luid);
            current = AUTH_SHA;
        } else if (0 != (_available & AUTH_ANON)) {
            send(_sock, AUTH, AUTH_TYPE_ANONYMOUS);
            current = AUTH_ANON;
        }
        return current;
    }

    /**
     * Tries to get the UID (user ID) of the current JVM process.
     * Will always return 0 on windows.
     * @return long
     */
    private long getUserId() {
        return 0;
    }

    public enum SaslMode {
        SERVER, CLIENT;
    }

    public enum SaslCommand {
        AUTH,
        DATA,
        REJECTED,
        OK,
        BEGIN,
        CANCEL,
        ERROR,
        NEGOTIATE_UNIX_FD,
        AGREE_UNIX_FD;
    }

    enum SaslAuthState {
        INITIAL_STATE,
        WAIT_DATA,
        WAIT_OK,
        WAIT_REJECT,
        WAIT_AUTH,
        WAIT_BEGIN,
        NEGOTIATE_UNIX_FD,
        FINISHED,
        FAILED;
    }

    public enum SaslResult {
        OK,
        CONTINUE,
        ERROR,
        REJECT;
    }

    public static class Command {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private SaslCommand    command;
        private int    mechs;
        private String data;
        private String response;

        public Command() {
        }

        public Command(String _s) throws IOException {
            String[] ss = _s.split(" ");
            LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Creating command from: {}", Arrays.toString(ss)));
            if (0 == COL.compare(ss[0], "OK")) {
                command = OK;
                data = ss[1];
            } else if (0 == COL.compare(ss[0], "AUTH")) {
                command = AUTH;
                if (ss.length > 1) {
                    if (0 == COL.compare(ss[1], AUTH_TYPE_EXTERNAL)) {
                        mechs = AUTH_EXTERNAL;
                    } else if (0 == COL.compare(ss[1], AUTH_TYPE_DBUS_COOKIE_SHA1)) {
                        mechs = AUTH_SHA;
                    } else if (0 == COL.compare(ss[1], AUTH_TYPE_ANONYMOUS)) {
                        mechs = AUTH_ANON;
                    }
                }
                if (ss.length > 2) {
                    data = ss[2];
                }
            } else if (0 == COL.compare(ss[0], "DATA")) {
                command = DATA;
                // ss[1] might be non-existing or empty (e.g. AUTH ANON)
                data = ss.length < 2 ? null : ss[1];
            } else if (0 == COL.compare(ss[0], "REJECTED")) {
                command = REJECTED;
                for (int i = 1; i < ss.length; i++) {
                    if (0 == COL.compare(ss[i], AUTH_TYPE_EXTERNAL)) {
                        mechs |= AUTH_EXTERNAL;
                    } else if (0 == COL.compare(ss[i], AUTH_TYPE_DBUS_COOKIE_SHA1)) {
                        mechs |= AUTH_SHA;
                    } else if (0 == COL.compare(ss[i], AUTH_TYPE_ANONYMOUS)) {
                        mechs |= AUTH_ANON;
                    }
                }
            } else if (0 == COL.compare(ss[0], "BEGIN")) {
                command = BEGIN;
            } else if (0 == COL.compare(ss[0], "CANCEL")) {
                command = CANCEL;
            } else if (0 == COL.compare(ss[0], "ERROR")) {
                command = ERROR;
                data = ss[1];
            } else if (0 == COL.compare(ss[0], "NEGOTIATE_UNIX_FD")) {
                command = NEGOTIATE_UNIX_FD;
            } else if (0 == COL.compare(ss[0], "AGREE_UNIX_FD")) {
                command = AGREE_UNIX_FD;
            } else {
                throw new IOException("Invalid Command " + ss[0]);
            }
            logger.trace("Created command: {}", this);
        }

        public SaslCommand getCommand() {
            return command;
        }

        public int getMechs() {
            return mechs;
        }

        public String getData() {
            return data;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String _s) {
            response = _s;
        }

        @Override
        public String toString() {
            return "Command(" + command + ", " + mechs + ", " + data + ")";
        }
    }

}
