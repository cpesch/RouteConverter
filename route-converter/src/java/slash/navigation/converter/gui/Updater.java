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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui;

import slash.navigation.util.InputOutput;
import slash.navigation.util.Version;
import slash.navigation.util.Conversion;
import slash.navigation.gui.Constants;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.Locale;

/**
 * Knows how to retrieve the information which is the latest version.
 *
 * @author Christian Pesch
 */
public class Updater {
    private static final Logger log = Logger.getLogger(Updater.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(Updater.class);
    private static final String START_COUNT_PREFERENCE = "startCount";
    private static final String CHARSET = "ISO8859-1";

    public static int getStartCount() {
        return preferences.getInt(START_COUNT_PREFERENCE, 0);
    }

    static {
        preferences.putInt(START_COUNT_PREFERENCE, getStartCount() + 1);
    }

    private UpdateResult check(int timeout) {
        UpdateResult result = new UpdateResult();
        try {
            result.myVersion = Version.parseVersionFromManifest().getVersion();
            String payload = Version.getRouteConverterVersion(result.myVersion) +
                    "routeconverter.startcount=" + getStartCount() + "," +
                    "user.locale=" + Locale.getDefault() + "," +
                    Version.getSystemProperty("java.version") +
                    Version.getSystemProperty("os.name") +
                    Version.getSystemProperty("os.version") +
                    Version.getSystemProperty("os.arch");
            log.fine("Payload: " + payload);

            URL url = new URL("http://www.routeconverter.de/routeconverter/versioncheck.jsp");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(timeout * 1000);
            connection.setReadTimeout(timeout * 1000);

            OutputStream os = connection.getOutputStream();
            os.write(payload.getBytes(CHARSET));
            os.close();

            byte[] resultArray = InputOutput.readBytes(connection.getInputStream());
            String parameters = new String(resultArray, CHARSET);

            result.latestVersion = Version.parseVersionFromParameters(parameters);
            log.fine("My version: " + result.myVersion);
            log.fine("Latest version on server: " + result.latestVersion);

            result.isLatestVersion = Version.isLatestVersionFromParameters(parameters);
            log.fine("Server thinks it's latest: " + result.isLatestVersion);
            log.fine("I think it's latest: " + Version.isLatestVersion(result.latestVersion, result.myVersion));

            // some people reported update dialogs with null as latest version
            if (Conversion.trim(result.latestVersion) == null)
                result.isLatestVersion = true;
        } catch (Throwable t) {
            log.severe("Cannot check for update: " + t.getMessage());
        }
        return result;
    }

    private void offerUpdate(Window window, UpdateResult result) {
        int confirm = JOptionPane.showConfirmDialog(window,
                MessageFormat.format(RouteConverter.getBundle().getString("confirm-update"), result.myVersion, result.latestVersion),
                RouteConverter.getTitle(), JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        RouteConverter.getInstance().createExternalPrograms().startBrowserForUpdate(window);
    }

    private void noUpdateAvailable(Window window) {
        JOptionPane.showMessageDialog(window,
                RouteConverter.getBundle().getString("no-update-available"),
                RouteConverter.getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }


    public void implicitCheck(final Window window) {
        if (!RouteConverter.getInstance().isAutomaticUpdateCheck())
            return;

        new Thread(new Runnable() {
            public void run() {
                final UpdateResult result = check(60);
                if (result.existsLaterVersion()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            offerUpdate(window, result);
                        }
                    });
                }
            }
        }, "UpdateChecker").start();
    }

    public void explicitCheck(Window window) {
        // synchronous call, 20 seconds timeout
        final UpdateResult result = check(20);
        if (!result.existsLaterVersion())
            noUpdateAvailable(window);
        else {
            offerUpdate(window, result);
        }
    }

    private static class UpdateResult {
        public String myVersion = "";
        public String latestVersion = "";
        public boolean isLatestVersion = true;

        public boolean existsLaterVersion() {
            return !isLatestVersion && latestVersion != null;
        }
    }
}
