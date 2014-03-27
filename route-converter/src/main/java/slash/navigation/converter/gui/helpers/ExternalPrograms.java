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

package slash.navigation.converter.gui.helpers;

import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.util.Locale.GERMAN;
import static java.util.Locale.getDefault;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 * Knows how to cope with external programs like mail.
 *
 * @author Christian Pesch
 */
public class ExternalPrograms {
    protected static final Logger log = Logger.getLogger(ExternalPrograms.class.getName());

    private ExternalPrograms() {
    }

    private static boolean isGerman() {
        return getDefault().getLanguage().equals(GERMAN.getLanguage());
    }

    public static void startBrowserForHomepage(Window window) {
        startBrowser(window, isGerman() ? "http://www.routeconverter.de/" : "http://www.routeconverter.com/");
    }

    public static void startBrowserForUpdateCheck(Window window, String version, long startTime) {
        String rootUrl = System.getProperty("feedback", "http://www.routeconverter.com/feedback/");
        startBrowser(window, rootUrl + "update-check/" + getDefault().getLanguage() + "/" + version + "/" + startTime + "/");
    }

    public static void startBrowserForTerms(Window window) {
        startBrowser(window, "http://www.routeconverter.com/routecatalog_terms/" + getDefault().getLanguage());
    }

    public static void startBrowserForForum(Window window) {
        startBrowser(window, isGerman() ? "http://forum.routeconverter.de/" : "http://forum.routeconverter.com/");
    }

    public static void startBrowserForGeonames(Window window) {
        startBrowser(window, "http://www.geonames.org/");
    }

    public static void startBrowserForDouglasPeucker(Window window) {
        String url = isGerman() ?
                "http://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus" :
                "http://en.wikipedia.org/wiki/Ramer-Douglas-Peucker_algorithm";
        startBrowser(window, url);
    }

    public static void startBrowserForJava(Window window) {
        startBrowser(window, "http://java.com/download/");
    }

    private static void startBrowser(Window window, String uri) {
        if (isDesktopSupported()) {
            try {
                getDesktop().browse(new URI(uri));
            } catch (Exception e) {
                log.severe("Start browser error: " + e.getMessage());

                showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-browser-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void startMail(Window window) {
        startMail(window, isGerman() ? "mailto:support@routeconverter.de" : "mailto:support@routeconverter.com");
    }

    private static void startMail(Window window, String uri) {
        if (isDesktopSupported()) {
            try {
                getDesktop().mail(new URI(uri));
            } catch (Exception e) {
                log.severe("Start mail error: " + e.getMessage());

                showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-mail-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
