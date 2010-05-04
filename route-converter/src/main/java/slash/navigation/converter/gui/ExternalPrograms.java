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

package slash.navigation.converter.gui;

import java.awt.*;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Knows how to cope with external programs like mail.
 *
 * @author Christian Pesch
 */
public abstract class ExternalPrograms {
    protected static final Logger log = Logger.getLogger(ExternalPrograms.class.getName());

    public void startBrowserForHomepage(Window window) {
        startBrowser(window, "www.routeconverter.de");
    }

    public void startBrowserForUpdate(Window window) {
        String locale = Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()) ? "_de" : "";
        startBrowser(window, "www.routeconverter.de/download" + locale + ".html");
    }

    public void startBrowserForTerms(Window window) {
        startBrowser(window, "www.routeconverter.de/routecatalog/terms.html");
    }

    public void startBrowserForForum(Window window) {
        startBrowser(window, "www.routeconverter.de/forum/");
    }

    public void startBrowserForGeonames(Window window) {
        startBrowser(window, "www.geonames.org");
    }

    protected abstract void startBrowser(Window window, String uri);

    public void startMail(Window window) {
        startMail(window, "mailto:support@routeconverter.de");
    }

    protected abstract void startMail(Window window, String uri);

}
