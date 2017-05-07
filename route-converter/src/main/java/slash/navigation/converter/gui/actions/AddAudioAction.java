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
import slash.navigation.gui.helpers.WindowHelper;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.InputOutput.copyAndClose;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractFile;
import static slash.navigation.gui.helpers.UIHelper.createJFileChooser;

/**
 * {@link Action} that adds audio to a {@link WaypointType#Voice} waypoint.
 *
 * @author Christian Pesch
 */

public class AddAudioAction extends FrameAction {
    private static final Logger log = getLogger(AddAudioAction.class.getName());

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
        int open = chooser.showOpenDialog(WindowHelper.getFrame());
        if (open != APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null)
            return;

        r.setAddAudioPreference(selected);
        r.getPointOfInterestPanel().addAudio(position, selected);

        File track = new File(r.getConvertPanel().getUrlModel().getString());
        if (!track.exists())
            return;

        File nextToTrack = new File(track.getParentFile(), selected.getName());
        if(!nextToTrack.exists()) {
            try {
                copyAndClose(new FileInputStream(selected), new FileOutputStream(nextToTrack));
            }
            catch (IOException e) {
                log.severe(format("Could copy audio %s to %s: %s", selected, nextToTrack, e));
                showMessageDialog(r.getFrame(),
                        MessageFormat.format(RouteConverter.getBundle().getString("add-audio-error"), getLocalizedMessage(e)),
                        r.getFrame().getTitle(), ERROR_MESSAGE);

            }
        }
    }
}
