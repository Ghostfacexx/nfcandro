package com.ghostfacexx.relay;

import java.util.concurrent.CountDownLatch;

public class SendBase {
    private final byte[] payload;
    private byte[] response;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final int timeoutMs;

    public SendBase(byte[] payload, int timeoutMs) {
        this.payload = payload;
        this.timeoutMs = timeoutMs;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setResponse(byte[] resp) {
        this.response = resp;
        latch.countDown();
    }

    public byte[] waitForResponse() throws InterruptedException {
        boolean ok = latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!ok) return null;
        return response;
    }
}