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

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Knows how to cope with external programs like mail.
 *
 * @author Christian Pesch
 */
public class ExternalPrograms {
    protected static final Logger log = Logger.getLogger(ExternalPrograms.class.getName());

    public void startBrowserForHomepage(Window window) {
        startBrowser(window, "http://www.routeconverter.com");
    }

    public void startBrowserForUpdate(Window window) {
        startBrowser(window, "http://www.routeconverter.com/downloads/" + Locale.getDefault().getLanguage());
    }

    public void startBrowserForTerms(Window window) {
        startBrowser(window, "http://www.routeconverter.com/routecatalog_terms/" + Locale.getDefault().getLanguage());
    }

    public void startBrowserForForum(Window window) {
        startBrowser(window, "http://www.routeconverter.com/forum/");
    }

    public void startBrowserForGeonames(Window window) {
        startBrowser(window, "http://www.geonames.org");
    }

    public void startBrowserForDouglasPeucker(Window window) {
        String url = Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()) ?
                "http://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus" :
                "http://en.wikipedia.org/wiki/Ramer-Douglas-Peucker_algorithm";
        startBrowser(window, url);
    }

    public void startBrowserForJava(Window window) {
        startBrowser(window, "http://java.com/download/");
    }

    protected void startBrowser(Window window, String uri) {
        if (Desktop.isDesktopSupported())
            try {
                Desktop.getDesktop().browse(new URI(uri));
            } catch (Exception e) {
                log.severe("Start browser error: " + e.getMessage());

                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-browser-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
    }

    public void startMail(Window window) {
        startMail(window, "mailto:support@routeconverter.com");
    }

    protected void startMail(Window window, String uri) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().mail(new URI(uri));
            } catch (Exception e) {
                log.severe("Start mail error: " + e.getMessage());

                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-mail-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
