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

import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * {@link Action} that adds a new position list to the {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class AddPositionListAction extends FrameAction {
    private final ConvertPanel convertPanel;

    public AddPositionListAction(ConvertPanel convertPanel) {
        this.convertPanel = convertPanel;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        FormatAndRoutesModel formatAndRoutesModel = convertPanel.getFormatAndRoutesModel();
        NavigationFormat format = formatAndRoutesModel.getFormat();
        BaseRoute route = format.createRoute((RouteCharacteristics) convertPanel.getCharacteristicsModel().getSelectedItem(),
                MessageFormat.format(getBundle().getString("new-positionlist-name"), formatAndRoutesModel.getSize() + 1),
                new ArrayList<NavigationPosition>());
        formatAndRoutesModel.addPositionList(formatAndRoutesModel.getSize(), route);
        formatAndRoutesModel.setSelectedItem(route);
    }
}