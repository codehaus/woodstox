/* ValidateXML utility
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.tools;

import java.io.*;
import java.net.URL;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.dtd.DTDSchemaFactory;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.util.URLUtil;

/**
 * Simple command-line utility that allows validating a file
 * (or, actually, any resource that can be validated with an URL
 * that JDK recognizes)
 * either using DTD it specifies, or
 * DTD specified with command line argument. Will validate the
 * document, and indicate any errors it finds, if any.
 */
public class ValidateXML
{
    final static String SWITCH_HELP = "help";
    final static String SWITCH_DTD = "dtd=";

    private ValidateXML() { }

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

        String dtdRef = null;
        int i = 0;
        int len = args.length;

        for (; i < len; ++i) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                break;
            }

            arg = arg.substring(2);
            if (arg.startsWith(SWITCH_DTD)) {
                dtdRef = arg.substring(SWITCH_DTD.length());
                continue;
            }
            if (arg.equals(SWITCH_HELP)) {
                showHelp();
                continue;
            }
            System.err.println("FAIL: Unrecognized option '"+args[i]+"'.");
            showUsage();
        }

        // Any documents to check?
        if (i == len) {
            System.err.println("FAIL: no documents to validate.");
            System.exit(1);
        }

        // DTD to use instead of doc specified one(s)?
        URL dtdSrc = null;
        if (dtdRef != null) {
            try {
                dtdSrc = URLUtil.urlFromSystemId(dtdRef);
            } catch (IOException ie) {
                System.err.println("FAIL: could not open DTD '"+dtdRef+"': "+ie);
                System.exit(1);
            }
        }
        
        WstxInputFactory f = new WstxInputFactory();
        ReaderConfig cfg = f.getConfig();
    
        cfg.doSupportNamespaces(true);
        cfg.doSupportDTDs(true);
        cfg.doValidateWithDTD(true);
        cfg.setXMLReporter(new Reporter());

        // Ok, then, let's loop over docs

        for (; i < len; ++i) {
            String sysId = args[i];
            URL xmlSrc = null;
            
            try {
                xmlSrc = URLUtil.urlFromSystemId(sysId);
            } catch (IOException ie) {
                System.err.println("FAIL: could not open xml document '"+sysId+"': "+ie);
                continue;
            }

            InputStream in = null;
            
            try {
                in = xmlSrc.openStream();
                XMLStreamReader sr = f.createXMLStreamReader(xmlSrc.toExternalForm(), in);
                // Override DTD specified?
                if (dtdSrc != null) {
                    DTDSchemaFactory vf = new DTDSchemaFactory();
                    XMLValidationSchema schema = vf.createSchema(dtdSrc);
                    System.out.println("  [trying to use dtd '"+dtdSrc+" for validation]");
                    ((XMLStreamReader2) sr).setFeature(XMLStreamReader2.FEATURE_DTD_OVERRIDE,
                                                       schema);
                }
                
                while (sr.hasNext()) {
                    sr.next();
                }
                
                System.out.println("OK: document '"+sysId+"' succesfully validated!");
            } catch (XMLStreamException strEx) {
                System.out.println("FAIL: document '"+sysId+"' has validity problem: "+strEx);
//strEx.printStackTrace();
            } catch (IOException ie) {
                System.out.println("FAIL: document '"+sysId+"' has I/O error: "+ie);
             } finally {
                 if (in != null) {
                     try {
                         in.close();
                     } catch (IOException ie2) {
                         System.err.println("Failed to close the XML document: "+ie2);
                     }
                 }
             }
        } // doc loop
    }

    private final static void showUsage()
    {
        printUsage(System.err);
        System.exit(1);
    }

    private final static void printUsage(PrintStream out)
    {
        out.println("Usage: "+(ValidateXML.class)+" [options] [xml doc 1] ... [xml doc N]");
        out.println(" options:");
        out.println("   --"+SWITCH_DTD+"[DTD to use instead of doc specified one]");
        out.println("   --"+SWITCH_HELP+" [displays full help]");
    }

    private final static void showHelp()
    {
        PrintStream out = System.out;
        printUsage(out);
        out.println();
        out.println("Options allow you to do following things:");
        out.println();
        out.println("* Specify a DTD (by URL that points to it) that should be used for");
        out.println("  validation instead of whatever document points to (if any)");
    }


    static class Reporter
        implements XMLReporter
    {
        public void report(String msg, String errorType, Object info, Location location)
        {
            System.err.println("WARNING: "+msg+" [at "+location+"]");
        }
    }
}

