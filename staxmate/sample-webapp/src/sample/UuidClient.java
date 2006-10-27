package sample;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This is the simple client class that can be used for load testing
 * UUID service. Depending on command line arguments, it can use
 * either GET or POST based access; as well as any of 3 generation
 * methods.
 */
public final class UuidClient
    implements Runnable
{
    /**
     * Let's do batches of requests, to minimize sync overhead for
     * updating stats, as well as to maybe stabilize total counts
     * a bit. 10 seems like a reasonable number.
     */
    final static int BUNDLE_SIZE = 10;

    /**
     * Available options; key is option name, value default value
     */
    final static Map<String,String> OPTIONS = new HashMap<String,String>();
    static {
        OPTIONS.put("threadCount", "10");
        OPTIONS.put("httpMethod", "GET");
        OPTIONS.put("uuidMethod", "RANDOM");
        OPTIONS.put("pipeline", "true");
    }

    // // // Stats counts, locking

    public static int sRespCount = 0;
    public static int sThreadCount = 0;

    public final static Object sStatLock = new Object();
    public final static Object sThreadLock = new Object();

    // // // Actual config settings:

    final URL mMethodURL;

    final int mThreadId;

    final boolean mIsPost;

    final boolean mUsePipelining;

    final String mXmlRequest;

    // // // Request state etc

    final byte[] mInputBuffer = new byte[4000];

    private UuidClient(URL methodURL, int threadId, boolean isPost,
                       String uuidMethod,
                       boolean pipeline)
    {
        mMethodURL = methodURL;
        mThreadId = threadId;
        mIsPost = isPost;
        mUsePipelining = pipeline;

        if (mIsPost) {
            // !!! TBI
            mXmlRequest = "";
        } else {
            mXmlRequest = "";
        }
    }

    public void run() 
    {
        int counter = 0;
        try {
            while (true) {
                for (int i = 0; i < BUNDLE_SIZE; ++i) {
                    fetchUsingJdk(counter);
                    ++counter;
                }
                synchronized (sStatLock) {
                    sRespCount += BUNDLE_SIZE;
                }
            }
        } catch (IOException ioe) {
            synchronized (sThreadLock) {
                --sThreadCount;
                System.err.println("Thread dying (now will have "+sThreadCount+" threads running): "+ioe);
            }
            System.err.println("STACK-->");
            ioe.printStackTrace();
            System.err.println("<--STACK:");
        }
    }

    private void fetchUsingJdk(int seqNr)
        throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) mMethodURL.openConnection();
        
        // Want to disable pipelining?
        if (!mUsePipelining) {
            conn.setRequestProperty("Connection", "close");
        }
        
        conn.setDoInput(true);
        conn.setDoOutput(mIsPost);
        conn.setUseCaches(false);
        conn.connect();

        if (mIsPost) {
            // !!! TBI
        }
        
        int respCode = conn.getResponseCode();
        InputStream in = conn.getInputStream();
        
        // should we check respCode?

        try {
            int count = readAll(in, mInputBuffer);
            if (count < 1) {
                System.err.println("Warning: got an empty reply");
            }
            
            if (seqNr == 0) {
                System.out.println("First request("+mThreadId+"), resp code: "+respCode+"; contents("+count+"): ["+new String(mInputBuffer, 0, count)+"]");
            }
        } catch (IOException ioe) {
            System.err.println("Warning: i/o error on read: "+ioe);
        }
        in.close();
        
        // Could probably prevent pipelining by this too?
        //conn.disconnect();
    }

    private int readAll(InputStream in, byte[] buf)
        throws IOException
    {
        int count;
        int max = buf.length;
        int offset = 0;

        while (max > 0 &&
               (count = in.read(buf, offset, max)) > 0) {
            offset += count;
            max -= count;
        }
        return offset;
    }

    public static void controlLoop()
    {
        long expTime = System.currentTimeMillis();
        int lastCount = 0;
        long delay = 0L;

        while (true) {
            long next = expTime + 1000L;
            delay = next - System.currentTimeMillis();
            expTime = next;
            if (delay > 5L) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    break;
                }
            }
            int currReq;
            synchronized (sStatLock) {
                currReq = sRespCount;
            }
            int count = currReq - lastCount;
            lastCount = currReq;
            System.out.println("Got "+count+" requests (sleep "+((int) delay)+" ms)");
        }

        System.out.println("Done! Got "+sRespCount+" responses all in all");
    }

    public static void main(String[] args)
    {
        if (args.length < 1) {
            showUsage();
        }

        // Let's start with defaults:
        HashMap<String,String> opts = new HashMap<String,String>(OPTIONS);
        for (int i = 0; i < args.length-1; ++i) {
            String arg = args[i];
            int ix = arg.indexOf('=');
            if (ix < 0) {
                System.err.println("Invalid option definition '"+arg+"': has to be of form 'optionName=value'");
                showUsage();
            }
            String key = arg.substring(0, ix);
            if (!OPTIONS.containsKey(key)) {
                System.err.println("Unrecognized option '"+key+"'.");
                showUsage();
            }
            String value = arg.substring(ix+1);
            opts.put(key, value);
        }
        String urlStr = args[args.length-1];

        // Parsing is not very robust: should improve it in future
        int tc = Integer.parseInt(opts.get("threadCount"));
        String uuidMethod = opts.get("uuidMethod");
        // should check that it's valid?
        String httpMethod = opts.get("httpMethod");
        boolean isPost = httpMethod.equals("POST");
        if (!isPost) {
            if (!httpMethod.equals("GET")) {
                System.err.println("Unrecognized httpMethod '"+httpMethod+"'; expecting POST or GET");
                System.exit(1);
            }
            // For get, need to add query params...
            if (urlStr.indexOf('?') < 0) {
                urlStr += "?";
            } else {
                urlStr += "&";
            }
            urlStr += "method="+uuidMethod+"&count=1";
        }
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException mex) {
            System.err.println("Malformed URL '"+urlStr+", can't proceed");
            System.exit(1);
        }
        System.out.println("Using URL: "+url.toExternalForm());
        boolean enablePipeline = Boolean.valueOf(opts.get("pipeline"));

        System.out.println("Starting staggered creation of "+tc+" request threads...");
        // First, let's create the "prototype" thread, with config
        {
            Thread t = new Thread() {
                    public void run() {
                        controlLoop();
                    }
                };
            t.start();
        }

        // And then the actual request threads
        for (int i = 0; i < tc; ++i) {
            System.out.println("Creating thread #"+i);
            Thread t = new Thread(new UuidClient(url, i, isPost,
                                                 uuidMethod, enablePipeline));
            synchronized (sThreadLock) {
                ++sThreadCount;
            }
            t.start();
            try { // let's sleep 0.1 secs between rounds...
                Thread.sleep(100L);
            } catch (InterruptedException ie) {
                System.err.println("Error: interrupted, need to bail out!");
                return;
            }
        }

        // All started... can exit the main method
        System.out.println("All threads created succesfully!");
    }

    static void showUsage() 
    {
        System.err.println("Java <optionName=value> ... [URL]");
        System.err.println("Available options: (with default value)");
        for (String key : OPTIONS.keySet()) {
            System.err.print(" "+key+" (");
            System.err.println(OPTIONS.get(key)+")");
        }
        System.exit(1);
    }
}
