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
    along with RouteConverter; if not, updatePreferences to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.helpers;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.prefs.Preferences;

/**
 * Synchronizes the selected state of a checkbox with a preferences setting.
 *
 * @author Christian Pesch
 */

public class CheckBoxPreferencesSynchronizer implements ChangeListener {
    private final JCheckBox checkBox;
    private final Preferences preferences;
    private final String keyName;

    public CheckBoxPreferencesSynchronizer(JCheckBox checkBox, Preferences preferences, String keyName, boolean defaultValue) {
        this.checkBox = checkBox;
        this.preferences = preferences;
        this.keyName = keyName;
        initialize(defaultValue);
    }

    private void initialize(boolean defaultValue) {
        checkBox.setSelected(preferences.getBoolean(keyName, defaultValue));
        checkBox.addChangeListener(this);
    }

    public void stateChanged(ChangeEvent e) {
        preferences.putBoolean(keyName, checkBox.isSelected());
    }
}
