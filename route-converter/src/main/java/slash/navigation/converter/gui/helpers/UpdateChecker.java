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

import slash.common.system.Version;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.feedback.domain.RouteFeedback;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;
import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Transfer.trim;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.initializeFeatures;
import static slash.feature.client.Feature.initializePreferences;
import static slash.navigation.converter.gui.RouteConverter.AUTOMATIC_UPDATE_CHECK_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.getPreferences;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForJava;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForUpdateCheck;

/**
 * Knows how to retrieve the information which is the latest version.
 *
 * @author Christian Pesch
 */
public class UpdateChecker {
    private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());
    private static final String START_COUNT_PREFERENCE = "startCount";
    private static final String START_TIME_PREFERENCE = "startTime";
    private RouteFeedback routeFeedback;

    static {
        getPreferences().putInt(START_COUNT_PREFERENCE, getStartCount() + 1);
        if (getPreferences().getLong(START_TIME_PREFERENCE, -1) == -1)
            getPreferences().putLong(START_TIME_PREFERENCE, currentTimeMillis());
    }

    public UpdateChecker(RouteFeedback routeFeedback) {
        this.routeFeedback = routeFeedback;
    }

    private static int getStartCount() {
        return getPreferences().getInt(START_COUNT_PREFERENCE, 0);
    }

    private static long getStartTime() {
        return getPreferences().getLong(START_TIME_PREFERENCE, currentTimeMillis());
    }

    private UpdateResult check() {
        String myRouteConverterVersion = parseVersionFromManifest().getVersion();
        String myJavaVersion = System.getProperty("java.version");
        UpdateResult result = new UpdateResult(myRouteConverterVersion, myJavaVersion);
        try {
            String parameters = routeFeedback.checkForUpdate(myRouteConverterVersion,
                    RouteConverter.getInstance().getEditionId(),
                    getStartCount(),
                    myJavaVersion,
                    System.getProperty("sun.arch.data.model"),
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    System.getProperty("os.arch"),
                    getStartTime());
            result.setParameters(parameters);
        } catch (Throwable t) {
            log.severe("Cannot check for update: " + t);
        }
        return result;
    }

    private void offerRouteConverterUpdate(Window window, UpdateResult result) {
        int confirm = showConfirmDialog(window,
                format(RouteConverter.getBundle().getString("confirm-routeconverter-update"),
                        result.getMyRouteConverterVersion(),
                        RouteConverter.getInstance().getEdition(),
                        result.getLatestRouteConverterVersion()),
                RouteConverter.getTitle(), YES_NO_OPTION);
        if (confirm == YES_OPTION)
            startBrowserForUpdateCheck(window, result.getMyRouteConverterVersion(), getStartTime());
    }

    private void noUpdateAvailable(Window window) {
        showMessageDialog(window, format(RouteConverter.getBundle().getString("no-update-available"),
                RouteConverter.getInstance().getEdition()),
                RouteConverter.getTitle(), INFORMATION_MESSAGE);
    }

    private void offerJavaUpdate(Window window, UpdateResult result) {
        int confirm = showConfirmDialog(window,
                format(RouteConverter.getBundle().getString("confirm-java-update"), result.getMyJavaVersion(), result.getLatestJavaVersion()),
                RouteConverter.getTitle(), YES_NO_OPTION);
        if (confirm == YES_OPTION)
            startBrowserForJava(window);
    }

    public void implicitCheck(final Window window) {
        if (!getPreferences().getBoolean(AUTOMATIC_UPDATE_CHECK_PREFERENCE, true))
            return;

        new Thread(new Runnable() {
            public void run() {
                final UpdateResult result = check();
                if (result.existsLaterRouteConverterVersion()) {
                    invokeLater(new Runnable() {
                        public void run() {
                            offerRouteConverterUpdate(window, result);
                        }
                    });

                } else if (result.existsLaterJavaVersion()) {
                    invokeLater(new Runnable() {
                        public void run() {
                            offerJavaUpdate(window, result);
                        }
                    });
                }
            }
        }, "UpdateChecker").start();
    }

    public void explicitCheck(Window window) {
        UpdateResult result = check();
        if (result.existsLaterRouteConverterVersion())
            offerRouteConverterUpdate(window, result);
        else
            noUpdateAvailable(window);

        if (result.existsLaterJavaVersion())
            offerJavaUpdate(window, result);
    }

    static class UpdateResult {
        private static final String ROUTECONVERTER_VERSION_KEY = "routeconverter.version";
        private static final String JAVA7_VERSION_KEY = "java7.version";
        private static final String JAVA8_VERSION_KEY = "java8.version";
        private static final String JAVA9_VERSION_KEY = "java9.version";

        private final String myRouteConverterVersion;
        private final String myJavaVersion;
        private Map<String, String> parameters = new HashMap<>();

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
            String latestRouteConverterVersion = getLatestRouteConverterVersion();
            return latestRouteConverterVersion != null &&
                    new Version(latestRouteConverterVersion).isLaterVersionThan(new Version(getMyRouteConverterVersion()));
        }

        public String getMyJavaVersion() {
            return myJavaVersion;
        }

        public String getLatestJavaVersion() {
            Version version = new Version(myJavaVersion);
            String latestVersionKey = version.isLaterVersionThan(new Version("8.9")) ? JAVA9_VERSION_KEY :
                    version.isLaterVersionThan(new Version("1.8.0")) ? JAVA8_VERSION_KEY :
                            JAVA7_VERSION_KEY;
            return getValue(latestVersionKey);
        }

        public boolean existsLaterJavaVersion() {
            String latestJavaVersion = getLatestJavaVersion();
            return latestJavaVersion != null &&
                    new Version(latestJavaVersion).isLaterVersionThan(new Version(getMyJavaVersion()));
        }

        String getValue(String key) {
            return trim(parameters.get(key));
        }

        private Map<String, String> parseParameters(String parameters) {
            StringTokenizer tokenizer = new StringTokenizer(parameters, ",");
            Map<String, String> map = new HashMap<>();
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
            initializePreferences(getPreferences());
        }
    }
}
