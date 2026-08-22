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

    /**
     * Wires an existing {@code ?} button to the help topic of {@code owner}. A button declared in a
     * GUI Designer form is instantiated by the generated {@code $$$setupUI$$$} method.
     */
    public void registerHelpButton(final AbstractButton button, final JComponent owner) {
        button.addActionListener(e -> openTopicForComponent(owner));
    }

    public void installF1KeyListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof java.awt.event.KeyEvent ke)) return;
            if (ke.getID() != KEY_PRESSED || ke.getKeyCode() != VK_F1) return;
            openTopicForComponent(ke.getComponent());
            ke.consume();
        }, KEY_EVENT_MASK);
    }

    /**
     * Walks {@code start} and its {@link Component#getParent()} chain, skipping any
     * {@link JLayeredPane} and any synthetic name, and returns the first usable
     * {@link Component#getName()}. Returns {@code null} when the chain is exhausted
     * without finding one.
     */
    static String resolveTopicId(Component start) {
        for (Component c = start; c != null; c = c.getParent()) {
            if (c instanceof JLayeredPane) continue;
            String name = c.getName();
            if (name != null && !name.isEmpty() && !isSyntheticName(name)) return name;
        }
        return null;
    }

    /**
     * {@link javax.swing.JRootPane} names the panes it creates after its own name:
     * "null.contentPane", "null.layeredPane", "null.glassPane". Those names sit between a
     * dialog's content and the {@link java.awt.Window} that carries the topic id, so without
     * this guard a dialog resolves to "null.contentPane" (the layeredPane variant of the same
     * bug was #303). Topic ids come from resource-bundle keys and are kebab-case, so a dot
     * never appears in a real one.
     */
    private static boolean isSyntheticName(String name) {
        return name.indexOf('.') >= 0;
    }

    private void openTopicForComponent(Component component) {
        String topic = resolveTopicId(component);
        if (topic != null) openTopic(topic); else openContents();
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
