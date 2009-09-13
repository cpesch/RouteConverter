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

    public void upload(String userName, char[] password, String url, String name, String description) {
        throw new UnsupportedOperationException();

        /*

HTTP POST

The following is a sample HTTP POST request and response. The placeholders shown need to be replaced with actual values.

POST /services/LiveTracking.asmx/UploadGPX HTTP/1.1
Host: www.crossingways.com
Content-Type: application/x-www-form-urlencoded
Content-Length: length

username=string&password=string&trackname=string&gpx=string

HTTP/1.1 200 OK
Content-Type: text/xml; charset=utf-8
Content-Length: length

<?xml version="1.0" encoding="utf-8"?>
<string xmlns="http://www.crossingways.com/">string</string>

         */
    }
}