package network.udp;

import network.Utils;
import protocol.*;
import pipeline.enums.CommandType;
import protocol.Package;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientUDP {
    private final String host;
    private final int port;
    private DatagramSocket socket;
    private final InetAddress serverAddress;
    private final Encoder encoder = new Encoder();
    private final Decoder decoder = new Decoder();
    private final AtomicLong packetId = new AtomicLong(0);

    public StoreClientUDP(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        this.serverAddress = InetAddress.getByName(host);
        createSocket();
    }

    private void createSocket() throws SocketException {
        if (socket != null && !socket.isClosed()) socket.close();
        socket = new DatagramSocket();
        socket.setSoTimeout(Utils.UDP_TIMEOUT_MILLIS);
    }

    public String sendCommand(CommandType cmd, String args) throws Exception {
        long pid = packetId.incrementAndGet();
        Message msg = new Message(cmd.getCode(), 1, args);
        Package pkg = new Package((byte)1, pid, msg);
        byte[] requestData = encoder.encode(pkg);
        DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, serverAddress, port);
        byte[] recvBuf = new byte[65536];
        DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
        for (int attempt = 1; attempt <= Utils.UDP_MAX_RETRIES; attempt++) {
            try {
                socket.send(requestPacket);
                socket.receive(recvPacket);
                if (recvPacket.getAddress().equals(serverAddress) && recvPacket.getPort() == port) {
                    byte[] respBytes = new byte[recvPacket.getLength()];
                    System.arraycopy(recvPacket.getData(), 0, respBytes, 0, recvPacket.getLength());
                    Package response = decoder.decode(respBytes);
                    if (response.getbPktId() == pid) return response.getMessage().getMessage();
                    else System.err.println("Wrong pktId, ignoring");
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Timeout, retry " + attempt);
                createSocket();
            } catch (IOException e) {
                System.err.println("IO error, retry " + attempt);
                createSocket();
            }
        }
        throw new IOException("No response after " + Utils.UDP_MAX_RETRIES + " attempts");
    }

    public void close() { if (socket != null) socket.close(); }
}