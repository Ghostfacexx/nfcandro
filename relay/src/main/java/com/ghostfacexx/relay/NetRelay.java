package com.ghostfacexx.relay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NetRelay {
    
    /**
     * Forwards APDU data to a remote host over TCP and returns the response
     * 
     * @param host Remote host address
     * @param port Remote port number
     * @param data APDU data to send
     * @param timeoutMs Timeout in milliseconds
     * @return Response bytes from remote, or null if error
     */
    public static byte[] forwardToRemote(String host, int port, byte[] data, int timeoutMs) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setSoTimeout(timeoutMs);
            socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Send length-prefixed data (4 bytes length + data)
            out.write((data.length >> 24) & 0xFF);
            out.write((data.length >> 16) & 0xFF);
            out.write((data.length >> 8) & 0xFF);
            out.write(data.length & 0xFF);
            out.write(data);
            out.flush();
            
            // Read response length (4 bytes)
            int responseLength = 0;
            for (int i = 0; i < 4; i++) {
                int b = in.read();
                if (b == -1) {
                    throw new IOException("Unexpected end of stream while reading response length");
                }
                responseLength = (responseLength << 8) | (b & 0xFF);
            }
            
            // Validate response length
            if (responseLength < 0 || responseLength > 4096) {
                throw new IOException("Invalid response length: " + responseLength);
            }
            
            // Read response data
            byte[] response = new byte[responseLength];
            int totalRead = 0;
            while (totalRead < responseLength) {
                int bytesRead = in.read(response, totalRead, responseLength - totalRead);
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of stream while reading response data");
                }
                totalRead += bytesRead;
            }
            
            return response;
            
        } catch (SocketTimeoutException e) {
            // Timeout occurred
            return null;
        } catch (IOException e) {
            // Network error
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
}