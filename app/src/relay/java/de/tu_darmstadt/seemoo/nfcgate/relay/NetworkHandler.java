package de.tu_darmstadt.seemoo.nfcgate.relay;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles network communication for the relay service.
 * Manages TCP socket connections and data transfer without external dependencies.
 */
public class NetworkHandler {
    private static final String TAG = "NetworkHandler";
    
    public interface DataCallback {
        void onData(byte[] data);
    }
    
    public interface DisconnectCallback {
        void onDisconnected();
    }
    
    private final Socket mSocket;
    private final DataCallback mDataCallback;
    private final DisconnectCallback mDisconnectCallback;
    
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private boolean mIsRunning = false;
    
    private Thread mReceiveThread;
    private Thread mSendThread;
    private BlockingQueue<byte[]> mSendQueue;
    
    public NetworkHandler(Socket socket, DataCallback dataCallback, DisconnectCallback disconnectCallback) {
        mSocket = socket;
        mDataCallback = dataCallback;
        mDisconnectCallback = disconnectCallback;
        mSendQueue = new LinkedBlockingQueue<>();
    }
    
    public void start() throws IOException {
        mInputStream = mSocket.getInputStream();
        mOutputStream = mSocket.getOutputStream();
        mIsRunning = true;
        
        // Start receive thread
        mReceiveThread = new Thread(this::receiveLoop, "NetworkReceive");
        mReceiveThread.start();
        
        // Start send thread
        mSendThread = new Thread(this::sendLoop, "NetworkSend");
        mSendThread.start();
        
        Log.i(TAG, "Network handler started");
    }
    
    public void sendData(byte[] data) {
        if (mIsRunning) {
            mSendQueue.offer(data);
        }
    }
    
    public void disconnect() {
        mIsRunning = false;
        
        try {
            if (mSocket != null && !mSocket.isClosed()) {
                mSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
        
        // Interrupt threads
        if (mReceiveThread != null) {
            mReceiveThread.interrupt();
        }
        if (mSendThread != null) {
            mSendThread.interrupt();
        }
        
        Log.i(TAG, "Network handler disconnected");
    }
    
    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        
        while (mIsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                int bytesRead = mInputStream.read(buffer);
                if (bytesRead > 0) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    
                    if (mDataCallback != null) {
                        mDataCallback.onData(data);
                    }
                } else if (bytesRead == -1) {
                    // End of stream
                    break;
                }
            } catch (IOException e) {
                if (mIsRunning) {
                    Log.e(TAG, "Error reading from socket", e);
                }
                break;
            }
        }
        
        if (mIsRunning) {
            mIsRunning = false;
            if (mDisconnectCallback != null) {
                mDisconnectCallback.onDisconnected();
            }
        }
    }
    
    private void sendLoop() {
        while (mIsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] data = mSendQueue.take(); // Blocks until data is available
                
                if (mOutputStream != null) {
                    // Simple protocol: send length followed by data
                    writeInt(mOutputStream, data.length);
                    mOutputStream.write(data);
                    mOutputStream.flush();
                    
                    Log.d(TAG, "Sent " + data.length + " bytes to server");
                }
            } catch (InterruptedException e) {
                // Thread interrupted, exit loop
                break;
            } catch (IOException e) {
                if (mIsRunning) {
                    Log.e(TAG, "Error writing to socket", e);
                    mIsRunning = false;
                    if (mDisconnectCallback != null) {
                        mDisconnectCallback.onDisconnected();
                    }
                }
                break;
            }
        }
    }
    
    private void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }
}