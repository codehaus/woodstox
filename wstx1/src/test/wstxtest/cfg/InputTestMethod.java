package wstxtest.cfg;

import com.ctc.wstx.stax.WstxInputFactory;

public interface InputTestMethod
{
    public void runTest(WstxInputFactory f, InputConfigIterator it)
        throws Exception;
}
