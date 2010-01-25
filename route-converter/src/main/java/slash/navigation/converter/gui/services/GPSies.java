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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import slash.navigation.rest.Post;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The {@link RouteService} at http://gpsies.com
 *
 * @author Christian Pesch
 */

public class GPSies implements RouteService {
    private static final Logger log = Logger.getLogger(GPSies.class.getName());

    public String getName() {
        return "GPSies";
    }

    public boolean isOriginOf(String url) {
        return url.startsWith("http://www.gpsies.com/api.do");
    }

    public void upload(String username, String password, String url, String name, String description) throws IOException {
        String validationString = username + "|" + DigestUtils.md5Hex(password);
        String encryptedString = new String(Base64.encodeBase64(validationString.getBytes()));

        Post post = new Post("http://www.gpsies.com/upload.do");
        post.addString("device", "RouteConverter");
        post.addString("authenticateHash", encryptedString);
        post.addString("trackTypes", "miscellaneous");
        post.addString("filename", name);
        post.addString("fileDescription", description);
        post.addString("startpointCountry", "DE");
        post.addString("endpointCountry", "DE");
        post.addString("uploadButton", "speichern");
        post.addFile("formFile", UploadHelper.toFile(url));
        post.addString("status", "3"); // 1=public, 3=private
        // post.addString("websiteUrl", url); has to be a valid URL
        String result = post.execute();
        log.info("Result: " + result);
        // OK RouteConverter [http://www.gpsies.com/map.do?fileId=mvfkkkoeteyxfnqz]
        if (!result.startsWith("OK RouteConverter"))
            throw new IOException("Cannot upload url: " + result);
    }
}