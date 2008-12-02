package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

public final class Wstx4Driver
    extends DbconvDriver
{
    public Wstx4Driver()
    {
        super(getConverter());
    }

    final static DbConverter getConverter()
    {
        return new Stax2XmlConverter("com.ctc.wstx.stax.WstxInputFactory",
                                     "com.ctc.wstx.stax.WstxOutputFactory");
    }
}
