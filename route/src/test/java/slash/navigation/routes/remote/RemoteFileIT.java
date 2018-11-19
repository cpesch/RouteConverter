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
package slash.navigation.routes.remote;

import org.junit.Test;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.routes.NotFoundException;
import slash.navigation.routes.NotOwnerException;
import slash.navigation.routes.remote.binding.FileType;

import java.io.File;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static slash.navigation.routes.remote.RemoteCatalog.FILE_URI;

public class RemoteFileIT extends BaseRemoteCatalogTest {
    private static final String STATIC_ROUTES = "static/routes/";

    @Test
    public void testCreateAndDeleteFile() throws IOException {
        String url = catalog.addFile(SAMPLE_FILE);
        assertTrue(url.startsWith(API + FILE_URI));
        assertTrue(url.endsWith("/"));

        FileType fileType = catalog.getFile(url);
        assertEquals(SAMPLE_FILE_NAME, fileType.getName());
        assertEquals(USERNAME, fileType.getCreator());
        assertEquals(url, fileType.getHref());
        String fileTypeUrl = fileType.getUrl();
        assertTrue(fileTypeUrl.startsWith(API + STATIC_ROUTES));

        File tempFile = getUrlAsFile(fileTypeUrl);
        assertEquals(SAMPLE_FILE.length(), tempFile.length());

        catalog.deleteFile(url);

        assertNull(catalog.getFile(url));
        assertNotFound(fileTypeUrl);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotDeleteNotExistingFile() throws IOException {
        catalog.deleteFile(API + FILE_URI + currentTimeMillis() + "/");
    }

    @Test(expected = NotOwnerException.class)
    public void testCannotDeleteFileFromOtherUser() throws IOException {
        RemoteCatalog another = new RemoteCatalog(API, new SimpleCredentials(ANOTHER_USERNAME, PASSWORD));
        String url = another.addFile(SAMPLE_FILE);
        catalog.deleteFile(url);
    }

    @Test
    public void testSuperuserCanDeleteFileFromOtherUser() throws IOException {
        String url = catalog.addFile(SAMPLE_FILE);

        RemoteCatalog superUser = new RemoteCatalog(API, new SimpleCredentials(SUPER_USERNAME, PASSWORD));
        superUser.deleteFile(url);

        assertNull(catalog.getFile(url));
    }
}
