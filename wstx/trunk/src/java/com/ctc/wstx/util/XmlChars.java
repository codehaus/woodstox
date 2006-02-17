package com.ctc.wstx.util;

/**
 * Simple utility class that encapsulates logic of determining validity
 * of characters outside basic 7-bit range of Unicode, for XML 1.0
 */
public final class XmlChars
{
    /* We don't need full 64k bits... (0x80 - 0x312C) / 32. But to
     * simplify things, let's just include first 0x80 entries in there etc
     */
    final static int SIZE = (0x3140 >> 5); // 32 bits per int

    final static int[] sXml10StartChars = new int[SIZE];
    static {
        FILL(sXml10StartChars, 0xC0, 0xD6);
        FILL(sXml10StartChars, 0xD8, 0xF6);
        FILL(sXml10StartChars, 0xF8, 0xFF);
        FILL(sXml10StartChars, 0x100, 0x131);
        FILL(sXml10StartChars, 0x134, 0x13e);
        FILL(sXml10StartChars, 0x141, 0x148);
        FILL(sXml10StartChars, 0x14a, 0x17e);
        FILL(sXml10StartChars, 0x180, 0x1c3);
        FILL(sXml10StartChars, 0x1cd, 0x1f0);
        FILL(sXml10StartChars, 0x1f4, 0x1f5);
        FILL(sXml10StartChars, 0x1fa, 0x217);
        FILL(sXml10StartChars, 0x250, 0x2a8);
        FILL(sXml10StartChars, 0x2bb, 0x2c1);
        FILL(sXml10StartChars, 0x386);
        FILL(sXml10StartChars, 0x388, 0x38a);
        FILL(sXml10StartChars, 0x38c);
        FILL(sXml10StartChars, 0x38e, 0x3a1);
        FILL(sXml10StartChars, 0x3a3, 0x3c3);
        FILL(sXml10StartChars, 0x3d0, 0x3d6);
        FILL(sXml10StartChars, 0x3da);
        FILL(sXml10StartChars, 0x3dc);
        FILL(sXml10StartChars, 0x3de);
        FILL(sXml10StartChars, 0x3e0);
        FILL(sXml10StartChars, 0x3e2, 0x3f3);
        FILL(sXml10StartChars, 0x401, 0x40c);
        FILL(sXml10StartChars, 0x40e, 0x44f);
        FILL(sXml10StartChars, 0x451, 0x45c);
        FILL(sXml10StartChars, 0x45e, 0x481);
        FILL(sXml10StartChars, 0x490, 0x4c4);
        FILL(sXml10StartChars, 0x4c7, 0x4c8);
        FILL(sXml10StartChars, 0x4cb, 0x4cc);
        FILL(sXml10StartChars, 0x4d0, 0x4eb);
        FILL(sXml10StartChars, 0x4ee, 0x4f5);
        FILL(sXml10StartChars, 0x4f8, 0x4f9);

        FILL(sXml10StartChars, 0x531, 0x556);
        FILL(sXml10StartChars, 0x559);
        FILL(sXml10StartChars, 0x561, 0x586);
        FILL(sXml10StartChars, 0x5d0, 0x5ea);
        FILL(sXml10StartChars, 0x5f0, 0x5f2);
        FILL(sXml10StartChars, 0x621, 0x63a);
        FILL(sXml10StartChars, 0x641, 0x64a);
        FILL(sXml10StartChars, 0x671, 0x6b7);
        FILL(sXml10StartChars, 0x6ba, 0x6be);
        FILL(sXml10StartChars, 0x6c0, 0x6ce);
        FILL(sXml10StartChars, 0x6d0, 0x6d3);
        FILL(sXml10StartChars, 0x6d5);

        FILL(sXml10StartChars, 0x6e5, 0x6e6);
        FILL(sXml10StartChars, 0x905, 0x939);
        FILL(sXml10StartChars, 0x93d);
        FILL(sXml10StartChars, 0x958, 0x961);
        FILL(sXml10StartChars, 0x985, 0x98c);
        FILL(sXml10StartChars, 0x98f, 0x990);
        FILL(sXml10StartChars, 0x993, 0x9a8);
        FILL(sXml10StartChars, 0x9aa, 0x9b0);
        FILL(sXml10StartChars, 0x9b2);
        FILL(sXml10StartChars, 0x9b6, 0x9b9);
        FILL(sXml10StartChars, 0x9dc);
        FILL(sXml10StartChars, 0x9dd);
        FILL(sXml10StartChars, 0x9df, 0x9e1);
        FILL(sXml10StartChars, 0x9f0); FILL(sXml10StartChars, 0x9f1);
        FILL(sXml10StartChars, 0xA05, 0xA0A);
        FILL(sXml10StartChars, 0xA0F); FILL(sXml10StartChars, 0xA10);
        FILL(sXml10StartChars, 0xA13, 0xA28);
        FILL(sXml10StartChars, 0xA2A, 0xA30);
        FILL(sXml10StartChars, 0xA32); FILL(sXml10StartChars, 0xA33);
        FILL(sXml10StartChars, 0xA35); FILL(sXml10StartChars, 0xA36);
        FILL(sXml10StartChars, 0xA38); FILL(sXml10StartChars, 0xA39);
        FILL(sXml10StartChars, 0xA59, 0xA5C);
        FILL(sXml10StartChars, 0xA5E);
        FILL(sXml10StartChars, 0xA72, 0xA74);
        FILL(sXml10StartChars, 0xA85, 0xA8B);
        FILL(sXml10StartChars, 0xA8D);
        FILL(sXml10StartChars, 0xA8F, 0xA91);
        FILL(sXml10StartChars, 0xA93, 0xAA8);
        FILL(sXml10StartChars, 0xAAA, 0xAB0);
        FILL(sXml10StartChars, 0xAB2, 0xAB3);
        FILL(sXml10StartChars, 0xAB5, 0xAB9);
        FILL(sXml10StartChars, 0xABD);
        FILL(sXml10StartChars, 0xAE0);
        FILL(sXml10StartChars, 0xB05, 0xB0C);
        FILL(sXml10StartChars, 0xB0F); FILL(sXml10StartChars, 0xB10);
        FILL(sXml10StartChars, 0xB13, 0xB28);

        FILL(sXml10StartChars, 0xB2A, 0xB30);
        FILL(sXml10StartChars, 0xB32); FILL(sXml10StartChars, 0xB33);
        FILL(sXml10StartChars, 0xB36, 0xB39);
        FILL(sXml10StartChars, 0xB3D);
        FILL(sXml10StartChars, 0xB5C); FILL(sXml10StartChars, 0xB5D);
        FILL(sXml10StartChars, 0xB5F, 0xB61);
        FILL(sXml10StartChars, 0xB85, 0xB8A);
        FILL(sXml10StartChars, 0xB8E, 0xB90);

        FILL(sXml10StartChars, 0xB92, 0xB95);
        FILL(sXml10StartChars, 0xB99, 0xB9A);
        FILL(sXml10StartChars, 0xB9C);
        FILL(sXml10StartChars, 0xB9E); FILL(sXml10StartChars, 0xB9F);
        FILL(sXml10StartChars, 0xBA3); FILL(sXml10StartChars, 0xBA4);
        FILL(sXml10StartChars, 0xBA8, 0xBAA);
        FILL(sXml10StartChars, 0xBAE, 0xBB5);
        FILL(sXml10StartChars, 0xBB7, 0xBB9);
        FILL(sXml10StartChars, 0xC05, 0xC0C);
        FILL(sXml10StartChars, 0xC0E, 0xC10);

        //FILL(sXml10StartChars, 0x, 0x);
    }

