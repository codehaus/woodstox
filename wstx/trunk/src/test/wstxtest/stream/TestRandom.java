package wstxtest.stream;

import java.io.*;
import java.util.Random;

import javax.xml.stream.*;

import wstxtest.cfg.*;

import com.ctc.wstx.stax.WstxInputFactory;

/**
 * Unit test suite that ensures that independent of combinations of settings
 * such as namespace-awareness, coalescing, automatic entity replacement,
 * parsing results remain the same when they should.
 */
public class TestRandom
    extends BaseStreamTest
    implements InputTestMethod
{
    InputConfigIterator mConfigs;

    public TestRandom(String name) {
        super(name);
        mConfigs = new InputConfigIterator();
        mConfigs.addConfig(Configs.getLazyParsingConfig())
            .addConfig(Configs.getNormalizeLFsConfig())
            .addConfig(Configs.getInputBufferSizeConfig())
            .addConfig(Configs.getMinTextSegmentConfig())
            ;
    }

    public void testCoalescingAutoEntity()
        throws Exception
    {
        doTest(false, true, true); // non-ns
        doTest(true, true, true); // ns-aware
    }

    public void testNonCoalescingAutoEntity()
        throws Exception
    {
        doTest(false, false, true); // non-ns
        doTest(true, false, true); // ns-aware
    }

    public void testCoalescingNonAutoEntity()
        throws Exception
    {
        doTest(false, true, false); // non-ns
        doTest(true, true, false); // ns-aware
    }

    public void testNonCoalescingNonAutoEntity()
        throws Exception
    {
        doTest(false, false, false); // non-ns
        doTest(true, false, false); // ns-aware
    }

    /*
    ////////////////////////////////////////
    // Private methods, common test code
    ////////////////////////////////////////
     */

    String mInput, mExpOutput;

    /**
     * Main branching point has settings for standard features; it
     * will further need to loop over Woodstox-specific settings.
     */
    private void doTest(boolean ns, boolean coalescing, boolean autoEntity)
        throws Exception
    {
        /* Let's generate seed from args so it's reproducible; String hash
         * code only depend on text it contains, so it'll be fixed for
         * specific String.
         */
        String baseArgStr = "ns: "+ns+", coalesce: "+coalescing+", entityExp: "+autoEntity;
        long seed = baseArgStr.hashCode();

        WstxInputFactory f = getInputFactory();

        // Settings we always need:
        f.doSupportDTDs(true);
        f.doValidateWithDTD(false);

        // Then variable ones we got settings for:
        f.doSupportNamespaces(ns);
        f.doCoalesceText(coalescing);
        f.doReplaceEntityRefs(autoEntity);

        /* How many random permutations do we want to try?
         */
        final int ROUNDS = 5;

        for (int round = 0; round < ROUNDS; ++round) {
            Random r = new Random(seed+round);
            StringBuffer inputBuf = new StringBuffer(1000);
            StringBuffer expOutputBuf = new StringBuffer(1000);

            generateData(r, inputBuf, expOutputBuf, autoEntity);

            mInput = inputBuf.toString();
            mExpOutput = expOutputBuf.toString();
            mConfigs.iterate(f, this);
        }
    }

    /**
     * Method called via input config iterator, with all possible
     * configurations
     */
    public void runTest(WstxInputFactory f, InputConfigIterator it)
        throws Exception
    {
        // First, let's skip through it all
        streamAndSkip(f, it, mInput);

        // and then the 'real' test:
        streamAndCheck(f, it, mInput, mExpOutput);
    }

    private void generateData(Random r, StringBuffer input,
                              StringBuffer output, boolean autoEnt)
    {
        final String PREAMBLE =
            "<?xml version='1.0' encoding='UTF-8'?>"
            +"<!DOCTYPE root [\n"
            +" <!ENTITY ent1 'ent1Value'>\n"
            +" <!ENTITY x 'Y'>\n"
            +" <!ENTITY both '&ent1;&x;'>\n"
            +"]>";

        /* Ok; template will use '*' chars as placeholders, to be replaced
         * by pseudo-randomly selected choices.
         */
        final String TEMPLATE =
            "<root>"

            // Short one for trouble shooting:
            +" * Text ****<empty></empty>\n</root>"

            // Real one for regression testing:
            /*
            +" * Text ****<empty></empty>\n"
            +"<empty>*</empty>*  * xx<empty></empty>\n"
            +"<tag>Text ******</tag>\n"
            +"<a>*...</a><b>...*</b><c>*</c>"
            +"<c>*</c><c>*</c><c>*</c><c>*</c><c>*</c><c>*</c>"
            +"<c>*<d>*<e>*</e></d></c>"
            +"<c><d><e>*</e>*</d>*</c>"
            +"a*b*c*d*e*f*g*h*i*j*k"
            +"</root>"
            */
            ;

        input.append(TEMPLATE);
        output.append(TEMPLATE);

        for (int i = TEMPLATE.length(); --i >= 0; ) {
            char c = TEMPLATE.charAt(i);

            if (c == '*') {
                replaceEntity(input, output, autoEnt, r, i);
            }
        }

        // Let's also insert preamble into input now
        input.insert(0, PREAMBLE);
    }

    private void replaceEntity(StringBuffer input, StringBuffer output,
                               boolean autoEnt,
                               Random r, int index)
    {
        String in, out;
        
        switch (Math.abs(r.nextInt()) % 5) {
        case 0: // Let's use one of pre-def'd entities:
            switch (Math.abs(r.nextInt()) % 5) {
            case 0:
                in = "&amp;";
                out = "&";
                break;
            case 1:
                in = "&apos;";
                out = "'";
                break;
            case 2:
                in = "&lt;";
                out = "<";
                break;
            case 3:
                in = "&gt;";
                out = ">";
                break;
            case 4:
                in = "&quot;";
                out = "\"";
                break;
            default: throw new Error("Internal error!");
            }
            break;
        case 1: // How about some CDATA?
            switch (Math.abs(r.nextInt()) % 4) {
            case 0:
                in = "<![CDATA[]] >]]>";
                out = "]] >";
                break;
            case 1:
                in = "<![CDATA[xyz&abc]]>";
                out = "xyz&abc";
                break;
            case 2:
                in = "<!--comment-->";
                out = "<!--comment-->";
                break;
            case 3:
                in = "<![CDATA[ ]]>";
                out = " ";
                break;
            default: throw new Error("Internal error!");
            }
            break;
        case 2: // Char entities?
            switch (Math.abs(r.nextInt()) % 4) {
            case 0:
                in = "&#35;";
                out = "#";
                break;
            case 1:
                in = "&#x24;";
                out = "$";
                break;
            case 2:
                in = "&#169;"; // above US-Ascii, copyright symbol
                out = "\u00A9";
                break;
            case 3:
                in = "&#xc4;"; // Upper-case a with umlauts
                out = "\u00C4";
                break;
            default: throw new Error("Internal error!");
            }
            break;
        case 3: // Full entities
            switch (Math.abs(r.nextInt()) % 3) {
            case 0:
                in = "&ent1;";
                out = "ent1Value";
                break;
            case 1:
                in = "&x;";
                out = "Y";
                break;
            case 2:
                in = "&both;";
                out = autoEnt ? "ent1ValueY" : "&ent1;&x;";
                break;
            default: throw new Error("Internal error!");
            }
            break;

        case 4: // Plain text, ISO-Latin chars:
            in = out = "(\u00A9)"; // copyright symbol
            break;

        default:
            throw new Error("Internal error!");
        }
        input.replace(index, index+1, in);
        output.replace(index, index+1, out);
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */
}
