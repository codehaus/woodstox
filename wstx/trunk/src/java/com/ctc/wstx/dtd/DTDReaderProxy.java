/* Woodstox XML processor
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

package com.ctc.wstx.dtd;

import java.io.Writer;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.sr.StreamScanner;

/**
 * Interface that defines functionality for Objects that act as DTD reader
 * proxies. Proxy approach is just used to allow code insulation; it would
 * have been possible to use factory/instance approach too, but this way
 * we end up with few more classes.
 */
public abstract class DTDReaderProxy
{
    /**
     * Method called to read in the internal subset definition.
     */
    public abstract DTDSubset readInternalSubset(StreamScanner master, WstxInputSource input,
                                                 ReaderConfig cfg)
        throws IOException, XMLStreamException;
    
    /**
     * Method called to read in the external subset definition.
     */
    public abstract DTDSubset readExternalSubset
        (StreamScanner master, WstxInputSource src, ReaderConfig cfg,
         DTDSubset intSubset)
        throws IOException, XMLStreamException;
    
    /**
     * Method similar to {@link #readInternalSubset}, in that it skims
     * through structure of internal subset, but without doing any sort
     * of validation, or parsing of contents. Method may still throw an
     * exception, if skipping causes EOF or there's an I/O problem.
     */
    public abstract void skipInternalSubset(StreamScanner master, WstxInputSource input,
                                            ReaderConfig cfg)
        throws IOException, XMLStreamException;
}