    /*

 | [#x0C12-#x0C28] | [#x0C2A-#x0C33] | [#x0C35-#x0C39] | [#x0C60-#x0C61] | 
[#x0C85-#x0C8C] | [#x0C8E-#x0C90] | [#x0C92-#x0CA8] | [#x0CAA-#x0CB3] | 
[#x0CB5-#x0CB9] | #x0CDE | [#x0CE0-#x0CE1] | [#x0D05-#x0D0C] | [#x0D0E-#x0D10]
 | [#x0D12-#x0D28] |

 [#x0D2A-#x0D39] | [#x0D60-#x0D61] | [#x0E01-#x0E2E] | #x0E30 | 
[#x0E32-#x0E33] | [#x0E40-#x0E45] | [#x0E81-#x0E82] | #x0E84 | 
[#x0E87-#x0E88] | #x0E8A | #x0E8D | [#x0E94-#x0E97] | [#x0E99-#x0E9F]
 | [#x0EA1-#x0EA3] | #x0EA5 | #x0EA7 | [#x0EAA-#x0EAB] | [#x0EAD-#x0EAE]
 | #x0EB0 | [#x0EB2-#x0EB3] | #x0EBD 

| [#x0EC0-#x0EC4] | [#x0F40-#x0F47] | [#x0F49-#x0F69] | [#x10A0-#x10C5] 
| [#x10D0-#x10F6] | #x1100 | [#x1102-#x1103] | [#x1105-#x1107] | #x1109 |
 [#x110B-#x110C] | [#x110E-#x1112] | #x113C | #x113E | #x1140 | #x114C |
 #x114E | #x1150 | [#x1154-#x1155] | #x1159 | [#x115F-#x1161] | #x1163 |
 #x1165 | #x1167 | #x1169 |

 [#x116D-#x116E] | [#x1172-#x1173] | #x1175 | #x119E | #x11A8 | #x11AB | 
[#x11AE-#x11AF] | [#x11B7-#x11B8] | #x11BA | [#x11BC-#x11C2] | #x11EB | 
#x11F0 | #x11F9 | [#x1E00-#x1E9B] | [#x1EA0-#x1EF9] | [#x1F00-#x1F15] | 
[#x1F18-#x1F1D]
 | [#x1F20-#x1F45] | [#x1F48-#x1F4D] | [#x1F50-#x1F57] | #x1F59 | #x1F5B 
| #x1F5D | [#x1F5F-#x1F7D] | [#x1F80-#x1FB4] | [#x1FB6-#x1FBC] | #x1FBE 
| [#x1FC2-#x1FC4] | [#x1FC6-#x1FCC] | [#x1FD0-#x1FD3] | [#x1FD6-#x1FDB]
 | [#x1FE0-#x1FEC]
 | [#x1FF2-#x1FF4] | [#x1FF6-#x1FFC] | #x2126 | [#x212A-#x212B] | #x212E 
| [#x2180-#x2182] | [#x3041-#x3094] | [#x30A1-#x30FA] | [#x3105-#x312C] 
| [#xAC00-#xD7A3]

[86]   	Ideographic	   ::=   	[#x4E00-#x9FA5] | #x3007 | [#x3021-#x3029]
*/

