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

package slash.navigation.viamichelin;

import org.junit.Test;
import slash.navigation.viamichelin.binding.PoiList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ViaMichelinUtil} unmarshal/marshal round-trips.
 */
public class ViaMichelinUtilTest {

    // Minimal ViaMichelin poi_list XML. The entity resolver replaces the DTD URI with
    // an empty preamble so no network access is needed.
    private static final String VIAMICHELIN_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<!DOCTYPE poi_list SYSTEM \"http://www2.viamichelin.com/vmw2/dtd/export.dtd\">" +
            "<poi_list version=\"2.0\" folder=\"RouteConverter\"></poi_list>";

    // --- unmarshal ---

    @Test
    public void testUnmarshalReturnsNonNull() throws IOException {
        PoiList result = ViaMichelinUtil.unmarshal(new StringReader(VIAMICHELIN_XML));
        assertNotNull(result);
    }

    @Test
    public void testUnmarshalVersionAttribute() throws IOException {
        PoiList result = ViaMichelinUtil.unmarshal(new StringReader(VIAMICHELIN_XML));
        assert "2.0".equals(result.getVersion()) : "version should be 2.0, got " + result.getVersion();
    }

    @Test
    public void testUnmarshalFolderAttribute() throws IOException {
        PoiList result = ViaMichelinUtil.unmarshal(new StringReader(VIAMICHELIN_XML));
        assert "RouteConverter".equals(result.getFolder()) : "folder should be RouteConverter";
    }

    // --- marshal ---

    @Test
    public void testMarshalEmptyPoiListProducesOutput() throws Exception {
        PoiList poiList = new PoiList();
        poiList.setVersion("2.0");
        poiList.setFolder("Test");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ViaMichelinUtil.marshal(poiList, out);
        String result = out.toString("ISO-8859-1");
        assertNotNull(result);
        assertTrue("Marshalled output should contain poi_list", result.contains("poi_list"));
        assertTrue("Marshalled output should contain version", result.contains("2.0"));
    }

    // --- round-trip: unmarshal ? marshal ? unmarshal ---

    @Test
    public void testRoundTrip() throws Exception {
        PoiList original = ViaMichelinUtil.unmarshal(new StringReader(VIAMICHELIN_XML));
        original.setFolder("RoundTrip");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ViaMichelinUtil.marshal(original, out);

        // Re-read the marshalled bytes: prepend entity-resolver-compatible DOCTYPE
        String marshalled = out.toString("ISO-8859-1");
        // The marshaller already prepends the DOCTYPE header
        PoiList roundtripped = ViaMichelinUtil.unmarshal(new StringReader(marshalled));
        assertNotNull(roundtripped);
    }
}

