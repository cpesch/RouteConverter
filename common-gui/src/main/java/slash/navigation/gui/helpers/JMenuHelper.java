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

import com.bulenkov.iconloader.IconLoader;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ToggleBooleanModelAction;
import slash.navigation.gui.models.BooleanModel;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.io.Transfer.trim;
import static slash.common.system.Platform.isMac;

/**
 * A helper for simplified {@link JMenu} operations.
 *
 * @author Christian Pesch
 */

public class JMenuHelper {
    private static String getString(String key) {
        return Application.getInstance().getContext().getBundle().getString(key);
    }

    private static String getOptionalString(String key) {
        ResourceBundle bundle = Application.getInstance().getContext().getBundle();
        return bundle.containsKey(key) ? trim(bundle.getString(key)) : null;
    }

    public static JMenu createMenu(String name) {
        JMenu menu = new JMenu(getString(name + "-menu"));
        menu.setName(name);
        String mnemonic = getOptionalString(name + "-menu-mnemonic");
        if (mnemonic != null && mnemonic.length() > 0)
            menu.setMnemonic(mnemonic.charAt(0));
        return menu;
    }

    private static int getMnemonicAmpersandIndex(String text) {
        int i = -1;
        do {
            i = text.indexOf('&', i + 1);
            if (i >= 0 && (i + 1) < text.length()) {
                // before ' '
                if (text.charAt(i + 1) == ' ') {
                    continue;
                    // before ', and after '
                } else if (text.charAt(i + 1) == '\'' && i > 0 && text.charAt(i - 1) == '\'') {
                    continue;
                }
                // ampersand is marking mnemonics
                return i;
            }
        } while (i >= 0);
        return -1;
    }

    private static void setMnemonic(AbstractButton item, char mnemonic) {
        item.setMnemonic(mnemonic);
        String text = item.getText();
        int ampersandIndex = getMnemonicAmpersandIndex(text);
        if (ampersandIndex != -1) {
            item.setText(text.substring(0, ampersandIndex) + text.substring(ampersandIndex + 1));
            item.setDisplayedMnemonicIndex(ampersandIndex);
        }
    }

    public static void setMnemonic(AbstractButton button, String key) {
        String mnemonic = getOptionalString(key);
        if (mnemonic != null && mnemonic.length() > 0)
            setMnemonic(button, mnemonic.charAt(0));
    }

    public static JMenuItem createItem(String name) {
        Action action = Application.getInstance().getContext().getActionManager().get(name);
        JMenuItem item = new JMenuItem(action);
        initializeItem(name, item);
        return item;
    }

    public static JCheckBoxMenuItem createCheckBoxItem(String name, BooleanModel booleanModel) {
        Action action = new ToggleBooleanModelAction(booleanModel);
        Application.getInstance().getContext().getActionManager().register(name, action);
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        item.setState(booleanModel.getBoolean());
        initializeItem(name, item);
        return item;
    }

    public static JRadioButtonMenuItem createRadioItem(String name) {
        Action action = Application.getInstance().getContext().getActionManager().get(name);
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
        initializeItem(name, item);
        return item;
    }

    private static void initializeItem(String name, JMenuItem item) {
        item.setName(name);
        item.setText(getString(name + "-action"));
        String tooltip = getOptionalString(name + "-action-tooltip");
        if (tooltip != null)
            item.setToolTipText(tooltip);
        setMnemonic(item, name + "-action-mnemonic");
        String keystroke = getOptionalString(name + "-action-keystroke" + (isMac() ? "-mac" : ""));
        if (keystroke != null)
            item.setAccelerator(getKeyStroke(keystroke));
        String iconUrl = getOptionalString(name + "-action-icon");
        if (iconUrl != null) {
            Icon icon = IconLoader.getIcon(iconUrl);
            item.setIcon(icon);
            item.setDisabledIcon(icon);
        }
    }

    public static void registerAction(AbstractButton component, String name) {
        String text = component.getText();
        String toolTipText = component.getToolTipText();
        Icon icon = component.getIcon();
        component.setAction(Application.getInstance().getContext().getActionManager().get(name));
        component.setText(text);
        component.setToolTipText(toolTipText);
        component.setIcon(icon);
    }

    public static void registerKeyStroke(JComponent component, String name) {
        String keystroke = getString(name + "-action-keystroke");
        component.getInputMap().put(getKeyStroke(keystroke), name);
        component.getActionMap().put(name, Application.getInstance().getContext().getActionManager().get(name));
    }

    public static JMenu findMenu(JMenuBar menuBar, String menuName) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menuName.equals(menu.getName()))
                return menu;
        }
        return null;
    }

    public static JMenu findMenu(JMenuBar menuBar, String menuName, String subMenuName) {
        return findMenuComponent(menuBar, menuName, subMenuName, JMenu.class);
    }

    public static JMenuItem findItem(JMenuBar menuBar, String menuName, String menuItemName) {
        return findMenuComponent(menuBar, menuName, menuItemName, JMenuItem.class);
    }

    public static Component findMenuComponent(JPopupMenu menu, String menuComponentName) {
        for (int i = 0; i < menu.getComponentCount(); i++) {
            Component component = menu.getComponent(i);
            if (menuComponentName.equals(component.getName()))
                return component;
        }
        return null;
    }

    private static <T extends Component> T findMenuComponent(JMenu menu, String menuComponentName, Class<T> componentClass) {
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            Component component = menu.getMenuComponent(i);
            if (menuComponentName.equals(component.getName()) && componentClass.isInstance(component))
                return componentClass.cast(component);
        }
        return null;
    }

    public static <T extends Component> T findMenuComponent(JMenuBar menuBar, String menuName, String menuComponentName, Class<T> componentClass) {
        JMenu menu = findMenu(menuBar, menuName);
        if (menu != null) {
            return findMenuComponent(menu, menuComponentName, componentClass);
        }
        return null;
    }
}
