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

package slash.navigation.tcx;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static slash.common.helpers.JAXBHelper.*;

public class TcxUtil {
    public static final String TCX_1_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1";
    public static final String TCX_2_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2";

    private static Unmarshaller newUnmarshaller1() {
        return newUnmarshaller(newContext(slash.navigation.tcx.binding1.ObjectFactory.class));
    }

    private static Marshaller newMarshaller1() {
        return newMarshaller(newContext(slash.navigation.tcx.binding1.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshaller2() {
        return newUnmarshaller(newContext(slash.navigation.tcx.binding2.ObjectFactory.class));
    }

    private static Marshaller newMarshaller2() {
        return newMarshaller(newContext(slash.navigation.tcx.binding2.ObjectFactory.class));
    }


    public static slash.navigation.tcx.binding1.TrainingCenterDatabaseT unmarshal1(InputStream in) throws IOException {
        slash.navigation.tcx.binding1.TrainingCenterDatabaseT result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller1().unmarshal(in);
            result = (slash.navigation.tcx.binding1.TrainingCenterDatabaseT) element.getValue();
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal1(slash.navigation.tcx.binding1.TrainingCenterDatabaseT trainingCenterDatabaseT, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller1().marshal(new slash.navigation.tcx.binding1.ObjectFactory().createTrainingCenterDatabase(trainingCenterDatabaseT), outputStream);
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }


    public static slash.navigation.tcx.binding2.TrainingCenterDatabaseT unmarshal2(InputStream in) throws IOException {
        slash.navigation.tcx.binding2.TrainingCenterDatabaseT result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller2().unmarshal(in);
            result = (slash.navigation.tcx.binding2.TrainingCenterDatabaseT) element.getValue();
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal2(slash.navigation.tcx.binding2.TrainingCenterDatabaseT trainingCenterDatabaseT, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller2().marshal(new slash.navigation.tcx.binding2.ObjectFactory().createTrainingCenterDatabase(trainingCenterDatabaseT), outputStream);
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }
}
