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

import slash.common.io.Transfer;
import slash.common.io.Version;
import slash.navigation.feedback.domain.RouteFeedback;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static javax.swing.JOptionPane.*;
import static slash.common.io.Version.parseVersionFromManifest;
import static slash.common.io.Version.parseVersionFromParameters;

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
        UpdateResult result = new UpdateResult();
        try {
            result.myVersion = parseVersionFromManifest().getVersion();

            String parameters = routeFeedback.checkForUpdate(result.myVersion, getStartCount(),
                    System.getProperty("java.version"),
                    System.getProperty("os.arch"),
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    System.getProperty("javawebstart.version"),
                    getStartTime());

            result.latestVersion = parseVersionFromParameters(parameters);
            log.fine("My version: " + result.myVersion);
            log.fine("Latest version on server: " + result.latestVersion);

            result.isLatestVersion = Version.isLatestVersionFromParameters(parameters);
            log.fine("Server thinks it's latest: " + result.isLatestVersion);
            result.isLatestVersion = new Version(result.myVersion).isLaterVersionThan(new Version(result.latestVersion));
            log.fine("I think it's latest: " + result.isLatestVersion);

            // some people reported update dialogs with null as latest version
            if (Transfer.trim(result.latestVersion) == null)
                result.isLatestVersion = true;
        } catch (Throwable t) {
            log.severe("Cannot check for update: " + t.getMessage());
        }
        return result;
    }

    private void offerUpdate(Window window, UpdateResult result) {
        int confirm = showConfirmDialog(window,
                MessageFormat.format(RouteConverter.getBundle().getString("confirm-update"), result.myVersion, result.latestVersion),
                RouteConverter.getTitle(), YES_NO_OPTION);
        if (confirm == YES_OPTION)
            new ExternalPrograms().startBrowserForUpdateCheck(window, result.myVersion, getStartTime());
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
        UpdateResult result = check();
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
