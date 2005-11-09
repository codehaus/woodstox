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
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.io.WstxInputSource;

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
    public abstract DTDSubset readInternalSubset(WstxInputData srcData, WstxInputSource input,
                                                 ReaderConfig cfg)
        throws IOException, XMLStreamException;
    
    /**
     * Method called to read in the external subset definition.
     */
    public abstract DTDSubset readExternalSubset
        (WstxInputSource src, ReaderConfig cfg, DTDSubset intSubset)
        throws IOException, XMLStreamException;
}

