/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.common.helpers;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;

/**
 * XML parsing hardened against XXE and entity expansion attacks.
 * <p>
 * JAXB's {@code Unmarshaller.unmarshal(InputStream|Reader|File|URL|InputSource)} creates its own
 * SAX parser which resolves external entities and external DTDs: a route file with a DOCTYPE can
 * read local files into the converted output or make the server open network connections.
 * Route formats (GPX, KML, RTZ, TCX, ...) never need a DOCTYPE, so it is rejected outright.
 *
 * @author Christian Pesch
 */

public class SecureXml {
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private SecureXml() {
    }

    public static SAXParserFactory newSaxParserFactory() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // not silently skipped if unsupported: a parser that cannot be hardened must not parse
        factory.setFeature(DISALLOW_DOCTYPE, true);
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(LOAD_EXTERNAL_DTD, false);
        return factory;
    }

    public static SAXParser newSaxParser() throws ParserConfigurationException, SAXException {
        SAXParser parser = newSaxParserFactory().newSAXParser();
        try {
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            // property only known to JAXP 1.5+ parsers; the features above already apply
        }
        return parser;
    }

    public static XMLReader newXmlReader() throws ParserConfigurationException, SAXException {
        return newSaxParser().getXMLReader();
    }

    public static XMLInputFactory harden(XMLInputFactory factory) {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    private static Source secure(Source source) throws JAXBException {
        try {
            if (source instanceof StreamSource streamSource) {
                InputSource inputSource = new InputSource();
                inputSource.setByteStream(streamSource.getInputStream());
                inputSource.setCharacterStream(streamSource.getReader());
                inputSource.setSystemId(streamSource.getSystemId());
                return new SAXSource(newXmlReader(), inputSource);
            }
            if (source instanceof SAXSource saxSource && saxSource.getXMLReader() == null)
                return new SAXSource(newXmlReader(), saxSource.getInputSource());
            return source;
        } catch (ParserConfigurationException | SAXException e) {
            throw new JAXBException("Cannot create secure XML reader: " + e, e);
        }
    }

    private static Object secure(Object argument) throws JAXBException {
        try {
            if (argument instanceof InputStream || argument instanceof Reader)
                return new SAXSource(newXmlReader(), argument instanceof InputStream in ?
                        new InputSource(in) : new InputSource((Reader) argument));
            if (argument instanceof File file)
                return new SAXSource(newXmlReader(), new InputSource(file.toURI().toASCIIString()));
            if (argument instanceof URL url)
                return new SAXSource(newXmlReader(), new InputSource(url.toExternalForm()));
            if (argument instanceof InputSource inputSource)
                return new SAXSource(newXmlReader(), inputSource);
            if (argument instanceof Source source)
                return secure(source);
            return argument;
        } catch (ParserConfigurationException | SAXException e) {
            throw new JAXBException("Cannot create secure XML reader: " + e, e);
        }
    }

    /**
     * Wraps the unmarshaller so every {@code unmarshal(..)} overload that would make JAXB create its
     * own (unhardened) parser is routed through a hardened {@link XMLReader} instead.
     */
    public static Unmarshaller harden(Unmarshaller unmarshaller) {
        return (Unmarshaller) Proxy.newProxyInstance(Unmarshaller.class.getClassLoader(),
                new Class<?>[]{Unmarshaller.class}, (proxy, method, args) -> {
                    try {
                        if ("unmarshal".equals(method.getName()) && args != null && args.length >= 1) {
                            Object secured = secure(args[0]);
                            if (secured != args[0]) {
                                Object[] securedArgs = args.clone();
                                securedArgs[0] = secured;
                                // unmarshal(Source[, Class]) is the overload that takes the SAXSource
                                Class<?>[] types = method.getParameterTypes().clone();
                                types[0] = Source.class;
                                return Unmarshaller.class.getMethod("unmarshal", types).invoke(unmarshaller, securedArgs);
                            }
                        }
                        return method.invoke(unmarshaller, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
