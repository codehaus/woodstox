package test;

import java.net.URL;

import javax.xml.stream.*;

import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;

/**
 * Simple debug 'resolver'...
 */
public class TestResolver2
    implements WstxInputResolver
{
    public WstxInputSource resolveReference(WstxInputSource refCtxt, String entityId,
                                            String publicId, String systemId,
                                            URL assumedLoc)
    {
        System.err.println("WstxInputResolver: '"+publicId+"'/'"+systemId
                           +", assumed '"+assumedLoc+"', entity '"+entityId+"'.");
        return null;
    }
}
