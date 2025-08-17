package com.ghostfacexx.relay;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages a persistent socket to the remote and runs dedicated send/receive threads.
 * Producers enqueue SendBase instances containing payloads; the send thread writes requests
 * and then reads responses, delivering them back via SendBase.setResponse().
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private final BlockingQueue<SendBase> sendQueue = new LinkedBlockingQueue<>();
    private final Object lifecycleLock = new Object();
    private Thread sendThread;
    private volatile boolean running = false;
    private volatile Socket socket;
    private String host;
    private int port;
    private int connTimeoutMs = 5000;

    public static final NetworkManager INSTANCE = new NetworkManager();

    private NetworkManager() {}

    public void configure(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        synchronized (lifecycleLock) {
            if (running) return;
            running = true;
            sendThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendLoop();
                }
            }, "RelaySendThread");
            sendThread.start();
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            running = false;
            if (sendThread != null) sendThread.interrupt();
            closeSocket();
        }
    }

    public void enqueue(SendBase sb) throws InterruptedException {
        sendQueue.put(sb);
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        closeSocket();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), Math.max(1000, connTimeoutMs));
        socket.setSoTimeout(5000);
    }

    private void closeSocket() {
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
            socket = null;
        }
    }

    private void sendLoop() {
        DataOutputStream dos = null;
        DataInputStream dis = null;
        while (running) {
            SendBase sb = null;
            try {
                sb = sendQueue.take();
            } catch (InterruptedException e) {
                break;
            }
            if (sb == null) continue;
            try {
                ensureConnected();
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                byte[] payload = sb.getPayload();
                // write length-prefixed
                dos.writeInt(payload.length);
                dos.write(payload);
                dos.flush();

                // read response length
                int respLen = dis.readInt();
                if (respLen < 0 || respLen > 10_000_000) {
                    sb.setResponse(null);
                } else {
                    byte[] resp = new byte[respLen];
                    int read = 0;
                    while (read < respLen) {
                        int r = dis.read(resp, read, respLen - read);
                        if (r < 0) break;
                        read += r;
                    }
                    if (read != respLen) {
                        // partial
                        byte[] part = new byte[read];
                        System.arraycopy(resp, 0, part, 0, read);
                        sb.setResponse(part);
                    } else {
                        sb.setResponse(resp);
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "IO error in sendLoop: " + e.getMessage());
                // signal failure to caller
                if (sb != null) sb.setResponse(null);
                closeSocket();
                // small backoff
                try { Thread.sleep(500); } catch (InterruptedException ignored) { }
            } catch (Exception e) {
                Log.w(TAG, "Unexpected error: " + e.getMessage());
                if (sb != null) sb.setResponse(null);
            }
        }
        closeSocket();
    }
}