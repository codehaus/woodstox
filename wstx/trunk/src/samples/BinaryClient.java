package samples;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Simple example of a client that downloads a set of files from
 * a server.
 *<p>
 * Here is a sample document that shows expected structure:
 * <pre>
 *  &lt;files>
 *    &lt;file name="test.jpg" checksumType="SHA">... base64 content ...
 *    &lt;/file>
 *    &lt;checksum value="...base64 encoded hash..." />
 *    &lt;!-- ... and more files, if need be... -->
 *  &lt;/files> 
 *</pre>
 */
public class BinaryClient
{
    final XMLInputFactory _xmlInputFactory;

    public BinaryClient()
    {
        _xmlInputFactory = XMLInputFactory.newInstance();
    }

    /**
     * @param urlStr Full URL (including query parameters if any)
     *   used to access web service for downloading files.
     * @param toDir Directory under which to save downloaded files
     */
    public List<File> fetchFiles(URL serviceURL, File toDir) throws Exception
    {
        List<File> files = new ArrayList<File>();
        URLConnection conn = serviceURL.openConnection();
        conn.setDoOutput(false); // only true when POSTing
        conn.connect();
        // note, should check 'if (conn.getResponseCode() != 200) ...'
        
        // Ok, let's read it then... (note: StaxMate could simplify a lot!)
        InputStream in = conn.getInputStream();
        XMLStreamReader2 sr = (XMLStreamReader2) _xmlInputFactory.createXMLStreamReader(in);
        sr.nextTag(); // to "files"
        byte[] buffer = new byte[4000];
        
        while (sr.nextTag() != XMLStreamConstants.END_ELEMENT) { // one more 'file'
            String filename = sr.getAttributeValue("", "name");
            String csumType = sr.getAttributeValue("", "checksumType");
            File outputFile = new File(toDir, filename);
            FileOutputStream out = new FileOutputStream(outputFile);
            files.add(outputFile);
            MessageDigest md = MessageDigest.getInstance(csumType);
            
            int count;
            // Read binary contents of the file, calc checksum and write
            while ((count = sr.readElementAsBinary(buffer, 0, buffer.length)) != -1) {
                md.update(buffer, 0, count);
                out.write(buffer, 0, count);
            }
            out.close();
            // Then verify checksum
            sr.nextTag();  
            byte[] expectedCsum = sr.getAttributeAsBinary(sr.getAttributeIndex("", "value"));
            byte[] actualCsum = md.digest();
            if (!Arrays.equals(expectedCsum, actualCsum)) {
                throw new IllegalArgumentException("File '"+filename+"' corrupt: content checksum does not match expected");
            }
            sr.nextTag(); // to match closing "checksum"
        }
        return files;
    }

    public static void main(String[] args) throws Exception
    {
        // arg like "localhost:8080/testServlet"
        if (args.length != 1) {
            System.err.println("Usage: java BinaryClient [URL]");
            System.exit(1);
        }
        URL serviceURL = new URL(args[0]);
        // Will just save in current dir
        File dir = new File("").getAbsoluteFile();
        System.out.println("Fetching files from '"+serviceURL.toExternalForm()+"': saving in directory '"+dir+"'");
        List<File> files = new BinaryClient().fetchFiles(serviceURL, dir);
        System.out.println("OK: Fetched "+files.size()+" files with correct checksums:");
        for (File f : files) {
            System.out.println(" File '"+f.getAbsolutePath()+"'");
        }
        System.out.println("Done.");
    }
}
