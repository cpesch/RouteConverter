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
import slash.common.helpers.TimeZoneAndId;
import slash.common.helpers.TimeZoneAndIds;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.AddPhotosAction;
import slash.navigation.converter.gui.actions.DeletePositionAction;
import slash.navigation.converter.gui.actions.TagPhotosAction;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.helpers.PhotosTableHeaderMenu;
import slash.navigation.converter.gui.helpers.PhotosTablePopupMenu;
import slash.navigation.converter.gui.helpers.TagStrategy;
import slash.navigation.converter.gui.models.FilteringPositionsModel;
import slash.navigation.converter.gui.models.OverlayPositionsModel;
import slash.navigation.converter.gui.models.PhotoTagStateToJLabelAdapter;
import slash.navigation.converter.gui.models.PhotosTableColumnModel;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.converter.gui.predicates.FilterPredicate;
import slash.navigation.converter.gui.predicates.TagStatePhotoPredicate;
import slash.navigation.converter.gui.predicates.TautologyPredicate;
import slash.navigation.converter.gui.renderer.DescriptionColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.FilterPredicateListCellRenderer;
import slash.navigation.converter.gui.renderer.TagStrategyListCellRenderer;
import slash.navigation.converter.gui.renderer.TimeZoneAndIdListCellRenderer;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.photo.PhotoPosition;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.lang.Integer.MAX_VALUE;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Backup_In_Subdirectory;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Tagged_Photo_In_Subdirectory;
import static slash.navigation.converter.gui.models.LocalActionConstants.PHOTOS;
import static slash.navigation.converter.gui.models.PositionColumns.EXIF_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.GPS_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.PHOTO_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.calculateRowHeight;
import static slash.navigation.photo.TagState.NotTaggable;
import static slash.navigation.photo.TagState.Taggable;
import static slash.navigation.photo.TagState.Tagged;

