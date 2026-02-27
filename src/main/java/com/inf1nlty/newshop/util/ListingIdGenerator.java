package com.inf1nlty.newshop.util;

import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe incrementing ID generator for shop listings. */
public final class ListingIdGenerator {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private ListingIdGenerator() {}

    public static int nextId() {
        return SEQ.getAndIncrement();
    }

    /** Seeds the sequence above the highest existing ID to avoid conflicts. */
    public static void seed(int maxExisting) {
        SEQ.set(Math.max(maxExisting + 1, SEQ.get()));
    }
}