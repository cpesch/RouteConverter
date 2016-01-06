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

import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.UrlDocument;
import slash.navigation.gui.actions.FrameAction;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.navigation.base.WaypointType.Voice;

/**
 * {@link Action} that plays the voice of a {@link slash.navigation.base.WaypointType#Voice} waypoint.
 *
 * @author Christian Pesch
 */

public class PlayVoiceAction extends FrameAction {
    private static final Logger log = Logger.getLogger(PlayVoiceAction.class.getName());
    private final JTable table;
    private final PositionsModel positionsModel;
    private final UrlDocument urlModel;

    public PlayVoiceAction(JTable table, PositionsModel positionsModel, UrlDocument urlModel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.urlModel = urlModel;
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
            File file = new File(urlModel.getString());
            if (!file.exists())
                continue;

            File voice = new File(file.getParentFile(), wgs84Position.getDescription() + ".wav");
            try {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(voice);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } catch (Exception e) {
                log.severe(format("Cannot play voice %s: %s", voice, getLocalizedMessage(e)));
                showMessageDialog(getFrame(),
                        MessageFormat.format(RouteConverter.getBundle().getString("cannot-play-voice"), voice, e), getFrame().getTitle(),
                        ERROR_MESSAGE);
            }
        }
    }
}
