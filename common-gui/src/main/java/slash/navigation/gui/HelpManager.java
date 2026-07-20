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

package slash.navigation.gui;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.logging.Logger;

import static java.awt.AWTEvent.KEY_EVENT_MASK;
import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.VK_F1;

/**
 * Opens web-based help pages in the system browser (specs/00030 §11).
 * Topic-id carrier is {@link Component#getName()}.
 *
 * @author Christian Pesch
 */
public class HelpManager {
    private static final Logger log = Logger.getLogger(HelpManager.class.getName());
    private static final String DRY_RUN = "help.dryRun";

    private String baseUrl;
    private String localeTag = "en";

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public void setLocale(String localeTag) {
        if (localeTag != null && !localeTag.isEmpty()) this.localeTag = localeTag;
    }

    private String resolveBaseUrl() {
        String override = System.getenv("RC_HELP_BASE_URL");
        if (override != null && !override.isEmpty()) return stripTrailingSlash(override);
        if (baseUrl != null && !baseUrl.isEmpty()) return stripTrailingSlash(baseUrl);
        return localeTag.toLowerCase().startsWith("de")
                ? "https://www.routeconverter.de" : "https://www.routeconverter.com";
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public void openTopic(String topicId) { browse(resolveBaseUrl() + "/help/" + topicId + "/"); }
    public void openContents() { browse(resolveBaseUrl() + "/help/"); }

    public JButton helpButton(final JComponent owner) {
        JButton button = new JButton("?");
        button.setName("help-button");
        button.addActionListener(e -> {
            String topic = owner.getName();
            if (topic != null && !topic.isEmpty()) openTopic(topic); else openContents();
        });
        return button;
    }

    public void installF1KeyListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof java.awt.event.KeyEvent)) return;
            java.awt.event.KeyEvent ke = (java.awt.event.KeyEvent) event;
            if (ke.getID() != KEY_PRESSED || ke.getKeyCode() != VK_F1) return;
            openTopicForComponent(ke.getComponent());
            ke.consume();
        }, KEY_EVENT_MASK);
    }

    private void openTopicForComponent(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            String name = c.getName();
            if (name != null && !name.isEmpty()) { openTopic(name); return; }
        }
        openContents();
    }

    private void browse(String url) {
        if (Boolean.getBoolean(DRY_RUN)) { System.out.println("HelpManager dry-run: " + url); return; }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(URI.create(url));
            else log.warning("Desktop browse unsupported; cannot open " + url);
        } catch (Exception e) { log.warning("Failed to open help URL " + url + ": " + e); }
    }
}
