package org.codehaus.staxbind.twitter;

import org.codehaus.staxbind.std.StdJaxbConverter;

public final class JaxbDriver
    extends TwitterDriver
{
    public JaxbDriver() throws Exception
    {
        super(new StdJaxbConverter(TwitterSearch.class));
    }
}
