package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.codehaus.jackson.*;

/**
 * Converter that uses Jackson JSON processor for data binding,
 * using hand-written bindings, and gzip compresses content being
 * read and written.
 */
public class JacksonConverterManualGzip
    extends JacksonConverterManual
{
    final ReusableGzipInputStream _gzipIn = new ReusableGzipInputStream();

    final ReusableGzipOutputStream _gzipOut = new ReusableGzipOutputStream();

    @Override
    public DbData readData(InputStream in)
        throws IOException
    {
        _gzipIn.initialize(in);
        return super.readData(_gzipIn);
    }

    @Override
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        _gzipOut.initialize(out);
        return super.writeData(_gzipOut, data);
    }

    /*
    ////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////
     */

    final static class ReusableGzipInputStream
        extends InputStream
    {
        private final static int GZIP_MAGIC = 0x8b1f;

        /*
         * File header flags.
         */
        private final static int FTEXT	= 1;	// Extra text
        private final static int FHCRC	= 2;	// Header CRC
        private final static int FEXTRA	= 4;	// Extra field
        private final static int FNAME	= 8;	// File name
        private final static int FCOMMENT	= 16;	// File comment

        enum State {
            GZIP_HEADER, GZIP_CONTENT, GZIP_TRAILER, GZIP_COMPLETE;
        };

        /**
         * Size of input buffer for compressed input data.
         */
        final static int BUFFER_SIZE = 1024;

        protected final byte[] _buffer = new byte[BUFFER_SIZE];

        protected byte[] _tmpBuffer;

        protected int _bufferPtr;

        protected int _bufferEnd;

        protected Inflater _inflater;

        /**
         * Underlying input stream from which compressed data is to be
         * read from.
         */
        protected InputStream _rawInput;

        protected CRC32 _crc;

        /**
         * Flag set to true during handling of header processing
         */
        protected State _state;

        public ReusableGzipInputStream()
        {
            super();
        }

        /* InputStream impl */

        public int available()
        {
            if (_state == State.GZIP_COMPLETE) {
                return 0;
            }
            // not sure if this makes sense but...
            return _inflater.finished() ? 0 : 1;
        }

        public void close() throws IOException
        {
            _state = State.GZIP_COMPLETE;
            if (_rawInput != null) {
                _rawInput.close();
                _rawInput = null;
            }
        }

        public void mark(int limit) {
            // not supported... but not lethal to call
        }

        public boolean markSupported() {
            return false;
        }

        public final int read() throws IOException
        {
            byte[] tmp = _getTmpBuffer();
            int count = read(tmp, 0, 1);
            if (count < 0) {
                return -1;
            }
            return tmp[0] & 0xFF;
        }

        public final int read(byte[] buf) throws IOException
        {
            return read(buf, 0, buf.length);
        }

        public final int read(byte[] buf, int offset, int len) throws IOException
        {
            if (buf == null) {
                throw new NullPointerException();
            }
            if (offset < 0 || len < 0 || len > buf.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (_state == State.GZIP_COMPLETE) { // closed or EOF
                return -1;
            }
            if (len == 0) {
                return 0;
            }
            try {
                int count;
                while ((count = _inflater.inflate(buf, offset, len)) == 0) {
                    if (_inflater.finished() || _inflater.needsDictionary()) {
                        _readTrailer();
                        _state = State.GZIP_COMPLETE;
                        return -1;
                    }
                    if (_inflater.needsInput()) {
                        _loadMore();
                        _inflater.setInput(_buffer, 0, _bufferEnd);
                    }
                }
                _crc.update(buf, offset, count);
                return count;
            } catch (DataFormatException e) {
                String s = e.getMessage();
                throw new ZipException(s != null ? s : "Invalid ZLIB data format");
            }
        }

        public void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }

        public long skip(long n) throws IOException
        {
            if (n < 0) {
                throw new IllegalArgumentException("negative skip length");
            }
            byte[] tmp = _getTmpBuffer();
            long total = 0;
            int count;

            while ((count = read(tmp)) > 0) {
                total += count;
            }
            return total;
        }

        /* Extended API */

        public void initialize(InputStream compIn) throws IOException
        {
            _rawInput = compIn;
            _bufferPtr = _bufferEnd = 0;
            if (_inflater == null) { // true -> no wrapping (gzip)
                _inflater = new Inflater(true);
            } else {
                _inflater.reset();
            }
            // and construct/clear out CRC
            if (_crc == null) {
                _crc = new CRC32();
            } else {
                _crc.reset();
            }
            // And then need to process header...
            _readHeader();
            _state = State.GZIP_CONTENT;
            _crc.reset();
            // and if all is good, kick start inflater etc
            if (_bufferPtr >= _bufferEnd) { // need more data
                _loadMore();
            }
            _inflater.setInput(_buffer, _bufferPtr, _bufferEnd-_bufferPtr);
        }

        /* Internal */

        protected byte[] _getTmpBuffer()
        {
            if (_tmpBuffer == null) {
                _tmpBuffer = new byte[256];
            }
            return _tmpBuffer;
        }

        protected final void _readHeader() throws IOException
        {
            _state = State.GZIP_HEADER;

            // Check header magic
            int sig = _readShort();
            if (sig != GZIP_MAGIC) {
                throw new ZipException("Not in GZIP format (got 0x"+Integer.toHexString(sig)+", should be 0x"+Integer.toHexString(GZIP_MAGIC)+")");
            }
            // Check compression method
            if (_readByte() != Deflater.DEFLATED) {
                throw new ZipException("Unsupported compression method (only support Deflate, "+Deflater.DEFLATED+")");
            }
            // Read flags
            int flg = _readByte();
            // Skip MTIME, XFL, and OS fields
            _skipBytes(6);
            // Skip optional extra field
            if ((flg & FEXTRA) == FEXTRA) {
                _skipBytes(_readShort());
            }
            // Skip optional file name
            if ((flg & FNAME) == FNAME) {
                while (_readByte() != 0) ;
            }
            // Skip optional file comment
            if ((flg & FCOMMENT) == FCOMMENT) {
                while (_readByte() != 0) ;
            }
            // Check optional header CRC
            if ((flg & FHCRC) == FHCRC) {
                int act = (int)_crc.getValue() & 0xffff;
                int exp = _readShort();
                if (act != exp) {
                    throw new IOException("Corrupt GZIP header (header CRC 0x"
                                          +Integer.toHexString(act)+", expected 0x "
                                          +Integer.toHexString(exp));
                }
            }        
        }

        protected final void _readTrailer() throws IOException
        {
            int actCrc = (int) _crc.getValue();
            // does Inflater have leftovers?
            int remains = _inflater.getRemaining();
            if (remains > 0) {
                // ok, let's update ptr to indicate where we are at...
                _bufferPtr = _bufferEnd - remains;
            } else { // if not, just load more
                _loadMore(8);
            }
            int expCrc = _readInt();
            int expCount = _readInt();
            int actCount32 = (int) _inflater.getBytesWritten();

            if (actCount32 != expCount) {
                throw new ZipException("Corrupt trailer: expected byte count "+expCount+", read "+actCount32);
            }
            if (expCrc != actCrc) {
                throw new ZipException("Corrupt trailer: expected CRC "+Integer.toHexString(expCrc)+", computed "+Integer.toHexString(actCrc));
            }
        }

        private final void _skipBytes(int count) throws IOException
        {
            while (--count >= 0) {
                _readByte();
            }
        }

        private final int _readByte() throws IOException
        {
            if (_bufferPtr >= _bufferEnd) {
                _loadMore();
            }
            byte b = _buffer[_bufferPtr++];
            if (_state == State.GZIP_HEADER) {
                _crc.update(b);
            }
            return b & 0xFF;
        }

        private final int _readShort() throws IOException
        {
            // LSB... blech
            return _readByte() | (_readByte() << 8);
        }

        private final int _readInt() throws IOException
        {
            // LSB... yuck
            return _readByte() | (_readByte() << 8)
                | (_readByte() << 16) | (_readByte() << 24);
        }

        private final void _loadMore() throws IOException
        {
            _loadMore(_buffer.length);
        }

        private final void _loadMore(int max) throws IOException
        {
            int count = _rawInput.read(_buffer, 0, max);
            if (count < 1) {
                String prob = (count < 0) ?
                    "Unexpected end of input" : "Strange underlying stream (returned 0 bytes for read)";
                throw new IOException(prob+" when reading "+_state);
            }
            _bufferPtr = 0;
            _bufferEnd = count;
        }
    }

    final static class ReusableGzipOutputStream
        extends OutputStream
    {
    /**
     * GZIP header magic number.
     */
    private final static int GZIP_MAGIC = 0x8b1f;
    
    /**
     * For now, static header seems fine, since JDK default gzip writer
     * does it too:
     */
    final static byte[] DEFAULT_HEADER = new byte[] {
        (byte) GZIP_MAGIC,                // Magic number (short)
        (byte)(GZIP_MAGIC >> 8),          // Magic number (short)
        Deflater.DEFLATED,                // Compression method (CM)
        0,                                // Flags (FLG)
        0,                                // Modification time MTIME (int)
        0,                                // Modification time MTIME (int)
        0,                                // Modification time MTIME (int)
        0,                                // Modification time MTIME (int)
        0,                                // Extra flags (XFLG)
        (byte) 0xff                       // Operating system (OS), UNKNOWN
    };

    protected final byte[] _eightByteBuffer = new byte[8];

    protected Deflater _deflater;

    /**
     * Underlying output stream that header, compressed content and
     * footer go to
     */
    protected OutputStream _rawOut;

    protected DeflaterOutputStream _deflaterOut;
    
    protected CRC32 _crc;

    public ReusableGzipOutputStream()
    {
        super();
    }

    /*
    //////////////////////////////////////////////////
    // OutputStream API
    //////////////////////////////////////////////////
     */

    public void close() throws IOException
    {
        _deflaterOut.finish();
        _writeTrailer(_rawOut);
        _rawOut.close();
    }

    public void flush() throws IOException
    {
        _deflaterOut.flush();
    }

    public final void write(byte[] buf) throws IOException
    {
        write(buf, 0, buf.length);
    }

    public final void write(int c) throws IOException
    {
        _eightByteBuffer[0] = (byte) c;
        write(_eightByteBuffer, 0, 1);
    }

    public void write(byte[] buf, int off, int len) throws IOException
    {
        _deflaterOut.write(buf, off, len);
        _crc.update(buf, off, len);
    }

    /*
    //////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////
     */

    public void initialize(OutputStream compOut) throws IOException
    {
        _rawOut = compOut;
        // write header:
        _rawOut.write(DEFAULT_HEADER);
        // construct deflater stream we need:
        if (_deflater == null) {
            // lowest compression level (1)
            //_deflater = new Deflater(1, true);
            _deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        } else {
            _deflater.reset();
        }
        _deflaterOut = new DeflaterOutputStream(_rawOut, _deflater, 512);
        // and construct/clear out CRC
        if (_crc == null) {
            _crc = new CRC32();
        } else {
            _crc.reset();
        }
    }

    /*
    //////////////////////////////////////////////////
    // Internal
    //////////////////////////////////////////////////
     */

    private void _writeTrailer(OutputStream out) throws IOException
    {
        _putInt(_eightByteBuffer, 0, (int) _crc.getValue());
        _putInt(_eightByteBuffer, 4, _deflater.getTotalIn());
        out.write(_eightByteBuffer, 0, 8);
    }

    /**
     * Stupid GZIP, writes stuff in wrong order (not network, but x86)
     */
    private void _putInt(byte[] buf, int offset, int value)
    {
        buf[offset++] = (byte) (value);
        buf[offset++] = (byte) (value >> 8);
        buf[offset++] = (byte) (value >> 16);
        buf[offset] = (byte) (value >> 24);
    }
    }
}
