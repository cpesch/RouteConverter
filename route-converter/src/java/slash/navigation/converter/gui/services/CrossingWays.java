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

import slash.common.io.Files;
import slash.common.io.InputOutput;
import slash.navigation.NavigationFileParser;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.rest.Post;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * The {@link RouteService} at http://crossingways.com
 *
 * @author Christian Pesch
 */

public class CrossingWays implements RouteService {
    public String getName() {
        return "crossingways";
    }

    public boolean isOriginOf(String url) {
        return url.startsWith("http://www.crossingways.com/services/");
    }

    private String readFile(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputOutput inout = new InputOutput(new FileInputStream(file), baos);
        inout.start();
        return baos.toString();
    }

    public void upload(String userName, String password, String url, String name, String description) throws IOException {
        NavigationFileParser parser = new NavigationFileParser();
        List<URL> urls = Files.toUrls(url);
        if (urls.size() == 0)
            throw new IllegalArgumentException("Cannot read url " + url);
        if (!parser.read(urls.iterator().next()))
            throw new IllegalArgumentException("Cannot parse url " + url);
        File tempFile = File.createTempFile("crossingwaysclient", ".xml");
        // TODO extend write to write to OutputStream and File
        parser.write(parser.getRoutes(), new Gpx10Format(), tempFile);
        String gpx = readFile(tempFile);
        if (!tempFile.delete())
            throw new IOException("Cannot delete temp file " + tempFile);

        Post post = new Post("http://www.crossingways.com/services/LiveTracking.asmx/UploadGPX");
        String body = "username=" + userName + "&password=" + password + "&trackname=" + name + "&gpx=" + gpx;
        post.setBody(body);
        System.out.println("Body: " + body);   // TODO remove me
        String result = post.execute();
        System.out.println("Result: " + result);        // TODO remove me
        throw new IOException(result);    // TODO fix return code stuff
    }
}