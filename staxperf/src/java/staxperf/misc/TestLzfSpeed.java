package staxperf.misc;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import staxperf.TestUtil;

/**
 * Micro-benchmark to test LZF decompression speed for given data.
 */
public class TestLzfSpeed
    extends BaseCompressTest
{
    private TestLzfSpeed() { super("LZF"); }

    @Override
    protected byte[] doCompress(byte[] inputData) throws Exception
    {
        return LZFEncoder.encode(inputData);
    }

    @Override
    protected byte[] doDecompress(byte[] compData, int uncompLen) throws Exception
    {
        return LZFDecoder.decode(compData);
    }

    public static void main(String[] args) throws Exception
    {
        new TestLzfSpeed().test(args);
    }

    /*
    ******************************************************
    * LZF Encoder from Voldemort
    ******************************************************
     */

    static class ChunkEncoder
    {
        // Beyond certain point we won't be able to compress:
        private static final int MIN_BLOCK_TO_COMPRESS = 16;
        
        private static final int MIN_HASH_SIZE = 256;
        // Not much point in bigger tables, with 8k window
        private static final int MAX_HASH_SIZE = 16384;
        
        private static final int MAX_OFF = 1 << 13; // 8k
        private static final int MAX_REF = (1 << 8) + (1 << 3); // 264
        
        // // Encoding tables
        
        /**
         * Buffer in which encoded content is stored during processing
         */
        private final byte[] _encodeBuffer;    
        
        private final int[] _hashTable;
        
        private final int _hashModulo;
        
        /**
         * @param totalLength Total encoded length; used for calculating size
         *   of hash table to use
         */
        public ChunkEncoder(int totalLength)
        {
            int largestChunkLen = Math.max(totalLength, LZFChunk.MAX_CHUNK_LEN);
            
            int hashLen = calcHashLen(largestChunkLen);
            _hashTable = new int[hashLen];
            _hashModulo = hashLen-1;
            // Ok, then, what's the worst case output buffer length?
            // length indicator for each 32 literals, so:
            int bufferLen = largestChunkLen + ((largestChunkLen + 31) >> 5);
            _encodeBuffer = new byte[bufferLen];
        }
        
        /**
         * Method for compressing (or not) individual chunks
         */
        public LZFChunk encodeChunk(byte[] data, int offset, int len)
        {
            if (len >= MIN_BLOCK_TO_COMPRESS) {
                /* If we have non-trivial block, and can compress it by at least
                 * 2 bytes (since header is 2 bytes longer), let's compress:
                 */
                int compLen = tryCompress(data, offset, offset+len, _encodeBuffer, 0);
                if (compLen < (len-2)) { // nah; just return uncompressed
                    return LZFChunk.createCompressed(len, _encodeBuffer, 0, compLen);
                }
            }
            // Otherwise leave uncompressed:
            return LZFChunk.createNonCompressed(data, offset, len);
        }
        
        private static int calcHashLen(int chunkSize)
        {
            // in general try get hash table size of 2x input size
            chunkSize += chunkSize;
            // but no larger than max size:
            if (chunkSize >= MAX_HASH_SIZE) {
                return MAX_HASH_SIZE;
            }
            // otherwise just need to round up to nearest 2x
            int hashLen = MIN_HASH_SIZE;
            while (hashLen < chunkSize) {
                hashLen += hashLen;
            }
            return hashLen;
        }
        
        private int first(byte[] in, int inPos) {
            return (in[inPos] << 8) + (in[inPos + 1] & 255);
        }
        
        private static int next(int v, byte[] in, int inPos) {
            return (v << 8) + (in[inPos + 2] & 255);
        }


        private int hash(int h) {
            // or 184117; but this seems to give better hashing?
            return ((h * 57321) >> 9) & _hashModulo;
            // original lzf-c.c used this:
            //return (((h ^ (h << 5)) >> (24 - HLOG) - h*5) & _hashModulo;
            // but that didn't seem to provide better matches
        }
        
        private int tryCompress(byte[] in, int inPos, int inEnd, byte[] out, int outPos)
        {
            int literals = 0;
            outPos++;
            int hash = first(in, 0);
            inEnd -= 4;
            final int firstPos = inPos; // so that we won't have back references across block boundary
            while (inPos < inEnd) {
                byte p2 = in[inPos + 2];
                // next
                hash = (hash << 8) + (p2 & 255);
                int off = hash(hash);
                int ref = _hashTable[off];
                _hashTable[off] = inPos;
                if (ref < inPos
                    && ref >= firstPos
                    && (off = inPos - ref - 1) < MAX_OFF
                    && in[ref + 2] == p2
                    && in[ref + 1] == (byte) (hash >> 8)
                    && in[ref] == (byte) (hash >> 16)) {
                    // match
                    int maxLen = inEnd - inPos + 2;
                    if (maxLen > MAX_REF) {
                        maxLen = MAX_REF;
                    }
                    if (literals == 0) {
                        outPos--;
                    } else {
                        out[outPos - literals - 1] = (byte) (literals - 1);
                        literals = 0;
                    }
                    int len = 3;
                    while (len < maxLen && in[ref + len] == in[inPos + len]) {
                        len++;
                    }
                    len -= 2;
                    if (len < 7) {
                        out[outPos++] = (byte) ((off >> 8) + (len << 5));
                    } else {
                        out[outPos++] = (byte) ((off >> 8) + (7 << 5));
                        out[outPos++] = (byte) (len - 7);
                    }
                    out[outPos++] = (byte) off;
                    outPos++;
                    inPos += len;
                    hash = first(in, inPos);
                    hash = next(hash, in, inPos);
                    _hashTable[hash(hash)] = inPos++;
                    hash = next(hash, in, inPos);
                    _hashTable[hash(hash)] = inPos++;
                } else {
                    out[outPos++] = in[inPos++];
                    literals++;
                    if (literals == LZFChunk.MAX_LITERAL) {
                        out[outPos - literals - 1] = (byte) (literals - 1);
                        literals = 0;
                        outPos++;
                    }
                }
            }
            inEnd += 4;
            while (inPos < inEnd) {
                out[outPos++] = in[inPos++];
                literals++;
                if (literals == LZFChunk.MAX_LITERAL) {
                    out[outPos - literals - 1] = (byte) (literals - 1);
                    literals = 0;
                    outPos++;
                }
            }
            out[outPos - literals - 1] = (byte) (literals - 1);
            if (literals == 0) {
                outPos--;
            }
        return outPos;
        }
    }

    static class LZFEncoder
    {
        // Static methods only, no point in instantiating
        private LZFEncoder() { }
    
        /**
         * Method for compressing given input data using LZF encoding and
         * block structure (compatible with lzf command line utility).
         * Result consists of a sequence of chunks.
         */
        public static byte[] encode(byte[] data) throws IOException
        {
            int left = data.length;
            ChunkEncoder enc = new ChunkEncoder(left);
            int chunkLen = Math.min(LZFChunk.MAX_CHUNK_LEN, left);
            LZFChunk first = enc.encodeChunk(data, 0, chunkLen);
            left -= chunkLen;
            // shortcut: if it all fit in, no need to coalesce:
            if (left < 1) {
                return first.getData();
            }
            // otherwise need to get other chunks:
            int resultBytes = first.length();
            int inputOffset = chunkLen;
            LZFChunk last = first;

            do {
                chunkLen = Math.min(left, LZFChunk.MAX_CHUNK_LEN);
                LZFChunk chunk = enc.encodeChunk(data, inputOffset, chunkLen);
                inputOffset += chunkLen;
                left -= chunkLen;
                resultBytes += chunk.length();
                last.setNext(chunk);
                last = chunk;
            } while (left > 0);
            // and then coalesce returns into single contiguous byte array
            byte[] result = new byte[resultBytes];
            int ptr = 0;
            for (; first != null; first = first.next()) {
                ptr = first.copyTo(result, ptr);
            }
            return result;
        }
    }


    static class LZFDecoder
    {
        final static byte BYTE_NULL = 0;    

        // static methods, no need to instantiate
        private LZFDecoder() { }
    
        /**
         * Method for decompressing whole input data, which encoded in LZF
         * block structure (compatible with lzf command line utility),
         * and can consist of any number of blocks
         */
        public static byte[] decode(byte[] data) throws IOException
        {
            /* First: let's calculate actual size, so we can allocate
             * exact result size. Also useful for basic sanity checking;
             * so that after call we know header structure is not corrupt
             * (to the degree that lengths etc seem valid)
             */
            byte[] result = new byte[calculateUncompressedSize(data)];
            int inPtr = 0;
            int outPtr = 0;

            while (inPtr < (data.length - 1)) { // -1 to offset possible end marker
                inPtr += 2; // skip 'ZV' marker
                int type = data[inPtr++];
                int len = uint16(data, inPtr);
                inPtr += 2;
                if (type == LZFChunk.BLOCK_TYPE_NON_COMPRESSED) { // uncompressed
                    System.arraycopy(data, inPtr, result, outPtr, len);
                    outPtr += len;
                } else { // compressed
                    int uncompLen = uint16(data, inPtr);
                    inPtr += 2;
                    decompressChunk(data, inPtr, result, outPtr, outPtr+uncompLen);
                    outPtr += uncompLen;
                }
                inPtr += len;
            }
            return result;
        }

        private static int calculateUncompressedSize(byte[] data) throws IOException
        {
            int uncompressedSize = 0;
            int ptr = 0;
            int blockNr = 0;

            while (ptr < data.length) {
                // can use optional end marker
                if (ptr == (data.length + 1) && data[ptr] == BYTE_NULL) {
                    ++ptr; // so that we'll be at end
                    break;
                }
                // simpler to handle bounds checks by catching exception here...
                try {
                    if (data[ptr] != LZFChunk.BYTE_Z || data[ptr+1] != LZFChunk.BYTE_V) {
                        throw new IOException("Corrupt input data, block #"+blockNr+" (at offset "+ptr+"): did not start with 'ZV' signature bytes");
                    }
                    int type = (int) data[ptr+2];
                    int blockLen = uint16(data, ptr+3);
                    if (type == LZFChunk.BLOCK_TYPE_NON_COMPRESSED) { // uncompressed
                        ptr += 5;
                        uncompressedSize += blockLen;
                    } else if (type == LZFChunk.BLOCK_TYPE_COMPRESSED) { // compressed
                        uncompressedSize += uint16(data, ptr+5);
                        ptr += 7;
                    } else { // unknown... CRC-32 would be 2, but that's not implemented by cli tool
                        throw new IOException("Corrupt input data, block #"+blockNr+" (at offset "+ptr+"): unrecognized block type "+(type & 0xFF));
                    }
                    ptr += blockLen;
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IOException("Corrupt input data, block #"+blockNr+" (at offset "+ptr+"): truncated block header");
                }
                ++blockNr;
            }
            // one more sanity check:
            if (ptr != data.length) {
                throw new IOException("Corrupt input data: block #"+blockNr+" extends "+(data.length - ptr)+" beyond end of input");
            }
            return uncompressedSize;
        }

        /**
         * Main decode method for individual chunks.
         */
        public static void decompressChunk(byte[] in, int inPos, byte[] out, int outPos, int outEnd)
            throws IOException
        {
            do {
                int ctrl = in[inPos++] & 255;
                if (ctrl < LZFChunk.MAX_LITERAL) { // literal run
                    ctrl += inPos;
                    do {
                        out[outPos++] = in[inPos];
                    } while (inPos++ < ctrl);
                } else {
                    // back reference
                    int len = ctrl >> 5;
                    ctrl = -((ctrl & 0x1f) << 8) - 1;
                    if (len == 7) {
                        len += in[inPos++] & 255;
                    }
                    ctrl -= in[inPos++] & 255;
                    len += outPos + 2;
                    out[outPos] = out[outPos++ + ctrl];
                    out[outPos] = out[outPos++ + ctrl];
                    while (outPos < len - 8) {
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                        out[outPos] = out[outPos++ + ctrl];
                    }
                    while (outPos < len) {
                        out[outPos] = out[outPos++ + ctrl];
                    }
                }
            } while (outPos < outEnd);

            // sanity check to guard against corrupt data:
            if (outPos != outEnd) throw new IOException("Corrupt data: overrun in decompress, input offset "+inPos+", output offset "+outPos);
        }
    
        private static int uint16(byte[] data, int ptr)
        {
            return ((data[ptr] & 0xFF) << 8) + (data[ptr+1] & 0xFF);
        }    
    }

    static class LZFChunk
    {
        /**
         * Maximum length of literal run for LZF encoding.
         */
        public static final int MAX_LITERAL = 1 << 5; // 32

        // Chunk length is limited by 2-byte length indicator, to 64k
        public static final int MAX_CHUNK_LEN = 0xFFFF;

        public final static byte BYTE_Z = 'Z';
        public final static byte BYTE_V = 'V';

        public final static int BLOCK_TYPE_NON_COMPRESSED = 0;
        public final static int BLOCK_TYPE_COMPRESSED = 1;

    
        final byte[] _data;
        LZFChunk _next;

        private LZFChunk(byte[] data) { _data = data; }

        /**
         * Factory method for constructing compressed chunk
         */
        public static LZFChunk createCompressed(int origLen, byte[] encData, int encPtr, int encLen)
        {
            byte[] result = new byte[encLen + 7];
            result[0] = BYTE_Z;
            result[1] = BYTE_V;
            result[2] = BLOCK_TYPE_COMPRESSED;
            result[3] = (byte) (encLen >> 8);
            result[4] = (byte) encLen;
            result[5] = (byte) (origLen >> 8);
            result[6] = (byte) origLen;
            System.arraycopy(encData, encPtr, result, 7, encLen);
            return new LZFChunk(result);
        }

        /**
         * Factory method for constructing compressed chunk
         */
        public static LZFChunk createNonCompressed(byte[] plainData, int ptr, int len)
        {
            byte[] result = new byte[len + 5];
            result[0] = BYTE_Z;
            result[1] = BYTE_V;
            result[2] = BLOCK_TYPE_NON_COMPRESSED;
            result[3] = (byte) (len >> 8);
            result[4] = (byte) len;
            System.arraycopy(plainData, ptr, result, 5, len);
            return new LZFChunk(result);
        }
    
        public void setNext(LZFChunk next) { _next = next; }

        public LZFChunk next() { return _next; }
        public int length() { return _data.length; }
        public byte[] getData() { return _data; }

        public int copyTo(byte[] dst, int ptr) {
            int len = _data.length;
            System.arraycopy(_data, 0, dst, ptr, len);
            return ptr+len;
        }
    }

}

