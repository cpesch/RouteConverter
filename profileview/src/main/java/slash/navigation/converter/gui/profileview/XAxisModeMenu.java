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

package slash.navigation.converter.gui.profileview;

import slash.navigation.converter.gui.models.ProfileModeModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static slash.navigation.gui.helpers.JMenuHelper.createRadioItem;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;

/**
 * Creates a {@link JMenu} for {@link XAxisMode}.
 *
 * @author Christian Pesch
 */

public class XAxisModeMenu {
    private final ProfileModeModel profileModeModel;

    public XAxisModeMenu(JMenuBar menuBar, ProfileModeModel profileModeModel) {
        this(findMenu(menuBar, "view", "show-profile-x-axis"), profileModeModel);
    }

    public XAxisModeMenu(JMenu menu, ProfileModeModel profileModeModel) {
        this.profileModeModel = profileModeModel;
        initializeMenu(menu);
    }

    private void initializeMenu(JMenu menu) {
        ButtonGroup buttonGroup = new ButtonGroup();
        for (XAxisMode mode : XAxisMode.values()) {
            JRadioButtonMenuItem menuItem = createRadioItem("show-" + mode.name().toLowerCase());
            profileModeModel.addChangeListener(new XAxisModeListener(menuItem, mode));
            buttonGroup.add(menuItem);
            menu.add(menuItem);
        }
    }

    private class XAxisModeListener implements ChangeListener {
        private JRadioButtonMenuItem menuItem;
        private XAxisMode mode;

        public XAxisModeListener(JRadioButtonMenuItem menuItem, XAxisMode mode) {
            this.menuItem = menuItem;
            this.mode = mode;
            updateSelected();
        }

        public void stateChanged(ChangeEvent e) {
            updateSelected();
        }

        private void updateSelected() {
            if (mode.equals(profileModeModel.getXAxisMode()))
                menuItem.setSelected(true);
        }
    }
}
