package org.codehaus.staxbind.dbconv;

import java.io.*;

import org.codehaus.staxbind.japex.BaseJapexDriver;

public abstract class DbconvDriver
    extends BaseJapexDriver<DbConverter.Operation>
{
    final DbConverter _converter;

    /**
     * For write tests, we hold serializable objects here
     */
    DbData[] _writableData;

    /**
     * For read tests, we have byte arrays to read from here
     */
    byte[][] _readableData;

    protected DbconvDriver(DbConverter conv)
    {
        super(DbConverter.Operation.READ);
        if (conv == null) {
            throw new IllegalArgumentException();
        }
        _converter = conv;
    }

    protected int runTest(DbConverter.Operation oper) throws Exception
    {
        if (oper == null) {
            throw new RuntimeException("Internal exception: no operation passed");
        }

        final boolean doRead = (oper != DbConverter.Operation.WRITE);
        final boolean doWrite = (oper != DbConverter.Operation.READ);

        // read or read-write?
        if (doRead) {
            int result = 0;
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(16000);
            for (byte[] input : _readableData) {
                DbData dd = _converter.readData(new ByteArrayInputStream(input));
                if (dd == null) {
                    throw new IllegalStateException("Deserialized doc to null");
                }
                result += dd.size();
                _totalLength += input.length;
                if (doWrite) {
                    result += _converter.writeData(bos, dd);
                    _totalLength += bos.size();
                    bos.reset();
                }
            }
            return result;
        }

        // write-only:
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(16000);
        int result = 0;
        for (DbData input : _writableData) {
            result += _converter.writeData(bos, input);
            _totalLength += bos.size();
            bos.reset();
        }
        return result;
    }

    protected void doLoadTestData(DbConverter.Operation oper, File dir) throws Exception
    {
        /* First of all, read in all the data, bind to in-memory object(s),
         * and then (if read test), convert to the specific type converter
         * uses.
         */
        byte[] readBuffer = new byte[DEFAULT_BUF_SIZE];
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream(DEFAULT_BUF_SIZE);
        _totalLength = 0;

        File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

        DbConverter stdConverter = getStdConverter();
        _readableData = (oper == DbConverter.Operation.WRITE) ?
            null : new byte[files.length][];
        _writableData = (oper == DbConverter.Operation.WRITE) ?
            new DbData[files.length] : null;

        for (int i = 0, len = files.length; i < len; ++i) {
            File f = files[i];
            // Read file contents, bind to in-memory object (using std conv)
            readAll(f, readBuffer, tmpStream);
            byte[] fileData = tmpStream.toByteArray();
            DbData origData = stdConverter.readData(new ByteArrayInputStream(fileData));
            if (_writableData != null) {
                _writableData[i] = origData;
            }

            /* Then we better verify that we can round-trip content from
             * object to native format and back: and if it comes back
             * equal to original data, we are good to go.
             */

            tmpStream.reset();
            _converter.writeData(tmpStream, origData);
            byte[] convData = tmpStream.toByteArray();
            
            if (_readableData != null) {
                _readableData[i] = convData;
            }

            DbData convResults = _converter.readData(new ByteArrayInputStream(convData));
            if (!convResults.equals(origData)) {
                // Not very clean, but let's output for debugging:
                System.err.println("Incorrect mapping");
                System.err.println("Source xml: ["+origData+"]");
                System.err.println("Using "+_converter+": ["+convResults+"]");
                throw new IllegalStateException("Item #"+i+"/"+len+" differs for '"+_converter+"'");
            }
        }
    }

    protected DbConverter getStdConverter()
    {
        // Let's use Woodstox 3 driver as the trusted one...
        return Wstx3Driver.getConverter();
    }
}