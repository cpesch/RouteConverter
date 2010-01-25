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

package slash.navigation.converter.gui.services;

import slash.navigation.rest.Post;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The {@link RouteService} at http://www.openstreetmap.org/api/
 *
 * Documented at http://wiki.openstreetmap.org/wiki/API_v0.6
 *
 * @author Christian Pesch
 */

public class OpenStreetMap implements RouteService {
    private static final Logger log = Logger.getLogger(OpenStreetMap.class.getName());

    public String getName() {
        return "OpenStreetMap";
    }

    public boolean isOriginOf(String url) {
        return url.startsWith("http://www.openstreetmap.org/api/");
    }

    public void upload(String username, String password, String url, String name, String description) throws IOException {
        Post post = new Post("http://www.openstreetmap.org/api/0.6/gpx/create");
        post.setAuthentication(username, password, "www.openstreetmap.org", "BASIC");
        post.addFile("file", UploadHelper.toFile(url));
        // TODO use FilePart, missing: Content-Disposition: form-data; name=" + name + "; filename=" + gpxFile.getName() + "
        // OutputStream baos = UploadHelper.parseUrlToGpx(url);
        // post.addString("file", baos.toString());
        post.addString("description", "Hamburg Luebeck"); // description.replaceAll("\\.;&?,/","_"));
        post.addString("tags", name.replaceAll("\\\\.;&?,/","_"));
        post.addString("public", "0");
        post.addString("visibility", "private"); // One of the following: private, public, trackable, identifiable
        String resultBody = post.execute();
        log.info("ResultBody: " + resultBody);
        if (!post.isSuccessful()) {
            String error = post.getHeader("Error");
            log.info("Error: " + error);
            if(!error.equals(resultBody))
                error = resultBody + "/" + error;
            throw new IOException("Cannot upload url: " + error + " (" + post.getResult() + ")");
        }
    }

}