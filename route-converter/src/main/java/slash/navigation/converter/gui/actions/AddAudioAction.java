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

import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.io.File;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractFile;
import static slash.navigation.gui.helpers.UIHelper.createJFileChooser;

/**
 * {@link Action} that adds audio to a {@link WaypointType#Voice} waypoint.
 *
 * @author Christian Pesch
 */

public class AddAudioAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;

    public AddAudioAction(JTable table, PositionsModel positionsModel) {
        this.table = table;
        this.positionsModel = positionsModel;
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        for (final int selectedRow : selectedRows) {
            NavigationPosition position = positionsModel.getPosition(selectedRow);
            if (!(position instanceof Wgs84Position))
                continue;
            Wgs84Position wgs84Position = (Wgs84Position) position;
            if (!(wgs84Position.getWaypointType().equals(Voice)))
                continue;

            addAudio(wgs84Position);
        }
    }

    private void addAudio(Wgs84Position position) {
        RouteConverter r = RouteConverter.getInstance();

        File file = extractFile(position);

        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("add-audio"));
        chooser.setSelectedFile(file != null ? file.getParentFile() : r.getAddAudioPreference());
        chooser.setFileSelectionMode(FILES_ONLY);
        int open = chooser.showOpenDialog(getFrame());
        if (open != APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null)
            return;

        r.setAddAudioPreference(selected);
        r.getPointOfInterestPanel().addAudio(position, selected);
    }
}
