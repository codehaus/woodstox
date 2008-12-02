package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

public final class WstxDriver
    extends DbconvDriver
{
    public WstxDriver()
    {
        super(getConverter());
    }

    final static DbConverter getConverter()
    {
        return new StaxXmlConverter("com.ctc.wstx.stax.WstxInputFactory",
                                    "com.ctc.wstx.stax.WstxOutputFactory");
    }
}
