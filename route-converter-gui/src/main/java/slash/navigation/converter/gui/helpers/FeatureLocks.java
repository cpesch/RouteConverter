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

import java.util.List;
import java.util.prefs.Preferences;

import static slash.navigation.converter.gui.panels.ConvertPanel.LOCK_CLICKED_PREFERENCE;
import static slash.navigation.converter.gui.panels.ConvertPanel.LOCK_LOGIN_PREFERENCE;
import static slash.navigation.converter.gui.panels.ConvertPanel.LOCK_SHOWN_PREFERENCE;

/**
 * Encodes the sponsor feature lock counters for the update-check.
 *
 * @author Christian Pesch
 */
public class FeatureLocks {
    private FeatureLocks() {
    }

    /**
     * Serialises how often the lock dialog of a sponsor feature was shown, its donate button
     * was clicked and its log-in prompt was used, e.g.
     * {@code "fpl-g1000:s3,c1,l0;msfs-pln:s1,c0,l0"}.
     *
     * Only features with a shown count above zero are sent - shown is the gate, so it is the
     * only event that proves a user actually hit the wall. The order follows the given list,
     * which is registry order, not the order of the preference store. Returns the empty string
     * when no lock was ever shown, so nothing is added to the update-check payload.
     *
     * @param preferences the node the lock counters are counted in
     * @param featureNames the sponsor feature ids in the order they should be sent
     * @return the encoded counters or an empty string if no lock was shown
     */
    public static String encode(Preferences preferences, List<String> featureNames) {
        StringBuilder result = new StringBuilder();
        for (String featureName : featureNames) {
            int shown = preferences.getInt(LOCK_SHOWN_PREFERENCE + featureName, 0);
            if (shown <= 0)
                continue;
            int clicked = preferences.getInt(LOCK_CLICKED_PREFERENCE + featureName, 0);
            int login = preferences.getInt(LOCK_LOGIN_PREFERENCE + featureName, 0);
            if (result.length() > 0)
                result.append(";");
            result.append(featureName).append(":s").append(shown)
                    .append(",c").append(clicked)
                    .append(",l").append(login);
        }
        return result.toString();
    }
}
