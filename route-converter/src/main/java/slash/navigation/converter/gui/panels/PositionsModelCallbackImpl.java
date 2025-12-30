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
package slash.navigation.converter.gui.panels;

import slash.navigation.common.DegreeFormat;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelCallback;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.List;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractPattern;

/**
 * Shared implementation from {@link ConvertPanel} and {@link PhotoPanel} for callbacks
 * from the {@link PositionsModel} to other RouteConverter services.
 *
 * @author Christian Pesch
 */

public class PositionsModelCallbackImpl implements PositionsModelCallback {
        public List<DegreeFormat> getDegreeFormats() {
            DegreeFormat preferred = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
            return DegreeFormat.getDegreeFormatsWithPreferredDegreeFormat(preferred);
        }
        public List<UnitSystem> getUnitSystems() {
            UnitSystem preferred = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
            return UnitSystem.getUnitSystemsWithPreferredUnitSystem(preferred);
        }
        public void handleDateTimeParseException(String stringValue, String messageBundleKey, DateFormat format) {
            showMessageDialog(RouteConverter.getInstance().getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString(messageBundleKey),
                            stringValue, extractPattern(format)), RouteConverter.getTitle(), ERROR_MESSAGE);
        }
}
