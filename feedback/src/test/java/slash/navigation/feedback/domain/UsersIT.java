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
package slash.navigation.feedback.domain;

import org.junit.Assert;
import org.junit.Test;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.routecatalog10.UserextensionType;

import javax.xml.bind.JAXBElement;
import java.util.List;

public class UsersIT extends RouteFeedbackServiceBase {

    @Test
    public void testAddUser() throws Exception {
        String name = "User " + System.currentTimeMillis();
        String location = routeFeedback.addUser(name, "secret", "First", "Last", "first@last.com");
        Assert.assertNotNull(location);
        GpxType gpxType = routeFeedback.fetchGpx(location);
        Assert.assertNotNull(gpxType);
        Assert.assertEquals(name, gpxType.getMetadata().getName());
        List<Object> anys = gpxType.getMetadata().getExtensions().getAny();
        Assert.assertEquals(1, anys.size());
        JAXBElement any = (JAXBElement) anys.get(0);
        UserextensionType extension = (UserextensionType) any.getValue();
        Assert.assertEquals("first@last.com", extension.getEmail());
        Assert.assertEquals("First", extension.getFirstname());
        Assert.assertNotNull(extension.getLastlogin());
        Assert.assertEquals("Last", extension.getLastname());
        Assert.assertNull(extension.getPassword());
    }

    @Test
    public void testAddUserWithUmlauts() throws Exception {
        String name = "User äöüßÄÖÜ Umlauts " + System.currentTimeMillis();
        String location = routeFeedback.addUser(name, "secretÄÖÜ", "First ÄÖÜ", "Last ÄÖÜ", "first@last.com");
        Assert.assertNotNull(location);
        GpxType gpxType = routeFeedback.fetchGpx(location);
        Assert.assertNotNull(gpxType);
        Assert.assertEquals(name, gpxType.getMetadata().getName());
        List<Object> anys = gpxType.getMetadata().getExtensions().getAny();
        Assert.assertEquals(1, anys.size());
        JAXBElement any = (JAXBElement) anys.get(0);
        UserextensionType extension = (UserextensionType) any.getValue();
        Assert.assertEquals("first@last.com", extension.getEmail());
        Assert.assertEquals("First ÄÖÜ", extension.getFirstname());
        Assert.assertNotNull(extension.getLastlogin());
        Assert.assertEquals("Last ÄÖÜ", extension.getLastname());
        Assert.assertNull(extension.getPassword());
    }
}