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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

/**
 * A helper for {@link RouteService}s
 *
 * @author Christian Pesch
 */

public class UploadHelper {
    public static OutputStream parseUrlToGpx(String url) throws IOException {
        NavigationFileParser parser = new NavigationFileParser();
        List<URL> urls = Files.toUrls(url);
        if (urls.size() == 0)
            throw new IOException("Cannot read url " + url);
        if (!parser.read(urls.iterator().next()))
            throw new IOException("Cannot parse url " + url);

        OutputStream baos = new ByteArrayOutputStream();
        parser.write(parser.getTheRoute(), new Gpx10Format(), false, false, baos);
        return baos;
    }

    public static File toFile(String url) throws IOException {
        File file = new File(url);
        if(file.exists())
            return file;
        byte[] bytes = InputOutput.readBytes(new URL(url));
        return Files.writeToTempFile(bytes);
    }
}
