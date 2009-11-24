package org.codehaus.staxbind.dbconv;

/**
 * Driver that uses "manual" (hand-written) serialization with Jackson
 * using LZF
 * (compared to Bean-generated one, gzip-compressed etc)
 */
public class JacksonDriverManualLZF
    extends DbconvDriver
{
    public JacksonDriverManualLZF() throws Exception
    {
        super(new JacksonConverterManualLZF());
    }
}
