package org.codehaus.staxbind.dbconv;

import org.codehaus.staxbind.std.StdJaxbConverter;

public final class JaxbDriver
    extends DbconvDriver
{
    /* 15-Jan-2009, tatu: Not good, should do some other way...
     *    (for one, to support using Aalto here)
     */
    protected final static String WSTX_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    protected final static String WSTX_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";


   public JaxbDriver() throws Exception
    {
        super(new StdJaxbConverter(DbData.class, WSTX_INPUT_FACTORY, WSTX_OUTPUT_FACTORY));
    }
}
