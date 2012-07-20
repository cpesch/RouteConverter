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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.io.Transfer.trim;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.initializeFeatures;
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
        if (preferences.getLong(START_TIME_PREFERENCE, -1) == -1)
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
            result.setParameters(parameters);
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

    static class UpdateResult {
        private static final String ROUTECONVERTER_VERSION_KEY = "routeconverter.version";
        private static final String JAVA6_VERSION_KEY = "java6.version";
        private static final String JAVA7_VERSION_KEY = "java7.version";

        private final String myRouteConverterVersion;
        private final String myJavaVersion;
        private Map<String, String> parameters;

        public UpdateResult(String myRouteConverterVersion, String myJavaVersion) {
            this.myRouteConverterVersion = myRouteConverterVersion;
            this.myJavaVersion = myJavaVersion;
        }

        public String getMyRouteConverterVersion() {
            return myRouteConverterVersion;
        }

        public String getLatestRouteConverterVersion() {
            return getValue(ROUTECONVERTER_VERSION_KEY);
        }

        public boolean existsLaterRouteConverterVersion() {
            boolean isLatestRouteConverterVersion = new Version(myRouteConverterVersion).isLaterVersionThan(new Version(getLatestRouteConverterVersion()));
            return !isLatestRouteConverterVersion;
        }

        public String getMyJavaVersion() {
            return myJavaVersion;
        }

        public String getLatestJava6Version() {
            return getValue(JAVA6_VERSION_KEY);
        }

        public String getLatestJava7Version() {
            return getValue(JAVA7_VERSION_KEY);
        }

        public boolean existsLaterJavaVersion() {
            String latestJavaVersion = new Version(myJavaVersion).isLaterVersionThan(new Version("1.7.0")) ? getLatestJava7Version() : getLatestJava6Version();
            return myJavaVersion.compareTo(latestJavaVersion) < 0;
        }

        String getValue(String key) {
            return trim(parameters.get(key));
        }

        private Map<String, String> parseParameters(String parameters) {
            StringTokenizer tokenizer = new StringTokenizer(parameters, ",");
            Map<String, String> map = new HashMap<String, String>();
            while (tokenizer.hasMoreTokens()) {
                String nv = tokenizer.nextToken();
                StringTokenizer nvTokenizer = new StringTokenizer(nv, "=");
                if (!nvTokenizer.hasMoreTokens())
                    continue;
                String key = nvTokenizer.nextToken();
                if (!nvTokenizer.hasMoreTokens())
                    continue;
                String value = nvTokenizer.nextToken();
                map.put(key, value);
            }
            return map;
        }

        public void setParameters(String parameters) {
            this.parameters = parseParameters(parameters);
            initializeFeatures(getValue("features"));
        }
    }
}
