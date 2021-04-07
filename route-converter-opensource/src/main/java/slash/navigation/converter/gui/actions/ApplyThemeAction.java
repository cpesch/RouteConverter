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

package slash.navigation.converter.gui.actions;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import javax.swing.*;

import static java.text.MessageFormat.format;

/**
 * {@link Action} that applies a {@link LocalTheme} from the {@link MapsforgeMapManager}.
 *
 * @author Christian Pesch
 */

public class ApplyThemeAction extends DialogAction {
    private final JTable table;
    private final MapsforgeMapManager mapManager;

    public ApplyThemeAction(JDialog dialog, JTable table, MapsforgeMapManager mapManager) {
        super(dialog);
        this.table = table;
        this.mapManager = mapManager;
    }

    private Action getAction() {
        return Application.getInstance().getContext().getActionManager().get("show-themes");
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    public void run() {
        int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
        if (selectedRow == -1)
            return;
        LocalTheme theme = mapManager.getAvailableThemesModel().getItem(selectedRow);
        mapManager.getAppliedThemeModel().setItem(theme);
        getNotificationManager().showNotification(format(RouteConverter.getBundle().getString("theme-applied"), theme.getDescription()), getAction());

    }
}
