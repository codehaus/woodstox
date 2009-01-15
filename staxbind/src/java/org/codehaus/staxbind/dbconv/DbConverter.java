package org.codehaus.staxbind.dbconv;

import java.io.*;

import org.codehaus.staxbind.std.StdConverter;
import org.codehaus.staxbind.std.StdItem;

/**
 * Base class for per-format converters used for "DB Converter" performance
 * test suite.
 */
public abstract class DbConverter
    extends StdConverter<DbData>
{
    final static String FIELD_TABLE = "table";
    final static String FIELD_ROW = "row";

    protected final static String WSTX_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    protected final static String WSTX_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";

    /*
    final static String FIELD_ID = "id";
    final static String FIELD_FIRSTNAME = "firstname";
    final static String FIELD_LASTNAME = "lastname";
    final static String FIELD_ZIP = "zip";
    final static String FIELD_STREET = "street";
    final static String FIELD_CITY = "city";
    final static String FIELD_STATE = "state";
    */

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
