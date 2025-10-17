package ru.nsu.vyaznikova.server;

import ru.nsu.vyaznikova.common.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

public class KeyServer {
    private final int port;

    public KeyServer(int port) {
        this.port = port;
    }

    private static void printUsage() {
        System.out.println("Usage: key-server --port <port>");
        System.out.println("Options:");
        System.out.println("  -p, --port    Server TCP port (required)");
        System.out.println("  -?, --help    Show this help");
    }

    private static int parsePort(String[] args) {
        Integer port = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-p":
                case "--port":
                    if (i + 1 >= args.length) { System.err.println("--port requires a value"); printUsage(); System.exit(2); }
                    try { port = Integer.parseInt(args[++i]); }
                    catch (NumberFormatException ex) { System.err.println("--port must be an integer"); System.exit(2); }
                    break;
                case "-?":
                case "--help":
                case "-help":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown option: " + a);
                    printUsage();
                    System.exit(2);
            }
        }
        if (port == null || port == 0) {
            System.err.println("Missing required option --port");
            printUsage();
            System.exit(2);
        }
        return port;
    }

    public int run() throws Exception {
        System.out.printf("[KeyServer] Starting skeleton on port %d...%n", port);

        try (Selector selector = Selector.open();
             ServerSocketChannel server = ServerSocketChannel.open()) {
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);

            Map<SocketChannel, ConnState> states = new HashMap<>();

            while (true) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    try {
                        if (!key.isValid()) continue;
                        if (key.isAcceptable()) {
                            SocketChannel ch = server.accept();
                            if (ch != null) {
                                ch.configureBlocking(false);
                                ch.register(selector, SelectionKey.OP_READ);
                                states.put(ch, new ConnState());
                            }
                        } else if (key.isReadable()) {
                            SocketChannel ch = (SocketChannel) key.channel();
                            ConnState st = states.get(ch);
                            if (st == null) {
                                closeQuiet(ch, states);
                                continue;
                            }
                            int r = ch.read(st.buf);
                            if (r == -1) { // client closed
                                closeQuiet(ch, states);
                                continue;
                            }
                            st.buf.flip();
                            while (st.buf.hasRemaining() && !st.nameCompleted) {
                                byte b = st.buf.get();
                                if (b == Protocol.NAME_TERMINATOR) {
                                    st.nameCompleted = true;
                                    break;
                                }
                                // enforce ASCII printable (plus space) and max length
                                if (b < 0x20 || b > 0x7E) {
                                    if (b != 0x20) {
                                        System.out.println("[KeyServer] Invalid character in name. Closing.");
                                        closeQuiet(ch, states);
                                        st.buf.compact();
                                        continue;
                                    }
                                }
                                if (st.nameBytes.size() >= Protocol.MAX_NAME_LEN) {
                                    System.out.println("[KeyServer] Name is too long. Closing.");
                                    closeQuiet(ch, states);
                                    st.buf.compact();
                                    continue;
                                }
                                st.nameBytes.add(b);
                            }
                            st.buf.compact();

                            if (st.nameCompleted && !st.logged) {
                                st.logged = true;
                                String name = st.getName(Protocol.NAME_CHARSET);
                                System.out.printf("[KeyServer] Received name: '%s' from %s%n", name, ch);
                                // for skeleton stage: close connection after logging
                                closeQuiet(ch, states);
                            }
                        }
                    } catch (CancelledKeyException ignored) {
                    } catch (IOException io) {
                        Channel ch = key.channel();
                        if (ch instanceof SocketChannel sc) {
                            closeQuiet(sc, states);
                        }
                    }
                }
            }
        }
    }

    private static void closeQuiet(SocketChannel ch, Map<SocketChannel, ConnState> map) {
        map.remove(ch);
        try { ch.close(); } catch (IOException ignored) {}
    }

    private static class ConnState {
        final ByteBuffer buf = ByteBuffer.allocate(Protocol.MAX_NAME_LEN);
        final ArrayList<Byte> nameBytes = new ArrayList<>();
        boolean nameCompleted = false;
        boolean logged = false;

        String getName(Charset cs) {
            byte[] a = new byte[nameBytes.size()];
            for (int i = 0; i < nameBytes.size(); i++) a[i] = nameBytes.get(i);
            return new String(a, cs).trim();
        }
    }

    public static void main(String[] args) {
        int port = parsePort(args);
        int exit = 0;
        try {
            exit = new KeyServer(port).run();
        } catch (Exception e) {
            e.printStackTrace();
            exit = 1;
        }
        System.exit(exit);
    }
}
