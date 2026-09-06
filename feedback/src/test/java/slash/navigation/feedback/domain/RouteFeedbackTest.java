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

package slash.navigation.feedback.domain;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class RouteFeedbackTest {
    private final RouteFeedback routeFeedback = new RouteFeedback("https://api.routeconverter.com/", null);

    @Test
    public void testWindowsAmd64GermanUsesDeDomain() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "offline", "Windows 10", "amd64", Locale.GERMANY);
        assertEquals("https://www.routeconverter.de/downloads/thanks/?from=app&os=windows&arch=x64&v=3.2&product=routeconverter", url);
    }

    @Test
    public void testMacAarch64EnglishUsesComDomain() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "offline", "Mac OS X", "aarch64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&os=mac&arch=aarch64&v=3.2&product=routeconverter", url);
    }

    @Test
    public void testLinuxX86_64EnglishUsesComDomain() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "offline", "Linux", "x86_64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&os=linux&arch=x64&v=3.2&product=routeconverter", url);
    }

    @Test
    public void testTimeAlbumProEditionAddsProductParameter() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "timealbum", "Windows 11", "amd64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&os=windows&arch=x64&v=3.2&product=timealbumpro", url);
    }

    @Test
    public void testUnknownOsOmitsOsParameter() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "offline", "FreeBSD", "amd64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&arch=x64&v=3.2&product=routeconverter", url);
    }

    @Test
    public void testUnknownArchOmitsArchParameter() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "offline", "Linux", "riscv64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&os=linux&v=3.2&product=routeconverter", url);
    }

    @Test
    public void testUnknownEditionOmitsProductParameter() {
        String url = routeFeedback.getUpdateCheckUrl("3.2", "columbus", "Linux", "amd64", Locale.US);
        assertEquals("https://www.routeconverter.com/downloads/thanks/?from=app&os=linux&arch=x64&v=3.2", url);
    }
}
