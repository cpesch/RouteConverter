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

package slash.navigation.download.queue;

import slash.navigation.download.queue.binding.ObjectFactory;
import slash.navigation.download.queue.binding.QueueType;
import slash.navigation.jaxb.JaxbUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static slash.navigation.jaxb.JaxbUtils.newContext;

class QueueUtil {
    private static Unmarshaller newUnmarshaller() {
        return JaxbUtils.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        return JaxbUtils.newMarshaller(newContext(ObjectFactory.class));
    }

    public static QueueType unmarshal(InputStream in) throws JAXBException {
        QueueType result = null;
        try {
            JAXBElement<QueueType> element = (JAXBElement<QueueType>) newUnmarshaller().unmarshal(in);
            result = element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e.getMessage(), e);
        }
        return result;
    }

    public static void marshal(QueueType queueType, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new ObjectFactory().createQueue(queueType), out);
            } finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e.getMessage());
        }
    }
}
