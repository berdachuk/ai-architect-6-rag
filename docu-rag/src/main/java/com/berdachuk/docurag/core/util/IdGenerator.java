package com.berdachuk.docurag.core.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MongoDB-compatible ObjectId-style identifiers: 12 bytes as 24 lowercase hex chars.
 * Layout matches BSON ObjectId: 4-byte big-endian Unix timestamp, then 5 random bytes, then 3-byte counter.
 * Hand-written test/seed IDs should keep the same leading timestamp convention for consistency.
 */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicInteger COUNTER = new AtomicInteger(RANDOM.nextInt());

    private IdGenerator() {
    }

    public static String generateId() {
        int ts = (int) Instant.now().getEpochSecond();
        byte[] rand = new byte[5];
        RANDOM.nextBytes(rand);
        int c = COUNTER.getAndIncrement() & 0xffffff;
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putInt(ts);
        buf.put(rand);
        buf.put((byte) ((c >> 16) & 0xff));
        buf.put((byte) ((c >> 8) & 0xff));
        buf.put((byte) (c & 0xff));
        return HexFormat.of().formatHex(buf.array());
    }

    public static boolean isValidId(String id) {
        return id != null && id.length() == 24 && id.matches("[0-9a-fA-F]{24}");
    }
}
