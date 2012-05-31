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

import slash.common.system.Version;
import slash.navigation.feedback.domain.RouteFeedback;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.system.Version.getLatestJavaVersion;
import static slash.common.system.Version.getLatestRouteConverterVersion;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.navigation.converter.gui.helper.ExternalPrograms.startBrowserForUpdateCheck;

/**
 * Knows how to retrieve the information which is the latest version.
 *
 * @author Christian Pesch
 */
public class UpdateChecker {
    private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(UpdateChecker.class);
    private static final String START_COUNT_PREFERENCE = "startCount";
    private static final String START_TIME_PREFERENCE = "startTime";
    private RouteFeedback routeFeedback;

    static {
        preferences.putInt(START_COUNT_PREFERENCE, getStartCount() + 1);
        if(preferences.getLong(START_TIME_PREFERENCE, -1) == -1)
            preferences.putLong(START_TIME_PREFERENCE, currentTimeMillis());
    }

    public UpdateChecker(RouteFeedback routeFeedback) {
        this.routeFeedback = routeFeedback;
    }

    private static int getStartCount() {
        return preferences.getInt(START_COUNT_PREFERENCE, 0);
    }

    private static long getStartTime() {
        return preferences.getLong(START_TIME_PREFERENCE, currentTimeMillis());
    }

    private UpdateResult check() {
        String myRouteConverterVersion = parseVersionFromManifest().getVersion();
        String myJavaVersion = System.getProperty("java.version");
        UpdateResult result = new UpdateResult(myRouteConverterVersion, myJavaVersion);
        try {
            String parameters = routeFeedback.checkForUpdate(myRouteConverterVersion,
                    parseVersionFromManifest().getBits(),
                    getStartCount(),
                    myJavaVersion,
                    System.getProperty("sun.arch.data.model"),
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    System.getProperty("os.arch"),
                    System.getProperty("javawebstart.version"),
                    getStartTime());

            String latestRouteConverterVersion = getLatestRouteConverterVersion(parameters);
            String latestJavaVersion = getLatestJavaVersion(parameters);
            boolean isLatestRouteConverterVersion = new Version(myRouteConverterVersion).isLaterVersionThan(new Version(latestRouteConverterVersion));
            boolean isLatestJavaVersion = new Version(myJavaVersion).isLaterVersionThan(new Version(latestJavaVersion));
            result.setResult(latestRouteConverterVersion, isLatestRouteConverterVersion, latestJavaVersion, isLatestJavaVersion);

            log.fine("My RouteConverter version: " + myRouteConverterVersion);
            log.fine("Latest RouteConverter version: " + latestRouteConverterVersion);
            log.fine("Is latest RouteConverter version: " + isLatestRouteConverterVersion);
            log.fine("My Java version: " + myJavaVersion);
            log.fine("Latest Java version: " + latestJavaVersion);
            log.fine("Is latest Java version: " + isLatestJavaVersion);
        } catch (Throwable t) {
            log.severe("Cannot check for update: " + t.getMessage());
        }
        return result;
    }

    private void offerUpdate(Window window, UpdateResult result) {
        int confirm = showConfirmDialog(window,
                MessageFormat.format(RouteConverter.getBundle().getString("confirm-update"), result.getMyRouteConverterVersion(), result.getLatestRouteConverterVersion()),
                RouteConverter.getTitle(), YES_NO_OPTION);
        if (confirm == YES_OPTION)
            startBrowserForUpdateCheck(window, result.getMyRouteConverterVersion(), getStartTime());
    }

    private void noUpdateAvailable(Window window) {
        showMessageDialog(window,
                RouteConverter.getBundle().getString("no-update-available"),
                RouteConverter.getTitle(), INFORMATION_MESSAGE);
    }


    public void implicitCheck(final Window window) {
        if (!RouteConverter.getInstance().isAutomaticUpdateCheck())
            return;

        new Thread(new Runnable() {
            public void run() {
                final UpdateResult result = check();
                if (result.existsLaterRouteConverterVersion()) {
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
        UpdateResult result = check();
        if (!result.existsLaterRouteConverterVersion())
            noUpdateAvailable(window);
        else {
            offerUpdate(window, result);
        }
    }

    private static class UpdateResult {
        private final String myRouteConverterVersion;
        private String latestRouteConverterVersion = "";
        private boolean isLatestRouteConverterVersion = true;
        private final String myJavaVersion;
        private String latestJavaVersion = "";
        private boolean isLatestJavaVersion = true;

        public UpdateResult(String myRouteConverterVersion, String myJavaVersion) {
            this.myRouteConverterVersion = myRouteConverterVersion;
            this.myJavaVersion = myJavaVersion;
        }

        public String getMyRouteConverterVersion() {
            return myRouteConverterVersion;
        }

        public String getLatestRouteConverterVersion() {
            return latestRouteConverterVersion;
        }

        public boolean existsLaterRouteConverterVersion() {
            return !isLatestRouteConverterVersion;
        }

        public String getMyJavaVersion() {
            return myJavaVersion;
        }

        public String getLatestJavaVersion() {
            return latestJavaVersion;
        }

        public boolean existsLaterJavaVersion() {
            return !isLatestJavaVersion;
        }

        public void setResult(String latestRouteConverterVersion, boolean isLatestRouteConverterVersion, String latestJavaVersion, boolean isLatestJavaVersion) {
            this.latestRouteConverterVersion = latestRouteConverterVersion;
            this.isLatestRouteConverterVersion = isLatestRouteConverterVersion;
            this.latestJavaVersion = latestJavaVersion;
            this.isLatestJavaVersion = isLatestJavaVersion;
        }
    }
}
