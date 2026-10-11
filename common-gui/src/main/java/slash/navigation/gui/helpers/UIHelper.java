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
package slash.navigation.gui.helpers;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.WAIT_CURSOR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.prefs.Preferences.userNodeForPackage;
import static slash.common.system.Platform.*;

/**
 * Helpers used throughout the UI
 *
 * @author Christian Pesch
 */

public class UIHelper {
    private static final Preferences preferences = userNodeForPackage(UIHelper.class);
    private static final String LOOK_AND_FEEL_CLASS_PREFERENCE = "lookAndFeelClass";
    public static final String AUTOMATIC_LOOK_AND_FEEL = "default";
    public static final String LIGHT_LOOK_AND_FEEL = "light";
    public static final String DARK_LOOK_AND_FEEL = "dark";
    public static final String SYSTEM_LOOK_AND_FEEL = "system";
    private static final Pattern WINDOWS_APPS_DARK_PATTERN = Pattern.compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x0+\\s*$", Pattern.MULTILINE);

    public static String getLookAndFeelPreference() {
        return preferences.get(LOOK_AND_FEEL_CLASS_PREFERENCE, AUTOMATIC_LOOK_AND_FEEL);
    }

    public static void setLookAndFeelPreference(String lookAndFeel) {
        preferences.put(LOOK_AND_FEEL_CLASS_PREFERENCE, lookAndFeel);
    }

    /**
     * Maps the look-and-feel preference to a look-and-feel class: FlatLaf light or dark
     * following the operating system on Windows and Linux, the system look-and-feel (Aqua)
     * on macOS, FlatLaf light or dark on request, the system look-and-feel on request, or
     * any other value as a look-and-feel class name.
     */
    static String getLookAndFeelClassName(String preference, boolean mac, BooleanSupplier operatingSystemDark,
                                          String systemLookAndFeelClassName) {
        return switch (preference) {
            case AUTOMATIC_LOOK_AND_FEEL -> mac ? systemLookAndFeelClassName :
                    getFlatLookAndFeelClassName(false, operatingSystemDark.getAsBoolean());
            case LIGHT_LOOK_AND_FEEL -> getFlatLookAndFeelClassName(mac, false);
            case DARK_LOOK_AND_FEEL -> getFlatLookAndFeelClassName(mac, true);
            case SYSTEM_LOOK_AND_FEEL -> systemLookAndFeelClassName;
            default -> preference;
        };
    }

    private static String getFlatLookAndFeelClassName(boolean mac, boolean dark) {
        if (mac)
            return dark ? FlatMacDarkLaf.class.getName() : FlatMacLightLaf.class.getName();
        return dark ? FlatDarkLaf.class.getName() : FlatLightLaf.class.getName();
    }

    private static boolean isOperatingSystemDark() {
        if (isWindows())
            return isWindowsAppsDark(execute("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"));
        if (isLinux())
            return isGnomeDark(execute("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"),
                    execute("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"));
        return false;
    }

    static boolean isWindowsAppsDark(String regQueryOutput) {
        return WINDOWS_APPS_DARK_PATTERN.matcher(regQueryOutput).find();
    }

    static boolean isGnomeDark(String colorScheme, String gtkTheme) {
        return colorScheme.contains("prefer-dark") || gtkTheme.toLowerCase(Locale.ROOT).contains("dark");
    }

    private static String execute(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(2, SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), UTF_8);
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    public static void setLookAndFeel() {
        // to get menus out of the window and into the menu bar
        // https://developer.apple.com/library/content/documentation/Java/Conceptual/Java14Development/04-JavaUIToolkits/JavaUIToolkits.html
        if(isMac())
            System.setProperty("apple.laf.useScreenMenuBar", "true");

        // override what the JRE reads from the user's desktop settings as the user's desktop anti-aliased text preferences
        // https://docs.oracle.com/javase/7/docs/technotes/guides/2d/flags.html#aaFonts
        if (isLinux())
            System.setProperty("awt.useSystemAAFontSettings", "lcd");

        // enable the XRender-based Java 2D rendering pipeline for modern X11-based desktops, offering improved graphics performance
        // https://docs.oracle.com/javase/7/docs/technotes/guides/2d/flags.html#xrender
        if (isLinux())
            System.setProperty("sun.java2d.xrender", "true");

        try {
            String lookAndFeelClass = getLookAndFeelClassName(getLookAndFeelPreference(), isMac(),
                    UIHelper::isOperatingSystemDark, UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(lookAndFeelClass);
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException | RuntimeException e) {
            // intentionally do nothing
        }
        // FlatLaf draws its own title bars on Linux; on Windows it uses the native window decorations
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    public static boolean isDarkLookAndFeel() {
        return UIManager.getLookAndFeel() instanceof FlatLaf && FlatLaf.isLafDark();
    }

    public static void startWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        startWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(true);
    }

    private static void startWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(WAIT_CURSOR));
    }

    public static void stopWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        stopWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(false);
    }

    public static void stopWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(DEFAULT_CURSOR));
    }

    public static JFileChooser createJFileChooser() {
        if(isWindows()) {
            // workaround for https://bugs.openjdk.java.net/browse/JDK-8179014
            UIManager.put("FileChooser.useSystemExtensionHiding", false);
        }
        return new JFileChooser();
    }

    /**
     * A JLabel rendered as a clickable hyperlink that runs the given action when clicked.
     */
    public static JLabel createLinkLabel(String text, Runnable onClick) {
        JLabel link = new JLabel("<html><a href=\"\">" + text + "</a></html>");
        link.setAlignmentX(Component.LEFT_ALIGNMENT);
        link.setCursor(Cursor.getPredefinedCursor(HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
        return link;
    }

    public static File chooseDirectory(Component parent, String title, String currentPath) {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(title);
        if (currentPath != null && !currentPath.isEmpty())
            chooser.setSelectedFile(new File(currentPath));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
            return null;
        File selected = chooser.getSelectedFile();
        return selected == null || selected.getName().isEmpty() ? null : selected;
    }

    public static File chooseFile(Component parent, String title, String currentPath) {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(title);
        if (currentPath != null && !currentPath.isEmpty())
            chooser.setSelectedFile(new File(currentPath));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
            return null;
        File selected = chooser.getSelectedFile();
        return selected == null || selected.getName().isEmpty() ? null : selected;
    }

    public static void patchUIManager(ResourceBundle bundle, String... keys) {
        for (String key : keys) {
            try {
                if (bundle.containsKey(key))
                    UIManager.getDefaults().put(key, bundle.getString(key));
            } catch (MissingResourceException e) {
                // intentionally left empty
            }
        }
    }

    private static FontMetrics fontMetrics;

    public static int getMaxWidth(String string, int extraWidth) {
        if (fontMetrics == null) {
            JLabel label = new JLabel();
            fontMetrics = label.getFontMetrics(label.getFont());
        }
        int width = fontMetrics.stringWidth(string);
        return width + extraWidth;
    }
}