    final static int[] sXml10Chars = new int[SIZE];
    static {
        // Let's start with all valid start chars:
        System.arraycopy(sXml10StartChars, 0, sXml10Chars, 0, SIZE);

        FILL(sXml10StartChars, 0x300, 0x345);
        FILL(sXml10StartChars, 0x360, 0x361);
        FILL(sXml10StartChars, 0x483, 0x486);
        FILL(sXml10StartChars, 0x591, 0x5a1);
        FILL(sXml10StartChars, 0x5a3, 0x5b9);
        FILL(sXml10StartChars, 0x5bb, 0x5bd);
        FILL(sXml10StartChars, 0x5bf);

        FILL(sXml10StartChars, 0x5c1, 0x5c2);
        FILL(sXml10StartChars, 0x5c4);
        FILL(sXml10StartChars, 0x64b, 0x652);
        FILL(sXml10StartChars, 0x670);
        FILL(sXml10StartChars, 0x6d6, 0x6dc);
        FILL(sXml10StartChars, 0x6dd, 0x6df);
        FILL(sXml10StartChars, 0x6e0, 0x6e4);
        FILL(sXml10StartChars, 0x6e7, 0x6e8);
        FILL(sXml10StartChars, 0x6ea, 0x6ed);

        //FILL(sXml10StartChars, 0x0, 0x0);

       /*
[87]   	CombiningChar	   ::=

[#x0901-#x0903] | #x093C | [#x093E-#x094C] | #x094D | [#x0951-#x0954] | 
[#x0962-#x0963] | [#x0981-#x0983] | #x09BC | #x09BE | #x09BF | [#x09C0-#x09C4] | [#x09C7-#x09C8] | [#x09CB-#x09CD] | #x09D7 | [#x09E2-#x09E3] | #x0A02 |
 #x0A3C | #x0A3E | #x0A3F | [#x0A40-#x0A42] | [#x0A47-#x0A48] | [#x0A4B-#x0A4D] | [#x0A70-#x0A71] | [#x0A81-#x0A83] | #x0ABC | [#x0ABE-#x0AC5] | [#x0AC7-#x0AC9] | [#x0ACB-#x0ACD] | [#x0B01-#x0B03] | #x0B3C | [#x0B3E-#x0B43] | [#x0B47-#x0B48] | [#x0B4B-#x0B4D] | [#x0B56-#x0B57] | [#x0B82-#x0B83] | [#x0BBE-#x0BC2]
 | [#x0BC6-#x0BC8] | [#x0BCA-#x0BCD] | #x0BD7 | [#x0C01-#x0C03] | [#x0C3E-#x0C44] | [#x0C46-#x0C48] | [#x0C4A-#x0C4D] | [#x0C55-#x0C56] | [#x0C82-#x0C83] | [#x0CBE-#x0CC4] | [#x0CC6-#x0CC8] | [#x0CCA-#x0CCD] | [#x0CD5-#x0CD6] |
 [#x0D02-#x0D03] | [#x0D3E-#x0D43] | [#x0D46-#x0D48] | [#x0D4A-#x0D4D] | #x0D57 | #x0E31 | [#x0E34-#x0E3A] | [#x0E47-#x0E4E] | #x0EB1 | [#x0EB4-#x0EB9] |
 [#x0EBB-#x0EBC] | [#x0EC8-#x0ECD] | [#x0F18-#x0F19] | #x0F35 | #x0F37 | #x0F39 | #x0F3E | #x0F3F | [#x0F71-#x0F84] | [#x0F86-#x0F8B] | [#x0F90-#x0F95]
 | #x0F97 | [#x0F99-#x0FAD] | [#x0FB1-#x0FB7] | #x0FB9 | [#x20D0-#x20DC] | #x20E1 | [#x302A-#x302F] | #x3099 | #x309A
       */
            // Digits:
        FILL(sXml10StartChars, 0x660, 0x669);
        FILL(sXml10StartChars, 0x6f0, 0x6f9);
        FILL(sXml10StartChars, 0x966, 0x96f);
        FILL(sXml10StartChars, 0x9e6, 0x9ef);
        FILL(sXml10StartChars, 0xa66, 0xa6f);
        FILL(sXml10StartChars, 0xae6, 0xaef);
        FILL(sXml10StartChars, 0xb66, 0xb6f);
        FILL(sXml10StartChars, 0xbe7, 0xbef);
        FILL(sXml10StartChars, 0xc66, 0xc6f);
        FILL(sXml10StartChars, 0xce6, 0xcef);
        FILL(sXml10StartChars, 0xd66, 0xd6f);
        FILL(sXml10StartChars, 0xe50, 0xe59);
        FILL(sXml10StartChars, 0xed0, 0xed9);
        FILL(sXml10StartChars, 0xf20, 0xf29);
        
        // Extenders:
        FILL(sXml10StartChars, 0xb7);
        FILL(sXml10StartChars, 0x2d0);
        FILL(sXml10StartChars, 0x2d1);
        FILL(sXml10StartChars, 0x387);
        FILL(sXml10StartChars, 0x640);
        FILL(sXml10StartChars, 0xE46);
        FILL(sXml10StartChars, 0xEC6);
        FILL(sXml10StartChars, 0x3005);
        FILL(sXml10StartChars, 0x3031, 0x3035);
        FILL(sXml10StartChars, 0x309d, 0x309e);
        FILL(sXml10StartChars, 0x30fc, 0x30fe);
    }


    
    private XmlChars() { }

