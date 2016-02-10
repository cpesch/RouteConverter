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
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserCallback;
import slash.navigation.base.ParserResult;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.columbus.ImageNavigationFormatRegistry;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.DeletePositionAction;
import slash.navigation.converter.gui.actions.PlayVoiceAction;
import slash.navigation.converter.gui.helpers.PointsOfInterestTableHeaderMenu;
import slash.navigation.converter.gui.helpers.PointsOfInterestTablePopupMenu;
import slash.navigation.converter.gui.models.FilteringPositionsModel;
import slash.navigation.converter.gui.models.PointsOfInterestTableColumnModel;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.image.ImageFormat;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.awt.event.KeyEvent.VK_DELETE;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.base.WaypointType.PointOfInterest;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.converter.gui.models.LocalNames.POINTS_OF_INTEREST;
import static slash.navigation.converter.gui.models.PositionColumns.IMAGE_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * The Points of Interest panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class PointOfInterestPanel implements PanelInTab {
    private static final Logger log = Logger.getLogger(PointOfInterestPanel.class.getName());
    private static final int ROW_HEIGHT_FOR_IMAGE_COLUMN = 200;

    private JPanel pointsOfInterestPanel;
    private JTable tablePointsOfInterest;
    private JButton buttonDeletePointsOfInterest;
    private JButton buttonPlayVoice;

    private FilteringPositionsModel positionsModel;
    private int defaultTableRowHeight;

    public PointOfInterestPanel() {
        $$$setupUI$$$();
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        positionsModel = new FilteringPositionsModel(r.getPositionsModel(), new FilteringPositionsModel.FilterPredicate() {
            private final List<WaypointType> POINTS_OF_INTEREST_WAYPOINT_TYPES = asList(Photo, PointOfInterest, Voice);

            public boolean shouldInclude(NavigationPosition position) {
                return position instanceof Wgs84Position &&
                        POINTS_OF_INTEREST_WAYPOINT_TYPES.contains(((Wgs84Position) position).getWaypointType());
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

        tablePointsOfInterest.setModel(getPositionsModel());
        PointsOfInterestTableColumnModel tableColumnModel = new PointsOfInterestTableColumnModel();
        tablePointsOfInterest.setColumnModel(tableColumnModel);

        defaultTableRowHeight = tablePointsOfInterest.getRowHeight();
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

        actionManager.register("delete-points-of-interest", new DeletePositionAction(tablePointsOfInterest, getPositionsModel()));
        actionManager.registerLocal("delete", POINTS_OF_INTEREST, "delete-points-of-interest");
        actionManager.register("play-voice", new PlayVoiceAction(tablePointsOfInterest, getPositionsModel(), r.getUrlModel()));

        registerAction(buttonDeletePointsOfInterest, "delete-points-of-interest");
        registerAction(buttonPlayVoice, "play-voice");

        setHelpIDString(tablePointsOfInterest, "point-of-interest-list");

        handlePositionsUpdate();
        for (PositionTableColumn column : tableColumnModel.getPreparedColumns())
            handleColumnVisibilityUpdate(column);
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

    public void addPhotosToPositionList(List<File> files) {
        try {
            for (File file : files) {
                addPhotoToPositionList(file);
            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO need dialog here later
        }
    }

    private void addPhotoToPositionList(File file) throws IOException {
        long start = currentTimeMillis();
        try {
            Wgs84Route extractedRoute = extractMetadata(file);
            log.info("reading photo " + file + " after " + (currentTimeMillis() - start) + " milliseconds");

            Wgs84Position extractedPosition = extractedRoute.getPosition(0);

            PositionsModel originalPositionsModel = RouteConverter.getInstance().getPositionsModel();
            // TODO 1. search by coordinates

            // TODO 2. time
            // int index = originalPositionsModel.getClosestPosition(extractedPosition.getTime(), 15 * 60 * 1000); // TODO No Timezone offset yet
            int index = (int) (Math.random() * originalPositionsModel.getRowCount()); // TODO No Timezone offset yet
            if (index == -1) {
                extractedPosition.setDescription("No Geotagging possible");
                //noinspection unchecked
                originalPositionsModel.add(originalPositionsModel.getRowCount(), (BaseRoute) extractedRoute);

            } else {
                NavigationPosition position = originalPositionsModel.getPosition(index);
                if (!(position instanceof Wgs84Position))
                    throw new UnsupportedOperationException("Writing images not supported for " + position);

                Wgs84Position closestPosition = Wgs84Position.class.cast(position);
                closestPosition.setOrigin(file);
                extractedRoute.remove(0);
                extractedRoute.add(0, closestPosition);
                log.info("preparing write " + file + " after " + (currentTimeMillis() - start) + " milliseconds");

                NavigationFormatParser parser = new NavigationFormatParser(new ImageNavigationFormatRegistry());
                parser.write(extractedRoute, new ImageFormat(), false, false, new ParserCallback() {
                    public void preprocess(BaseRoute route, NavigationFormat format) {
                    }
                }, new File(file.getAbsolutePath() + ".new"));    // TODO currently writing to separate file
                log.info("write " + file + " after " + (currentTimeMillis() - start) + " milliseconds");

                closestPosition.setDescription(file.getAbsolutePath());

                closestPosition.setWaypointType(Photo);
                closestPosition.setOrigin(file);
                originalPositionsModel.fireTableRowsUpdated(index, index, ALL_COLUMNS);
            }
        } finally {
            long end = currentTimeMillis();
            log.info("adding photo " + file + " took " + (end - start) + " milliseconds");
        }
    }

    private Wgs84Route extractMetadata(File file) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new ImageNavigationFormatRegistry());
        ParserResult parserResult = parser.read(file);
        if (parserResult.isSuccessful()) {
            Wgs84Route route = Wgs84Route.class.cast(parserResult.getTheRoute());
            if (route.getPositionCount() > 0) {
                return route;
            }
        }
        Wgs84Position position = new Wgs84Position(null, null, null, null, fromMillis(file.lastModified()), "No Metadata found", file);
        return new Wgs84Route(new ImageFormat(), Waypoints, new ArrayList<>(singletonList(position)));
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

        if (r.isPointsOfInterestSelected())
            r.selectPositionsInMap(getPositionsModel().mapRows(selectedRows));
    }

    private void handleColumnVisibilityUpdate(PositionTableColumn column) {
        if (column.getModelIndex() == IMAGE_COLUMN_INDEX)
            tablePointsOfInterest.setRowHeight(column.isVisible() ? ROW_HEIGHT_FOR_IMAGE_COLUMN : defaultTableRowHeight);
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
        pointsOfInterestPanel.setLayout(new GridLayoutManager(4, 2, new Insets(3, 3, 3, 3), -1, -1));
        buttonDeletePointsOfInterest = new JButton();
        this.$$$loadButtonText$$$(buttonDeletePointsOfInterest, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-points-of-interest-action"));
        buttonDeletePointsOfInterest.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-points-of-interest-action-tooltip"));
        pointsOfInterestPanel.add(buttonDeletePointsOfInterest, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        pointsOfInterestPanel.add(scrollPane1, new GridConstraints(0, 0, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePointsOfInterest = new JTable();
        tablePointsOfInterest.setAutoCreateColumnsFromModel(false);
        tablePointsOfInterest.setShowHorizontalLines(false);
        tablePointsOfInterest.setShowVerticalLines(false);
        scrollPane1.setViewportView(tablePointsOfInterest);
        final Spacer spacer1 = new Spacer();
        pointsOfInterestPanel.add(spacer1, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonPlayVoice = new JButton();
        this.$$$loadButtonText$$$(buttonPlayVoice, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action"));
        buttonPlayVoice.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("play-voice-action-tooltip"));
        pointsOfInterestPanel.add(buttonPlayVoice, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
