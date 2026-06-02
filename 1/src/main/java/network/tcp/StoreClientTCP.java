package network.tcp;

import network.Utils;
import protocol.*;
import pipeline.enums.CommandType;
import protocol.Package;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientTCP {
    private final String host;
    private final int port;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private final Encoder encoder = new Encoder();
    private final Decoder decoder = new Decoder();
    private final AtomicLong packetId = new AtomicLong(0);
    private volatile boolean connected = false;
    private final Object sendLock = new Object();

    public StoreClientTCP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() throws IOException {
        if (connected && socket != null && !socket.isClosed()) return;
        disconnect();
        socket = new Socket(host, port);
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());
        connected = true;
        System.out.println("TCP Client connected to " + host + ":" + port);
    }

    public synchronized void disconnect() {
        connected = false;
        try { if (dos != null) dos.close(); } catch (IOException ignored) {}
        try { if (dis != null) dis.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private boolean ensureConnection() {
        if (connected && socket != null && !socket.isClosed() && socket.isConnected())
            return true;
        for (int attempt = 0; attempt < Utils.TCP_MAX_RECONNECT_ATTEMPTS; attempt++) {
            try {
                disconnect();
                connect();
                return true;
            } catch (IOException e) {
                System.err.println("Reconnect attempt " + (attempt+1) + " failed: " + e.getMessage());
                try { TimeUnit.MILLISECONDS.sleep(Utils.TCP_RECONNECT_DELAY_MS); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
            }
        }
        return false;
    }

    public String sendCommand(CommandType cmd, String args) throws IOException, InterruptedException {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (!ensureConnection()) {
                if (attempt == maxAttempts) throw new IOException("Cannot establish connection");
                continue;
            }
            long pid = packetId.incrementAndGet();
            Message message = new Message(cmd.getCode(), 1, args);
            Package pkg = new Package((byte)1, pid, message);
            byte[] requestData = encoder.encode(pkg);
            synchronized (sendLock) {
                try {
                    dos.writeInt(requestData.length);
                    dos.write(requestData);
                    dos.flush();
                    int len = dis.readInt();
                    if (len <= 0 || len > 65536) throw new IOException("Invalid packet length");
                    byte[] responseData = new byte[len];
                    dis.readFully(responseData);
                    Package response = decoder.decode(responseData);
                    return response.getMessage().getMessage();
                } catch (IOException e) {
                    System.err.println("Command failed (attempt " + attempt + "/" + maxAttempts + "): " + e.getMessage());
                    disconnect();
                    if (attempt == maxAttempts) throw e;
                    TimeUnit.MILLISECONDS.sleep(2000);
                }
            }
        }
        throw new IOException("Unexpected error");
    }

    public void close() { disconnect(); }
}