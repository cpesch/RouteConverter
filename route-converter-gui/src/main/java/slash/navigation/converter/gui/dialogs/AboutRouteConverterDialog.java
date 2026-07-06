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

package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.common.system.Platform;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.feature.client.Feature.getFeature;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.*;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog to show information about the program.
 *
 * @author Christian Pesch
 */

public class AboutRouteConverterDialog extends SimpleDialog {
    private JPanel contentPane;
    private JLabel labelAbout;
    private JLabel labelContact;
    private JLabel labelResources;
    private JLabel labelUserNameCaption;
    private JLabel labelUserName;
    private JLabel labelFeatureCaption;
    private JLabel labelFeature;
    private JButton buttonClose;
    private JLabel labelRouteConverterVersion;
    private JLabel labelJavaVersion;
    private JLabel labelOperatingSystem;
    private JButton buttonCopySystemInfo;
    private JLabel labelReportProblem;
    private JLabel labelLogo;

    public AboutRouteConverterDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "about");
        setTitle(BaseRouteConverter.getBundle().getString("about-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        labelLogo.setIcon(new ImageIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/RouteConverter.png"))
                .getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH)));

        final BaseRouteConverter r = BaseRouteConverter.getInstance();

        enableLink(labelAbout, () -> startBrowserForRouteConverter(r.getFrame()));
        enableLink(labelResources, () -> startBrowserForRouteConverterResources(r.getFrame()));
        enableLink(labelContact, () -> startBrowserForRouteConverterForum(r.getFrame()));

        String username = BaseRouteConverter.getInstance().getUserNamePreference();
        if (username != null) {
            labelUserNameCaption.setText(captionOf("logged-in-as"));
            labelUserName.setText(username);
            labelUserNameCaption.setVisible(true);
            labelUserName.setVisible(true);
        }

        String featuredTo = getFeature("featured-to");
        if (featuredTo != null) {
            labelFeatureCaption.setText(captionOf("featured-to"));
            labelFeature.setText(maskEmailAddresses(featuredTo));
            labelFeatureCaption.setVisible(true);
            labelFeature.setVisible(true);
        }

        labelRouteConverterVersion.setText(BaseRouteConverter.getTitle());
        labelJavaVersion.setText(Platform.getJava());
        labelOperatingSystem.setText(Platform.getPlatform());

        labelReportProblem.setText("<html><a href=\"\">" +
                BaseRouteConverter.getBundle().getString("about-report-problem") + "</a></html>");
        enableLink(labelReportProblem, () -> new SendErrorReportDialog().showWithPreferences());

        buttonCopySystemInfo.addActionListener(new DialogAction(this) {
            public void run() {
                copySystemInfo();
            }
        });

        setMnemonic(buttonClose, "close-mnemonic");
        buttonClose.addActionListener(new DialogAction(this) {
            public void run() {
                close();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        contentPane.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                close();
            }
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void close() {
        dispose();
    }

    /**
     * The caption part of a "Caption: {0}" bundle string, so the caption can sit in one
     * column and its value in the next, aligned with the version/Java/OS rows.
     */
    private static String captionOf(String key) {
        String pattern = BaseRouteConverter.getBundle().getString(key);
        int index = pattern.indexOf("{0}");
        return (index != -1 ? pattern.substring(0, index) : pattern).trim();
    }

    /**
     * Turns a JLabel into a keyboard-accessible, clickable link: hand cursor, focusable,
     * and activated by mouse click or Enter/Space.
     */
    private static void enableLink(JLabel label, Runnable action) {
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFocusable(true);
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        label.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)
                    action.run();
            }
        });
    }

    private static void copySystemInfo() {
        String info = BaseRouteConverter.getTitle() + "\n" + Platform.getJava() + "\n" + Platform.getPlatform();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(info), null);
    }

    /**
     * Masks the local part of any e-mail address (keeps the first character and the domain)
     * so it is not fully exposed in screenshots or screen-shares.
     */
    static String maskEmailAddresses(String text) {
        return text.replaceAll("([\\w.+-])[\\w.+-]*(@[\\w.-]+)", "$1***$2");
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), 20, -1));
        labelLogo = new JLabel();
        contentPane.add(labelLogo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelAbout = new JLabel();
        this.$$$loadLabelText$$$(labelAbout, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-routeconverter"));
        panel1.add(labelAbout, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelContact = new JLabel();
        this.$$$loadLabelText$$$(labelContact, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-routeconverter-contact"));
        panel1.add(labelContact, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(3, 0, 1, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "close"));
        panel2.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelUserNameCaption = new JLabel();
        labelUserNameCaption.setVisible(false);
        panel3.add(labelUserNameCaption, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelUserName = new JLabel();
        labelUserName.setText("");
        labelUserName.setVisible(false);
        panel3.add(labelUserName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFeatureCaption = new JLabel();
        labelFeatureCaption.setVisible(false);
        panel3.add(labelFeatureCaption, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFeature = new JLabel();
        labelFeature.setVisible(false);
        panel3.add(labelFeature, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-routeconverter-version"));
        panel3.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelRouteConverterVersion = new JLabel();
        labelRouteConverterVersion.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        labelRouteConverterVersion.setText("?");
        panel3.add(labelRouteConverterVersion, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-java-version"));
        panel3.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelJavaVersion = new JLabel();
        labelJavaVersion.setText("?");
        panel3.add(labelJavaVersion, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-os-version"));
        panel3.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelOperatingSystem = new JLabel();
        labelOperatingSystem.setText("?");
        panel3.add(labelOperatingSystem, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(6, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCopySystemInfo = new JButton();
        this.$$$loadButtonText$$$(buttonCopySystemInfo, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-copy-system-info"));
        panel4.add(buttonCopySystemInfo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelReportProblem = new JLabel();
        this.$$$loadLabelText$$$(labelReportProblem, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-report-problem"));
        panel4.add(labelReportProblem, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        labelResources = new JLabel();
        this.$$$loadLabelText$$$(labelResources, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "about-routeconverter-resources"));
        panel1.add(labelResources, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    /**
     * @noinspection ALL
     */
    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