    public final static boolean is10NameStartChar(char c)
    {
        // First, let's deal with outliers
        if (c > 0x312C) { // Most valid chars are below this..
            if (c < 0xAC00) {
                return (c >= 0x4E00 && c <= 0x9FA5); // valid ideograms
            }
            if (c <= 0xD7A3) { // 0xAC00 - 0xD7A3, valid base chars
                return true;
            }
            // As to surrogate pairs... let's do the bare minimum;
            // 0xD800 - 0xDFFF (high, low surrogate) are ok
            return (c >= 0xD800 && c <= 0xDFFF);
        }
        // but then we'll just need to use the table...
        int ix = (int) c;
        return (sXml10StartChars[ix >> 5] & (1 << (ix & 31))) != 0;
    }

    public final static boolean is10NameChar(char c)
    {
        // First, let's deal with outliers
        if (c > 0x312C) { // Most valid chars are below this..
            if (c < 0xAC00) {
                return (c >= 0x4E00 && c <= 0x9FA5); // valid ideograms
            }
            if (c <= 0xD7A3) { // 0xAC00 - 0xD7A3, valid base chars
                return true;
            }
            // As to surrogate pairs... let's do the bare minimum;
            // 0xD800 - 0xDFFF (high, low surrogate) are ok
            return (c >= 0xD800 && c <= 0xDFFF);
        }
        // but then we'll just need to use the table...
        int ix = (int) c;
        return (sXml10Chars[ix >> 5] & (1 << (ix & 31))) != 0;
    }

