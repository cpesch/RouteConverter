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

package slash.common.helpers;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.common.prefs.InMemoryPreferences;

import java.io.StringWriter;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JAXBHelper}.
 *
 * @author Christian Pesch
 */

public class JAXBHelperTest {
    private final Preferences preferences = new InMemoryPreferences();

    @BeforeEach
    public void setUp() {
        JAXBHelper.setPreferences(preferences);
    }

    @AfterEach
    public void tearDown() {
        JAXBHelper.setPreferences(Preferences.userNodeForPackage(JAXBHelper.class));
    }

    @XmlRootElement(name = "route")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Route {
        @XmlElement(name = "name")
        private String name = "RouteConverter";
    }

    private String marshal() throws Exception {
        StringWriter writer = new StringWriter();
        JAXBHelper.newMarshaller(JAXBHelper.newContext(Route.class)).marshal(new Route(), writer);
        return writer.toString().replace("\r\n", "\n");
    }

    // pretty printing breaks the line and indents between the root and its child
    private boolean isPrettyPrinted(String xml) {
        return xml.matches("(?s).*<route>\\s+<name>.*");
    }

    @Test
    public void testNewMarshallerPrettyPrintsByDefault() throws Exception {
        assertTrue(preferences.getBoolean("prettyPrintXml", true));
        assertTrue(isPrettyPrinted(marshal()));
    }

    @Test
    public void testNewMarshallerDoesNotPrettyPrintWhenPreferenceIsFalse() throws Exception {
        preferences.put("prettyPrintXml", "false");
        assertFalse(isPrettyPrinted(marshal()));
    }

    @Test
    public void testNewMarshallerPrettyPrintsWhenPreferenceIsTrue() throws Exception {
        preferences.put("prettyPrintXml", "true");
        assertTrue(isPrettyPrinted(marshal()));
    }
}
