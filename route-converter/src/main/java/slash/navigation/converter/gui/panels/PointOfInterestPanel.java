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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.AddAudioAction;
import slash.navigation.converter.gui.actions.DeletePositionAction;
import slash.navigation.converter.gui.actions.PlayVoiceAction;
import slash.navigation.converter.gui.helpers.PointsOfInterestTableHeaderMenu;
import slash.navigation.converter.gui.helpers.PointsOfInterestTablePopupMenu;
import slash.navigation.converter.gui.models.FilteringPositionsModel;
import slash.navigation.converter.gui.models.PointsOfInterestTableColumnModel;
import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.predicates.PointOfInterestPositionPredicate;
import slash.navigation.converter.gui.renderer.DescriptionColumnTableCellEditor;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.io.File;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_DELETE;
import static java.lang.Integer.MAX_VALUE;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.navigation.converter.gui.models.LocalActionConstants.POINTS_OF_INTEREST;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.PHOTO_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.calculateRowHeight;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * The Points of Interest panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class PointOfInterestPanel implements PanelInTab {
    private static final int ROW_HEIGHT_FOR_PHOTO_COLUMN = 200;

    private JPanel pointsOfInterestPanel;
    private JTable tablePointsOfInterest;
    private JButton buttonPlayVoice;
    private JButton buttonAddAudio;
    private JButton buttonDeletePointsOfInterest;

    private FilteringPositionsModel positionsModel;

    public PointOfInterestPanel() {
        $$$setupUI$$$();
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        positionsModel = new FilteringPositionsModel<>(r.getConvertPanel().getPositionsModel(), new PointOfInterestPositionPredicate());
        tablePointsOfInterest.setModel(getPositionsModel());
        PointsOfInterestTableColumnModel tableColumnModel = new PointsOfInterestTableColumnModel();
        tablePointsOfInterest.setColumnModel(tableColumnModel);

        r.getUnitSystemModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getPositionsModel().fireTableRowsUpdated(0, MAX_VALUE, ALL_COLUMNS);
            }
        });
        r.getTimeZone().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getPositionsModel().fireTableRowsUpdated(0, MAX_VALUE, ALL_COLUMNS);
            }
        });

        tablePointsOfInterest.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                if (getPositionsModel().isContinousRange())
                    return;
                handlePositionsUpdate();
            }
        });
        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (!isFirstToLastRow(e))
                    return;
                if (getPositionsModel().isContinousRange())
                    return;
                handlePositionsUpdate();
            }
        });

        tableColumnModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleColumnVisibilityUpdate((PositionTableColumn) e.getSource());
            }
        });

        tablePointsOfInterest.registerKeyboardAction(new FrameAction() {
            public void run() {
                r.getContext().getActionManager().run("delete-points-of-interest");
            }
        }, getKeyStroke(VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePointsOfInterest.setDropMode(ON);

        ActionManager actionManager = r.getContext().getActionManager();
        new PointsOfInterestTableHeaderMenu(tablePointsOfInterest.getTableHeader(), tableColumnModel, actionManager);
        new PointsOfInterestTablePopupMenu(tablePointsOfInterest).createPopupMenu();

        actionManager.register("play-voice", new PlayVoiceAction(tablePointsOfInterest, getPositionsModel(), r.getUrlModel()));
        actionManager.register("add-audio", new AddAudioAction(tablePointsOfInterest, getPositionsModel()));
        actionManager.register("delete-points-of-interest", new DeletePositionAction(tablePointsOfInterest, getPositionsModel()));
        actionManager.registerLocal("delete", POINTS_OF_INTEREST, "delete-points-of-interest");

        registerAction(buttonPlayVoice, "play-voice");
        registerAction(buttonAddAudio, "add-audio");
        registerAction(buttonDeletePointsOfInterest, "delete-points-of-interest");

        setHelpIDString(tablePointsOfInterest, "point-of-interest-list");

        handlePositionsUpdate();
        for (PositionTableColumn column : tableColumnModel.getPreparedColumns())
            handleColumnVisibilityUpdate(column);
    }

    private int getDefaultRowHeight() {
        return calculateRowHeight(this, new DescriptionColumnTableCellEditor(), new SimpleNavigationPosition(null, null));
    }

    public Component getRootComponent() {
        return pointsOfInterestPanel;
    }

    public String getLocalName() {
        return POINTS_OF_INTEREST;
    }

    public JComponent getFocusComponent() {
        return tablePointsOfInterest;
    }

    public JButton getDefaultButton() {
        return buttonPlayVoice;
    }

    public void initializeSelection() {
        handlePositionsUpdate();
    }

    private FilteringPositionsModel getPositionsModel() {
        return positionsModel;
    }

    private void handlePositionsUpdate() {
        int[] selectedRows = tablePointsOfInterest.getSelectedRows();
        boolean existsASelectedPosition = selectedRows.length > 0;

        RouteConverter r = RouteConverter.getInstance();
        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.enableLocal("delete", POINTS_OF_INTEREST, existsASelectedPosition);
        actionManager.enable("play-voice", existsASelectedPosition);
        actionManager.enable("add-audio", existsASelectedPosition);

        if (r.isPointsOfInterestPanelSelected())
            r.selectPositionsInMap(getPositionsModel().mapRows(selectedRows));
    }

    private void handleColumnVisibilityUpdate(PositionTableColumn column) {
        if (column.getModelIndex() == PHOTO_COLUMN_INDEX)
            tablePointsOfInterest.setRowHeight(column.isVisible() ? ROW_HEIGHT_FOR_PHOTO_COLUMN : getDefaultRowHeight());
    }

    public void addAudio(Wgs84Position position, File file) {
        position.setOrigin(file);
        RouteConverter r = RouteConverter.getInstance();
        PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();
        int index = positionsModel.getIndex(position);
        positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, file.getName().replaceAll(".wav", "")), true, true);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        pointsOfInterestPanel = new JPanel();
        pointsOfInterestPanel.setLayout(new GridLayoutManager(2, 1, new Insets(3, 3, 3, 3), -1, -1));
        pointsOfInterestPanel.setMinimumSize(new Dimension(-1, -1));
        pointsOfInterestPanel.setPreferredSize(new Dimension(560, 560));
        final JScrollPane scrollPane1 = new JScrollPane();
        pointsOfInterestPanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePointsOfInterest = new JTable();
        tablePointsOfInterest.setAutoCreateColumnsFromModel(false);
        tablePointsOfInterest.setShowHorizontalLines(false);
        tablePointsOfInterest.setShowVerticalLines(false);
        scrollPane1.setViewportView(tablePointsOfInterest);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        pointsOfInterestPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonPlayVoice = new JButton();
        this.$$$loadButtonText$$$(buttonPlayVoice, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action"));
        buttonPlayVoice.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action-tooltip"));
        panel1.add(buttonPlayVoice, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeletePointsOfInterest = new JButton();
        this.$$$loadButtonText$$$(buttonDeletePointsOfInterest, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-points-of-interest-action"));
        buttonDeletePointsOfInterest.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-points-of-interest-action-tooltip"));
        panel1.add(buttonDeletePointsOfInterest, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonAddAudio = new JButton();
        this.$$$loadButtonText$$$(buttonAddAudio, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-audio-action"));
        buttonAddAudio.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-audio-action-tooltip"));
        panel1.add(buttonAddAudio, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pointsOfInterestPanel;
    }
}
