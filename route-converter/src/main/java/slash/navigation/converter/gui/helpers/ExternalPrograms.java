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

import java.awt.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.util.Locale.GERMAN;
import static java.util.Locale.getDefault;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

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

    public static void startBrowserForRouteConverter(Window window) {
        startBrowser(window, "https://www.routeconverter.com");
    }

    public static void startBrowserForTimeAlbumProDownload(Window window) {
        startBrowser(window, "https://cbgps.com/download_en.htm");
    }

    public static void startBrowserForTerms(Window window) {
        String language = isGerman() ? "de" : "en";
        startBrowser(window, "https://www.routeconverter.com/routecatalog-terms-" + language);
    }

    public static void startBrowserForTranslation(Window window) {
        startBrowser(window, "https://hosted.weblate.org/engage/routeconverter/");
    }

    public static void startBrowserForGoogleAPIKey(Window window) {
        String url = isGerman() ?
                "https://forum.routeconverter.com/thread-2489.html" :
                "https://forum.routeconverter.com/thread-2501.html";
        startBrowser(window, url);
    }

    public static void startBrowserForTimeAlbumProSupport(Window window) {
        startMail(window, "mailto:columbusservice@hotmail.com");
    }

    public static void startBrowserForRouteConverterForum(Window window) {
        startBrowser(window, isGerman() ? "https://forum.routeconverter.com/forum-4.html" :
                "https://forum.routeconverter.com/forum-12.html");
    }

    public static void startBrowserForRouteConverterResources(Window window) {
        startBrowser(window, "https://www.routeconverter.com/resources/");
    }

    public static void startBrowserForDouglasPeucker(Window window) {
        String url = isGerman() ?
                "https://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus" :
                "https://en.wikipedia.org/wiki/Ramer-Douglas-Peucker_algorithm";
        startBrowser(window, url);
    }

    public static void startBrowserForGoogleApiKey(Window window) {
        startBrowser(window, "https://developers.google.com/maps/documentation/javascript/get-api-key");
    }

    public static void startBrowserForThunderforestApiKey(Window window) {
        startBrowser(window, "https://www.thunderforest.com/docs/apikeys");
    }

    public static void startBrowserForGeonamesUserName(Window window) {
        startBrowser(window, "https://www.geonames.org/login");
    }

    public static void startBrowser(Window window, String uri) {
        try {
            if (!isDesktopSupported())
                throw new UnsupportedOperationException("No desktop support available");

            getDesktop().browse(new URI(uri));
        } catch (Exception e) {
            log.severe("Start browser error: " + e);

            showMessageDialog(window,
                    MessageFormat.format(RouteConverter.getBundle().getString("start-browser-error"), getLocalizedMessage(e)),
                    RouteConverter.getTitle(), ERROR_MESSAGE);
        }
    }

    private static void startMail(Window window, String uri) {
        try {
            if (!isDesktopSupported())
                throw new UnsupportedOperationException("No desktop support available");

            getDesktop().mail(new URI(uri));
        } catch (Exception e) {
            log.severe("Start mail error: " + e);

            showMessageDialog(window,
                    MessageFormat.format(RouteConverter.getBundle().getString("start-mail-error"), getLocalizedMessage(e)),
                    RouteConverter.getTitle(), ERROR_MESSAGE);
        }
    }
}
