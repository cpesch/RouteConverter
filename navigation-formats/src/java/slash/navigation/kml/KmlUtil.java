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

import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.kml.binding20.Kml;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.*;

public class KmlUtil {
    public static final JAXBContext CONTEXT_20 = JaxbUtils.newContext(slash.navigation.kml.binding20.ObjectFactory.class);
    public static final JAXBContext CONTEXT_21 = JaxbUtils.newContext(slash.navigation.kml.binding21.ObjectFactory.class);
    public static final JAXBContext CONTEXT_22_BETA = JaxbUtils.newContext(slash.navigation.kml.binding22beta.ObjectFactory.class);
    public static final JAXBContext CONTEXT_22 = JaxbUtils.newContext(slash.navigation.kml.binding22.ObjectFactory.class);

    public static final String KML_20_NAMESPACE_URI = "http://earth.google.com/kml/2.0";
    public static final String KML_21_NAMESPACE_URI = "http://earth.google.com/kml/2.1";
    public static final String KML_22_BETA_NAMESPACE_URI = "http://earth.google.com/kml/2.2";
    public static final String KML_22_NAMESPACE_URI = "http://www.opengis.net/kml/2.2";
    public static final String ATOM_2005_NAMESPACE_URI = "http://www.w3.org/2005/Atom";
    public static final String XAL_20_NAMESPACE_URI = "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0";

    public static Unmarshaller newUnmarshaller20() {
        return JaxbUtils.newUnmarshaller(CONTEXT_20);
    }

    public static Unmarshaller newUnmarshaller21() {
        return JaxbUtils.newUnmarshaller(CONTEXT_21);
    }

    public static Unmarshaller newUnmarshaller22Beta() {
        return JaxbUtils.newUnmarshaller(CONTEXT_22_BETA);
    }

    public static Unmarshaller newUnmarshaller22() {
        return JaxbUtils.newUnmarshaller(CONTEXT_22);
    }

    public static Marshaller newMarshaller20() {
        return JaxbUtils.newMarshaller(CONTEXT_20);
    }

    public static Marshaller newMarshaller21() {
        return JaxbUtils.newMarshaller(CONTEXT_21);
    }

    public static Marshaller newMarshaller22Beta() {
        return JaxbUtils.newMarshaller(CONTEXT_22_BETA,
                ATOM_2005_NAMESPACE_URI, "atom",
                XAL_20_NAMESPACE_URI, "xal"
        );
    }

    public static Marshaller newMarshaller22() {
        return JaxbUtils.newMarshaller(CONTEXT_22,
                ATOM_2005_NAMESPACE_URI, "atom",
                XAL_20_NAMESPACE_URI, "xal"
        );
    }


    public static Kml unmarshal20(Reader reader) throws JAXBException {
        Kml result = null;
        try {
            result = (Kml) newUnmarshaller20().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static Kml unmarshal20(String string) throws JAXBException {
        return unmarshal20(new StringReader(string));
    }

    public static Object unmarshal20(InputStream in) throws JAXBException {
        Object result = null;
        try {
            result = newUnmarshaller20().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        }
        return result;
    }

    public static Object unmarshal20(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal20(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(Reader reader) throws JAXBException {
        slash.navigation.kml.binding21.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller21().unmarshal(reader);
            result = (slash.navigation.kml.binding21.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(InputStream in) throws JAXBException {
        slash.navigation.kml.binding21.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller21().unmarshal(in);
            result = (slash.navigation.kml.binding21.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding21.KmlType unmarshal21(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal21(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(Reader reader) throws JAXBException {
        slash.navigation.kml.binding22beta.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22Beta().unmarshal(reader);
            result = (slash.navigation.kml.binding22beta.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(InputStream in) throws JAXBException {
        slash.navigation.kml.binding22beta.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22Beta().unmarshal(in);
            result = (slash.navigation.kml.binding22beta.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding22beta.KmlType unmarshal22Beta(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal22Beta(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(Reader reader) throws JAXBException {
        slash.navigation.kml.binding22.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22().unmarshal(reader);
            result = (slash.navigation.kml.binding22.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(InputStream in) throws JAXBException {
        slash.navigation.kml.binding22.KmlType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller22().unmarshal(in);
            result = (slash.navigation.kml.binding22.KmlType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static slash.navigation.kml.binding22.KmlType unmarshal22(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal22(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }


    public static void marshal20(Kml kml, Writer writer) throws JAXBException {
        newMarshaller20().marshal(new JAXBElement<Kml>(new QName(KML_20_NAMESPACE_URI, "kml"), Kml.class, kml), writer);
    }

    public static void marshal20(Kml kml, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller20().marshal(new JAXBElement<Kml>(new QName(KML_20_NAMESPACE_URI, "kml"), Kml.class, kml), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }

    public static void marshal21(slash.navigation.kml.binding21.KmlType kmlType, Writer writer) throws JAXBException {
        newMarshaller21().marshal(new JAXBElement<slash.navigation.kml.binding21.KmlType>(new QName(KML_21_NAMESPACE_URI, "kml"), slash.navigation.kml.binding21.KmlType.class, kmlType), writer);
    }

    public static void marshal21(slash.navigation.kml.binding21.KmlType kmlType, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller21().marshal(new JAXBElement<slash.navigation.kml.binding21.KmlType>(new QName(KML_21_NAMESPACE_URI, "kml"), slash.navigation.kml.binding21.KmlType.class, kmlType), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }

    public static void marshal22Beta(slash.navigation.kml.binding22beta.KmlType kmlType, Writer writer) throws JAXBException {
        newMarshaller22Beta().marshal(new JAXBElement<slash.navigation.kml.binding22beta.KmlType>(new QName(KML_22_BETA_NAMESPACE_URI, "kml"), slash.navigation.kml.binding22beta.KmlType.class, kmlType), writer);
    }

    public static void marshal22Beta(slash.navigation.kml.binding22beta.KmlType kmlType, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller22Beta().marshal(new JAXBElement<slash.navigation.kml.binding22beta.KmlType>(new QName(KML_22_BETA_NAMESPACE_URI, "kml"), slash.navigation.kml.binding22beta.KmlType.class, kmlType), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }

    public static void marshal22(slash.navigation.kml.binding22.KmlType kmlType, Writer writer) throws JAXBException {
        newMarshaller22().marshal(new JAXBElement<slash.navigation.kml.binding22.KmlType>(new QName(KML_22_NAMESPACE_URI, "kml"), slash.navigation.kml.binding22.KmlType.class, kmlType), writer);
    }

    public static void marshal22(slash.navigation.kml.binding22.KmlType kmlType, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller22().marshal(new JAXBElement<slash.navigation.kml.binding22.KmlType>(new QName(KML_22_NAMESPACE_URI, "kml"), slash.navigation.kml.binding22.KmlType.class, kmlType), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }
}
