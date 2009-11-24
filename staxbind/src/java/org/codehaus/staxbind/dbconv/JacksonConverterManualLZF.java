package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.Arrays;

/**
 *<p>
 * Note: modified
 * <a href="http://h2database.googlecode.com/svn/trunk/h2/src/main/org/h2/compress/">
 * code by H2 project</a>; all relevant copyrights and licensing applies as
 * per original open source work (H2 License, EPL).
 */
public class JacksonConverterManualLZF
    extends JacksonConverterManual
{
    final LZFCodec _codec = new LZFCodec();

    final byte[] _outputBuffer = new byte[LZFOutputStream.IO_BUFFER_SIZE_COMPRESS];
    final byte[] _outputBuffer2 = new byte[_outputBuffer.length + (_outputBuffer.length >> 3)];

    @Override
    public DbData readData(InputStream in)
        throws IOException
    {
        LZFInputStream comp = new LZFInputStream(in);
        return super.readData(comp);
    }

    @Override
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        LZFOutputStream comp = new LZFOutputStream(out, _codec, _outputBuffer, _outputBuffer2);
        return super.writeData(comp, data);
    }

    
    /**
     * Generic LZF codec
     */
    final static class LZFCodec
    {
        /**
         * The file header of a LZF file.
         */
        static final int MAGIC = ('H' << 24) | ('2' << 16) | ('I' << 8) | 'S';

        /**
         * Should probably dynamically choose hash size; big ones are wasteful for
         * small blocks.
         */
        //private static final int HASH_SIZE = 1 << 14; // 16k
        private static final int HASH_SIZE = 1 << 12; // 4k
        private static final int MAX_LITERAL = 1 << 5; // 32
        private static final int MAX_OFF = 1 << 13; // 8k
        private static final int MAX_REF = (1 << 8) + (1 << 3); // 264

        private int[] cachedHashTable;

        public LZFCodec() { }
        
        private final int first(byte[] in, int inPos) {
            return (in[inPos] << 8) + (in[inPos + 1] & 255);
        }

        private final int next(int v, byte[] in, int inPos) {
            return (v << 8) + (in[inPos + 2] & 255);
        }

        private final int hash(int h) {
            // or 57321
            return ((h * 184117) >> 9) & (HASH_SIZE - 1);
        }

        public int compress(byte[] in, int inLen, byte[] out, int outPos) {
            int inPos = 0;
            if (cachedHashTable == null) {
                cachedHashTable = new int[HASH_SIZE];
            } else {
                Arrays.fill(cachedHashTable, 0);
            }
            int[] hashTab = cachedHashTable;
            int literals = 0;
            int hash = first(in, inPos);
            while (true) {
                if (inPos < inLen - 4) {
                    hash = next(hash, in, inPos);
                    int off = hash(hash);
                    int ref = hashTab[off];
                    hashTab[off] = inPos;
                    off = inPos - ref - 1;
                    if (off < MAX_OFF && ref > 0 && in[ref + 2] == in[inPos + 2] && in[ref + 1] == in[inPos + 1] && in[ref] == in[inPos]) {
                        int maxLen = inLen - inPos - 2;
                        maxLen = maxLen > MAX_REF ? MAX_REF : maxLen;
                        int len = 3;
                        while (len < maxLen && in[ref + len] == in[inPos + len]) {
                            len++;
                        }
                        len -= 2;
                        if (literals != 0) {
                            out[outPos++] = (byte) (literals - 1);
                            literals = -literals;
                            do {
                                out[outPos++] = in[inPos + literals++];
                            } while (literals != 0);
                        }
                        if (len < 7) {
                            out[outPos++] = (byte) ((off >> 8) + (len << 5));
                        } else {
                            out[outPos++] = (byte) ((off >> 8) + (7 << 5));
                            out[outPos++] = (byte) (len - 7);
                        }
                        out[outPos++] = (byte) off;
                        inPos += len;
                        hash = first(in, inPos);
                        hash = next(hash, in, inPos);
                        hashTab[hash(hash)] = inPos++;
                        hash = next(hash, in, inPos);
                        hashTab[hash(hash)] = inPos++;
                        continue;
                    }
                } else if (inPos == inLen) {
                    break;
                }
                inPos++;
                literals++;
                if (literals == MAX_LITERAL) {
                    out[outPos++] = (byte) (literals - 1);
                    literals = -literals;
                    do {
                        out[outPos++] = in[inPos + literals++];
                    } while (literals != 0);
                }
            }
            if (literals != 0) {
                out[outPos++] = (byte) (literals - 1);
                literals = -literals;
                do {
                    out[outPos++] = in[inPos + literals++];
                } while (literals != 0);
            }
            return outPos;
        }
    }

    /**
     * LZF Input Stream
     */
    public class LZFInputStream extends InputStream {
        private final InputStream in;
        private int pos;
        private int bufferLength;
        private byte[] inBuffer;
        private byte[] buffer;
        private boolean closed;

        public LZFInputStream(InputStream in) throws IOException {
            this.in = in;
            if (readInt() != LZFCodec.MAGIC) {
                throw new IOException("Not an LZFInputStream");
            }
        }

        private byte[] ensureSize(byte[] buff, int len) {
            if (buff == null || buff.length < len) {
                buff = new byte[len+16];
            }
            return buff;
        }

        private void fillBuffer() throws IOException {
            if (buffer != null && pos < bufferLength) {
                return;
            }
            int len = readInt();
            if (closed) { // EOF
                this.bufferLength = 0;
            } else if (len < 0) {
                len = -len;
                buffer = ensureSize(buffer, len);
                readFully(buffer, len);
                this.bufferLength = len;
            } else {
                inBuffer = ensureSize(inBuffer, len);
                int size = readInt();
                readFully(inBuffer, len);
                buffer = ensureSize(buffer, size);
                expandLZFBlock(inBuffer, 0, len, buffer, 0, size);
                this.bufferLength = size;
            }
            pos = 0;
        }

        private void readFully(byte[] buff, int len) throws IOException {
            int off = 0;
            while (len > 0) {
                int l = in.read(buff, off, len);
                len -= l;
                off += l;
            }
        }

        private int readInt() throws IOException {
            int x = in.read();
            if (x < 0) {
                closed = true;
                return 0;
            }
            x = (x << 24) + (in.read() << 16) + (in.read() << 8) + in.read();
            return x;
        }

        public int read() throws IOException {
            fillBuffer();
            if (pos >= bufferLength) {
                return -1;
            }
            return buffer[pos++] & 255;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int read = 0;
            while (len > 0) {
                int r = readBlock(b, off, len);
                if (r < 0) {
                    break;
                }
                read += r;
                off += r;
                len -= r;
            }
            return read == 0 ? -1 : read;
        }

        private int readBlock(byte[] b, int off, int len) throws IOException {
            fillBuffer();
            if (pos >= bufferLength) {
                return -1;
            }
            int max = Math.min(len, bufferLength - pos);
            max = Math.min(max, b.length - off);
            System.arraycopy(buffer, pos, b, off, max);
            pos += max;
            return max;
        }

        public void close() throws IOException {
            in.close();
        }

        private void expandLZFBlock(byte[] in, int inPos, int inLen, byte[] out, int outPos, int outLen) {
            do {
                int ctrl = in[inPos++] & 255;
                if (ctrl < (1 << 5)) {
                    // literal run
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
            } while (outPos < outLen);
        }
        
    }

    /**
     * LZF Output Stream
     */
    final static class LZFOutputStream extends OutputStream {
        final static int IO_BUFFER_SIZE_COMPRESS = 4000;

        private final OutputStream _out;
        private final LZFCodec compress;

        /**
         * Buffer in which content to write is first buffered, before trying
         * to compress it.
         */
        private final byte[] buffer;
        private int pos;

        /**
         * Buffer in which content is compressed (from {@link #buffer}),
         * before being written to the underlying stream.
         */
        private byte[] compressedBuffer;

        public LZFOutputStream(OutputStream out, LZFCodec codec,
                               byte[] recycledBuffer, byte[] recycledCompBuffer)
            throws IOException
        {
            compress = codec;
            _out = out;
            buffer = recycledBuffer;
            compressedBuffer = recycledCompBuffer;
            writeInt(LZFCodec.MAGIC, buffer);
        }

        /**
         * Method that will give upper bound estimate of size of block after
         * compression.
         */
        public static int maxSizeForInput(int inputSize) {
            if (inputSize < 256) {
                return inputSize + 16;
            }
            // Let's estimate growth of at most 1/16 (~= 6%)
            return inputSize + (inputSize >> 4);
        }

        private void ensureOutput(int len) {
            // Not sure if this is safe estimation of largest chunk we'd need, but:
            int outputLen = (len < 100) ? (len + 100) : len;
            outputLen += outputLen;
            if (compressedBuffer == null || compressedBuffer.length < outputLen) {
                compressedBuffer = new byte[outputLen];
            }
        }

        public void write(int b) throws IOException {
            if (pos >= buffer.length) {
                flush();
            }
            buffer[pos++] = (byte) b;
        }

        private void compressAndWrite(byte[] buff, int len) throws IOException {
            if (len > 0) {
                ensureOutput(len);
                int compressed = compress.compress(buff, len, compressedBuffer, 0);
                // If size actually increased (instead of compressing), output uncompressed
                if (compressed > len) {
                    writeInt(-len, compressedBuffer);
                    _out.write(buff, 0, len);
                } else { // compressed, good:
                    writeInt(compressed, buffer);
                    writeInt(len, buffer);
                    _out.write(compressedBuffer, 0, compressed);
                }
            }
        }

        private void writeInt(int x, byte[] buffer) throws IOException {
            /*
            _out.write((byte) (x >> 24));
            _out.write((byte) (x >> 16));
            _out.write((byte) (x >> 8));
            _out.write((byte) x);
            */
            buffer[0] = (byte) (x >> 24);
            buffer[1] = (byte) (x >> 16);
            buffer[2] = (byte) (x >> 8);
            buffer[3] = (byte) x;
            _out.write(buffer, 0, 4);
        }

        public void write(byte[] buff, int off, int len) throws IOException {
            while (len > 0) {
                int copy = Math.min(buffer.length - pos, len);
                System.arraycopy(buff, off, buffer, pos, copy);
                pos += copy;
                if (pos >= buffer.length) {
                    flush();
                }
                off += copy;
                len -= copy;
            }
        }

        public void flush() throws IOException {
            compressAndWrite(buffer, pos);
            pos = 0;
        }

        public void close() throws IOException {
            flush();
            _out.close();
        }

    }
   
}
