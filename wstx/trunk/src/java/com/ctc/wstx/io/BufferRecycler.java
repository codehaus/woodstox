package com.ctc.wstx.io;

/**
 * This is a small utility class, whose main functionality is to allow
 * simple reuse of raw byte/char buffers using ThreadLocal soft referenced
 * buffers. Idea is that the parsing code asks recycler for specific buffers,
 * and returns them after use; in normal use case this should result in
 * high-level reuse with low level of overhead.
 */
public final class BufferRecycler
{
    // !!! TBI
}
