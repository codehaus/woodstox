package org.codehaus.staxbind.dbconv;

/**
 * Driver that uses "manual" (hand-written) serialization with Jackson
 * using GZIP
 * (compared to Bean-generated one, or non-gzip)
 */
public final class JacksonDriverManualGzip
    extends DbconvDriver
{
    public JacksonDriverManualGzip() throws Exception
    {
        super(new JacksonConverterManualGzip());
    }
}
