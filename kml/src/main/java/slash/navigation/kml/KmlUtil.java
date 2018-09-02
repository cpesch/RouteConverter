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

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.kml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import static slash.common.helpers.JAXBHelper.newContext;
import static slash.common.helpers.JAXBHelper.newMarshaller;
import static slash.common.helpers.JAXBHelper.newUnmarshaller;

public class KmlUtil {
    public static final String KML_20_NAMESPACE_URI = "http://earth.google.com/kml/2.0";
    public static final String KML_21_NAMESPACE_URI = "http://earth.google.com/kml/2.1";
    public static final String KML_22_BETA_NAMESPACE_URI = "http://earth.google.com/kml/2.2";
    public static final String KML_22_NAMESPACE_URI = "http://www.opengis.net/kml/2.2";
    public static final String ATOM_2005_NAMESPACE_URI = "http://www.w3.org/2005/Atom";
    public static final String XAL_20_NAMESPACE_URI = "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0";
    public static final String KML_22_EXT_NAMESPACE_URI = "http://www.google.com/kml/ext/2.2";

    public static Unmarshaller newUnmarshaller20() {
        return newUnmarshaller(newContext(slash.navigation.kml.binding20.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshaller21() {
        return newUnmarshaller(newContext(slash.navigation.kml.binding21.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshaller22Beta() {
        return newUnmarshaller(newContext(slash.navigation.kml.binding22beta.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshaller22() {
        return newUnmarshaller(newContext(slash.navigation.kml.binding22.ObjectFactory.class));
    }

    private static Marshaller newMarshaller20() {
        return newMarshaller(newContext(slash.navigation.kml.binding20.ObjectFactory.class));
    }

    private static Marshaller newMarshaller21() {
        return newMarshaller(newContext(slash.navigation.kml.binding21.ObjectFactory.class));
    }

    private static Marshaller newMarshaller22Beta() {
        return newMarshaller(newContext(slash.navigation.kml.binding22beta.ObjectFactory.class));
    }

    private static Marshaller newMarshaller22() {
        return newMarshaller(newContext(slash.navigation.kml.binding22.ObjectFactory.class));
    }


    public static slash.navigation.kml.binding20.Kml unmarshal20(Reader reader) throws JAXBException {
        slash.navigation.kml.binding20.Kml result;
        try {
            result = (slash.navigation.kml.binding20.Kml) newUnmarshaller20().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static Object unmarshal20(InputStream in) throws JAXBException {
        Object result;
        try {
            result = newUnmarshaller20().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(Reader reader) throws JAXBException {
        slash.navigation.kml.binding21.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller21().unmarshal(reader);
            result = (slash.navigation.kml.binding21.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(InputStream in) throws JAXBException {
        slash.navigation.kml.binding21.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller21().unmarshal(in);
            result = (slash.navigation.kml.binding21.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(Reader reader) throws JAXBException {
        slash.navigation.kml.binding22beta.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22Beta().unmarshal(reader);
            result = (slash.navigation.kml.binding22beta.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(InputStream in) throws JAXBException {
        slash.navigation.kml.binding22beta.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22Beta().unmarshal(in);
            result = (slash.navigation.kml.binding22beta.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(Reader reader) throws JAXBException {
        slash.navigation.kml.binding22.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22().unmarshal(reader);
            result = (slash.navigation.kml.binding22.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(InputStream in) throws JAXBException {
        slash.navigation.kml.binding22.KmlType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22().unmarshal(in);
            result = (slash.navigation.kml.binding22.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }


    public static void marshal20(slash.navigation.kml.binding20.Kml kml, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller20().marshal(new JAXBElement<>(new QName(KML_20_NAMESPACE_URI, "kml"), slash.navigation.kml.binding20.Kml.class, kml), outputStream);
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }

    public static void marshal21(slash.navigation.kml.binding21.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller21().marshal(new slash.navigation.kml.binding21.ObjectFactory().createKml(kmlType), outputStream);
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }

    public static void marshal22Beta(slash.navigation.kml.binding22beta.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller22Beta().marshal(new slash.navigation.kml.binding22beta.ObjectFactory().createKml(kmlType), outputStream);
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }

    public static void marshal22(slash.navigation.kml.binding22.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller22().marshal(new slash.navigation.kml.binding22.ObjectFactory().createKml(kmlType), outputStream);
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }
}
