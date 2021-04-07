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
import slash.navigation.gui.helpers.AbstractListDataListener;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import java.awt.event.ActionListener;

/**
 * {@link ActionListener} that inserts the position list of a {@link PositionsModel} at
 * the selected row of a {@link JTable} and removes it from the {@link FormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

public class MergePositionListAction extends FrameAction {
    private final ConvertPanel convertPanel;
    private BaseRoute sourceRoute;
    private ActionEnabler actionEnabler = new ActionEnabler();

    public MergePositionListAction(ConvertPanel convertPanel, BaseRoute sourceRoute) {
        this.convertPanel = convertPanel;
        this.sourceRoute = sourceRoute;
        initialize();
    }

    protected void initialize() {
        setEnabled();
        convertPanel.getFormatAndRoutesModel().addListDataListener(actionEnabler);
    }

    private void setEnabled() {
        setEnabled(!sourceRoute.equals(convertPanel.getFormatAndRoutesModel().getSelectedRoute()));
    }

    public void dispose() {
        convertPanel.getFormatAndRoutesModel().removeListDataListener(actionEnabler);
        this.actionEnabler = null;
        this.sourceRoute = null;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        JTable table = convertPanel.getPositionsView();
        int selectedRow = Math.min(table.getSelectedRow() + 1, table.getRowCount());
        convertPanel.getPositionsModel().add(selectedRow, sourceRoute.getPositions());
        convertPanel.getFormatAndRoutesModel().removePositionList(sourceRoute);
    }

    private class ActionEnabler extends AbstractListDataListener {
        public void process(ListDataEvent e) {
            setEnabled();
        }
    }
}
