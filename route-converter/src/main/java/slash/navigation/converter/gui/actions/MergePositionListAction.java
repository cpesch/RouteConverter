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
import slash.navigation.converter.gui.helpers.AbstractListDataListener;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;
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
    private JTable table;
    private BaseRoute sourceRoute;
    private FormatAndRoutesModel formatAndRoutesModel;
    private ActionEnabler actionEnabler = new ActionEnabler();

    public MergePositionListAction(JTable table, BaseRoute sourceRoute, FormatAndRoutesModel formatAndRoutesModel) {
        this.table = table;
        this.sourceRoute = sourceRoute;
        this.formatAndRoutesModel = formatAndRoutesModel;
        initialize();
    }

    protected void initialize() {
        setEnabled();
        formatAndRoutesModel.addListDataListener(actionEnabler);
    }

    private void setEnabled() {
        setEnabled(!sourceRoute.equals(formatAndRoutesModel.getSelectedRoute()));
    }

    public void dispose() {
        formatAndRoutesModel.removeListDataListener(actionEnabler);
        this.actionEnabler = null;
        this.table = null;
        this.sourceRoute = null;
        this.formatAndRoutesModel = null;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        int selectedRow = Math.min(table.getSelectedRow() + 1, table.getRowCount());
        formatAndRoutesModel.getPositionsModel().add(selectedRow, sourceRoute.getPositions());
        formatAndRoutesModel.removePositionList(sourceRoute);
    }

    private class ActionEnabler extends AbstractListDataListener {
        public void process(ListDataEvent e) {
            setEnabled();
        }
    }
}