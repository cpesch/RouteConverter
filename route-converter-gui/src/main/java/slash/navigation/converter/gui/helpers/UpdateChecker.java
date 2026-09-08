/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.helpers;

import slash.common.system.Version;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.feedback.domain.RouteFeedback;

import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static javax.swing.JOptionPane.*;
import static slash.navigation.converter.gui.helpers.UpdatePolicy.Nudge.HIGHLIGHTS_NO_SKIP;
import static slash.navigation.gui.helpers.WindowHelper.showInformation;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Transfer.trim;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.initializeFeatures;
import static slash.feature.client.Feature.initializePreferences;
import static slash.navigation.converter.gui.BaseRouteConverter.getPreferences;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowser;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForPayPal;
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
    private static final String OFFER_COUNT_PREFERENCE_PREFIX = "updateOfferCount.";
    private static final String EOL_NOTICE_SHOWN_PREFERENCE = "eolNoticeShown-2.x";
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

    private static int getOfferCount(String version) {
        return getPreferences().getInt(OFFER_COUNT_PREFERENCE_PREFIX + version, 0);
    }

    private static void incrementOfferCount(String version) {
        getPreferences().putInt(OFFER_COUNT_PREFERENCE_PREFIX + version, getOfferCount(version) + 1);
    }

    private static boolean isEolNoticeShown() {
        return getPreferences().getBoolean(EOL_NOTICE_SHOWN_PREFERENCE, false);
    }

    private static void markEolNoticeShown() {
        getPreferences().putBoolean(EOL_NOTICE_SHOWN_PREFERENCE, true);
    }

    public UpdateResult check() {
        String myRouteConverterVersion = parseVersionFromManifest().getVersion();
        String myJavaVersion = System.getProperty("java.version");
        UpdateResult result = new UpdateResult(myRouteConverterVersion, myJavaVersion);
        try {
            String parameters = routeFeedback.checkForUpdate(myRouteConverterVersion,
                    BaseRouteConverter.getInstance().getEditionId(),
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
        showUpdateMessage(window, message, url, () -> startBrowser(window, url));
    }

    private void showUpdateMessage(Window window, String message, String linkText, Runnable onLinkClick) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("<html>" + message.replace("\n", "<br>") + "</html>"), BorderLayout.NORTH);
        panel.add(createLink(linkText, onLinkClick), BorderLayout.SOUTH);
        showInformation(window, panel, BaseRouteConverter.getTitle());
    }

    private static JLabel htmlLabel(String html) {
        JLabel label = new JLabel("<html>" + html.replace("\n", "<br>") + "</html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // split out so the (possibly blocking, network-fetching) highlights lookup can be
    // resolved by the caller before the dialog is built on the EDT
    private UpdatePolicy.Nudge decideNudge(UpdateResult result) {
        String latestVersion = result.getLatestRouteConverterVersion();
        return UpdatePolicy.decide(new Version(result.getMyRouteConverterVersion()),
                new Version(latestVersion), getOfferCount(latestVersion));
    }

    private List<String> resolveHighlights(UpdateResult result, UpdatePolicy.Nudge nudge) {
        return nudge == UpdatePolicy.Nudge.SHORT ? emptyList() :
                ReleaseHighlights.first(result.getLatestRouteConverterVersion(), Locale.getDefault());
    }

    private void offerRouteConverterUpdate(Window window, UpdateResult result, UpdatePolicy.Nudge nudge, List<String> highlights) {
        String latestVersion = result.getLatestRouteConverterVersion();
        String downloadUrl = routeFeedback.getUpdateCheckUrl(result.getMyRouteConverterVersion(),
                BaseRouteConverter.getInstance().getEditionId(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Locale.getDefault());

        boolean showHighlights = !highlights.isEmpty();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (showHighlights) {
            String intro = format(BaseRouteConverter.getBundle().getString("update-highlights-intro"),
                    result.getMyRouteConverterVersion(), BaseRouteConverter.getInstance().getEdition(), latestVersion);
            panel.add(htmlLabel(intro));
            panel.add(Box.createVerticalStrut(5));
            for (String highlight : highlights)
                panel.add(htmlLabel("&#8226; " + escapeHtml(highlight)));
        } else {
            String message = format(BaseRouteConverter.getBundle().getString("confirm-routeconverter-update"),
                    result.getMyRouteConverterVersion(), BaseRouteConverter.getInstance().getEdition(), latestVersion);
            panel.add(htmlLabel(message));
        }
        panel.add(Box.createVerticalStrut(10));

        JPanel linkRow = new JPanel(new BorderLayout());
        linkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkRow.add(createLink(BaseRouteConverter.getBundle().getString("update-whats-new"),
                () -> startBrowserForRouteConverterForum(window)), BorderLayout.WEST);
        if (showHighlights) {
            JButton downloadButton = new JButton(format(BaseRouteConverter.getBundle().getString("update-download-button"), latestVersion));
            downloadButton.addActionListener(e -> startBrowser(window, downloadUrl));
            linkRow.add(downloadButton, BorderLayout.EAST);
        } else
            linkRow.add(createLink(downloadUrl, () -> startBrowser(window, downloadUrl)), BorderLayout.EAST);
        panel.add(linkRow);
        panel.add(Box.createVerticalStrut(10));

        boolean hideSkipCheckbox = showHighlights && nudge == HIGHLIGHTS_NO_SKIP;
        JCheckBox skipVersion = null;
        if (!hideSkipCheckbox) {
            skipVersion = new JCheckBox(format(BaseRouteConverter.getBundle().getString("update-skip-version"), latestVersion));
            skipVersion.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(skipVersion);
        }

        showInformation(window, panel, BaseRouteConverter.getTitle());

        if (skipVersion != null && skipVersion.isSelected())
            setSkippedVersion(latestVersion);
        incrementOfferCount(latestVersion);
    }

    private void offerEolNotice(Window window, UpdateResult result) {
        String downloadUrl = routeFeedback.getUpdateCheckUrl(result.getMyRouteConverterVersion(),
                BaseRouteConverter.getInstance().getEditionId(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Locale.getDefault());
        String message = BaseRouteConverter.getBundle().getString("eol-java8-notice");
        showUpdateMessage(window, message, downloadUrl);
    }

    private void noUpdateAvailable(Window window) {
        showInformation(window, format(BaseRouteConverter.getBundle().getString("no-update-available"),
                BaseRouteConverter.getInstance().getEdition()),
                BaseRouteConverter.getTitle());
    }

    private void offerJavaUpdate(Window window, UpdateResult result) {
        String message = format(BaseRouteConverter.getBundle().getString("confirm-java-update"),
                result.getMyJavaVersion(), result.getLatestJavaVersion());
        int javaMajor = new Version(result.getLatestJavaVersion()).getMajor();
        showUpdateMessage(window, message, "https://adoptium.net/temurin/releases/?version=" + javaMajor);
    }

    public void implicitCheck(final Window window) {
        new Thread(() -> {
            final UpdateResult result = check();
            if (result.existsLaterRouteConverterVersion()
                    && !result.getLatestRouteConverterVersion().equals(getSkippedVersion())) {
                UpdatePolicy.Nudge nudge = decideNudge(result);
                List<String> highlights = resolveHighlights(result, nudge);
                invokeLater(() -> offerRouteConverterUpdate(window, result, nudge, highlights));

            } else if (result.existsLaterJavaVersion()) {
                invokeLater(() -> offerJavaUpdate(window, result));
            }

            if (UpdatePolicy.isEndOfLife(new Version(result.getMyRouteConverterVersion())) && !isEolNoticeShown()) {
                markEolNoticeShown();
                invokeLater(() -> offerEolNotice(window, result));
            }
        }, "UpdateChecker").start();
    }

    public void checkSupportNudge(Window window) {
        Integer threshold = SupportNudge.thresholdToShow(getStartCount(), getPreferences());
        if (threshold == null)
            return;

        SupportNudge.markShown(threshold, getPreferences());
        invokeLater(() -> showSupportNudge(window, threshold));
    }

    private void showSupportNudge(Window window, int threshold) {
        String message = format(BaseRouteConverter.getBundle().getString("support-nudge-message"), threshold);
        showUpdateMessage(window, message, BaseRouteConverter.getBundle().getString("about-routeconverter-support-paypal"),
                () -> startBrowserForPayPal(window));
    }

    public void explicitCheck(Window window) {
        UpdateResult result = check();
        if (result.existsLaterRouteConverterVersion()) {
            UpdatePolicy.Nudge nudge = decideNudge(result);
            offerRouteConverterUpdate(window, result, nudge, resolveHighlights(result, nudge));
        } else
            noUpdateAvailable(window);

        if (result.existsLaterJavaVersion())
            offerJavaUpdate(window, result);
    }

    static class SupportNudge {
        static final String NUDGE_100_SHOWN_PREFERENCE = "supportNudge100Shown";
        static final String NUDGE_500_SHOWN_PREFERENCE = "supportNudge500Shown";

        private SupportNudge() {
        }

        private static String shownPreferenceFor(int threshold) {
            return threshold >= 500 ? NUDGE_500_SHOWN_PREFERENCE : NUDGE_100_SHOWN_PREFERENCE;
        }

        // higher threshold wins if a run happens to cross both at once, so at most one dialog shows per start
        static Integer thresholdToShow(int startCount, Preferences preferences) {
            for (int threshold : new int[]{500, 100}) {
                if (startCount >= threshold && !preferences.getBoolean(shownPreferenceFor(threshold), false))
                    return threshold;
            }
            return null;
        }

        static void markShown(int threshold, Preferences preferences) {
            preferences.putBoolean(shownPreferenceFor(threshold), true);
        }
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
