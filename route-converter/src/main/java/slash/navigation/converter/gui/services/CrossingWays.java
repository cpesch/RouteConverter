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

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import slash.navigation.rest.Post;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Logger;

/**
 * The {@link RouteService} at http://crossingways.com
 *
 * Documented at http://www.crossingways.com/services/LiveTracking.asmx?op=UploadGPX
 *
 * @author Christian Pesch
 */

public class CrossingWays implements RouteService {
    private static final Logger log = Logger.getLogger(CrossingWays.class.getName());

    public String getName() {
        return "crossingways";
    }

    public boolean isOriginOf(String url) {
        return url.startsWith("http://www.crossingways.com/services/");
    }

    String extractResult(String result) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }

        Document document;
        try {
            document = builder.parse(new InputSource(new StringReader(result)));
        } catch (SAXException e) {
            throw new IOException(e);
        }

        NodeList nodeList = document.getChildNodes();
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    public void upload(String username, String password, String url, String name, String description) throws IOException {
        OutputStream baos = UploadHelper.parseUrlToGpx(url);
        Post post = new Post("http://www.crossingways.com/services/LiveTracking.asmx/UploadGPX");
        String body = "username=" + username + "&password=" + new String(DigestUtils.sha(password)) + "&trackname=" + name + "&gpx=" + baos.toString();
        post.setBody(body);
        String resultBody = post.execute();
        log.info("ResultBody: " + resultBody);
        String result = extractResult(resultBody);
        log.info("Result: " + result);
        if (!"Track saved! Have a nice Day!".equals(result))
            throw new IOException("Cannot upload url: " + result);
    }
}