    public final static boolean is11NameStartChar(char c)
    {
        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x300) {
                if (c < 0x00C0) { // 8-bit ctrl chars
                    return false;
                }
                // most of the rest are fine...
                return (c != 0xD7 && c != 0xF7);
            }
            if (c >= 0x2C00) {
                // 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x370 || c > 0x218F) {
                // 0x300 - 0x36F, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c < 0x2000) {
                // 0x370 - 0x37D, 0x37F - 0x1FFF are ok
                return (c != 0x37E);
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok
                return (c <= 0x218F);
            }
            // And finally, 0x200C - 0x200D
            return (c == 0x200C || c == 0x200D);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow high surrogates here, without checking
             * that they are properly followed... crude basic support,
             * I know, but allow valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDBFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDBFF (high surrogate) are ok:
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes low surrogate (since one can not
                 * START an identifier), and byte-order markers..
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }

    public final static boolean is11NameChar(char c)
    {
        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x2000) { // only 8-bit ctrl chars and 0x37E to filter out
                return (c >= 0x00C0 && c != 0x37E);
            }
            if (c >= 0x2C00) {
                // 0x100 - 0x1FFF, 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x200C || c > 0x218F) {
                // 0x2000 - 0x200B, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok
                return true;
            }
            // And finally, 0x200C - 0x200D, 0x203F - 0x2040 are ok
            return (c == 0x200C || c == 0x200D
                || c == 0x203F || c == 0x2040);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow surrogate heres, without checking that
             * they have proper ordering. For non-first name chars, both are
             * ok, for valid names. Crude basic support,
             * I know, but allows valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDFFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDFFF (high, low surrogate) are ok:
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes other invalid chars (below valid
                 * range), and byte-order markers (0xFFFE, 0xFFFF).
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }

    private static void FILL(int[] array, int start, int end)
    {
        for (; start < end; ++start) {
            FILL(array, start);
        }
        // !!! TBI: optimize
    }

    private static void FILL(int[] array, int point) {
        int ix = (point >> 5);
        int bit = (point & 31);

        array[ix] |= (1 << bit);
    }
}
