package org.codehaus.staxbind.twitter;

import org.codehaus.staxbind.std.StdJaxbConverter;

public final class JaxbDriver
    extends TwitterDriver
{
    // Shouldn't hard code here, but...
    protected final static String WSTX_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    protected final static String WSTX_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";

   public JaxbDriver() throws Exception
    {
        super(new StdJaxbConverter(TwitterSearch.class, WSTX_INPUT_FACTORY, WSTX_OUTPUT_FACTORY));
    }
}
