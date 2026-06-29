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

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import static slash.common.helpers.JAXBHelper.*;

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

    // KML 2.1 / 2.2-beta / 2.2 all wrap the document in a <kml> JAXBElement, so unmarshalling
    // is "unwrap the element and cast its value to the version's KmlType". These helpers carry
    // that shared shape (and the uniform Parse-error wrapping) for both input sources.
    private static <T> T unmarshalValue(Unmarshaller unmarshaller, Reader reader, Class<T> type) throws IOException {
        try {
            return type.cast(((JAXBElement<?>) unmarshaller.unmarshal(reader)).getValue());
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
    }

    private static <T> T unmarshalValue(Unmarshaller unmarshaller, InputStream in, Class<T> type) throws IOException {
        try {
            return type.cast(((JAXBElement<?>) unmarshaller.unmarshal(in)).getValue());
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
    }

    private static void marshalElement(Marshaller marshaller, JAXBElement<?> element, OutputStream outputStream) throws JAXBException {
        try {
            try {
                marshaller.marshal(element, outputStream);
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }


    public static slash.navigation.kml.binding20.Kml unmarshal20(Reader reader) throws IOException {
        slash.navigation.kml.binding20.Kml result;
        try {
            result = (slash.navigation.kml.binding20.Kml) newUnmarshaller20().unmarshal(reader);
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static Object unmarshal20(InputStream in) throws IOException {
        Object result;
        try {
            result = newUnmarshaller20().unmarshal(in);
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(Reader reader) throws IOException {
        return unmarshalValue(newUnmarshaller21(), reader, slash.navigation.kml.binding21.KmlType.class);
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(InputStream in) throws IOException {
        return unmarshalValue(newUnmarshaller21(), in, slash.navigation.kml.binding21.KmlType.class);
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(Reader reader) throws IOException {
        return unmarshalValue(newUnmarshaller22Beta(), reader, slash.navigation.kml.binding22beta.KmlType.class);
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(InputStream in) throws IOException {
        return unmarshalValue(newUnmarshaller22Beta(), in, slash.navigation.kml.binding22beta.KmlType.class);
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(Reader reader) throws IOException {
        return unmarshalValue(newUnmarshaller22(), reader, slash.navigation.kml.binding22.KmlType.class);
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(InputStream in) throws IOException {
        return unmarshalValue(newUnmarshaller22(), in, slash.navigation.kml.binding22.KmlType.class);
    }


    public static void marshal20(slash.navigation.kml.binding20.Kml kml, OutputStream outputStream) throws JAXBException {
        marshalElement(newMarshaller20(),
                new JAXBElement<>(new QName(KML_20_NAMESPACE_URI, "kml"), slash.navigation.kml.binding20.Kml.class, kml),
                outputStream);
    }

    public static void marshal21(slash.navigation.kml.binding21.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        marshalElement(newMarshaller21(), new slash.navigation.kml.binding21.ObjectFactory().createKml(kmlType), outputStream);
    }

    public static void marshal22Beta(slash.navigation.kml.binding22beta.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        marshalElement(newMarshaller22Beta(), new slash.navigation.kml.binding22beta.ObjectFactory().createKml(kmlType), outputStream);
    }

    public static void marshal22(slash.navigation.kml.binding22.KmlType kmlType, OutputStream outputStream) throws JAXBException {
        marshalElement(newMarshaller22(), new slash.navigation.kml.binding22.ObjectFactory().createKml(kmlType), outputStream);
    }
}
