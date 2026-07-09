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
package slash.common.io;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static slash.common.io.Files.*;

/**
 * Covers the {@link File}/{@link URL} extension and URL-list helpers of {@link Files}.
 *
 * @author Christian Pesch
 */
public class FilesUrlTest {

    @Test
    public void getExtensionOfFileUsesTheName() {
        assertEquals(".gpx", getExtension(new File("/some/dir/track.gpx")));
    }

    @Test
    public void getExtensionOfUrlIsLowercased() throws Exception {
        assertEquals(".gpx", getExtension(new URL("http://host/path/TRACK.GPX")));
    }

    @Test
    public void getExtensionOfUrlListReturnsTheLongestFound() throws Exception {
        List<URL> urls = asList(new URL("http://host/a.gp"), new URL("http://host/b.gpx"));

        assertEquals(".gpx", getExtension(urls));
    }

    @Test
    public void toUrlsParsesUrlsAndFallsBackToFilePaths() {
        List<URL> urls = toUrls("http://host/x.gpx", "relative/local.gpx");

        assertEquals(2, urls.size());
        assertEquals("http", urls.get(0).getProtocol());
        assertEquals("file", urls.get(1).getProtocol());
    }

    @Test
    public void toUrlKeepsAbsoluteUrls() throws Exception {
        assertEquals("http", toUrl("http://host/x.gpx").getProtocol());
    }

    @Test
    public void toUrlFallsBackToFileForBareAndSpaceBearingPaths() throws Exception {
        // absolute local path, no scheme -> not an absolute URI -> file fallback
        assertEquals("file", toUrl("/some/dir/track.gpx").getProtocol());
        // spaces are illegal in a URI -> file fallback keeps leniency of the old new URL(String)
        URL url = toUrl("/some/dir/with space/track.gpx");
        assertEquals("file", url.getProtocol());
        // compare absolute forms: new File(url.toURI()) round-trips to getAbsoluteFile(), which on
        // Windows carries a drive letter while a bare "/some/..." File stays drive-relative
        assertEquals(new File("/some/dir/with space/track.gpx").getAbsoluteFile(), new File(url.toURI()));
    }

    @Test
    public void reverseReturnsTheUrlsInReverseOrder() throws Exception {
        URL a = new URL("http://host/a");
        URL b = new URL("http://host/b");
        URL c = new URL("http://host/c");

        assertEquals(asList(c, b, a), reverse(asList(a, b, c)));
    }
}
