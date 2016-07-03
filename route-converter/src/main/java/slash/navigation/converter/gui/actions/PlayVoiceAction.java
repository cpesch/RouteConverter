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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.UrlDocument;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.io.File;

import static slash.navigation.converter.gui.helpers.PositionHelper.extractFile;

/**
 * {@link Action} that plays the voice of a {@link WaypointType#Voice} waypoint.
 *
 * @author Christian Pesch
 */

public class PlayVoiceAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final UrlDocument urlModel;

    public PlayVoiceAction(JTable table, PositionsModel positionsModel, UrlDocument urlModel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.urlModel = urlModel;
    }

    public void run() {
        File file = new File(urlModel.getString());
        if (!file.exists())
            return;

        int[] selectedRows = table.getSelectedRows();
        for (final int selectedRow : selectedRows) {
            NavigationPosition position = positionsModel.getPosition(selectedRow);
            File voice = extractFile(position);
            if(voice == null)
                continue;
            RouteConverter.getInstance().getAudioPlayer().play(voice);
        }
    }
}
