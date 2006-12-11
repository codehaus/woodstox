package staxperf.write;

import staxperf.TestUtil;

/**
 * This class tests relative speeds of different strategies for
 * copying Strings into using output writers.
 */
public final class TestStringWrite
    extends TestUtil
{
    final static int MAX_LEN = 256;

    final static int TOTAL_LEN = 4 * 1000 * 1000;

    final static char[] TMP_BUFFER = new char[MAX_LEN];

    final static char[] OUT_BUFFER = new char[MAX_LEN * 4];

    final static String INPUT_STR_BASE_1;
    final static String INPUT_STR_BASE_2;
    static {
        final String STR = "abcde<defgjsfagoe&kclen lkwegweg\rj fweo4 645 ,nyerlj y54y45y54yln l45y4l5yjpj45y pj45y";
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        for (int i = 0; i < MAX_LEN; ++i) {
            sb1.append(((i & 1) == 0) ? 'a' : ' ');
            sb2.append(STR.charAt(i % STR.length()));
        }
        INPUT_STR_BASE_1 = sb1.toString();
        INPUT_STR_BASE_2 = sb2.toString();
    }

    final static int[] SIZES = new int[] {
        //3, 4, 7, 12, 16, 20, 27, 32, 48, 64, 128, 256
        3, 4, 7, 12, 16, 20, 27, 32, 48, 64, 128, 256
    };

    final static int[] ESCAPES = new int[256];
    static {
        for (int i = 0; i < 32; ++i) {
            ESCAPES[i] = 1;
        }
    }

    private TestStringWrite()
    {
    }

    public void test(boolean firstTime)
        throws Exception
    {
        // Minor warmup:
        if (firstTime) {
            System.err.print("Warm up with 10 rounds...");
            test1("a<>f", 10);
            test2("a<>f", 10);
            clear(30);
            System.err.print(" 50 rounds...");
            test1("abcdef", 50);
            test2("abcdef", 50);
            clear(30);
            System.err.println(" and 100 rounds.");
            test1("abc", 100);
            test2("abc", 100);
        }
        System.err.println("Starting real testing!");

        for (int i = 0, len = SIZES.length; i < len; ++i) {
            int size = SIZES[i];
            int counts = TOTAL_LEN / size;
            //String word = INPUT_STR_BASE_1.substring(0, size);
            String word = INPUT_STR_BASE_2.substring(0, size);

            clear(20);

            long now = System.currentTimeMillis();
            test1(word, counts);
            long time1 = System.currentTimeMillis() - now;

            clear(20);

            now = System.currentTimeMillis();
            test2(word, counts);
            long time2 = System.currentTimeMillis() - now;

            clear(20);

            now = System.currentTimeMillis();
            test3(word, counts);
            long time3 = System.currentTimeMillis() - now;

            System.out.println("Size "+size+", method-charAt : "+time1+", method-getChars: "+time2+", methodX: "+time3);

        }
    }

    private static void clear(int sleep)
    {
        try { Thread.sleep(sleep); } catch (Exception e) { }
        System.gc();
        try { Thread.sleep(sleep); } catch (Exception e) { }
    }

    private void test1(String word, int count)
    {
        mOutputBufLen = OUT_BUFFER.length;
        for (int i = 0; i < count; ++i) {
            mOutputPtr = 0;
            test1b(word);
        }
    }

    protected final static int HIGHEST_ENCODABLE_TEXT_CHAR = (int)'>';

    int mOutputPtr;
    int mOutputBufLen;
    final char[] mOutputBuffer = OUT_BUFFER;
    final int mEncHighChar = 256;

    private void test1b(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        final int highChar = mEncHighChar;

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = text.charAt(inPtr++);
                if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
                    if (c <= 0x0020) {
                        if (c == ' ' || c == '\n' || c == '\t') { // fine as is
                            ;
                        } else {
                            if (c != '\r') {
                                throw new Error();
                            }
                            break inner_loop; // need quoting ok
                        }
                    } else if (c == '<') {
                        ent = "&lt;";
                        break inner_loop;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break inner_loop;
                    } else if (c == '>') {
                        // Let's be conservative; and if there's any
                        // change it might be part of "]]>" quote it
                        if (inPtr < 2 || text.charAt(inPtr-2) == ']') {
                            ent = "&gt;";
                            break inner_loop;
                        }
                    }
                } else if (c >= highChar) {
                    break inner_loop;
                }
                /*
                if (mOutputPtr >= mOutputBufLen) {
                    flushBuffer();
                }
                */
                mOutputBuffer[mOutputPtr++] = c;
            }
                if (mOutputPtr >= mOutputBufLen) {
                    flushBuffer();
                }
            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void test2(String word, int count)
    {
        mOutputBufLen = OUT_BUFFER.length;
        for (int i = 0; i < count; ++i) {
            mOutputPtr = 0;
            //test2b(word);
            test2b_table(word);
        }
    }

    private void test2b(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        final int highChar = mEncHighChar;
        final char[] buffer = TMP_BUFFER;

        text.getChars(0, len, buffer, 0);

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = buffer[inPtr++];
                if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
                    if (c <= 0x0020) {
                        if (c == ' ' || c == '\n' || c == '\t') { // fine as is
                            ;
                        } else {
                            if (c != '\r') {
                                throw new Error();
                            }
                            break inner_loop; // need quoting ok
                        }
                    } else if (c == '<') {
                        ent = "&lt;";
                        break inner_loop;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break inner_loop;
                    } else if (c == '>') {
                        // Let's be conservative; and if there's any
                        // change it might be part of "]]>" quote it
                        if (inPtr < 2 || buffer[inPtr-2] == ']') {
                            ent = "&gt;";
                            break inner_loop;
                        }
                    }
                } else if (c >= highChar) {
                    break inner_loop;
                }
                /*
                if (mOutputPtr >= mOutputBufLen) {
                    flushBuffer();
                }
                */
                mOutputBuffer[mOutputPtr++] = c;
            }
            if (mOutputPtr >= mOutputBufLen) {
                flushBuffer();
            }
            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void test2b_table(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        final int highChar = mEncHighChar;
        final char[] buffer = TMP_BUFFER;
        //final int[] escapes = ESCAPES;

        text.getChars(0, len, buffer, 0);

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = buffer[inPtr++];
                //if (c > highChar || escapes[c] != 0) {
                if (c > highChar || ESCAPES[c] != 0) {
                    break inner_loop;
                }
                /*
                if (mOutputPtr >= mOutputBufLen) {
                    flushBuffer();
                }
                */
                mOutputBuffer[mOutputPtr++] = c;
            }
            if (mOutputPtr >= mOutputBufLen) {
                flushBuffer();
            }
            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void test3(String word, int count)
    {
        mOutputBufLen = OUT_BUFFER.length;
        for (int i = 0; i < count; ++i) {
            mOutputPtr = 0;
            //test3_getChars(word);
            //test3_charAt(word);
            test3_table(word);
        }
    }

    private void test3_getChars(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        final int highChar = mEncHighChar;
        final char[] buffer = TMP_BUFFER;

        text.getChars(0, len, buffer, 0);

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = buffer[inPtr++];
                if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
                    if (c <= 0x0020) {
                        if (c == ' ' || c == '\n' || c == '\t') { // fine as is
                            ;
                        } else {
                            if (c != '\r') {
                                throw new Error();
                            }
                            break inner_loop; // need quoting ok
                        }
                    } else if (c == '<') {
                        ent = "&lt;";
                        break inner_loop;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break inner_loop;
                    } else if (c == '>') {
                        // Let's be conservative; and if there's any
                        // change it might be part of "]]>" quote it
                        if (inPtr < 2 || buffer[inPtr-2] == ']') {
                            ent = "&gt;";
                            break inner_loop;
                        }
                    }
                } else if (c >= highChar) {
                    break inner_loop;
                }
                /*
                if (mOutputPtr >= mOutputBufLen) {
                    flushBuffer();
                }
                */
                mOutputBuffer[mOutputPtr++] = c;
            }
            if (mOutputPtr >= mOutputBufLen) {
                flushBuffer();
            }
            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void test3_charAt(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        final int highChar = mEncHighChar;

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = text.charAt(inPtr++);
                if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
                    if (c <= 0x0020) {
                        if (c == ' ' || c == '\n' || c == '\t') { // fine as is
                            ;
                        } else {
                            if (c != '\r') {
                                throw new Error();
                            }
                            break inner_loop; // need quoting ok
                        }
                    } else if (c == '<') {
                        ent = "&lt;";
                        break inner_loop;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break inner_loop;
                    } else if (c == '>') {
                        // Let's be conservative; and if there's any
                        // change it might be part of "]]>" quote it
                        if (inPtr < 2 || text.charAt(inPtr-2) == ']') {
                            ent = "&gt;";
                            break inner_loop;
                        }
                    }
                } else if (c >= highChar) {
                    break inner_loop;
                }
                mOutputBuffer[mOutputPtr++] = c;
            }
            if (mOutputPtr >= mOutputBufLen) {
                mOutputPtr = 0;
                flushBuffer();
            }

            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void test3_table(String text)
    {
        int inPtr = 0;
        final int len = text.length();
        //final int highChar = mEncHighChar;
        final int[] escapes = ESCAPES;

        main_loop:
        while (true) {
            String ent = null;

            inner_loop:
            while (true) {
                if (inPtr >= len) {
                    break main_loop;
                }
                char c = text.charAt(inPtr++);
                if (c > 255 || escapes[c] != 0) {
                    break inner_loop;
                }
                mOutputBuffer[mOutputPtr++] = c;
            }
            if (mOutputPtr >= mOutputBufLen) {
                mOutputPtr = 0;
                flushBuffer();
            }

            if (ent != null) {
                ++mOutputPtr;
            } else {
                ;
            }
        }
    }

    private void flushBuffer() // should never occur!
    {
        throw new Error("Overrun!");
    }

    public static void main(String[] args) throws Exception
    {
        TestStringWrite test = new TestStringWrite();
        int round = 0;

        while (true) {
            test.test((round == 0));
            System.err.println("Done! Will wait a bit...");
            clear(100);
            ++round;
        }
    }
}
