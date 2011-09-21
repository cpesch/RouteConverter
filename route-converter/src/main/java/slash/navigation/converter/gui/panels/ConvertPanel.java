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
import slash.navigation.converter.gui.dnd.ClipboardInteractor;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.dnd.PositionSelection;
import slash.navigation.converter.gui.helper.*;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.renderer.RouteCharacteristicsListCellRenderer;
import slash.navigation.converter.gui.renderer.RouteListCellRenderer;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gui.*;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static slash.common.io.Files.*;
import static slash.navigation.base.NavigationFileParser.getNumberOfFilesToWriteFor;
import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;
import static slash.navigation.base.NavigationFormats.getReadFormatsWithPreferredFormat;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.gui.Constants.*;

/**
 * The convert panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class ConvertPanel {
    private static final Logger log = Logger.getLogger(ConvertPanel.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(ConvertPanel.class);

    private static final String DUPLICATE_FIRST_POSITION_PREFERENCE = "duplicateFirstPosition";

    private UrlDocument urlModel = new UrlDocument();
    private RecentUrlsModel recentUrlsModel = new RecentUrlsModel();
    private FormatAndRoutesModel formatAndRoutesModel;
    private PositionsSelectionModel positionsSelectionModel;

    protected JPanel convertPanel;
    private JLabel labelFormat;
    private JLabel labelPositionLists;
    private JLabel labelPositions;
    private JLabel labelLength;
    private JLabel labelDuration;
    private JLabel labelOverallAscend;
    private JLabel labelOverallDescend;
    protected JTable tablePositions;
    private JComboBox comboBoxChoosePositionList;
    private JComboBox comboBoxChoosePositionListCharacteristics;
    private JButton buttonNewPositionList;
    private JButton buttonRenamePositionList;
    private JButton buttonDeletePositionList;
    private JButton buttonMovePositionToTop;
    private JButton buttonMovePositionUp;
    private JButton buttonNewPosition;
    private JButton buttonDeletePosition;
    private JButton buttonMovePositionDown;
    private JButton buttonMovePositionToBottom;
    private LengthCalculator lengthCalculator;

    public ConvertPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        formatAndRoutesModel = new FormatAndRoutesModel(r.getContext().getUndoManager());
        positionsSelectionModel = new PositionsSelectionModel() {
            public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
                if (replaceSelection) {
                    ListSelectionModel selectionModel = tablePositions.getSelectionModel();
                    selectionModel.clearSelection();
                }

                new ContinousRange(selectedPositions, new RangeOperation() {
                    public void performOnIndex(int index) {
                        ListSelectionModel selectionModel = tablePositions.getSelectionModel();
                        selectionModel.addSelectionInterval(index, index);
                    }

                    public void performOnRange(int firstIndex, int lastIndex) {
                        JTableHelper.scrollToPosition(tablePositions, firstIndex);
                    }

                    public boolean isInterrupted() {
                        return false;
                    }
                }).performMonotonicallyIncreasing(20);
            }
        };

        lengthCalculator = new LengthCalculator();
        lengthCalculator.initialize(getPositionsModel(), getCharacteristicsModel());

        new FormatToJLabelAdapter(formatAndRoutesModel, labelFormat);
        new PositionListsToJLabelAdapter(formatAndRoutesModel, labelPositionLists);
        new PositionsCountToJLabelAdapter(getPositionsModel(), labelPositions);
        new LengthToJLabelAdapter(getPositionsModel(), lengthCalculator, labelLength, labelDuration);
        new ElevationToJLabelAdapter(getPositionsModel(), labelOverallAscend, labelOverallDescend);

        JMenuHelper.registerAction(buttonNewPositionList, "new-positionlist");
        JMenuHelper.registerAction(buttonRenamePositionList, "rename-positionlist");
        JMenuHelper.registerAction(buttonDeletePositionList, "delete-positionlist");

        buttonMovePositionToTop.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().top(selectedRows);
                    selectPositions(selectedRows);
                }
            }
        });

        buttonMovePositionUp.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().up(selectedRows, 1);
                    selectPositions(Range.increment(selectedRows, -1));
                }
            }
        });

        buttonNewPosition.addActionListener(r.getContext().getActionManager().get("new-position"));
        buttonDeletePosition.addActionListener(r.getContext().getActionManager().get("delete"));

        buttonMovePositionDown.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().down(selectedRows, 1);
                    selectPositions(Range.increment(selectedRows, +1));
                }
            }
        });

        buttonMovePositionToBottom.addActionListener(new FrameAction() {
            public void run() {
                int[] selectedRows = tablePositions.getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().bottom(selectedRows);
                    selectPositions(selectedRows);
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

        tablePositions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                handlePositionsUpdate();
            }
        });

        final ActionManager actionManager = r.getContext().getActionManager();
        tablePositions.setModel(getPositionsModel());
        PositionsTableColumnModel tableColumnModel = new PositionsTableColumnModel();
        tablePositions.setColumnModel(tableColumnModel);
        tablePositions.registerKeyboardAction(new FrameAction() {
                    public void run() {
                        actionManager.run("delete");
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.setDragEnabled(true);
        tablePositions.setDropMode(DropMode.ON);
        tablePositions.setTransferHandler(new TableDragAndDropHandler());

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                handlePositionsUpdate();
            }
        });

        JMenuBar menuBar = Application.getInstance().getContext().getMenuBar();
        new TableHeaderMenu(tablePositions.getTableHeader(), menuBar, tableColumnModel);
        JPopupMenu menu = new TablePopupMenu(tablePositions).createMenu();
        JMenu mergeMenu = (JMenu) JMenuHelper.findMenuComponent(menu, "merge-positionlist");
        new MergePositionListMenu(mergeMenu, getPositionsView(), getFormatAndRoutesModel());

        ClipboardInteractor clipboardInteractor = new ClipboardInteractor();
        clipboardInteractor.watchClipboard();
        actionManager.register("undo", new UndoAction());
        actionManager.register("redo", new RedoAction());
        actionManager.register("copy", new CopyAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("cut", new CutAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("delete", new DeleteAction(getPositionsView(), getPositionsModel()));
        actionManager.register("new-position", new NewPositionAction(getPositionsView(), getPositionsModel(), getPositionsSelectionModel()));
        actionManager.register("new-file", new NewFileAction(this));
        actionManager.register("open", new OpenAction(this));
        actionManager.register("paste", new PasteAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("save", new SaveAction(this));
        actionManager.register("save-as", new SaveAsAction(this));
        actionManager.register("select-all", new SelectAllAction(getPositionsView()));
        actionManager.register("new-positionlist", new NewPositionListAction(getFormatAndRoutesModel()));
        actionManager.register("rename-positionlist", new RenamePositionListAction(getFormatAndRoutesModel()));
        actionManager.register("delete-positionlist", new DeletePositionListAction(getFormatAndRoutesModel()));
        BatchPositionAugmenter augmenter = new BatchPositionAugmenter(r.getFrame());
        actionManager.register("add-coordinates", new AddCoordinatesToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("add-elevation", new AddElevationToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("add-postal-address", new AddPostalAddressToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("add-populated-place", new AddPopulatedPlaceToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("add-speed", new AddSpeedToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("add-number", new AddNumberToPositions(tablePositions, getPositionsModel(), augmenter));
        actionManager.register("split-positionlist", new SplitPositionList(tablePositions, getPositionsModel(), formatAndRoutesModel));
        actionManager.register("import-positionlist", new ImportPositionList(RouteConverter.getInstance(), tablePositions, getPositionsModel()));

        JMenuHelper.registerKeyStroke(tablePositions, "copy");
        JMenuHelper.registerKeyStroke(tablePositions, "cut");
        JMenuHelper.registerKeyStroke(tablePositions, "paste");

        formatAndRoutesModel.addModifiedListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                actionManager.enable("save", formatAndRoutesModel.isModified());
            }
        });

        UndoManager undoManager = Application.getInstance().getContext().getUndoManager();
        undoManager.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleUndoUpdate();
            }
        });

        handleUndoUpdate();
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

        convertPanel.setTransferHandler(new PanelDropHandler());

        // make sure that Insert works directly after the program start on an empty position list
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                convertPanel.requestFocus();
            }
        });
    }

    public void dispose() {
        lengthCalculator.dispose();
    }

    public Component getRootComponent() {
        return convertPanel;
    }

    public JButton getDefaultButton() {
        return buttonNewPositionList;
    }

    // action methods

    public void openUrls(List<URL> urls) {
        if (!confirmDiscard())
            return;

        // make copy which we could modify freely
        List<URL> copy = new ArrayList<URL>(urls);
        for (Iterator<URL> it = copy.iterator(); it.hasNext(); ) {
            URL url = it.next();
            File file = Files.toFile(url);
            if (file != null && (!file.exists() || !file.isFile())) {
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

    public void openUrl(URL url) {
        if (!confirmDiscard())
            return;

        openPositionList(Arrays.asList(url));
    }

    public void openFile() {
        if (!confirmDiscard())
            return;

        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("open-file-dialog-title"));
        setReadFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedSource());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);
        UndoManager undoManager = Application.getInstance().getContext().getUndoManager();
        undoManager.discardAllEdits();

        List<URL> urls = toUrls(selected);
        List<NavigationFormat> formats = selectedFormat != null ?
                getReadFormatsWithPreferredFormat(selectedFormat) :
                getReadFormatsPreferredByExtension(Files.getExtension(urls));
        openPositionList(urls, formats);
    }

    public void openPositionList(final List<URL> urls) {
        UndoManager undoManager = Application.getInstance().getContext().getUndoManager();
        undoManager.discardAllEdits();
        openPositionList(urls, getReadFormatsPreferredByExtension(Files.getExtension(urls)));
    }

    @SuppressWarnings("unchecked")
    public void openPositionList(final List<URL> urls, final List<NavigationFormat> formats) {
        final RouteConverter r = RouteConverter.getInstance();

        final URL url = urls.get(0);
        final String path = createReadablePath(url);
        r.setOpenPathPreference(path);

        startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        new Thread(new Runnable() {
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            Gpx11Format gpxFormat = new Gpx11Format();
                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                            urlModel.clear();
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

                    if (parser.read(url, formats)) {
                        log.info("Opened: " + path);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                urlModel.setString(path);
                                recentUrlsModel.addUrl(url);
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
                    r.handleFileNotFound(path);
                } catch (Throwable t) {
                    r.handleOpenError(t, path);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "UrlOpener").start();
    }

    @SuppressWarnings("unchecked")
    private void appendPositionList(final List<URL> urls) { // TODO very similar to ImportPositionList#importPositionList()
        final RouteConverter r = RouteConverter.getInstance();

        try {
            for (final URL url : urls) {
                String path = createReadablePath(url);

                final NavigationFileParser parser = new NavigationFileParser();
                if (parser.read(url)) {
                    log.info("Appended: " + path);

                    final String finalPath = path;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                // if there is no file loaded: parseArgs()
                                if (formatAndRoutesModel.getRoutes() == null) {
                                    formatAndRoutesModel.setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                    comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                    urlModel.setString(finalPath);
                                    recentUrlsModel.addUrl(url);
                                } else {
                                    getPositionsModel().add(getPositionsModel().getRowCount(), parser.getTheRoute());
                                }
                            } catch (FileNotFoundException e) {
                                r.handleFileNotFound(finalPath);
                            } catch (IOException e) {
                                r.handleOpenError(e, finalPath);
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
        }
    }

    @SuppressWarnings("unchecked")
    public void newFile() {
        if (!confirmDiscard())
            return;

        startWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        try {
            Gpx11Format gpxFormat = new Gpx11Format();
            GpxRoute gpxRoute = new GpxRoute(gpxFormat);
            gpxRoute.setName(MessageFormat.format(RouteConverter.getBundle().getString("new-positionlist-name"), 1));
            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
            urlModel.clear();
            UndoManager undoManager = Application.getInstance().getContext().getUndoManager();
            undoManager.discardAllEdits();
        } finally {
            stopWaitCursor(RouteConverter.getInstance().getFrame().getRootPane());
        }
    }

    public File[] selectFilesToImport() {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("import-positionlist-source"));
        setReadFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedSource());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return null;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return null;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);
        return selected;
    }

    private void saveFile(File file, NavigationFormat format, boolean confirmOverwrite, boolean openAfterSave) {
        RouteConverter r = RouteConverter.getInstance();
        r.setSavePathPreference(format, file.getParent());

        BaseRoute route = formatAndRoutesModel.getSelectedRoute();
        boolean duplicateFirstPosition = format instanceof NmnFormat && !(format instanceof Nmn7Format);
        int fileCount = getNumberOfFilesToWriteFor(route, format, duplicateFirstPosition);
        if (fileCount > 1) {
            int confirm = JOptionPane.showConfirmDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-split"),
                            Files.shortenPath(file.getPath(), 60), route.getPositionCount(), format.getName(),
                            format.getMaximumPositionCount(), fileCount),
                    r.getFrame().getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
            switch (confirm) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    fileCount = 1;
                    break;
                default:
                    return;
            }
        }

        saveFile(file, format, route, fileCount, confirmOverwrite, openAfterSave);
    }

    private void saveFile(File file, NavigationFormat format, BaseRoute route, int fileCount,
                          boolean confirmOverwrite, boolean openAfterSave) {
        File[] targets = createTargetFiles(file, fileCount, format.getExtension(), 255);
        if (confirmOverwrite) {
            for (File target : targets) {
                if (target.exists()) {
                    String path = createReadablePath(target);
                    if (confirmOverwrite(path))
                        return;
                    break;
                }
            }
        }

        RouteConverter r = RouteConverter.getInstance();
        String targetsAsString = printArrayToDialogString(targets);
        startWaitCursor(r.getFrame().getRootPane());
        try {
            if (format.isSupportsMultipleRoutes()) {
                new NavigationFileParser().write(formatAndRoutesModel.getRoutes(), (MultipleRoutesFormat) format, targets[0]);
            } else {
                boolean duplicateFirstPosition = preferences.getBoolean(DUPLICATE_FIRST_POSITION_PREFERENCE, true);
                new NavigationFileParser().write(route, format, duplicateFirstPosition, true, targets);
            }
            formatAndRoutesModel.setModified(false);
            log.info("Saved: " + targetsAsString);

            if (openAfterSave && format.isSupportsReading()) {
                openPositionList(toUrls(targets), getReadFormatsWithPreferredFormat(format));
                log.info("Open after save: " + targets[0]);
            }
            if (confirmOverwrite) {
                URL url = targets[0].toURI().toURL();
                String path = createReadablePath(url);
                urlModel.setString(path);
                recentUrlsModel.addUrl(url);
            }
        } catch (Throwable t) {
            log.severe("Save error " + file + "," + format + ": " + t.getMessage());

            JOptionPane.showMessageDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-error"), urlModel.getShortUrl(), targetsAsString, t.getMessage()),
                    r.getFrame().getTitle(), JOptionPane.ERROR_MESSAGE);
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    public void saveFile() {
        saveFile(new File(urlModel.getString()), formatAndRoutesModel.getFormat(), false, false);
    }

    public void saveAsFile() {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("save-file-dialog-title"));
        setWriteFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedTarget());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int save = chooser.showSaveDialog(RouteConverter.getInstance().getFrame());
        if (save != JFileChooser.APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        if (selectedFormat == null)
            selectedFormat = formatAndRoutesModel.getFormat();
        setWriteFormatFileFilterPreference(selectedFormat);
        saveFile(selected, selectedFormat, true, !formatAndRoutesModel.getFormat().equals(selectedFormat));
    }

    private NavigationFormat getSelectedFormat(FileFilter fileFilter) {
        NavigationFormat result = null;
        if (fileFilter instanceof NavigationFormatFileFilter)
            result = ((NavigationFormatFileFilter) fileFilter).getFormat();
        return result;
    }

    // helpers for actions

    public boolean confirmDiscard() {
        if (formatAndRoutesModel.isModified()) {
            int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                    RouteConverter.getBundle().getString("confirm-discard"),
                    urlModel.getShortUrl(), JOptionPane.YES_NO_CANCEL_OPTION);
            switch (confirm) {
                case JOptionPane.YES_OPTION:
                    saveFile();
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean confirmOverwrite(String file) {
        int confirm = JOptionPane.showConfirmDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-overwrite"), file),
                RouteConverter.getInstance().getFrame().getTitle(), JOptionPane.YES_NO_OPTION);
        return confirm != JOptionPane.YES_OPTION;
    }

    public JTable getPositionsView() {
        return tablePositions;
    }

    // handle notifications

    private void handleUndoUpdate() {
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        UndoManager undoManager = Application.getInstance().getContext().getUndoManager();
        actionManager.enable("undo", undoManager.canUndo());
        actionManager.enable("redo", undoManager.canRedo());
    }

    private void handleRoutesUpdate() {
        boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;
        boolean existsARoute = formatAndRoutesModel.getSize() > 0;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;
        RouteCharacteristics characteristics = formatAndRoutesModel.getCharacteristicsModel().getSelectedCharacteristics();

        comboBoxChoosePositionList.setEnabled(existsMoreThanOneRoute);

        ActionManager actionManager = RouteConverter.getInstance().getContext().getActionManager();
        actionManager.enable("insert-positions", existsMoreThanOnePosition);
        actionManager.enable("delete-positions", existsMoreThanOnePosition);
        actionManager.enable("new-positionlist", supportsMultipleRoutes);
        actionManager.enable("rename-positionlist", existsARoute);
        actionManager.enable("convert-route-to-track", existsAPosition && characteristics.equals(Route));
        actionManager.enable("convert-track-to-route", existsAPosition && characteristics.equals(Track));
        actionManager.enable("delete-positionlist", existsMoreThanOneRoute);
        actionManager.enable("split-positionlist", supportsMultipleRoutes && existsARoute && existsMoreThanOnePosition);
    }

    private int[] selectedPositionIndices = null;

    private void handlePositionsUpdate() {
        int[] selectedRows = tablePositions.getSelectedRows();
        // avoid firing events of the selection hasn't changed
        if (Arrays.equals(this.selectedPositionIndices, selectedRows))
            return;

        this.selectedPositionIndices = selectedRows;
        boolean existsASelectedPosition = selectedRows.length > 0;
        boolean allPositionsSelected = selectedRows.length == tablePositions.getRowCount();
        boolean firstRowNotSelected = existsASelectedPosition && selectedRows[0] != 0;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;
        boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;

        buttonMovePositionToTop.setEnabled(firstRowNotSelected);
        buttonMovePositionUp.setEnabled(firstRowNotSelected);
        boolean lastRowNotSelected = existsASelectedPosition && selectedRows[selectedRows.length - 1] != tablePositions.getRowCount() - 1;
        buttonMovePositionDown.setEnabled(lastRowNotSelected);
        buttonMovePositionToBottom.setEnabled(lastRowNotSelected);

        ActionManager actionManager = RouteConverter.getInstance().getContext().getActionManager();
        actionManager.enable("cut", existsASelectedPosition);
        actionManager.enable("copy", existsASelectedPosition);
        actionManager.enable("delete", existsASelectedPosition);
        actionManager.enable("select-all", existsAPosition && !allPositionsSelected);
        JMenuHelper.findMenu(RouteConverter.getInstance().getFrame().getJMenuBar(), "position", "complete").setEnabled(existsASelectedPosition);
        actionManager.enable("add-coordinates", existsASelectedPosition);
        actionManager.enable("add-elevation", existsASelectedPosition);
        actionManager.enable("add-postal-address", existsASelectedPosition);
        actionManager.enable("add-populated-place", existsASelectedPosition);
        actionManager.enable("add-speed", existsASelectedPosition);
        actionManager.enable("add-number", existsASelectedPosition);
        actionManager.enable("split-positionlist", supportsMultipleRoutes && existsASelectedPosition);
        actionManager.enable("insert-positions", existsAPosition);
        actionManager.enable("delete-positions", existsAPosition);
        actionManager.enable("revert-positions", existsMoreThanOnePosition);

        RouteConverter.getInstance().selectPositions(selectedRows);
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
        if (format instanceof GoPal3RouteFormat)
            fileName = Files.createGoPalFileName(fileName);
        return new File(Files.calculateConvertFileName(new File(path, fileName), "", format.getMaximumFileNameLength()));
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

    public FormatAndRoutesModel getFormatAndRoutesModel() {
        return formatAndRoutesModel;
    }

    private PositionsModel getPositionsModel() {
        return getFormatAndRoutesModel().getPositionsModel();
    }

    public PositionsSelectionModel getPositionsSelectionModel() {
        return positionsSelectionModel;
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return formatAndRoutesModel.getCharacteristicsModel();
    }

    // helpers for external components

    public UrlDocument getUrlModel() {
        return urlModel;
    }

    public RecentUrlsModel getRecentUrlsModel() {
        return recentUrlsModel;
    }

    private void selectPositions(int[] selectedPositions) {
        clearSelection();
        new ContinousRange(selectedPositions, new RangeOperation() {
            public void performOnIndex(int index) {
            }

            public void performOnRange(int firstIndex, int lastIndex) {
                tablePositions.getSelectionModel().addSelectionInterval(firstIndex, lastIndex);
            }

            public boolean isInterrupted() {
                return false;
            }
        }).performMonotonicallyIncreasing();
    }

    public int selectPositionsWithinDistanceToPredecessor(int distance) {
        int[] indices = getPositionsModel().getPositionsWithinDistanceToPredecessor(distance);
        selectPositions(indices);
        return indices.length;
    }

    public int[] selectAllButEveryNthPosition(int order) {
        int rowCount = getPositionsModel().getRowCount();
        int[] indices = Range.allButEveryNthAndFirstAndLast(rowCount, order);
        selectPositions(indices);
        return new int[]{indices.length, rowCount - indices.length};
    }

    public int selectInsignificantPositions(int threshold) {
        int[] indices = getPositionsModel().getInsignificantPositions(threshold);
        selectPositions(indices);
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
        panel2.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
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
        labelOverallDescend = new JLabel();
        labelOverallDescend.setHorizontalAlignment(2);
        labelOverallDescend.setHorizontalTextPosition(2);
        labelOverallDescend.setText("-");
        labelOverallDescend.setVisible(true);
        panel2.add(labelOverallDescend, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setHorizontalAlignment(4);
        label5.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("overall-descend"));
        label5.setVisible(true);
        panel2.add(label5, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("overall-ascend"));
        panel2.add(label6, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelOverallAscend = new JLabel();
        labelOverallAscend.setHorizontalAlignment(2);
        labelOverallAscend.setHorizontalTextPosition(2);
        labelOverallAscend.setText("-");
        panel2.add(labelOverallAscend, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPositionLists = new JLabel();
        labelPositionLists.setHorizontalAlignment(2);
        labelPositionLists.setHorizontalTextPosition(2);
        labelPositionLists.setText("-");
        convertPanel.add(labelPositionLists, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("position-list"));
        label7.setVerticalAlignment(1);
        label7.setVerticalTextPosition(0);
        label7.setVisible(true);
        convertPanel.add(label7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel3, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewPositionList = new JButton();
        buttonNewPositionList.setHideActionText(true);
        buttonNewPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new-route.png")));
        buttonNewPositionList.setText("");
        buttonNewPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-positionlist-action-tooltip"));
        panel3.add(buttonNewPositionList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenamePositionList = new JButton();
        buttonRenamePositionList.setHideActionText(true);
        buttonRenamePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/rename-route.png")));
        buttonRenamePositionList.setText("");
        buttonRenamePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename-positionlist-action-tooltip"));
        panel3.add(buttonRenamePositionList, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeletePositionList = new JButton();
        buttonDeletePositionList.setHideActionText(true);
        buttonDeletePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-route.png")));
        buttonDeletePositionList.setText("");
        buttonDeletePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-positionlist-action-tooltip"));
        panel3.add(buttonDeletePositionList, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setHorizontalAlignment(4);
        label8.setHorizontalTextPosition(4);
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("characteristics"));
        panel3.add(label8, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionListCharacteristics = new JComboBox();
        panel3.add(comboBoxChoosePositionListCharacteristics, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionList = new JComboBox();
        comboBoxChoosePositionList.setVisible(true);
        convertPanel.add(comboBoxChoosePositionList, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel4, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonMovePositionToTop = new JButton();
        buttonMovePositionToTop.setFocusable(false);
        buttonMovePositionToTop.setHideActionText(true);
        buttonMovePositionToTop.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/top.png")));
        buttonMovePositionToTop.setText("");
        buttonMovePositionToTop.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-top-tooltip"));
        panel4.add(buttonMovePositionToTop, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionUp = new JButton();
        buttonMovePositionUp.setFocusable(false);
        buttonMovePositionUp.setHideActionText(true);
        buttonMovePositionUp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/up.png")));
        buttonMovePositionUp.setText("");
        buttonMovePositionUp.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-up-tooltip"));
        panel4.add(buttonMovePositionUp, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonNewPosition = new JButton();
        buttonNewPosition.setFocusable(false);
        buttonNewPosition.setHideActionText(true);
        buttonNewPosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new-position-action.png")));
        buttonNewPosition.setText("");
        buttonNewPosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-position-action-tooltip"));
        panel4.add(buttonNewPosition, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeletePosition = new JButton();
        buttonDeletePosition.setFocusable(false);
        buttonDeletePosition.setHideActionText(true);
        buttonDeletePosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/delete-action.png")));
        buttonDeletePosition.setText("");
        buttonDeletePosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-action-tooltip"));
        panel4.add(buttonDeletePosition, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionDown = new JButton();
        buttonMovePositionDown.setFocusable(false);
        buttonMovePositionDown.setHideActionText(true);
        buttonMovePositionDown.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/down.png")));
        buttonMovePositionDown.setText("");
        buttonMovePositionDown.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-down-tooltip"));
        panel4.add(buttonMovePositionDown, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToBottom = new JButton();
        buttonMovePositionToBottom.setFocusable(false);
        buttonMovePositionToBottom.setHideActionText(true);
        buttonMovePositionToBottom.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/bottom.png")));
        buttonMovePositionToBottom.setText("");
        buttonMovePositionToBottom.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-bottom-tooltip"));
        panel4.add(buttonMovePositionToBottom, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("positions"));
        convertPanel.add(label9, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    private class TableDragAndDropHandler extends TransferHandler {
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(PositionSelection.positionFlavor) ||
                    convertPanel.getTransferHandler().canImport(support);
        }

        private int[] toRows(List<BaseNavigationPosition> positions) {
            int[] result = new int[positions.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getPositionsModel().getIndex(positions.get(i));
            }
            return result;
        }

        private void moveRows(int[] rows, TransferSupport support) {
            JTable table = (JTable) support.getComponent();
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            int index = dropLocation.getRow();
            int max = table.getModel().getRowCount();
            if (index < 0 || index > max)
                index = max;

            if (rows[0] > index) {
                getPositionsModel().up(rows, rows[0] - index);
                JTableHelper.selectPositions(table, index, index + rows.length - 1);
            } else {
                getPositionsModel().down(Range.revert(rows), index - rows[0] - rows.length);
                JTableHelper.selectPositions(table, index - rows.length, index - 1);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            Transferable transferable = support.getTransferable();
            try {
                if (support.isDataFlavorSupported(PositionSelection.positionFlavor)) {
                    Object selection = transferable.getTransferData(PositionSelection.positionFlavor);
                    if (selection != null) {
                        PositionSelection positionsSelection = (PositionSelection) selection;
                        int[] rows = toRows(positionsSelection.getPositions());
                        if (rows.length > 0) {
                            moveRows(rows, support);
                            return true;
                        }
                    }
                }
            } catch (UnsupportedFlavorException e) {
                // intentionally left empty
            } catch (IOException e) {
                // intentionally left empty
            }
            return convertPanel.getTransferHandler().importData(support);
        }

        public int getSourceActions(JComponent comp) {
            return MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            int[] selectedRows = tablePositions.getSelectedRows();
            return new PositionSelection(getPositionsModel().getPositions(selectedRows),
                    getPositionsModel().getRoute().getFormat());
        }
    }

}
