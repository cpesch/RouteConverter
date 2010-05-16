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
import slash.common.io.ContinousRange;
import slash.common.io.Files;
import slash.common.io.Range;
import slash.common.io.RangeOperation;
import slash.navigation.babel.BabelException;
import slash.navigation.base.*;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.dialogs.RenameDialog;
import slash.navigation.converter.gui.dialogs.UploadDialog;
import slash.navigation.converter.gui.dnd.DnDHelper;
import slash.navigation.converter.gui.helper.*;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.renderer.RouteCharacteristicsListCellRenderer;
import slash.navigation.converter.gui.renderer.RouteListCellRenderer;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gui.ActionManager;
import slash.navigation.gui.Constants;
import slash.navigation.gui.FrameAction;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.util.RouteComments;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
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

    private final UrlDocument urlModel = new UrlDocument();
    private final FormatAndRoutesModel formatAndRoutesModel = new FormatAndRoutesModel();

    protected JPanel convertPanel;
    private JLabel labelFormat;
    private JLabel labelPositionLists;
    private JLabel labelPositions;
    private JLabel labelLength;
    private JLabel labelDuration;
    protected JTable tablePositions;
    private PositionTablePopupMenu popupTable;
    private JComboBox comboBoxChoosePositionList;
    private JComboBox comboBoxChoosePositionListCharacteristics;
    private JButton buttonNewPositionList;
    private JButton buttonRenamePositionList;
    private JButton buttonRemovePositionList;
    private JButton buttonMovePositionToTop;
    private JButton buttonMovePositionUp;
    private JButton buttonNewPosition;
    private JButton buttonDeletePosition;
    private JButton buttonMovePositionDown;
    private JButton buttonMovePositionToBottom;

    public ConvertPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        LengthCalculator lengthCalculator = new LengthCalculator();
        lengthCalculator.initialize(getPositionsModel(), getCharacteristicsModel());

        new FormatToJLabelAdapter(formatAndRoutesModel, labelFormat);
        new PositionListsToJLabelAdapter(formatAndRoutesModel, labelPositionLists);
        new PositionsCountToJLabelAdapter(getPositionsModel(), labelPositions);
        new LengthToJLabelAdapter(getPositionsModel(), lengthCalculator, labelLength, labelDuration);

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

        buttonNewPosition.addActionListener(r.getContext().getActionManager().get("new-position"));
        buttonDeletePosition.addActionListener(r.getContext().getActionManager().get("delete-position"));

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
        urlModel.addDocumentListener(new AbstractDocumentListener() {
            public void process(DocumentEvent e) {
                String url = urlModel.getShortUrl();
                String title = (url != null ? url + " - " : "") + RouteConverter.getTitle();
                RouteConverter.getInstance().getFrame().setTitle(title);
            }
        });

        convertPanel.registerKeyboardAction(new FrameAction() {
            public void run() {
                r.getContext().getActionManager().run("new-position");
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                r.getContext().getActionManager().run("delete-position");
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tablePositions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                handlePositionsUpdate();
            }
        });

        tablePositions.setModel(getPositionsModel());
        PositionsTableColumnModel tableColumnModel = new PositionsTableColumnModel();
        tablePositions.setColumnModel(tableColumnModel);

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                handlePositionsUpdate();
            }
        });

        new TableHeaderPopupMenu(tablePositions.getTableHeader(), tableColumnModel);
        popupTable = new PositionTablePopupMenu();

        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("delete-position", new DeleteAction(getPositionsView(), getPositionsModel()));
        actionManager.register("new-position", new NewPositionAction(getPositionsView(), getPositionsModel()));
        actionManager.register("new", new NewFileAction(this));
        actionManager.register("open", new OpenAction(this));
        actionManager.register("save", new SaveAction(this));
        actionManager.register("save-as", new SaveAsAction(this));
        actionManager.register("select-all", new SelectAllAction(getPositionsView()));
        actionManager.register("upload", new UploadAction(this));

        formatAndRoutesModel.addModifiedListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                ActionManager actionManager = RouteConverter.getInstance().getContext().getActionManager();
                actionManager.enable("save", formatAndRoutesModel.isModified());
            }
        });

        handleFormatUpdate(); // TODO do we need this?
        handleRoutesUpdate();
        handlePositionsUpdate();

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

        // make sure that Insert works directly after the program start on an empty position list
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                convertPanel.requestFocus();
            }
        });
    }

    protected abstract void addDragAndDrop();

    public Component getRootComponent() {
        return convertPanel;
    }

    public JButton getDefaultButton() {
        return buttonNewPositionList;
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

    public void openFile() {
        if (!confirmDiscard())
            return;

        getChooser().setDialogTitle(RouteConverter.getBundle().getString("open-file-dialog-title"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(true);
        int open = getChooser().showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File[] selected = getChooser().getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(getChooser().getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);
        openPositionList(Files.toUrls(selected), selectedFormat);
    }

    public void openPositionList(final List<URL> urls) {
        openPositionList(urls, null);
    }

    public void openPositionList(final List<URL> urls, final NavigationFormat preferredFormat) {
        final RouteConverter r = RouteConverter.getInstance();

        final URL url = urls.get(0);
        final String path = Files.createReadablePath(url);
        r.setOpenPathPreference(path);

        Constants.startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        new Thread(new Runnable() {
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            Gpx11Format gpxFormat = new Gpx11Format();
                            //noinspection unchecked
                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                            urlModel.setString(null);
                        }
                    });

                    final NavigationFileParser parser = new NavigationFileParser();
                    parser.addNavigationFileParserListener(new NavigationFileParserListener() {
                        public void reading(final NavigationFormat<BaseRoute> format) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    formatAndRoutesModel.setFormat(format);
                                }
                            });
                        }
                    });
                    List<NavigationFormat> formats = new ArrayList<NavigationFormat>(NavigationFormats.getReadFormats());
                    if (preferredFormat != null) {
                        formats.remove(preferredFormat);
                        formats.add(0, preferredFormat);
                    }

                    if (parser.read(url, formats)) {
                        log.info("Opened: " + path);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                //noinspection unchecked
                                formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                urlModel.setString(path);
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
                } catch (OutOfMemoryError e) {
                    r.handleOutOfMemoryError();
                } catch (FileNotFoundException e) {
                    r.handleUnsupportedFormat(path);
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
                                            //noinspection unchecked
                                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                            comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                            urlModel.setString(path);
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
                } catch (BabelException e) {
                    r.handleBabelError(e);
                } catch (OutOfMemoryError e) {
                    r.handleOutOfMemoryError();
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
            Gpx11Format gpxFormat = new Gpx11Format();
            GpxRoute gpxRoute = new GpxRoute(gpxFormat);
            gpxRoute.setName(RouteConverter.getBundle().getString("new-route-name"));
            //noinspection unchecked
            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
            urlModel.setString(null);
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

        NavigationFormat selectedFormat = getSelectedFormat(getChooser().getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);
        return selected;
    }

    private void renamePositionList() {
        RenameDialog renameDialog = new RenameDialog(getPositionsModel().getRoute().getName(), formatAndRoutesModel.getFormat());
        renameDialog.pack();
        renameDialog.restoreLocation();
        renameDialog.setVisible(true);
    }

    private void newPositionList() {
        Gpx11Format gpxFormat = new Gpx11Format();
        GpxRoute gpxRoute = new GpxRoute(gpxFormat);
        gpxRoute.setName(RouteConverter.getBundle().getString("new-route-name"));
        formatAndRoutesModel.addRoute(formatAndRoutesModel.getSize(), gpxRoute);
        formatAndRoutesModel.setSelectedItem(gpxRoute);
    }

    private void saveFile(File file, NavigationFormat format, boolean confirmOverwrite, boolean openAfterSave) {
        RouteConverter r = RouteConverter.getInstance();

        r.setSavePathPreference(format, file.getParent());

        BaseRoute route = formatAndRoutesModel.getSelectedRoute();
        boolean duplicateFirstPosition = format instanceof NmnFormat && !(format instanceof Nmn7Format);
        int fileCount = NavigationFileParser.getNumberOfFilesToWriteFor(route, format, duplicateFirstPosition);
        if (fileCount > 1) {
            int confirm = JOptionPane.showConfirmDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-split"),
                            Files.shortenPath(file.getPath()), route.getPositionCount(), format.getName(),
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

        saveFile(file, format, route, fileCount, confirmOverwrite, openAfterSave);
    }

    private void saveFile(File file, NavigationFormat format, BaseRoute route, int fileCount,
                          boolean confirmOverwrite, boolean openAfterSave) {
        File[] targets = Files.createTargetFiles(file, fileCount, format.getExtension(), format.getMaximumFileNameLength());
        if (confirmOverwrite) {
            for (File target : targets) {
                if (target.exists()) {
                    String path = Files.createReadablePath(target);
                    if (confirmOverwrite(path))
                        return;
                    break;
                }
            }
        }

        RouteConverter r = RouteConverter.getInstance();
        String targetsAsString = Files.printArrayToDialogString(targets);
        Constants.startWaitCursor(r.getFrame().getRootPane());
        try {
            if (format.isSupportsMultipleRoutes()) {
                new NavigationFileParser().write(formatAndRoutesModel.getRoutes(), (MultipleRoutesFormat) format, targets[0]);
            } else {
                boolean duplicateFirstPosition = format instanceof NmnFormat && !(format instanceof Nmn7Format);
                new NavigationFileParser().write(route, format, duplicateFirstPosition, true, targets);
            }
            formatAndRoutesModel.setModified(false);
            log.info("Saved: " + targetsAsString);

            if (openAfterSave) {
                openPositionList(Files.toUrls(targets[0]), format);
                log.info("Open after save: " + targets[0]);
            }
        } catch (Throwable t) {
            log.severe("Save error " + file + "," + format + ": " + t.getMessage());

            JOptionPane.showMessageDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-error"), urlModel.getShortUrl(), targetsAsString, t.getMessage()),
                    r.getFrame().getTitle(), JOptionPane.ERROR_MESSAGE);
        } finally {
            Constants.stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        }
    }

    public void saveFile() {
        saveFile(new File(urlModel.getString()), formatAndRoutesModel.getFormat(), false, false);
    }

    public void saveAsFile() {
        getChooser().setDialogTitle(RouteConverter.getBundle().getString("save-file-dialog-title"));
        setWriteFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedTarget());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(false);
        int save = getChooser().showSaveDialog(RouteConverter.getInstance().getFrame());
        if (save != JFileChooser.APPROVE_OPTION)
            return;

        File selected = getChooser().getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(getChooser().getFileFilter());
        if (selectedFormat == null)
            selectedFormat = formatAndRoutesModel.getFormat();
        setWriteFormatFileFilterPreference(selectedFormat);
        saveFile(selected, selectedFormat, true, true);
    }

    private NavigationFormat getSelectedFormat(FileFilter fileFilter) {
        NavigationFormat result = null;
        if (fileFilter instanceof NavigationFormatFileFilter)
            result = ((NavigationFormatFileFilter) fileFilter).getFormat();
        return result;
    }

    public void saveToWeb() {
        UploadDialog uploadDialog = new UploadDialog(formatAndRoutesModel, urlModel.getString());
        uploadDialog.pack();
        uploadDialog.restoreLocation();
        uploadDialog.setVisible(true);
        formatAndRoutesModel.setModified(false);
    }

    private final PositionAugmenter augmenter = new PositionAugmenter(RouteConverter.getInstance().getFrame());

    public void addCoordinatesToPositions() {
        new AddCoordinatesToPositions(tablePositions, getPositionsModel(), augmenter).actionPerformed(null);
    }

    public void addElevationToPositions() {
        new AddElevationToPositions(tablePositions, getPositionsModel(), augmenter).actionPerformed(null);
    }

    public void addSpeedToPositions() {
        new AddSpeedToPositions(tablePositions, getPositionsModel(), augmenter).actionPerformed(null);
    }

    public void addPostalAddressToPositions() {
        new AddPostalAddressToPositions(tablePositions, getPositionsModel(), augmenter).actionPerformed(null);
    }

    public void addPopulatedPlaceToPositions() {
        new AddPopulatedPlaceToPositions(tablePositions, getPositionsModel(), augmenter).actionPerformed(null);
    }

    // helpers for actions

    public boolean confirmDiscard() {
        if (formatAndRoutesModel.isModified()) {
            int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                    RouteConverter.getBundle().getString("confirm-discard"),
                    urlModel.getShortUrl(), JOptionPane.YES_NO_CANCEL_OPTION);
            if (confirm == JOptionPane.CANCEL_OPTION)
                return false;
            if (confirm == JOptionPane.YES_OPTION)
                saveFile();
        }
        return true;
    }

    private boolean confirmOverwrite(String file) {
        int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-overwrite"), file),
                RouteConverter.getInstance().getFrame().getTitle(), JOptionPane.YES_NO_OPTION);
        return confirm != JOptionPane.YES_OPTION;
    }

    private void reestablishPositionSelection(final int upOrDown) {
        final int minSelectedRow = tablePositions.getSelectionModel().getMinSelectionIndex();
        final int maxSelectedRow = tablePositions.getSelectionModel().getMaxSelectionIndex();
        if (minSelectedRow != -1 && maxSelectedRow != -1)
            JTableHelper.selectPositions(tablePositions, minSelectedRow + upOrDown, maxSelectedRow + upOrDown);
    }

    public JTable getPositionsView() {
        return tablePositions;
    }

    // handle notifications

    private void handleFormatUpdate() {
        boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;
        boolean existsOneRoute = formatAndRoutesModel.getSize() == 1;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        buttonNewPositionList.setEnabled(supportsMultipleRoutes);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);

        popupTable.handleFormatUpdate(supportsMultipleRoutes, existsMoreThanOnePosition);
    }

    private void handleRoutesUpdate() {
        boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;
        boolean existsARoute = formatAndRoutesModel.getSize() > 0;
        boolean existsOneRoute = formatAndRoutesModel.getSize() == 1;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        comboBoxChoosePositionList.setEnabled(existsMoreThanOneRoute);
        buttonNewPositionList.setEnabled(supportsMultipleRoutes);
        buttonRenamePositionList.setEnabled(existsARoute);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);

        ActionManager actionManager = RouteConverter.getInstance().getContext().getActionManager();

        popupTable.handleRoutesUpdate(supportsMultipleRoutes, existsARoute, existsMoreThanOnePosition);
    }

    private void handlePositionsUpdate() {
        int[] selectedRows = tablePositions.getSelectedRows();
        boolean existsASelectedPosition = selectedRows.length > 0;
        boolean allPositionsSelected = selectedRows.length == tablePositions.getRowCount();
        boolean firstRowNotSelected = existsASelectedPosition && selectedRows[0] != 0;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;

        buttonMovePositionToTop.setEnabled(firstRowNotSelected);
        buttonMovePositionUp.setEnabled(firstRowNotSelected);
        boolean lastRowNotSelected = existsASelectedPosition && selectedRows[selectedRows.length - 1] != tablePositions.getRowCount() - 1;
        buttonMovePositionDown.setEnabled(lastRowNotSelected);
        buttonMovePositionToBottom.setEnabled(lastRowNotSelected);
        buttonDeletePosition.setEnabled(existsASelectedPosition);

        ActionManager actionManager = RouteConverter.getInstance().getContext().getActionManager();
        actionManager.enable("delete-position", existsASelectedPosition);
        actionManager.enable("select-all", existsAPosition && !allPositionsSelected);

        RouteConverter.getInstance().selectPositionsOnMap(selectedRows);

        if (selectedRows.length > 0) {
            boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;
            popupTable.handlePositionsUpdate(supportsMultipleRoutes, selectedRows.length > 0);
        }
    }

    // helpers

    private File createSelectedSource() {
        File source = new File(urlModel.getString());
        source = Files.findExistingPath(source);
        File path = new File(RouteConverter.getInstance().getOpenPathPreference());
        path = Files.findExistingPath(path);
        if (path == null)
            return source;
        else if (source != null)
            return new File(path, source.getName());
        else
            return path;
    }

    private File createSelectedTarget() {
        File file = new File(urlModel.getString());
        NavigationFormat format = formatAndRoutesModel.getFormat();
        File path = new File(RouteConverter.getInstance().getSavePathPreference(format));
        if (!path.exists())
            path = file.getParentFile();
        String fileName = file.getName();
        if (format instanceof GoPalRouteFormat)
            fileName = Files.createGoPalFileName(fileName);
        return new File(Files.calculateConvertFileName(new File(path, fileName),
                format.getExtension(),
                format.getMaximumFileNameLength()));
    }

    // create this only once to make users choices stable at least for one program start
    private JFileChooser chooser;

    private synchronized JFileChooser getChooser() {
        if (chooser == null)
            chooser = Constants.createJFileChooser();
        return chooser;
    }

    private void setFormatFileFilters(JFileChooser chooser, List<NavigationFormat> formats, String selectedFormat) {
        chooser.resetChoosableFileFilters();
        FileFilter fileFilter = chooser.getFileFilter();
        for (NavigationFormat format : formats) {
            NavigationFormatFileFilter navigationFormatFileFilter = new NavigationFormatFileFilter(format);
            if (format.getClass().getName().equals(selectedFormat))
                fileFilter = navigationFormatFileFilter;
            chooser.addChoosableFileFilter(navigationFormatFileFilter);
        }
        chooser.setFileFilter(fileFilter);
    }

    private void setReadFormatFileFilters(JFileChooser chooser) {
        setFormatFileFilters(chooser, NavigationFormats.getReadFormatsSortedByName(),
                RouteConverter.getInstance().getOpenFormatPreference());
    }

    private void setReadFormatFileFilterPreference(NavigationFormat selectedFormat) {
        String preference = selectedFormat != null ? selectedFormat.getClass().getName() : "";
        RouteConverter.getInstance().setOpenFormatPreference(preference);
    }

    private void setWriteFormatFileFilters(JFileChooser chooser) {
        setFormatFileFilters(chooser, NavigationFormats.getWriteFormatsSortedByName(),
                RouteConverter.getInstance().getSaveFormatPreference());
    }

    private void setWriteFormatFileFilterPreference(NavigationFormat selectedFormat) {
        String preference = selectedFormat.getClass().getName();
        RouteConverter.getInstance().setSaveFormatPreference(preference);
    }

    // map view related helpers

    public PositionsModel getPositionsModel() {
        return formatAndRoutesModel.getPositionsModel();
    }

    public PositionsSelectionModel getPositionsSelectionModel() {
        return new PositionsSelectionModel() {
            public void setSelectedPositions(int[] selectedPositions) {
                new ContinousRange(selectedPositions, new RangeOperation() {
                    public void performOnIndex(int index) {
                        tablePositions.getSelectionModel().addSelectionInterval(index, index);
                    }

                    public void performOnRange(int firstIndex, int lastIndex) {
                        JTableHelper.scrollToPosition(tablePositions, firstIndex);
                    }
                }).performMonotonicallyIncreasing(20);
            }
        };
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return formatAndRoutesModel.getCharacteristicsModel();
    }

    // helpers for external components

    public void selectPosition(int index) {
        tablePositions.getSelectionModel().addSelectionInterval(index, index);
        JTableHelper.scrollToPosition(tablePositions, index);
    }

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
        convertPanel.setLayout(new GridLayoutManager(7, 2, new Insets(3, 3, 0, 3), -1, -1));
        convertPanel.setMinimumSize(new Dimension(-1, -1));
        convertPanel.setPreferredSize(new Dimension(560, 560));
        final JScrollPane scrollPane1 = new JScrollPane();
        convertPanel.add(scrollPane1, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePositions = new JTable();
        tablePositions.setAutoCreateColumnsFromModel(false);
        tablePositions.setShowHorizontalLines(false);
        tablePositions.setShowVerticalLines(false);
        scrollPane1.setViewportView(tablePositions);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFormat = new JLabel();
        labelFormat.setHorizontalAlignment(2);
        labelFormat.setHorizontalTextPosition(2);
        labelFormat.setText("-");
        panel1.add(labelFormat, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("content"));
        convertPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(10);
        label2.setHorizontalTextPosition(11);
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("format"));
        convertPanel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelPositions = new JLabel();
        labelPositions.setHorizontalAlignment(2);
        labelPositions.setHorizontalTextPosition(2);
        labelPositions.setText("-");
        panel2.add(labelPositions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelLength = new JLabel();
        labelLength.setHorizontalAlignment(2);
        labelLength.setHorizontalTextPosition(2);
        labelLength.setText("-");
        labelLength.setVisible(true);
        panel2.add(labelLength, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(4);
        label3.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("length"));
        label3.setVisible(true);
        panel2.add(label3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setHorizontalAlignment(4);
        label4.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("duration"));
        panel2.add(label4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelDuration = new JLabel();
        labelDuration.setHorizontalAlignment(2);
        labelDuration.setHorizontalTextPosition(2);
        labelDuration.setText("-");
        panel2.add(labelDuration, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPositionLists = new JLabel();
        labelPositionLists.setHorizontalAlignment(2);
        labelPositionLists.setHorizontalTextPosition(2);
        labelPositionLists.setText("-");
        convertPanel.add(labelPositionLists, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("positions"));
        convertPanel.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("position-list"));
        label6.setVerticalAlignment(1);
        label6.setVerticalTextPosition(0);
        label6.setVisible(true);
        convertPanel.add(label6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel3, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewPositionList = new JButton();
        buttonNewPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new-route.png")));
        buttonNewPositionList.setText("");
        buttonNewPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-positionlist-tooltip"));
        panel3.add(buttonNewPositionList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenamePositionList = new JButton();
        buttonRenamePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/rename-route.png")));
        buttonRenamePositionList.setText("");
        buttonRenamePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename-positionlist-tooltip"));
        panel3.add(buttonRenamePositionList, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemovePositionList = new JButton();
        buttonRemovePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-route.png")));
        buttonRemovePositionList.setText("");
        buttonRemovePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remove-position-list-tooltip"));
        panel3.add(buttonRemovePositionList, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setHorizontalAlignment(4);
        label7.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("characteristics"));
        panel3.add(label7, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionListCharacteristics = new JComboBox();
        panel3.add(comboBoxChoosePositionListCharacteristics, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionList = new JComboBox();
        comboBoxChoosePositionList.setVisible(true);
        convertPanel.add(comboBoxChoosePositionList, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel4, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonMovePositionToTop = new JButton();
        buttonMovePositionToTop.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/top.png")));
        buttonMovePositionToTop.setText("");
        buttonMovePositionToTop.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-top-tooltip"));
        panel4.add(buttonMovePositionToTop, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionUp = new JButton();
        buttonMovePositionUp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/up.png")));
        buttonMovePositionUp.setText("");
        buttonMovePositionUp.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-up-tooltip"));
        panel4.add(buttonMovePositionUp, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewPosition = new JButton();
        buttonNewPosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/insert-position.png")));
        buttonNewPosition.setText("");
        buttonNewPosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-position-action-tooltip"));
        panel4.add(buttonNewPosition, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeletePosition = new JButton();
        buttonDeletePosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/delete-position.png")));
        buttonDeletePosition.setText("");
        buttonDeletePosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-position-action-tooltip"));
        panel4.add(buttonDeletePosition, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionDown = new JButton();
        buttonMovePositionDown.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/down.png")));
        buttonMovePositionDown.setText("");
        buttonMovePositionDown.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-down-tooltip"));
        panel4.add(buttonMovePositionDown, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToBottom = new JButton();
        buttonMovePositionToBottom.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/bottom.png")));
        buttonMovePositionToBottom.setText("");
        buttonMovePositionToBottom.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-bottom-tooltip"));
        panel4.add(buttonMovePositionToBottom, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
    public JComponent $$$getRootComponent$$$() {
        return convertPanel;
    }

    public class PositionTablePopupMenu extends TablePopupMenu {
        private JMenuItem buttonDeletePositions, buttonAddCoordinates, buttonAddElevation,
                buttonAddPostalAddress, buttonAddPopulatedPlace, buttonAddSpeed, buttonAddIndex,
                buttonSplitPositionlist;

        public PositionTablePopupMenu() {
            super(tablePositions);
        }

        protected JPopupMenu createPopupMenu() {
            JPopupMenu popupMenu = new JPopupMenu();

            // TODO add cut, copy, paste menu items here
            popupMenu.add(JMenuHelper.createItem("select-all"));
            popupMenu.addSeparator();
            popupMenu.add(JMenuHelper.createItem("new-position"));
            popupMenu.add(JMenuHelper.createItem("delete-position"));
            popupMenu.addSeparator();

            buttonAddCoordinates = new JMenuItem(RouteConverter.getBundle().getString("add-coordinates"));
            buttonAddCoordinates.addActionListener(new AddCoordinatesToPositions(tablePositions, getPositionsModel(), augmenter));
            popupMenu.add(buttonAddCoordinates);

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
            final Map<JMenuItem, MergePositionList> menuItem2MergePositonList = new HashMap<JMenuItem, MergePositionList>();
            popupMenu.add(menuMergePositionlist);

            formatAndRoutesModel.addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                        BaseRoute route = formatAndRoutesModel.getRoute(i);
                        // initialization code
                        MergePositionList mergePositionList = new MergePositionList(RouteConverter.getInstance().getFrame(), tablePositions, comboBoxChoosePositionList, route, getPositionsModel(), formatAndRoutesModel);
                        JMenuItem menuItem = new JMenuItem(mergePositionList);
                        menuItem2MergePositonList.put(menuItem, mergePositionList);
                        // end of initialization code
                        menuItem.setText(RouteComments.shortenRouteName(route));
                        menuMergePositionlist.add(menuItem, i);
                    }
                }

                public void intervalRemoved(ListDataEvent e) {
                    for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                        // clean up code
                        JMenuItem menuItem = i < menuMergePositionlist.getMenuComponentCount() ? (JMenuItem) menuMergePositionlist.getMenuComponent(i) : null;
                        if (menuItem != null) {
                            MergePositionList mergePositionList = menuItem2MergePositonList.get(menuItem);
                            if (mergePositionList != null)
                                mergePositionList.cleanup();
                            menuItem2MergePositonList.remove(menuItem);
                            menuItem.setAction(null);
                        }
                        // end of clean up code
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
            buttonAddCoordinates.setEnabled(existsASelectedPosition);
            buttonAddElevation.setEnabled(existsASelectedPosition);
            buttonAddPostalAddress.setEnabled(existsASelectedPosition);
            buttonAddPopulatedPlace.setEnabled(existsASelectedPosition);
            buttonAddSpeed.setEnabled(existsASelectedPosition);
            buttonAddIndex.setEnabled(existsASelectedPosition);
            buttonSplitPositionlist.setEnabled(supportsMultipleRoutes && existsASelectedPosition);
        }
    }
}
