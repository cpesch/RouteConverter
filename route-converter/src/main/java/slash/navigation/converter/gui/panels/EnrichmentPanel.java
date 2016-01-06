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
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.DeleteAction;
import slash.navigation.converter.gui.actions.PlayVoiceAction;
import slash.navigation.converter.gui.helpers.EnrichmentTableHeaderMenu;
import slash.navigation.converter.gui.helpers.EnrichmentTablePopupMenu;
import slash.navigation.converter.gui.models.EnrichmentTableColumnModel;
import slash.navigation.converter.gui.models.FilteringPositionsModel;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_DELETE;
import static java.util.Arrays.asList;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.base.WaypointType.*;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * The voice and photo panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class EnrichmentPanel implements PanelInTab {
    private JPanel enrichmentPanel;
    private JTable tableEnrichments;
    private JButton buttonAddVoice;
    private JButton buttonRemoveEnrichment;
    private JButton buttonAddPhoto;
    private JButton buttonPlayVoice;

    private FilteringPositionsModel positionsModel;

    public EnrichmentPanel() {
        $$$setupUI$$$();
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        positionsModel = new FilteringPositionsModel(r.getPositionsModel(), new FilteringPositionsModel.FilterPredicate() {
            private final List<WaypointType> ENRICHED_WAYPOINT_TYPES = asList(Photo, PointOfInterest, Voice);

            public boolean shouldInclude(NavigationPosition position) {
                return position instanceof Wgs84Position &&
                        ENRICHED_WAYPOINT_TYPES.contains(((Wgs84Position) position).getWaypointType());
            }
        });

        tableEnrichments.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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

        tableEnrichments.setModel(getPositionsModel());
        EnrichmentTableColumnModel tableColumnModel = new EnrichmentTableColumnModel();
        tableEnrichments.setColumnModel(tableColumnModel);
        tableEnrichments.setDropMode(ON);

        tableEnrichments.registerKeyboardAction(new FrameAction() {
            public void run() {
                r.getContext().getActionManager().run("delete-enrichment");
            }
        }, getKeyStroke(VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ActionManager actionManager = r.getContext().getActionManager();
        new EnrichmentTableHeaderMenu(tableEnrichments.getTableHeader(), tableColumnModel, actionManager);
        new EnrichmentTablePopupMenu(tableEnrichments).createPopupMenu();

        registerAction(buttonRemoveEnrichment, "delete-enrichment");
        registerAction(buttonPlayVoice, "play-voice");

        actionManager.register("delete-enrichment", new DeleteAction(tableEnrichments, getPositionsModel()));
        actionManager.register("play-voice", new PlayVoiceAction(tableEnrichments, getPositionsModel(), r.getUrlModel()));

        setHelpIDString(tableEnrichments, "enrichment-list");

        handlePositionsUpdate();
    }

    public Component getRootComponent() {
        return enrichmentPanel;
    }

    public JComponent getFocusComponent() {
        return tableEnrichments;
    }

    public JButton getDefaultButton() {
        return buttonAddVoice;
    }

    private FilteringPositionsModel getPositionsModel() {
        return positionsModel;
    }

    private void handlePositionsUpdate() {
        int[] selectedRows = tableEnrichments.getSelectedRows();
        boolean existsASelectedPosition = selectedRows.length > 0;

        RouteConverter r = RouteConverter.getInstance();
        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.enable("delete-enrichment", existsASelectedPosition);
        actionManager.enable("play-voice", existsASelectedPosition);

        if (r.isEnrichmentSelected())
            r.selectPositions(getPositionsModel().mapRows(selectedRows));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        enrichmentPanel = new JPanel();
        enrichmentPanel.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        buttonAddVoice = new JButton();
        this.$$$loadButtonText$$$(buttonAddVoice, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-voice-action"));
        buttonAddVoice.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-voice-action-tooltip"));
        enrichmentPanel.add(buttonAddVoice, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemoveEnrichment = new JButton();
        this.$$$loadButtonText$$$(buttonRemoveEnrichment, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-enrichment-action"));
        buttonRemoveEnrichment.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-enrichment-action-tooltip"));
        enrichmentPanel.add(buttonRemoveEnrichment, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        enrichmentPanel.add(scrollPane1, new GridConstraints(0, 0, 5, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableEnrichments = new JTable();
        tableEnrichments.setAutoCreateColumnsFromModel(false);
        tableEnrichments.setShowHorizontalLines(false);
        tableEnrichments.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableEnrichments);
        final Spacer spacer1 = new Spacer();
        enrichmentPanel.add(spacer1, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonAddPhoto = new JButton();
        this.$$$loadButtonText$$$(buttonAddPhoto, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-photo-action"));
        buttonAddPhoto.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-photo-action-tooltip"));
        enrichmentPanel.add(buttonAddPhoto, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPlayVoice = new JButton();
        this.$$$loadButtonText$$$(buttonPlayVoice, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action"));
        buttonPlayVoice.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action-tooltip"));
        enrichmentPanel.add(buttonPlayVoice, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return enrichmentPanel;
    }
}
