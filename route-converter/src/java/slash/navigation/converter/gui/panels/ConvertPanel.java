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
import slash.navigation.*;
import slash.navigation.babel.BabelException;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.dialogs.FilterDialog;
import slash.navigation.converter.gui.dialogs.RenameDialog;
import slash.navigation.converter.gui.dnd.DnDHelper;
import slash.navigation.converter.gui.helper.*;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.renderer.RouteCharacteristicsListCellRenderer;
import slash.navigation.converter.gui.renderer.RouteListCellRenderer;
import slash.navigation.converter.gui.renderer.NavigationFormatListCellRenderer;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gui.Constants;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * The convert panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public abstract class ConvertPanel {
    private static final Logger log = Logger.getLogger(ConvertPanel.class.getName());

    private final FormatAndRoutesModel formatAndRoutesModel = new FormatAndRoutesModel();

    protected JPanel convertPanel;
    protected JTextField textFieldSource;
    private JLabel labelPositionLists;
    private JLabel labelPositions;
    private JLabel labelLength;
    private JLabel labelDuration;
    private JLabel labelFormat;
    protected JTable tablePositions;
    private PositionTablePopupMenu popupTable;
    private JButton buttonOpenFile;
    private JButton buttonNewFile;
    private JComboBox comboBoxChoosePositionList;
    private JComboBox comboBoxChoosePositionListCharacteristics;
    private JButton buttonNewPositionList;
    private JButton buttonRenamePositionList;
    private JButton buttonRemovePositionList;
    private JButton buttonMovePositionToTop;
    private JButton buttonMovePositionUp;
    private JButton buttonAddPosition;
    private JButton buttonRemovePosition;
    private JButton buttonFilterPositionList;
    private JButton buttonRevertPositionList;
    private JButton buttonMovePositionDown;
    private JButton buttonMovePositionToBottom;
    private JCheckBox checkboxDuplicateFirstPosition;
    private JCheckBox checkBoxSaveAsRouteTrackWaypoints;
    private JComboBox comboBoxChooseFormat;
    private JButton buttonSaveFile;

    public ConvertPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        new FormatToJLabelAdapter(formatAndRoutesModel, labelFormat);
        new PositionListsToJLabelAdapter(formatAndRoutesModel, labelPositionLists);
        new PositionsCountToJLabelAdapter(getPositionsModel(), labelPositions);
        new LengthToJLabelAdapter(formatAndRoutesModel, labelLength, labelDuration);

        buttonOpenFile.addActionListener(new FrameAction() {
            public void run() {
                openFile();
            }
        });

        buttonNewFile.addActionListener(new FrameAction() {
            public void run() {
                newFile();
            }
        });

        textFieldSource.registerKeyboardAction(new FrameAction() {
            public void run() {
                openPositionList(Files.toUrls(textFieldSource.getText()));
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        buttonNewPositionList.addActionListener(new FrameAction() {
            public void run() {
                newPositionList();
            }
        });

        buttonRenamePositionList.addActionListener(new FrameAction() {
            public void run() {
                renamePositionList();
            }
        });

        buttonRemovePositionList.addActionListener(new FrameAction() {
            public void run() {
                BaseRoute selectedRoute = formatAndRoutesModel.getSelectedRoute();
                if (selectedRoute != null)
                    formatAndRoutesModel.removeRoute(selectedRoute);
            }
        });

        buttonSaveFile.addActionListener(new FrameAction() {
            public void run() {
                saveFile();
            }
        });

        buttonMovePositionToTop.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().top(selectedRows);
                    reestablishPositionSelection(0);
                }
            }
        });

        buttonMovePositionUp.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().up(selectedRows);
                    reestablishPositionSelection(-1);
                }
            }
        });

        buttonAddPosition.addActionListener(new FrameAction() {
            public void run() {
                addPosition();
            }
        });

        buttonRemovePosition.addActionListener(new FrameAction() {
            public void run() {
                removePositions();
            }
        });

        buttonFilterPositionList.addActionListener(new FrameAction() {
            public void run() {
                FilterDialog options = new FilterDialog();
                options.pack();
                options.setLocationRelativeTo(r.getFrame());
                options.setVisible(true);
            }
        });

        buttonRevertPositionList.addActionListener(new FrameAction() {
            public void run() {
                getPositionsModel().revert();
                clearSelection();
            }
        });

        buttonMovePositionDown.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().down(selectedRows);
                    reestablishPositionSelection(+1);
                }
            }
        });

        buttonMovePositionToBottom.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().bottom(selectedRows);
                    reestablishPositionSelection(0);
                }
            }
        });

        formatAndRoutesModel.addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                handleRoutesUpdate();
            }
        });

        convertPanel.registerKeyboardAction(new FrameAction() {
            public void run() {
                addPosition();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                removePositions();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tablePositions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                handleSelectionUpdate();
            }
        });

        tablePositions.setModel(getPositionsModel());
        final PositionsTableColumnModel tableColumnModel = new PositionsTableColumnModel();
        tablePositions.setColumnModel(tableColumnModel);

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                handlePositionsUpdate();
            }
        });

        new TableHeaderPopupMenu(tablePositions.getTableHeader(), tableColumnModel);
        popupTable = new PositionTablePopupMenu();

        NavigationFormat[] formats = NavigationFormats.getWriteFormatsSortedByName();
        comboBoxChooseFormat.setModel(new DefaultComboBoxModel(formats));
        comboBoxChooseFormat.setRenderer(new NavigationFormatListCellRenderer());
        String preferredFormat = r.getTargetFormatPreference();
        for (NavigationFormat format : formats) {
            if (format.getClass().getName().equals(preferredFormat))
                comboBoxChooseFormat.setSelectedItem(format);
        }
        comboBoxChooseFormat.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                handleFormatUpdate();
            }
        });

        new CheckBoxPreferencesSynchronizer(checkboxDuplicateFirstPosition, r.getPreferences(), RouteConverter.DUPLICATE_FIRST_POSITION_PREFERENCE, false);
        new CheckBoxPreferencesSynchronizer(checkBoxSaveAsRouteTrackWaypoints, r.getPreferences(), RouteConverter.SAVE_AS_ROUTE_TRACK_WAYPOINTS_PREFERENCE, true);

        handleFormatUpdate();
        handleRoutesUpdate();
        handlePositionsUpdate();
        handleSelectionUpdate();

        comboBoxChoosePositionList.setModel(formatAndRoutesModel);
        comboBoxChoosePositionList.setRenderer(new RouteListCellRenderer());
        comboBoxChoosePositionList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    formatAndRoutesModel.setSelectedItem(e.getItem());
            }
        });
        comboBoxChoosePositionListCharacteristics.setModel(getCharacteristicsModel());
        comboBoxChoosePositionListCharacteristics.setRenderer(new RouteCharacteristicsListCellRenderer());

        addDragAndDrop();
    }

    protected abstract void addDragAndDrop();

    public Component getRootComponent() {
        return convertPanel;
    }

    public JButton getDefaultButton() {
        return buttonOpenFile;
    }

    // action methods

    protected void handleDrop(List<File> files) {
        if (RouteConverter.getInstance().isConvertPanelSelected())
            openUrls(Files.toUrls(files.toArray(new File[files.size()])));
        else if (RouteConverter.getInstance().isBrowsePanelSelected())
            RouteConverter.getInstance().addFilesToCatalog(files);
    }

    protected void handleDrop(String string) {
        if (RouteConverter.getInstance().isConvertPanelSelected()) {
            String url = DnDHelper.extractUrl(string);
            try {
                openPositionList(Arrays.asList(new URL(url)));
            }
            catch (MalformedURLException e) {
                log.severe("Could not create URL from '" + url + "'");
            }
        } else if (RouteConverter.getInstance().isBrowsePanelSelected()) {
            RouteConverter.getInstance().addUrlToCatalog(string);
        }
    }

    public void openUrls(List<URL> urls) {
        if (!confirmDiscard())
            return;

        // make copy which we could modify freely
        List<URL> copy = new ArrayList<URL>(urls);
        for (Iterator<URL> it = copy.iterator(); it.hasNext();) {
            URL url = it.next();
            File file = Files.toFile(url);
            if (file != null && !file.exists() && !file.isFile()) {
                log.warning(file + " does not exist or is not a file");
                it.remove();
            }
        }

        // start with a non-existant file
        if (copy.size() == 0) {
            newFile();
            return;
        }

        if (copy.size() > 0) {
            openPositionList(copy);
        }
    }

    private void openFile() {
        if (!confirmDiscard())
            return;

        getChooser().setDialogTitle(RouteConverter.getBundle().getString("open-file-source"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(false);
        int open = getChooser().showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File selected = getChooser().getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        setReadFormatFileFilterPreference(getChooser());
        openPositionList(Files.toUrls(selected));
    }

    public void openPositionList(final List<URL> urls) {
        final RouteConverter r = RouteConverter.getInstance();

        final URL url = urls.get(0);
        final String path = Files.createReadablePath(url);
        textFieldSource.setText(path);
        r.setSourcePreference(path);

        Constants.startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        new Thread(new Runnable() {
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            Gpx11Format gpxFormat = new Gpx11Format();
                            //noinspection unchecked
                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                        }
                    });

                    final NavigationFileParser parser = new NavigationFileParser();
                    parser.addNavigationFileParserListener(new NavigationFileParserListener() {
                        public void reading(final NavigationFormat format) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    formatAndRoutesModel.setFormat(format);
                                }
                            });
                        }
                    });
                    if (parser.read(url)) {
                        log.info("Opened: " + path);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                //noinspection unchecked
                                formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                            }
                        });

                        if (urls.size() > 1) {
                            List<URL> append = new ArrayList<URL>(urls);
                            append.remove(0);
                            // this way the route is always marked as modified :-(
                            appendPositionList(append);
                        }

                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Gpx11Format gpxFormat = new Gpx11Format();
                                //noinspection unchecked
                                formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                            }
                        });
                        r.handleUnsupportedFormat(path);
                    }
                } catch (BabelException e) {
                    r.handleBabelError(e);
                } catch (Throwable t) {
                    r.handleOpenError(t, path);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "UrlOpener").start();
    }


    private void appendPositionList(final List<URL> urls) {
        final RouteConverter r = RouteConverter.getInstance();

        Constants.startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        new Thread(new Runnable() {
            public void run() {
                try {
                    for (URL url : urls) {
                        final String path = Files.createReadablePath(url);

                        final NavigationFileParser parser = new NavigationFileParser();
                        if (parser.read(url)) {
                            log.info("Imported: " + path);

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        // if there is no file loaded: parseArgs()
                                        if (formatAndRoutesModel.getRoutes() == null) {
                                            textFieldSource.setText(path);
                                            //noinspection unchecked
                                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                            comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                        } else {
                                            getPositionsModel().add(getPositionsModel().getRowCount(), parser.getTheRoute());
                                        }
                                    } catch (IOException e) {
                                        r.handleOpenError(e, path);
                                    }
                                }
                            });

                        } else {
                            r.handleUnsupportedFormat(path);
                        }
                    }
                } catch (Throwable t) {
                    log.severe("Append error: " + t.getMessage());
                    r.handleOpenError(t, urls);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "UrlAppender").start();
    }

    public void newFile() {
        if (!confirmDiscard())
            return;

        Constants.startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        try {
            textFieldSource.setText(RouteConverter.getBundle().getString("new-route-name"));
            Gpx11Format gpxFormat = new Gpx11Format();
            GpxRoute gpxRoute = new GpxRoute(gpxFormat);
            gpxRoute.setName(RouteConverter.getBundle().getString("new-route-name"));
            //noinspection unchecked
            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
        }
        finally {
            Constants.stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        }
    }

    public File[] selectFilesToImport() {
        getChooser().setDialogTitle(RouteConverter.getBundle().getString("import-positionlist-source"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(true);
        int open = getChooser().showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return null;

        File[] selected = getChooser().getSelectedFiles();
        if (selected == null || selected.length == 0)
            return null;

        setReadFormatFileFilterPreference(getChooser());
        return selected;
    }

    private void renamePositionList() {
        RenameDialog renameDialog = new RenameDialog(getPositionsModel().getRoute().getName());
        renameDialog.pack();
        renameDialog.setLocationRelativeTo(RouteConverter.getInstance().getFrame());
        renameDialog.setVisible(true);
    }

    private void newPositionList() {
        Gpx11Format gpxFormat = new Gpx11Format();
        GpxRoute gpxRoute = new GpxRoute(gpxFormat);
        gpxRoute.setName(RouteConverter.getBundle().getString("new-route-name"));
        formatAndRoutesModel.addRoute(formatAndRoutesModel.getSize(), gpxRoute);
        formatAndRoutesModel.setSelectedItem(gpxRoute);
    }

    private void saveFile(File file, NavigationFormat format) {
        RouteConverter r = RouteConverter.getInstance();

        r.setTargetPreference(format, file.getParent());
        BaseRoute route = formatAndRoutesModel.getSelectedRoute();
        int fileCount = NavigationFileParser.getNumberOfFilesToWriteFor(route, format, checkboxDuplicateFirstPosition.isSelected());
        if (fileCount > 1) {
            int confirm = JOptionPane.showConfirmDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-split"), Files.shortenPath(getSourceFileName()),
                            route.getPositionCount(), format.getName(),
                            format.getMaximumPositionCount(), fileCount),
                    r.getFrame().getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
            switch (confirm) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    fileCount = 1;
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return;
            }
        }

        saveFile(file, format, route, fileCount);
    }

    private void saveFile(File file, NavigationFormat format, BaseRoute route, int fileCount) {
        RouteConverter r = RouteConverter.getInstance();

        File[] targets = createTargetFiles(file, fileCount, format.getExtension(), format.getMaximumFileNameLength());
        for (File target : targets) {
            if (target.exists()) {
                String path = Files.createReadablePath(target);
                if (confirmOverwrite(path))
                    return;
                break;
            }
        }

        String targetsAsString = Files.printArrayToDialogString(targets);
        Constants.startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        try {
            boolean saveAsRouteTrackWaypoints = checkBoxSaveAsRouteTrackWaypoints.isSelected();
            if (format.isSupportsMultipleRoutes() && (formatAndRoutesModel.getRoutes().size() > 1 || !saveAsRouteTrackWaypoints)) {
                new NavigationFileParser().write(formatAndRoutesModel.getRoutes(), (MultipleRoutesFormat) format, targets[0]);
            } else {
                new NavigationFileParser().write(route, format, checkboxDuplicateFirstPosition.isSelected(), true, targets);
            }
            formatAndRoutesModel.setModified(false);
            log.info("Saved: " + targetsAsString);
        } catch (Throwable t) {
            log.severe("Save error " + file + "," + format + ": " + t.getMessage());

            JOptionPane.showMessageDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-error"), Files.shortenPath(getSourceFileName()), targetsAsString, t.getMessage()),
                    r.getFrame().getTitle(), JOptionPane.ERROR_MESSAGE);
        } finally {
            Constants.stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        }
    }

    private void saveFile() {
        NavigationFormat format = getFormat();

        getChooser().setDialogTitle(RouteConverter.getBundle().getString("save-target"));
        getChooser().resetChoosableFileFilters();
        getChooser().setFileFilter(new NavigationFormatFileFilter(format));
        getChooser().setSelectedFile(createSelectedTarget());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(false);
        int save = getChooser().showSaveDialog(RouteConverter.getInstance().getFrame());
        if (save != JFileChooser.APPROVE_OPTION)
            return;

        File selected = getChooser().getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        saveFile(selected, format);
    }

    private BaseNavigationPosition calculateCenter(int row) {
        BaseNavigationPosition position = getPositionsModel().getPosition(row);
        // if there is only one position or it is the first row, choose the map center
        if (row >= getPositionsModel().getRowCount() - 1)
            return null;
        // otherwhise center between given positions
        BaseNavigationPosition second = getPositionsModel().getPosition(row + 1);
        if (!second.hasCoordinates() || !position.hasCoordinates())
            return null;
        return Calculation.center(Arrays.asList(second, position));
    }

    private void addPosition() {
        int[] selectedRows = tablePositions.getSelectedRows();
        int row = selectedRows.length > 0 ? selectedRows[0] : tablePositions.getRowCount();
        BaseNavigationPosition center = selectedRows.length > 0 ? calculateCenter(row) :
                getPositionsModel().getRowCount() > 0 ? calculateCenter(getPositionsModel().getRowCount() - 1) : null;
        final int insertRow = row > getPositionsModel().getRowCount() - 1 ? row : row + 1;

        RouteConverter r = RouteConverter.getInstance();
        if (center == null)
            center = r.getMapCenter();
        r.setLastMapCenter(center);

        getPositionsModel().add(insertRow, center.getLongitude(), center.getLatitude(),
                center.getElevation(), center.getSpeed(),
                center.getTime() != null ? center.getTime() : CompactCalendar.getInstance(),
                RouteConverter.getBundle().getString("add-position-comment"));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Rectangle rectangle = tablePositions.getCellRect(insertRow, 1, true);
                tablePositions.scrollRectToVisible(rectangle);
                selectPositions(insertRow, insertRow);
            }
        });
    }

    public void removePositions() {
        int[] selectedRows = tablePositions.getSelectedRows();
        if (selectedRows.length > 0) {
            getPositionsModel().remove(selectedRows);
            final int row = selectedRows[0] > 0 ? selectedRows[0] - 1 : 0;
            if (tablePositions.getRowCount() > 0)
                selectPositions(row, row);
        }
    }

    // helpers for actions

    public boolean confirmDiscard() {
        if (formatAndRoutesModel.isModified()) {
            int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                    RouteConverter.getBundle().getString("confirm-discard"),
                    RouteConverter.getInstance().getFrame().getTitle(), JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return false;
        }
        return true;
    }

    private boolean confirmOverwrite(String file) {
        int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-overwrite"), file),
                RouteConverter.getInstance().getFrame().getTitle(), JOptionPane.YES_NO_OPTION);
        return confirm != JOptionPane.YES_OPTION;
    }

    private void selectPositions(final int index0, final int index1) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                tablePositions.getSelectionModel().setSelectionInterval(index0, index1);
            }
        });
    }

    private void reestablishPositionSelection(final int upOrDown) {
        final int minSelectedRow = tablePositions.getSelectionModel().getMinSelectionIndex();
        final int maxSelectedRow = tablePositions.getSelectionModel().getMaxSelectionIndex();
        if (minSelectedRow != -1 && maxSelectedRow != -1)
            selectPositions(minSelectedRow + upOrDown, maxSelectedRow + upOrDown);
    }

    // handle notifications

    private void handleFormatUpdate() {
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsOneRoute = formatAndRoutesModel.getSize() == 1;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        checkboxDuplicateFirstPosition.setVisible(getFormat() instanceof NmnFormat && !(getFormat() instanceof Nmn7Format));
        checkBoxSaveAsRouteTrackWaypoints.setVisible(supportsMultipleRoutes && existsOneRoute);

        buttonNewPositionList.setEnabled(supportsMultipleRoutes);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);

        popupTable.handleFormatUpdate(supportsMultipleRoutes, existsMoreThanOnePosition);

        RouteConverter.getInstance().setTargetFormatPreference(getFormat().getClass().getName());
    }

    private void handleRoutesUpdate() {
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsARoute = formatAndRoutesModel.getSize() > 0;
        boolean existsOneRoute = formatAndRoutesModel.getSize() == 1;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        checkBoxSaveAsRouteTrackWaypoints.setVisible(supportsMultipleRoutes && existsOneRoute);

        comboBoxChoosePositionList.setEnabled(existsMoreThanOneRoute);
        buttonNewPositionList.setEnabled(supportsMultipleRoutes);
        buttonRenamePositionList.setEnabled(existsARoute);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);

        popupTable.handleRoutesUpdate(supportsMultipleRoutes, existsARoute, existsMoreThanOnePosition);
    }

    private void handlePositionsUpdate() {
        final boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        buttonMovePositionToTop.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionUp.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionDown.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionToBottom.setEnabled(existsMoreThanOnePosition);
        buttonRemovePosition.setEnabled(existsAPosition);
        buttonFilterPositionList.setEnabled(existsMoreThanOnePosition);
        buttonRevertPositionList.setEnabled(existsMoreThanOnePosition);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                popupTable.handlePositionsUpdate(supportsMultipleRoutes, tablePositions.getSelectedRowCount() > 0);
            }
        });
    }

    private void handleSelectionUpdate() {
        int[] selectedRows = tablePositions.getSelectedRows();
        if (selectedRows.length == 0)
            return;
        boolean firstRowNotSelected = selectedRows[0] != 0;
        buttonMovePositionToTop.setEnabled(firstRowNotSelected);
        buttonMovePositionUp.setEnabled(firstRowNotSelected);
        boolean lastRowNotSelected = selectedRows[selectedRows.length - 1] != tablePositions.getRowCount() - 1;
        buttonMovePositionDown.setEnabled(lastRowNotSelected);
        buttonMovePositionToBottom.setEnabled(lastRowNotSelected);
        if (RouteConverter.getInstance().isMapViewAvailable())
            RouteConverter.getInstance().selectPositionsOnMap(selectedRows);

        final boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        popupTable.handlePositionsUpdate(supportsMultipleRoutes, selectedRows.length > 0);
    }

    // helpers

    private String getSourceFileName() {
        String source = textFieldSource.getText();
        return (source != null && source.length() > 0) ? source : "<null>";
    }

    private File createSelectedSource() {
        File source = new File(textFieldSource.getText());
        source = Files.findExistingPath(source);
        File path = new File(RouteConverter.getInstance().getSourcePreference());
        path = Files.findExistingPath(path);
        if (path == null)
            return source;
        else if (source != null)
            return new File(path, source.getName());
        else
            return path;
    }

    private File createSelectedTarget() {
        File file = new File(getSourceFileName());
        File path = new File(RouteConverter.getInstance().getTargetPreference(getFormat()));
        if (!path.exists())
            path = file.getParentFile();
        String fileName = file.getName();
        if (getFormat() instanceof GoPalRouteFormat)
            fileName = Files.createGoPalFileName(fileName);
        return new File(Files.calculateConvertFileName(new File(path, fileName), getFormat().getExtension(), getFormat().getMaximumFileNameLength()));
    }

    // create this only once to make users choices stable at least for one program start
    private JFileChooser chooser;

    private synchronized JFileChooser getChooser() {
        if (chooser == null)
            chooser = Constants.createJFileChooser();
        return chooser;
    }

    private void setReadFormatFileFilters(JFileChooser chooser) {
        chooser.resetChoosableFileFilters();
        String preferredFormat = RouteConverter.getInstance().getSourceFormatPreference();
        FileFilter fileFilter = chooser.getFileFilter();
        for (NavigationFormat format : NavigationFormats.getReadFormatsSortedByName()) {
            NavigationFormatFileFilter navigationFormatFileFilter = new NavigationFormatFileFilter(format);
            if (format.getClass().getName().equals(preferredFormat))
                fileFilter = navigationFormatFileFilter;
            chooser.addChoosableFileFilter(navigationFormatFileFilter);
        }
        chooser.setFileFilter(fileFilter);
    }

    private void setReadFormatFileFilterPreference(JFileChooser chooser) {
        FileFilter fileFilter = chooser.getFileFilter();
        String preference = "";
        if (fileFilter instanceof NavigationFormatFileFilter)
            preference = ((NavigationFormatFileFilter) fileFilter).getFormat().getClass().getName();
        RouteConverter.getInstance().setSourceFormatPreference(preference);

    }

    private File[] createTargetFiles(File pattern, int fileCount, String extension, int fileNameLength) {
        File[] files = new File[fileCount];
        if (fileCount == 1) {
            files[0] = new File(Files.calculateConvertFileName(pattern, extension, fileNameLength));
        } else {
            for (int i = 0; i < fileCount; i++) {
                files[i] = new File(Files.calculateConvertFileName(pattern, i + 1, fileCount, extension, fileNameLength));
            }
        }
        return files;
    }

    private NavigationFormat getFormat() {
        return (NavigationFormat) comboBoxChooseFormat.getSelectedItem();
    }

    // map view related helpers

    public PositionsModel getPositionsModel() {
        return formatAndRoutesModel.getPositionsModel();
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return formatAndRoutesModel.getCharacteristicsModel();
    }

    // helpers for external components

    public int selectDuplicatesWithinDistance(int distance) {
        clearSelection();
        int[] indices = getPositionsModel().getDuplicatesWithinDistance(distance);
        for (int index : indices) {
            tablePositions.getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public int selectPositionsThatRemainingHaveDistance(int distance) {
        clearSelection();
        int[] indices = getPositionsModel().getPositionsThatRemainingHaveDistance(distance);
        for (int index : indices) {
            tablePositions.getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public int[] selectAllButEveryNthPosition(int order) {
        clearSelection();
        int rowCount = getPositionsModel().getRowCount();
        int[] indices = Range.allButEveryNthAndFirstAndLast(rowCount, order);
        for (int index : indices) {
            tablePositions.getSelectionModel().addSelectionInterval(index, index);
        }
        return new int[]{indices.length, rowCount - indices.length};
    }

    public int selectInsignificantPositions(int threshold) {
        clearSelection();
        int[] indices = getPositionsModel().getInsignificantPositions(threshold);
        for (int index : indices) {
            tablePositions.getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public void clearSelection() {
        tablePositions.getSelectionModel().clearSelection();
    }

    public void renameRoute(String name) {
        formatAndRoutesModel.renameRoute(name);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        convertPanel = new JPanel();
        convertPanel.setLayout(new GridLayoutManager(11, 3, new Insets(3, 3, 0, 3), -1, -1));
        convertPanel.setMinimumSize(new Dimension(-1, -1));
        convertPanel.setPreferredSize(new Dimension(560, 560));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("source"));
        convertPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldSource = new JTextField();
        textFieldSource.setDragEnabled(true);
        textFieldSource.setText("");
        convertPanel.add(textFieldSource, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFile = new JButton();
        buttonOpenFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/open.png")));
        buttonOpenFile.setInheritsPopupMenu(false);
        buttonOpenFile.setText("");
        buttonOpenFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("open-file-tooltip"));
        convertPanel.add(buttonOpenFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        convertPanel.add(scrollPane1, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePositions = new JTable();
        tablePositions.setAutoCreateColumnsFromModel(false);
        tablePositions.setShowHorizontalLines(false);
        tablePositions.setShowVerticalLines(false);
        scrollPane1.setViewportView(tablePositions);
        buttonSaveFile = new JButton();
        buttonSaveFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/save.png")));
        buttonSaveFile.setText("");
        buttonSaveFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("save-file-tooltip"));
        convertPanel.add(buttonSaveFile, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel1, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRevertPositionList = new JButton();
        buttonRevertPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/revert.png")));
        buttonRevertPositionList.setText("");
        buttonRevertPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("revert-tooltip"));
        panel1.add(buttonRevertPositionList, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionUp = new JButton();
        buttonMovePositionUp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/up.png")));
        buttonMovePositionUp.setText("");
        buttonMovePositionUp.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-up-tooltip"));
        panel1.add(buttonMovePositionUp, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionDown = new JButton();
        buttonMovePositionDown.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/down.png")));
        buttonMovePositionDown.setText("");
        buttonMovePositionDown.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-down-tooltip"));
        panel1.add(buttonMovePositionDown, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToTop = new JButton();
        buttonMovePositionToTop.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/top.png")));
        buttonMovePositionToTop.setText("");
        buttonMovePositionToTop.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-top-tooltip"));
        panel1.add(buttonMovePositionToTop, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToBottom = new JButton();
        buttonMovePositionToBottom.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/bottom.png")));
        buttonMovePositionToBottom.setText("");
        buttonMovePositionToBottom.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-bottom-tooltip"));
        panel1.add(buttonMovePositionToBottom, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemovePosition = new JButton();
        buttonRemovePosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-position.png")));
        buttonRemovePosition.setText("");
        buttonRemovePosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remove-tooltip"));
        panel1.add(buttonRemovePosition, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonFilterPositionList = new JButton();
        buttonFilterPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/filter.png")));
        buttonFilterPositionList.setText("");
        buttonFilterPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-tooltip"));
        panel1.add(buttonFilterPositionList, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddPosition = new JButton();
        buttonAddPosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/add-position.png")));
        buttonAddPosition.setText("");
        buttonAddPosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-tooltip"));
        panel1.add(buttonAddPosition, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkboxDuplicateFirstPosition = new JCheckBox();
        this.$$$loadButtonText$$$(checkboxDuplicateFirstPosition, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("duplicate-first-position"));
        convertPanel.add(checkboxDuplicateFirstPosition, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel2, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBoxChooseFormat = new JComboBox();
        panel3.add(comboBoxChooseFormat, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(4);
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("target-format"));
        panel3.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel4, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFormat = new JLabel();
        labelFormat.setHorizontalAlignment(2);
        labelFormat.setHorizontalTextPosition(2);
        labelFormat.setText("-");
        panel4.add(labelFormat, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        convertPanel.add(panel5, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 4), null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("content"));
        convertPanel.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setHorizontalAlignment(10);
        label4.setHorizontalTextPosition(11);
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("format"));
        convertPanel.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel6, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelPositions = new JLabel();
        labelPositions.setHorizontalAlignment(2);
        labelPositions.setHorizontalTextPosition(2);
        labelPositions.setText("-");
        panel6.add(labelPositions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelLength = new JLabel();
        labelLength.setHorizontalAlignment(2);
        labelLength.setHorizontalTextPosition(2);
        labelLength.setText("-");
        labelLength.setVisible(true);
        panel6.add(labelLength, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setHorizontalAlignment(4);
        label5.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("length"));
        label5.setVisible(true);
        panel6.add(label5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setHorizontalAlignment(4);
        label6.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("duration"));
        panel6.add(label6, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelDuration = new JLabel();
        labelDuration.setHorizontalAlignment(2);
        labelDuration.setHorizontalTextPosition(2);
        labelDuration.setText("-");
        panel6.add(labelDuration, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPositionLists = new JLabel();
        labelPositionLists.setHorizontalAlignment(2);
        labelPositionLists.setHorizontalTextPosition(2);
        labelPositionLists.setText("-");
        convertPanel.add(labelPositionLists, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("positions"));
        convertPanel.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel7, new GridConstraints(1, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonNewFile = new JButton();
        buttonNewFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new-position.png")));
        buttonNewFile.setInheritsPopupMenu(false);
        buttonNewFile.setText("");
        buttonNewFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-file-tooltip"));
        panel7.add(buttonNewFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxSaveAsRouteTrackWaypoints = new JCheckBox();
        checkBoxSaveAsRouteTrackWaypoints.setSelected(false);
        this.$$$loadButtonText$$$(checkBoxSaveAsRouteTrackWaypoints, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("save-as-route-track-waypoints"));
        convertPanel.add(checkBoxSaveAsRouteTrackWaypoints, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("position-list"));
        label8.setVerticalAlignment(1);
        label8.setVerticalTextPosition(0);
        label8.setVisible(true);
        convertPanel.add(label8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel8, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewPositionList = new JButton();
        buttonNewPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new-route.png")));
        buttonNewPositionList.setText("");
        buttonNewPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-positionlist-tooltip"));
        panel8.add(buttonNewPositionList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenamePositionList = new JButton();
        buttonRenamePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/rename-route.png")));
        buttonRenamePositionList.setText("");
        buttonRenamePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename-positionlist-tooltip"));
        panel8.add(buttonRenamePositionList, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemovePositionList = new JButton();
        buttonRemovePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-route.png")));
        buttonRemovePositionList.setText("");
        buttonRemovePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remove-position-list-tooltip"));
        panel8.add(buttonRemovePositionList, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel8.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setHorizontalAlignment(4);
        label9.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("characteristics"));
        panel8.add(label9, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionListCharacteristics = new JComboBox();
        panel8.add(comboBoxChoosePositionListCharacteristics, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionList = new JComboBox();
        comboBoxChoosePositionList.setVisible(true);
        convertPanel.add(comboBoxChoosePositionList, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return convertPanel;
    }

    public class PositionTablePopupMenu extends TablePopupMenu {
        private JMenuItem buttonAddElevation, buttonAddPostalAddress, buttonAddPopulatedPlace, buttonAddSpeed,
                buttonAddIndex, buttonSplitPositionlist;

        public PositionTablePopupMenu() {
            super(tablePositions);
        }

        protected JPopupMenu createPopupMenu() {
            JPopupMenu popupMenu = new JPopupMenu();
            PositionAugmenter augmenter = new PositionAugmenter(RouteConverter.getInstance().getFrame());

            buttonAddElevation = new JMenuItem(RouteConverter.getBundle().getString("add-elevation"));
            buttonAddElevation.addActionListener(new AddElevationToPositions(tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddElevation);

            buttonAddPostalAddress = new JMenuItem(RouteConverter.getBundle().getString("add-postal-address"));
            buttonAddPostalAddress.addActionListener(new AddPostalAddressToPositions(tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddPostalAddress);

            buttonAddPopulatedPlace = new JMenuItem(RouteConverter.getBundle().getString("add-populated-place"));
            buttonAddPopulatedPlace.addActionListener(new AddPopulatedPlaceToPositions(tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddPopulatedPlace);

            buttonAddSpeed = new JMenuItem(RouteConverter.getBundle().getString("add-speed"));
            buttonAddSpeed.addActionListener(new AddSpeedToPositions(tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddSpeed);

            buttonAddIndex = new JMenuItem(RouteConverter.getBundle().getString("add-index"));
            buttonAddIndex.addActionListener(new AddIndicesToPositions(RouteConverter.getInstance(), tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddIndex);

            popupMenu.addSeparator();

            buttonSplitPositionlist = new JMenuItem(RouteConverter.getBundle().getString("split-positionlist"));
            buttonSplitPositionlist.addActionListener(new SplitPositionList(RouteConverter.getInstance().getFrame(), tablePositions, getPositionsModel(), formatAndRoutesModel));
            popupMenu.add(buttonSplitPositionlist);

            final JMenu menuMergePositionlist = new JMenu(RouteConverter.getBundle().getString("merge-positionlist"));
            popupMenu.add(menuMergePositionlist);

            formatAndRoutesModel.addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                        BaseRoute route = formatAndRoutesModel.getRoute(i);
                        JMenuItem menuItem = new JMenuItem(new MergePositionList(RouteConverter.getInstance().getFrame(), tablePositions, comboBoxChoosePositionList, route, getPositionsModel(), formatAndRoutesModel));
                        menuItem.setText(RouteComments.shortenRouteName(route));
                        menuMergePositionlist.add(menuItem, i);
                    }
                }

                public void intervalRemoved(ListDataEvent e) {
                    for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                        menuMergePositionlist.remove(i);
                    }
                }

                public void contentsChanged(ListDataEvent e) {
                    for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                        if (i >= 0 && i < menuMergePositionlist.getMenuComponentCount()) {
                            BaseRoute route = formatAndRoutesModel.getRoute(i);
                            JMenuItem menuItem = (JMenuItem) menuMergePositionlist.getMenuComponent(i);
                            menuItem.setText(RouteComments.shortenRouteName(route));
                        }
                    }
                }
            });

            popupMenu.addSeparator();

            JMenuItem buttonImportPositionlist = new JMenuItem(RouteConverter.getBundle().getString("import-positionlist"));
            buttonImportPositionlist.addActionListener(new ImportPositionList(RouteConverter.getInstance().getFrame(), ConvertPanel.this, tablePositions, getPositionsModel()));
            popupMenu.add(buttonImportPositionlist);

            return popupMenu;
        }

        void handleFormatUpdate(boolean supportsMultipleRoutes, boolean existsMoreThanOnePosition) {
            buttonSplitPositionlist.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);
        }

        void handleRoutesUpdate(boolean supportsMultipleRoutes, boolean existsARoute, boolean existsMoreThanOnePosition) {
            buttonSplitPositionlist.setEnabled(supportsMultipleRoutes && existsARoute && existsMoreThanOnePosition);
        }

        void handlePositionsUpdate(final boolean supportsMultipleRoutes, boolean existsASelectedPosition) {
            buttonSplitPositionlist.setEnabled(supportsMultipleRoutes && existsASelectedPosition);
            buttonAddElevation.setEnabled(existsASelectedPosition);
            buttonAddPostalAddress.setEnabled(existsASelectedPosition);
            buttonAddPopulatedPlace.setEnabled(existsASelectedPosition);
            buttonAddSpeed.setEnabled(existsASelectedPosition);
            buttonAddIndex.setEnabled(existsASelectedPosition);
        }
    }
}
