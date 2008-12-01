package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

/**
 * Base class for per-format converters used for "DB Converter" performance
 * test suite.
 */
public abstract class DbConverter
{
    public enum Operation {
        READ, WRITE, READ_WRITE
    }

    /**
     * Method that is to read all the data and convert it to
     * representation of full database contents
     */
    public abstract DbData readData(InputStream in) throws Exception;

    /**
     * Method that is to read all the data and convert it to
     * representation of full database contents
     *
     * @return Bogus result value; ideally generated from data, arbitrary
     *   but not random. Need to ensure no dead code elimination occurs
     */
    public abstract int writeData(OutputStream out, DbData data) throws Exception;
}
