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

import jakarta.xml.bind.JAXBException;
import org.junit.Test;
import slash.navigation.download.queue.binding.QueueType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

/**
 * Tests for {@link QueueUtil} marshal/unmarshal round-trips.
 */
public class QueueUtilTest {

    @Test
    public void testMarshalEmptyQueueProducesOutput() throws JAXBException {
        QueueType queue = new QueueType();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueueUtil.marshal(queue, out);
        assertTrue("Marshalled output should be non-empty", out.size() > 0);
    }

    @Test
    public void testMarshalOutputContainsQueueElement() throws JAXBException {
        QueueType queue = new QueueType();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueueUtil.marshal(queue, out);
        String xml = out.toString();
        assertTrue("Output should contain queue element", xml.contains("queue"));
    }

    @Test
    public void testUnmarshalEmptyQueueReturnsNonNull() throws JAXBException {
        QueueType queue = new QueueType();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueueUtil.marshal(queue, out);

        QueueType result = QueueUtil.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(result);
    }

    @Test
    public void testRoundTripEmptyQueueHasZeroDownloads() throws JAXBException {
        QueueType queue = new QueueType();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QueueUtil.marshal(queue, out);

        QueueType result = QueueUtil.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, result.getDownload().size());
    }
}