/**
 * The Photos panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class PhotoPanel implements PanelInTab {
    private static final Preferences preferences = Preferences.userNodeForPackage(ConvertPanel.class);

    private static final String FILTER_PHOTO_PREDICATE_PREFERENCE = "filterPhotoPredicate";

    private static final int ROW_HEIGHT_FOR_PHOTO_COLUMN = 200;

    private JPanel photosPanel;
    private JTable tablePhotos;
    private JLabel labelPhotos;
    private JComboBox<FilterPredicate> comboBoxFilterPhotoPredicate;
    private JButton buttonAddPhotos;
    private JComboBox<TimeZoneAndId> comboBoxPhotoTimeZone;
    private JComboBox<TagStrategy> comboBoxTagStrategy;
    private JButton buttonTagPhotos;

    private static final ComboBoxModel<FilterPredicate> FILTER_PREDICATE_MODEL = new DefaultComboBoxModel<>(new FilterPredicate[]{
            new TautologyPredicate("All"),
            new TagStatePhotoPredicate(Tagged),
            new TagStatePhotoPredicate(Taggable),
            new TagStatePhotoPredicate(NotTaggable),
    });

    private PositionsModel photosModel = new OverlayPositionsModel(new PositionsModelImpl());
    private FilteringPositionsModel filteredPhotosModel;

    public PhotoPanel() {
        $$$setupUI$$$();
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        photosModel.setRoute(new Wgs84Route(new PhotoFormat(), Waypoints, new ArrayList<Wgs84Position>()));
        filteredPhotosModel = new FilteringPositionsModel(photosModel, getFilterPredicatePreference());
        tablePhotos.setModel(filteredPhotosModel);
        PhotosTableColumnModel tableColumnModel = new PhotosTableColumnModel();
        tablePhotos.setColumnModel(tableColumnModel);
        tablePhotos.setDropMode(ON);

        r.getUnitSystemModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getFilteredPhotosModel().fireTableRowsUpdated(0, MAX_VALUE, ALL_COLUMNS);
            }
        });
        r.getTimeZone().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getFilteredPhotosModel().fireTableRowsUpdated(0, MAX_VALUE, ALL_COLUMNS);
            }
        });

        tablePhotos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                if (getFilteredPhotosModel().isContinousRange())
                    return;
                handlePositionsUpdate();
            }
        });

        tableColumnModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleColumnVisibilityUpdate((PositionTableColumn) e.getSource());
            }
        });

        tablePhotos.registerKeyboardAction(new FrameAction() {
            public void run() {
                r.getContext().getActionManager().run("delete-photos");
            }
        }, getKeyStroke(VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePhotos.setDropMode(ON);

        new PhotoTagStateToJLabelAdapter(photosModel, labelPhotos);

        ActionManager actionManager = r.getContext().getActionManager();
        new PhotosTableHeaderMenu(tablePhotos.getTableHeader(), tableColumnModel, actionManager);
        new PhotosTablePopupMenu(tablePhotos).createPopupMenu();

        actionManager.register("add-photos", new AddPhotosAction());
        actionManager.register("delete-photos", new DeletePositionAction(tablePhotos, getFilteredPhotosModel()));
        actionManager.registerLocal("delete", PHOTOS, "delete-photos");
        actionManager.register("tag-photos", new TagPhotosAction());

        registerAction(buttonAddPhotos, "add-photos");
        registerAction(buttonTagPhotos, "tag-photos");

        setHelpIDString(tablePhotos, "photo-list");

        comboBoxFilterPhotoPredicate.setModel(FILTER_PREDICATE_MODEL);
        comboBoxFilterPhotoPredicate.setSelectedItem(getFilterPredicatePreference());
        comboBoxFilterPhotoPredicate.setRenderer(new FilterPredicateListCellRenderer());
        comboBoxFilterPhotoPredicate.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                FilterPredicate filterPredicate = FilterPredicate.class.cast(e.getItem());
                setFilterPredicatePreference(filterPredicate);
                filteredPhotosModel.setFilterPredicate(filterPredicate);
            }
        });

        TimeZoneAndIds timeZoneAndIds = TimeZoneAndIds.getInstance();
        ComboBoxModel<TimeZoneAndId> timeZoneModel = new DefaultComboBoxModel<>(timeZoneAndIds.getTimeZones());
        timeZoneModel.setSelectedItem(timeZoneAndIds.getTimeZoneAndIdFor(r.getPhotoTimeZone().getTimeZone()));
        comboBoxPhotoTimeZone.setModel(timeZoneModel);
        comboBoxPhotoTimeZone.setRenderer(new TimeZoneAndIdListCellRenderer());
        comboBoxPhotoTimeZone.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                TimeZoneAndId timeZoneAndId = TimeZoneAndId.class.cast(e.getItem());
                r.getPhotoTimeZone().setTimeZone(timeZoneAndId.getTimeZone());
            }
        });

        r.getPhotoTimeZone().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                r.getGeoTagger().updateClosestPositionsForTagging();
            }
        });

        ComboBoxModel<TagStrategy> tagStrategyModel = new DefaultComboBoxModel<>(new TagStrategy[]{
                Create_Backup_In_Subdirectory, Create_Tagged_Photo_In_Subdirectory
        });
        tagStrategyModel.setSelectedItem(r.getTagStrategyPreference());
        comboBoxTagStrategy.setModel(tagStrategyModel);
        comboBoxTagStrategy.setRenderer(new TagStrategyListCellRenderer());
        comboBoxTagStrategy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                TagStrategy tagStrategy = TagStrategy.class.cast(e.getItem());
                r.setTagStrategyPreference(tagStrategy);
            }
        });

        photosPanel.setTransferHandler(new PanelDropHandler());

        handlePositionsUpdate();
        for (PositionTableColumn column : tableColumnModel.getPreparedColumns())
            handleColumnVisibilityUpdate(column);
    }

    private int getDefaultRowHeight() {
        return calculateRowHeight(this, new DescriptionColumnTableCellEditor(), new SimpleNavigationPosition(null, null));
    }

    public Component getRootComponent() {
        return photosPanel;
    }

    public String getLocalName() {
        return PHOTOS;
    }

    public JComponent getFocusComponent() {
        return tablePhotos;
    }

    public JButton getDefaultButton() {
        return buttonAddPhotos;
    }

    public JTable getPhotosView() {
        return tablePhotos;
    }

    public PositionsModel getPhotosModel() {
        return photosModel;
    }

    private PositionsModel getFilteredPhotosModel() {
        return filteredPhotosModel;
    }

    public void initializeSelection() {
        handlePositionsUpdate();
    }

    private void handlePositionsUpdate() {
        int[] selectedRows = tablePhotos.getSelectedRows();
        boolean existsASelectedPosition = selectedRows.length > 0;

        RouteConverter r = RouteConverter.getInstance();
        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.enableLocal("delete", PHOTOS, existsASelectedPosition);
        actionManager.enable("tag-photos", existsASelectedPosition);

        if (r.isPhotosPanelSelected()) {
            List<NavigationPosition> selectedPositions = new ArrayList<>();
            for (int selectedRow : selectedRows) {
                PhotoPosition photoPosition = (PhotoPosition) getFilteredPhotosModel().getPosition(selectedRow);
                NavigationPosition closestPositionForTagging = photoPosition.getClosestPositionForTagging();
                NavigationPosition position = closestPositionForTagging != null ? closestPositionForTagging : photoPosition;
                if (position.hasCoordinates())
                    selectedPositions.add(position);
            }
            r.selectPositionsInMap(selectedPositions);
        }
    }

    private void handleColumnVisibilityUpdate(PositionTableColumn column) {
        if (column.getModelIndex() == PHOTO_COLUMN_INDEX || column.getModelIndex() == EXIF_COLUMN_INDEX ||
                column.getModelIndex() == GPS_COLUMN_INDEX)
            tablePhotos.setRowHeight(column.isVisible() ? ROW_HEIGHT_FOR_PHOTO_COLUMN : getDefaultRowHeight());
    }

    private FilterPredicate getFilterPredicatePreference() {
        FilterPredicate result = FILTER_PREDICATE_MODEL.getElementAt(0);
        String name = preferences.get(FILTER_PHOTO_PREDICATE_PREFERENCE, result.getName());
        for (int i = 0, c = FILTER_PREDICATE_MODEL.getSize(); i < c; i++) {
            FilterPredicate filterPredicate = FILTER_PREDICATE_MODEL.getElementAt(i);
            if (filterPredicate.getName().equals(name)) {
                result = filterPredicate;
                break;
            }
        }
        return result;
    }

    private void setFilterPredicatePreference(FilterPredicate filterPredicate) {
        preferences.put(FILTER_PHOTO_PREDICATE_PREFERENCE, filterPredicate.getName());
    }

    public void addPhotos(List<File> files) {
        RouteConverter.getInstance().getGeoTagger().addPhotos(files);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        photosPanel = new JPanel();
        photosPanel.setLayout(new GridLayoutManager(3, 1, new Insets(3, 3, 3, 3), -1, -1));
        photosPanel.setMinimumSize(new Dimension(-1, -1));
        photosPanel.setPreferredSize(new Dimension(560, 560));
        final JScrollPane scrollPane1 = new JScrollPane();
        photosPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePhotos = new JTable();
        tablePhotos.setAutoCreateColumnsFromModel(false);
        scrollPane1.setViewportView(tablePhotos);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        photosPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("photos-colon"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-photos"));
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPhotos = new JLabel();
        labelPhotos.setHorizontalAlignment(2);
        labelPhotos.setHorizontalTextPosition(2);
        labelPhotos.setText("-");
        panel1.add(labelPhotos, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddPhotos = new JButton();
        this.$$$loadButtonText$$$(buttonAddPhotos, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-photos-action"));
        buttonAddPhotos.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-photos-action-tooltip"));
        panel1.add(buttonAddPhotos, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxFilterPhotoPredicate = new JComboBox();
        panel1.add(comboBoxFilterPhotoPredicate, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        photosPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("camera-timezone"));
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxPhotoTimeZone = new JComboBox();
        panel2.add(comboBoxPhotoTimeZone, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(300, -1), 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("tag-strategy"));
        panel2.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxTagStrategy = new JComboBox();
        panel2.add(comboBoxTagStrategy, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonTagPhotos = new JButton();
        this.$$$loadButtonText$$$(buttonTagPhotos, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("tag-photos-action"));
        buttonTagPhotos.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("tag-photos-action-tooltip"));
        panel2.add(buttonTagPhotos, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
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
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
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
        return photosPanel;
    }
}
