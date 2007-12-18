package test;

import java.io.*;
import java.util.List;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.ctc.wstx.api.WstxInputProperties;

import com.ctc.wstx.sr.BasicStreamReader;
import com.ctc.wstx.sr.InputElementStack;

/**
 * Simple non-automated testing class used for checking that W3C Schema
 * validation features work ok.
 */
public class TestW3CSchema
    implements XMLStreamConstants
{
    private TestW3CSchema() { }

    protected int test(File schemaFile, File xmlFile)
        throws Exception
    {
        XMLValidationSchemaFactory schF = XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA);
        XMLValidationSchema schema = schF.createSchema(schemaFile);

        System.err.println("Schema succesfully loaded, instance: "+schema);

        XMLInputFactory2 f = (XMLInputFactory2) XMLInputFactory.newInstance();
        System.out.println("Factory instance: "+f.getClass());

        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

        f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        InputStream in = new FileInputStream(xmlFile);
        XMLStreamReader2 streamReader = (XMLStreamReader2) f.createXMLStreamReader(xmlFile.toURL().toString(), in);

        streamReader.validateAgainst(schema);

        int total = 0;
        while (streamReader.hasNext()) {
            int type = streamReader.next();
            total += type; // so it won't be optimized out...
            boolean hasName = streamReader.hasName();

            System.out.print("["+type+"]");

            if (streamReader.hasText()) {
                int textLen = streamReader.getTextLength();
                //total += textLen;
                String text = streamReader.getText();
                // Sanity check (note: RI tends to return nulls?)
                if (text != null) {
                    char[] textBuf = streamReader.getTextCharacters();
                    int start = streamReader.getTextStart();
                    String text2 = new String(textBuf, start, textLen);
                    if (!text.equals(text2)) {
                        throw new Error("Text access via 'getText()' different from accessing via buffer!");
                    }
                }

                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length(); // to prevent dead code elimination
                }
                if (type == CHARACTERS || type == CDATA) {
                    System.out.println(" Text = '"+text+"'.");
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    List entities = (List) streamReader.getProperty("javax.xml.stream.entities");
                    List notations = (List) streamReader.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.println(" DTD ("+entCount+" entities, "+notCount
                                       +" notations), declaration = <<\n");
                    System.out.println(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+streamReader.getLocalName()+" -> '"+streamReader.getText()+"'.");
                    hasName = false; // to suppress further output
                } else if (type == COMMENT) {
                    System.out.println(" Comment <!--"+text+"-->");
                } else { // comment, PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+streamReader.getPITarget()+"'.");
                System.out.println(" PI data = '"+streamReader.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                int acount = streamReader.getAttributeCount();
                System.out.print(" ["+acount+" attrs]");
                for (int i = 0; i < acount; ++i) {
                    System.out.println("  #"+i+": "+streamReader.getAttributeName(i)+" -> '"+streamReader.getAttributeValue(i)+"'");
                }
            }
            if (hasName) {
                System.out.print(" Name: '"+streamReader.getName()+"' (prefix <"
                                   +streamReader.getPrefix()+">)");
            }
            System.out.println();
        }
        streamReader.close();

        return total;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 2) {
            System.err.println("Usage: java ... "+TestW3CSchema.class+" [schema] [xmlfile]");
            System.exit(1);
        }
        try {
            int total = new TestW3CSchema().test(new File(args[0]), new File(args[1]));
            System.out.println("Total: "+total);
        } catch (Throwable t) {
            System.err.println("Error: "+t);
            t.printStackTrace();
        }
    }
}