package com.ctc.wstx.tools;

import java.io.*;
import java.net.URL;

import com.ctc.wstx.stax.dtd.DTDSubset;
import com.ctc.wstx.stax.dtd.FullDTDReader;
import com.ctc.wstx.stax.io.InputSourceFactory;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.util.LineSuppressWriter;
import com.ctc.wstx.util.URLUtil;

/**
 * Simple command-line utility that allows "flattening" of external DTD
 * subsets. It will read in the DTD, expand all parameter entities, and
 * output result to standard output.
 */
public class DTDFlatten
{
    final static String SWITCH_OUTPUT = "output";
    final static String SWITCH_STRIP = "strip";

    final static String SWITCH_COMMENT = "comments";
    final static String SWITCH_CONDITIONAL = "conditional-sections";
    final static String SWITCH_PARAM_ENTITIES = "pe-decls";
    final static String SWITCH_WHITESPACE = "whitespace";
    final static String SWITCH_HELP = "help";

    final static String SWITCH_WS_ALL = "all";
    final static String SWITCH_WS_COMPACT = "compact";
    final static String SWITCH_WS_MINIMUM = "minimum";

    final static boolean DEFAULT_INCL_COMMENTS = true;
    final static boolean DEFAULT_INCL_CONDITIONAL = false;
    final static boolean DEFAULT_INCL_PARAM_ENTITIES = false;

    public static void main(String[] args)
        throws Exception
    {
        if (args.length < 1) {
            showUsage();
        }

        // Simple case: only checking help
        if (args.length == 1) {
            if (args[0].equals("--"+SWITCH_HELP)) {
                showHelp();
                return;
            }
        }

        PrintWriter pw = new PrintWriter(System.out);
        LineSuppressWriter out = new LineSuppressWriter(pw);
        out.setMaxConsequtiveEmptyLines(1); // compact
        out.setTrim(false, true);

        boolean inclComments = DEFAULT_INCL_COMMENTS;
        boolean inclConditional = DEFAULT_INCL_CONDITIONAL;
        boolean inclParamEntities = DEFAULT_INCL_PARAM_ENTITIES;

        for (int i = 0, len = args.length-1; i < len; ++i) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                arg = arg.substring(2);
                if (arg.startsWith(SWITCH_OUTPUT+"-")) {
                    arg = arg.substring(SWITCH_OUTPUT.length()+1);
                    if (arg.equals(SWITCH_COMMENT)) {
                        inclComments = true;
                        continue;
                    }
                    if (arg.equals(SWITCH_CONDITIONAL)) {
                        inclConditional = true;
                        continue;
                    }
                    if (arg.equals(SWITCH_PARAM_ENTITIES)) {
                        inclParamEntities = true;
                        continue;
                    }
                    if (arg.startsWith(SWITCH_WHITESPACE+":")) {
                        arg = arg.substring(SWITCH_WHITESPACE.length()+1);
                        if (arg.equals(SWITCH_WS_ALL)) {
                            out.setMaxConsequtiveEmptyLines(Integer.MAX_VALUE);
                            out.setTrim(false, false);
                        } else if (arg.equals(SWITCH_WS_COMPACT)) {
                            out.setMaxConsequtiveEmptyLines(1);
                            out.setTrim(false, true);
                        } else if (arg.equals(SWITCH_WS_MINIMUM)) {
                            out.setMaxConsequtiveEmptyLines(0);
                            out.setTrim(true, true);
                        } else {
                            System.err.println
("Unrecognized value '"+arg+"' for option --"+SWITCH_OUTPUT+"-"
 +SWITCH_WHITESPACE+": excepted '"
 +SWITCH_WS_ALL+"', '"
 +SWITCH_WS_COMPACT+"' or '"
 +SWITCH_WS_MINIMUM+"'.");
                            System.exit(1);
                        }
                        continue;
                    }
                } else if (arg.startsWith(SWITCH_STRIP+"-")) {
                    arg = arg.substring(SWITCH_STRIP.length()+1);
                    if (arg.equals(SWITCH_COMMENT)) {
                        inclComments = false;
                        continue;
                    }
                    if (arg.equals(SWITCH_CONDITIONAL)) {
                        inclConditional = false;
                        continue;
                    }
                    if (arg.equals(SWITCH_PARAM_ENTITIES)) {
                        inclParamEntities = false;
                        continue;
                    }
                } else if (arg.equals(SWITCH_HELP)) {
                    showHelp();
                }
            }
            System.err.println("Unrecognized option '"+args[i]+"'.");
            showUsage();
        }

        String sysId = args[args.length-1];
        Reader fr = new FileReader(sysId);
        URL url = URLUtil.urlFromSystemId(sysId);
        /* Let's not do any bootstrapping for the input source -- that way
         * xml declaration will also be included (as long as 'xml' in
         * what looks like a proc. instr. won't cause trouble)
         */
        WstxInputSource input = InputSourceFactory.constructReaderSource
            (null, null, null, // no parent, not from entity, no bootstrapper
             null, sysId, url, fr, true, 4000);
        DTDSubset ss = FullDTDReader.flattenExternalSubset
            (input, out,
             inclComments, inclConditional, inclParamEntities);
        out.flush();

        fr.close();
    }

    private final static void showUsage()
    {
        printUsage(System.err);
        System.exit(1);
    }

    private final static void printUsage(PrintStream out)
    {
        out.println("Usage: "+(DTDFlatten.class)+"[flags] [DTD file]");
        out.println(" flags:");
        out.println("   --"+SWITCH_OUTPUT+"-"+SWITCH_COMMENT+" (default)");
        out.println("   --"+SWITCH_STRIP+"-"+SWITCH_COMMENT);
        out.println("   --"+SWITCH_OUTPUT+"-"+SWITCH_CONDITIONAL);
        out.println("   --"+SWITCH_STRIP+"-"+SWITCH_CONDITIONAL+" (default)");
        out.println("   --"+SWITCH_OUTPUT+"-"+SWITCH_PARAM_ENTITIES);
        out.println("   --"+SWITCH_STRIP+"-"+SWITCH_PARAM_ENTITIES+" (default)");
        out.println("   --"+SWITCH_OUTPUT+"-"+SWITCH_WHITESPACE+":<mode> (mode: all/compact/minimum; default 'compact'");
        out.println("   --"+SWITCH_HELP+" [displays full help]");
    }

    private final static void showHelp()
    {
        PrintStream out = System.out;
        printUsage(out);
        out.println();
        out.println("Switches allow you to do following things:");
        out.println();
        out.println(
"* Include/exclude comments from output."
);
        out.println(
"   [default is to include comments in output]"
);
        out.println(
"* Include/exclude conditional sections ('<![IGNORE ... ]]>'"
);
        out.println(
"  and <![INCLUDE ...]]> from output, or not: if not output, both"
);
        out.println(
"  markers will be removed, as well as contents inside ignored section."
);
        out.println(
"   [default is to exclude conditional sections from output]"
);

        out.println(
"* Include/exclude parameter entity declarations."
);
        out.println(
"   [default is to exclude PE declarations from output]"
);
        out.println(
"* Compress some of whitespace out."
);
        out.println(
"   [default is to do moderate compression, only include at most one empty"
);
        out.println(
"    line, and strip trailing white space]"
);
    }
}

