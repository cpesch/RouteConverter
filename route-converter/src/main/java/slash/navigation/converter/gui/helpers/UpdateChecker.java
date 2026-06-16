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

import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
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
import static slash.navigation.converter.gui.RouteConverter.getPreferences;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowser;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForRouteConverterForum;

/**
 * Knows how to retrieve the information which is the latest version.
 *
 * @author Christian Pesch
 */
public class UpdateChecker {
    private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());
    private static final String START_COUNT_PREFERENCE = "startCount";
    private static final String START_TIME_PREFERENCE = "startTime";
    private static final String SKIP_VERSION_PREFERENCE = "skipUpdateVersion";
    private final RouteFeedback routeFeedback;

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

    private static String getSkippedVersion() {
        return getPreferences().get(SKIP_VERSION_PREFERENCE, "");
    }

    private static String getMaxMemory() {
        return Long.toString(Runtime.getRuntime().maxMemory());
    }

    private static String getTotalMemory() {
        try {
            OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return Long.toString(bean.getTotalMemorySize());
        } catch (Throwable t) {
            return "?";
        }
    }

    private static String getScreenResolution() {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                return (int) size.getWidth() + "x" + (int) size.getHeight();
            }
        } catch (Throwable t) {
            // ignore and fall through
        }
        return "?";
    }

    private static void setSkippedVersion(String version) {
        getPreferences().put(SKIP_VERSION_PREFERENCE, version);
    }

    public UpdateResult check() {
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
                    getMaxMemory(),
                    getTotalMemory(),
                    getScreenResolution(),
                    getStartTime());
            result.setParameters(parameters);
        } catch (Throwable t) {
            log.severe("Cannot check for update: " + t.getMessage());
        }
        return result;
    }

    /**
     * A JLabel rendered as a clickable hyperlink that runs the given action when clicked.
     */
    private static JLabel createLink(String text, Runnable onClick) {
        JLabel link = new JLabel("<html><a href=\"\">" + text + "</a></html>");
        link.setAlignmentX(Component.LEFT_ALIGNMENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
        return link;
    }

    /**
     * Shows an informational dialog with the given message and a clickable link below it.
     * The link (not a Yes/No prompt) is the call to action, to foster updates.
     */
    private void showUpdateMessage(Window window, String message, String url) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("<html>" + message.replace("\n", "<br>") + "</html>"), BorderLayout.NORTH);
        panel.add(createLink(url, () -> startBrowser(window, url)), BorderLayout.SOUTH);
        showMessageDialog(window, panel, RouteConverter.getTitle(), INFORMATION_MESSAGE);
    }

    private void offerRouteConverterUpdate(Window window, UpdateResult result) {
        String latestVersion = result.getLatestRouteConverterVersion();
        String downloadUrl = routeFeedback.getUpdateCheckUrl(result.getMyRouteConverterVersion(), getStartTime());
        String message = format(RouteConverter.getBundle().getString("confirm-routeconverter-update"),
                result.getMyRouteConverterVersion(),
                RouteConverter.getInstance().getEdition(),
                latestVersion);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel messageLabel = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLink(RouteConverter.getBundle().getString("update-whats-new"),
                () -> startBrowserForRouteConverterForum(window)));
        panel.add(createLink(downloadUrl, () -> startBrowser(window, downloadUrl)));
        panel.add(Box.createVerticalStrut(10));
        JCheckBox skipVersion = new JCheckBox(format(RouteConverter.getBundle().getString("update-skip-version"), latestVersion));
        skipVersion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(skipVersion);

        showMessageDialog(window, panel, RouteConverter.getTitle(), INFORMATION_MESSAGE);

        if (skipVersion.isSelected())
            setSkippedVersion(latestVersion);
    }

    private void noUpdateAvailable(Window window) {
        showMessageDialog(window, format(RouteConverter.getBundle().getString("no-update-available"),
                RouteConverter.getInstance().getEdition()),
                RouteConverter.getTitle(), INFORMATION_MESSAGE);
    }

    private void offerJavaUpdate(Window window, UpdateResult result) {
        String message = format(RouteConverter.getBundle().getString("confirm-java-update"),
                result.getMyJavaVersion(), result.getLatestJavaVersion());
        showUpdateMessage(window, message, "https://adoptium.net/de/temurin/releases?version=17");
    }

    public void implicitCheck(final Window window) {
        new Thread(() -> {
            final UpdateResult result = check();
            if (result.existsLaterRouteConverterVersion()
                    && !result.getLatestRouteConverterVersion().equals(getSkippedVersion())) {
                invokeLater(() -> offerRouteConverterUpdate(window, result));

            } else if (result.existsLaterJavaVersion()) {
                invokeLater(() -> offerJavaUpdate(window, result));
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
        private static final String JAVA_VERSION_KEY = "java%s.version";

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
            String latestVersionKey = String.format(JAVA_VERSION_KEY, version.getMajor());
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
