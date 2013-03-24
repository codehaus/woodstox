package org.codehaus.stax.test.stream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.exc.WstxLazyException;

/**
 * 
 */
public class TestCharacterLimits
    extends BaseStreamTest
{
    public TestCharacterLimits() {
    }

    private Reader createLongReader(final String pre, final String post, final boolean ws) {
        final int max = Integer.MAX_VALUE;
        final StringBuffer start = new StringBuffer("<ns:element xmlns:ns=\"http://foo.com\">" + pre);
        return new Reader() {
            StringReader sreader = new StringReader(start.toString());
            int count;
            boolean done;
            public int read(char[] cbuf, int off, int len) throws IOException {
                int i = sreader.read(cbuf, off, len);
                if (i == -1) {
                    if (count < max) {
                        if (ws) {
                            sreader = new StringReader("                              ");
                        } else {
                            sreader = new StringReader("1234567890123<?foo?>78901234567890");                            
                        }
                        count++;
                    } else if (!done) {
                        if (ws) {
                            sreader = new StringReader(post + "</ns:element>");
                        } else {
                            sreader = new StringReader(post + "<ns:el2>foo</ns:el2></ns:element>");
                        }
                        done = true;
                    }
                    i = sreader.read(cbuf, off, len);
                }
                return i;
            }
            public void close() throws IOException {
            }
        };
    }
    public void testLongGetElementText() throws Exception {
        try {
            Reader reader = createLongReader("", "", false);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            System.out.println(xmlreader.getElementText());
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    public void testLongElementText() throws Exception {
        try {
            Reader reader = createLongReader("", "", false);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(100000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            assertEquals(XMLStreamReader.CHARACTERS, xmlreader.next());
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }    
    public void testLongWhitespaceNextTag() throws Exception {
        try {
            Reader reader = createLongReader("", "", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
        
    public void testLongWhitespace() throws Exception {
        try {
            Reader reader = createLongReader("", "", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(50000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            assertEquals(XMLStreamReader.CHARACTERS, xmlreader.next());
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    
    
    public void testLongCDATA() throws Exception {
        try {
            Reader reader = createLongReader("<![CDATA[", "]]>", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(50000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            assertEquals(XMLStreamReader.CDATA, xmlreader.next());
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    public void testLongCDATANextTag() throws Exception {
        try {
            Reader reader = createLongReader("<![CDATA[", "]]>", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    public void testLongComment() throws Exception {
        try {
            Reader reader = createLongReader("<!--", "-->", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.COMMENT) {
            }
            System.out.println(xmlreader.getText());
            fail("Should have failed");
        } catch (WstxLazyException ex) {
            //expected
        }
    }
    public void testLongComment2() throws Exception {
        try {
            Reader reader = createLongReader("<!--", "-->", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.COMMENT) {
            }
            System.out.println(new String(xmlreader.getTextCharacters()));
            fail("Should have failed");
        } catch (WstxLazyException ex) {
            //expected
        }
    }
    public void testLongCommentNextTag() throws Exception {
        try {
            Reader reader = createLongReader("<!--", "-->", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.COMMENT) {
            }
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    
    public void testLongCommentCoalescing() throws Exception {
        try {
            Reader reader = createLongReader("<!--", "-->", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }

    
    public void testLongWhitespaceCoalescing() throws Exception {
        try {
            Reader reader = createLongReader("", "", true);
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            factory.setProperty("com.ctc.wstx.maxTextLength", Integer.valueOf(1000));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.START_ELEMENT) {
            }
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }

    public void testLongAttribute() throws Exception {
        final int max = 500;
        Reader reader = new Reader() {
            StringReader sreader = new StringReader("<ns:element xmlns:ns=\"http://foo.com\" blah=\"");
            int count;
            boolean done;
            public int read(char[] cbuf, int off, int len) throws IOException {
                int i = sreader.read(cbuf, off, len);
                if (i == -1) {
                    if (count < max) {
                        sreader = new StringReader("          ");
                        count++;
                    } else if (!done) {
                        sreader = new StringReader("\"/>");
                        done = true;
                    }
                    i = sreader.read(cbuf, off, len);
                }
                return i;
            }
            public void close() throws IOException {
            }
        };
        try {
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty("com.ctc.wstx.maxAttributeSize", Integer.valueOf(100));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.END_DOCUMENT) {
            }
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            //expected
        }
    }
    

}
