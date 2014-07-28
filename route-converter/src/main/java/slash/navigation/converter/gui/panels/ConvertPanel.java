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
import slash.navigation.babel.BabelException;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.FormatAndRoutes;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatParserListener;
import slash.navigation.base.ParserCallback;
import slash.navigation.base.ParserResult;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.AddCoordinatesToPositionsAction;
import slash.navigation.converter.gui.actions.AddElevationToPositionsAction;
import slash.navigation.converter.gui.actions.AddNumberToPositionsAction;
import slash.navigation.converter.gui.actions.AddPopulatedPlaceToPositionsAction;
import slash.navigation.converter.gui.actions.AddPositionAction;
import slash.navigation.converter.gui.actions.AddPositionListAction;
import slash.navigation.converter.gui.actions.AddPostalAddressToPositionsAction;
import slash.navigation.converter.gui.actions.AddSpeedToPositionsAction;
import slash.navigation.converter.gui.actions.AddTimeToPositionsAction;
import slash.navigation.converter.gui.actions.CopyAction;
import slash.navigation.converter.gui.actions.CutAction;
import slash.navigation.converter.gui.actions.DeleteAction;
import slash.navigation.converter.gui.actions.ExportPositionListAction;
import slash.navigation.converter.gui.actions.ImportPositionListAction;
import slash.navigation.converter.gui.actions.NewFileAction;
import slash.navigation.converter.gui.actions.OpenAction;
import slash.navigation.converter.gui.actions.PasteAction;
import slash.navigation.converter.gui.actions.RemovePositionListAction;
import slash.navigation.converter.gui.actions.RenamePositionListAction;
import slash.navigation.converter.gui.actions.SaveAction;
import slash.navigation.converter.gui.actions.SaveAsAction;
import slash.navigation.converter.gui.actions.SelectAllAction;
import slash.navigation.converter.gui.actions.SplitPositionListAction;
import slash.navigation.converter.gui.dialogs.CompleteFlightPlanDialog;
import slash.navigation.converter.gui.dnd.ClipboardInteractor;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.dnd.PositionSelection;
import slash.navigation.converter.gui.helpers.AbstractDocumentListener;
import slash.navigation.converter.gui.helpers.AbstractListDataListener;
import slash.navigation.converter.gui.helpers.LengthCalculator;
import slash.navigation.converter.gui.helpers.MergePositionListMenu;
import slash.navigation.converter.gui.helpers.NavigationFormatFileFilter;
import slash.navigation.converter.gui.helpers.TableHeaderMenu;
import slash.navigation.converter.gui.helpers.TablePopupMenu;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.ElevationToJLabelAdapter;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.FormatToJLabelAdapter;
import slash.navigation.converter.gui.models.LengthToJLabelAdapter;
import slash.navigation.converter.gui.models.PositionListsToJLabelAdapter;
import slash.navigation.converter.gui.models.PositionsCountToJLabelAdapter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;
import slash.navigation.converter.gui.models.RecentFormatsModel;
import slash.navigation.converter.gui.models.RecentUrlsModel;
import slash.navigation.converter.gui.models.UrlDocument;
import slash.navigation.converter.gui.renderer.RouteCharacteristicsListCellRenderer;
import slash.navigation.converter.gui.renderer.RouteListCellRenderer;
import slash.navigation.converter.gui.undo.UndoFormatAndRoutesModel;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.fpl.GarminFlightPlanRoute;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.RangeOperation;
import slash.navigation.gui.helpers.JTableHelper;
import slash.navigation.gui.undo.RedoAction;
import slash.navigation.gui.undo.UndoAction;
import slash.navigation.gui.undo.UndoManager;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.simple.GoRiderGpsFormat;
import slash.navigation.simple.HaicomLoggerFormat;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_END;
import static java.awt.event.KeyEvent.VK_HOME;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.io.Files.calculateConvertFileName;
import static slash.common.io.Files.createGoPalFileName;
import static slash.common.io.Files.createReadablePath;
import static slash.common.io.Files.createTargetFiles;
import static slash.common.io.Files.findExistingPath;
import static slash.common.io.Files.getExtension;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.common.io.Files.reverse;
import static slash.common.io.Files.shortenPath;
import static slash.common.io.Files.toFile;
import static slash.common.io.Files.toUrls;
import static slash.feature.client.Feature.hasFeature;
import static slash.navigation.base.NavigationFormatParser.getNumberOfFilesToWriteFor;
import static slash.navigation.base.NavigationFormats.getFormatsSortedByName;
import static slash.navigation.base.NavigationFormats.getReadFormats;
import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;
import static slash.navigation.base.NavigationFormats.getReadFormatsSortedByName;
import static slash.navigation.base.NavigationFormats.getReadFormatsWithPreferredFormat;
import static slash.navigation.base.NavigationFormats.getWriteFormatsWithPreferredFormats;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.converter.gui.dnd.PositionSelection.positionFlavor;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startMail;
import static slash.navigation.gui.events.Range.allButEveryNthAndFirstAndLast;
import static slash.navigation.gui.events.Range.increment;
import static slash.navigation.gui.events.Range.revert;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;
import static slash.navigation.gui.helpers.JMenuHelper.findMenuComponent;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JMenuHelper.registerKeyStroke;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;
import static slash.navigation.gui.helpers.JTableHelper.selectAndScrollToPosition;
import static slash.navigation.gui.helpers.UIHelper.createJFileChooser;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;

/**
 * The convert panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class ConvertPanel implements PanelInTab {
    private static final Logger log = Logger.getLogger(ConvertPanel.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(ConvertPanel.class);

    private static final String READ_COUNT_PREFERENCE = "readCount";
    private static final String READ_FORMAT_PREFERENCE = "readFormat";
    private static final String READ_PATH_PREFERENCE = "readPath";
    private static final String WRITE_COUNT_PREFERENCE = "writeCount";
    private static final String WRITE_FORMAT_PREFERENCE = "writeFormat";
    private static final String WRITE_PATH_PREFERENCE = "writePath";
    private static final String DUPLICATE_FIRST_POSITION_PREFERENCE = "duplicateFirstPosition";

    private UrlDocument urlModel = new UrlDocument();
    private RecentUrlsModel recentUrlsModel = new RecentUrlsModel();
    private RecentFormatsModel recentFormatsModel = new RecentFormatsModel();
    private FormatAndRoutesModel formatAndRoutesModel;
    private PositionsSelectionModel positionsSelectionModel;
    private LengthCalculator lengthCalculator;

    private JPanel convertPanel;
    private JLabel labelFormat;
    private JLabel labelPositionLists;
    private JLabel labelPositions;
    private JLabel labelLength;
    private JLabel labelDuration;
    private JLabel labelOverallAscend;
    private JLabel labelOverallDescend;
    private JTable tablePositions;
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
    private TableHeaderMenu tableHeaderMenu;

    public ConvertPanel() {
        $$$setupUI$$$();
        initialize();
        logFormatUsage();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        formatAndRoutesModel = new UndoFormatAndRoutesModel(r.getContext().getUndoManager());
        positionsSelectionModel = new PositionsSelectionModel() {
            public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
                if (replaceSelection) {
                    ListSelectionModel selectionModel = tablePositions.getSelectionModel();
                    selectionModel.clearSelection();
                }

                int maximumRangeLength = selectedPositions.length > 19 ? selectedPositions.length / 100 : selectedPositions.length;
                new ContinousRange(selectedPositions, new RangeOperation() {
                    public void performOnIndex(int index) {
                    }

                    public void performOnRange(int firstIndex, int lastIndex) {
                        ListSelectionModel selectionModel = tablePositions.getSelectionModel();
                        selectionModel.addSelectionInterval(firstIndex, lastIndex);
                        scrollToPosition(tablePositions, firstIndex);
                    }

                    public boolean isInterrupted() {
                        return false;
                    }
                }).performMonotonicallyIncreasing(maximumRangeLength);
            }
        };

        lengthCalculator = new LengthCalculator();
        lengthCalculator.initialize(getPositionsModel(), getCharacteristicsModel());

        new FormatToJLabelAdapter(formatAndRoutesModel, labelFormat);
        new PositionListsToJLabelAdapter(formatAndRoutesModel, labelPositionLists);
        new PositionsCountToJLabelAdapter(getPositionsModel(), labelPositions);
        new LengthToJLabelAdapter(getPositionsModel(), lengthCalculator, labelLength, labelDuration);
        new ElevationToJLabelAdapter(getPositionsModel(), labelOverallAscend, labelOverallDescend);

        registerAction(buttonNewPositionList, "new-positionlist");
        registerAction(buttonRenamePositionList, "rename-positionlist");
        registerAction(buttonDeletePositionList, "delete-positionlist");

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
                    selectPositions(increment(selectedRows, -1));
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
                    selectPositions(increment(selectedRows, +1));
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
        r.getUnitSystemModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getPositionsModel().fireTableRowsUpdated(0, MAX_VALUE, ALL_COLUMNS);
            }
        });

        tablePositions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                if (getPositionsModel().isContinousRange())
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
        }, getKeyStroke(VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tablePositions, 0, 0);
            }
        }, getKeyStroke(VK_HOME, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tablePositions, 0, tablePositions.getSelectedRow());
            }
        }, getKeyStroke(VK_HOME, SHIFT_DOWN_MASK), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                int lastRow = tablePositions.getRowCount() - 1;
                selectAndScrollToPosition(tablePositions, lastRow, lastRow);
            }
        }, getKeyStroke(VK_END, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tablePositions, tablePositions.getRowCount() - 1, tablePositions.getSelectedRow());
            }
        }, getKeyStroke(VK_END, SHIFT_DOWN_MASK), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tablePositions.setDragEnabled(true);
        tablePositions.setDropMode(ON);
        TableDragAndDropHandler dropHandler = new TableDragAndDropHandler(new PanelDropHandler());
        tablePositions.setTransferHandler(dropHandler);

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (!isFirstToLastRow(e))
                    return;
                if (getPositionsModel().isContinousRange())
                    return;

                handlePositionsUpdate();
            }
        });

        JMenuBar menuBar = Application.getInstance().getContext().getMenuBar();
        tableHeaderMenu = new TableHeaderMenu(tablePositions.getTableHeader(), menuBar, getPositionsModel(),
                tableColumnModel, actionManager);
        JPopupMenu menu = new TablePopupMenu(tablePositions).createPopupMenu();
        JMenu mergeMenu = (JMenu) findMenuComponent(menu, "merge-positionlist");
        new MergePositionListMenu(mergeMenu, getPositionsView(), getFormatAndRoutesModel());

        ClipboardInteractor clipboardInteractor = new ClipboardInteractor();
        clipboardInteractor.watchClipboard();
        actionManager.register("undo", new UndoAction());
        actionManager.register("redo", new RedoAction());
        actionManager.register("copy", new CopyAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("cut", new CutAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("delete", new DeleteAction(getPositionsView(), getPositionsModel()));
        actionManager.register("new-position", new AddPositionAction(getPositionsView(), getPositionsModel(), getPositionsSelectionModel()));
        actionManager.register("new-file", new NewFileAction(this));
        actionManager.register("open", new OpenAction(this));
        actionManager.register("paste", new PasteAction(getPositionsView(), getPositionsModel(), clipboardInteractor));
        actionManager.register("save", new SaveAction(this));
        actionManager.register("save-as", new SaveAsAction(this));
        actionManager.register("select-all", new SelectAllAction(getPositionsView()));
        actionManager.register("new-positionlist", new AddPositionListAction(getFormatAndRoutesModel()));
        actionManager.register("rename-positionlist", new RenamePositionListAction(getFormatAndRoutesModel()));
        actionManager.register("delete-positionlist", new RemovePositionListAction(getFormatAndRoutesModel()));
        actionManager.register("add-coordinates", new AddCoordinatesToPositionsAction());
        actionManager.register("add-elevation", new AddElevationToPositionsAction());
        actionManager.register("add-postal-address", new AddPostalAddressToPositionsAction());
        actionManager.register("add-populated-place", new AddPopulatedPlaceToPositionsAction());
        actionManager.register("add-speed", new AddSpeedToPositionsAction());
        actionManager.register("add-time", new AddTimeToPositionsAction());
        actionManager.register("add-number", new AddNumberToPositionsAction());
        actionManager.register("split-positionlist", new SplitPositionListAction(tablePositions, getPositionsModel(), formatAndRoutesModel));
        actionManager.register("import-positionlist", new ImportPositionListAction(this));
        actionManager.register("export-positionlist", new ExportPositionListAction(this));

        registerKeyStroke(tablePositions, "copy");
        registerKeyStroke(tablePositions, "cut");
        registerKeyStroke(tablePositions, "paste");

        formatAndRoutesModel.addModifiedListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                actionManager.enable("save", formatAndRoutesModel.isModified() &&
                        formatAndRoutesModel.getFormat() != null &&
                        formatAndRoutesModel.getFormat().isSupportsWriting());
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
                if (e.getStateChange() == SELECTED) {
                    r.getBatchPositionAugmenter().interrupt();
                    formatAndRoutesModel.setSelectedItem(e.getItem());
                }
            }
        });
        comboBoxChoosePositionListCharacteristics.setModel(getCharacteristicsModel());
        comboBoxChoosePositionListCharacteristics.setRenderer(new RouteCharacteristicsListCellRenderer());

        convertPanel.setTransferHandler(dropHandler);

        // make sure that Insert works directly after the program start on an empty position list
        invokeLater(new Runnable() {
            public void run() {
                convertPanel.requestFocus();
            }
        });
    }

    public void dispose() {
        lengthCalculator.dispose();
    }

    private void prepareForNewPositionList() {
        Application.getInstance().getContext().getUndoManager().discardAllEdits();
        RouteConverter.getInstance().getBatchPositionAugmenter().interrupt();
    }

    public Component getRootComponent() {
        return convertPanel;
    }

    public JComponent getFocusComponent() {
        return tablePositions;
    }

    public JButton getDefaultButton() {
        return buttonNewPositionList;
    }

    // action methods

    public void openUrls(List<URL> urls) {
        if (!confirmDiscard())
            return;

        // make copy which we could modify freely
        List<URL> copy = new ArrayList<>(urls);
        for (Iterator<URL> it = copy.iterator(); it.hasNext(); ) {
            URL url = it.next();
            File file = toFile(url);
            if (file != null && (!file.exists() || !file.isFile())) {
                log.warning(file + " does not exist or is not a file");
                it.remove();
            }
        }

        // start with a non-existent file
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

        RouteConverter r = RouteConverter.getInstance();
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("open-file-dialog-title"));
        setReadFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedSource());
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(r.getFrame());
        if (open != APPROVE_OPTION)
            return;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);
        prepareForNewPositionList();

        List<URL> urls = toUrls(selected);
        List<NavigationFormat> formats = selectedFormat != null ?
                getReadFormatsWithPreferredFormat(selectedFormat) :
                getReadFormatsPreferredByExtension(getExtension(urls));
        openPositionList(urls, formats);
    }

    public void openPositionList(List<URL> urls) {
        if (!confirmDiscard())
            return;

        prepareForNewPositionList();
        openPositionList(urls, getReadFormatsPreferredByExtension(getExtension(urls)));
    }

    @SuppressWarnings("unchecked")
    public void openPositionList(final List<URL> urls, final List<NavigationFormat> formats) {
        final RouteConverter r = RouteConverter.getInstance();

        final URL url = urls.get(0);
        final String path = createReadablePath(url);
        preferences.put(READ_PATH_PREFERENCE, path);

        startWaitCursor(r.getFrame().getRootPane());
        new Thread(new Runnable() {
            public void run() {
                NavigationFormatParser parser = new NavigationFormatParser();
                NavigationFormatParserListener listener = new NavigationFormatParserListener() {
                    public void reading(final NavigationFormat<BaseRoute> format) {
                        invokeLater(new Runnable() {
                            public void run() {
                                formatAndRoutesModel.setFormat(format);
                            }
                        });
                    }
                };
                parser.addNavigationFileParserListener(listener);

                try {
                    invokeAndWait(new Runnable() {
                        public void run() {
                            Gpx11Format gpxFormat = new Gpx11Format();
                            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                            urlModel.clear();
                        }
                    });

                    final ParserResult result = parser.read(url, formats);
                    if (result.isSuccessful()) {
                        log.info("Opened: " + path);
                        final NavigationFormat format = result.getFormat();
                        countRead(format);
                        if (!checkReadFormat(format))
                            return;
                        invokeLater(new Runnable() {
                            public void run() {
                                formatAndRoutesModel.setRoutes(new FormatAndRoutes(format, result.getAllRoutes()));
                                comboBoxChoosePositionList.setModel(formatAndRoutesModel);
                                urlModel.setString(path);
                                recentUrlsModel.addUrl(url);

                                if (urls.size() > 1) {
                                    List<URL> append = new ArrayList<>(urls);
                                    append.remove(0);
                                    // this way the route is always marked as modified :-(
                                    appendPositionList(-1, append);
                                }
                            }
                        });

                    } else {
                        invokeLater(new Runnable() {
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
                    parser.removeNavigationFileParserListener(listener);
                    invokeLater(new Runnable() {
                        public void run() {
                            stopWaitCursor(r.getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "UrlOpener").start();
    }

    private void appendPositionList(final int row, final List<URL> urls) {
        final RouteConverter r = RouteConverter.getInstance();

        new Thread(new Runnable() {
            public void run() {
                try {
                    for (URL url : urls) {
                        String path = createReadablePath(url);

                        NavigationFormatParser parser = new NavigationFormatParser();
                        final ParserResult result = parser.read(url, getReadFormats());
                        if (result.isSuccessful()) {
                            log.info("Appended: " + path);
                            countRead(result.getFormat());

                            final String finalPath = path;
                            // avoid parallelism to ensure the URLs are processed in order
                            invokeAndWait(new Runnable() {
                                public void run() {
                                    if (getFormatAndRoutesModel().getFormat().isSupportsMultipleRoutes()) {
                                        for (BaseRoute route : result.getAllRoutes()) {
                                            int appendIndex = getFormatAndRoutesModel().getSize();
                                            getFormatAndRoutesModel().addPositionList(appendIndex, route);
                                        }
                                    } else {
                                        try {
                                            int appendRow = row > 0 ? row : getPositionsModel().getRowCount();
                                            getPositionsModel().add(appendRow, result.getTheRoute());
                                        } catch (FileNotFoundException e) {
                                            r.handleFileNotFound(finalPath);
                                        } catch (IOException e) {
                                            r.handleOpenError(e, finalPath);
                                        }
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
        }, "UrlAppender").start();
    }

    @SuppressWarnings("unchecked")
    public void newFile() {
        if (!confirmDiscard())
            return;

        RouteConverter r = RouteConverter.getInstance();
        startWaitCursor(r.getFrame().getRootPane());
        try {
            Gpx11Format gpxFormat = new Gpx11Format();
            GpxRoute gpxRoute = new GpxRoute(gpxFormat);
            gpxRoute.setName(MessageFormat.format(RouteConverter.getBundle().getString("new-positionlist-name"), 1));
            formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
            urlModel.clear();
            prepareForNewPositionList();
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    public void importPositionList() {
        int selectedRow = getPositionsView().getSelectedRow() + 1;

        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("import-file-dialog-title"));
        setReadFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedSource());
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != APPROVE_OPTION)
            return;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        setReadFormatFileFilterPreference(selectedFormat);

        appendPositionList(selectedRow, reverse(toUrls(selected)));
    }

    public void exportPositionList() {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("export-file-dialog-title"));
        setWriteFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedTarget());
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int save = chooser.showSaveDialog(RouteConverter.getInstance().getFrame());
        if (save != APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        if (selectedFormat == null)
            selectedFormat = formatAndRoutesModel.getFormat();
        setWriteFormatFileFilterPreference(selectedFormat);
        saveFile(selected, selectedFormat, true, true, !formatAndRoutesModel.getFormat().equals(selectedFormat));
    }

    private void saveFile(File file, NavigationFormat format,
                          boolean exportSelectedRoute, boolean confirmOverwrite, boolean openAfterSave) {
        RouteConverter r = RouteConverter.getInstance();
        if (file.getParent() != null)
            preferences.put(WRITE_PATH_PREFERENCE + format.getClass().getName(), file.getParent());

        boolean duplicateFirstPosition = format instanceof NmnFormat && !(format instanceof Nmn7Format) || format instanceof CoPilotFormat;
        BaseRoute route = formatAndRoutesModel.getSelectedRoute();
        int fileCount = getNumberOfFilesToWriteFor(route, format, duplicateFirstPosition);
        if (fileCount > 1) {
            int confirm = showConfirmDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-split"),
                            shortenPath(file.getPath(), 60), route.getPositionCount(), format.getName(),
                            format.getMaximumPositionCount(), fileCount),
                    r.getFrame().getTitle(), YES_NO_CANCEL_OPTION
            );
            switch (confirm) {
                case YES_OPTION:
                    break;
                case NO_OPTION:
                    fileCount = 1;
                    break;
                default:
                    return;
            }
        }

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

        saveFiles(targets, format, route, exportSelectedRoute, confirmOverwrite, openAfterSave);
    }

    private void saveFiles(File[] files, NavigationFormat format, BaseRoute route,
                           boolean exportSelectedRoute, boolean confirmOverwrite, boolean openAfterSave) {
        final RouteConverter r = RouteConverter.getInstance();
        String targetsAsString = printArrayToDialogString(files);
        startWaitCursor(r.getFrame().getRootPane());
        try {
            if (!checkWriteFormat(format))
                return;
            if (format.isSupportsMultipleRoutes()) {
                List<BaseRoute> routes = exportSelectedRoute ? asList(route) : formatAndRoutesModel.getRoutes();
                new NavigationFormatParser().write(routes, (MultipleRoutesFormat) format, files[0]);
            } else {
                boolean duplicateFirstPosition = preferences.getBoolean(DUPLICATE_FIRST_POSITION_PREFERENCE, true);
                ParserCallback parserCallback = new ParserCallback() {
                    public void preprocess(BaseRoute route, NavigationFormat format) {
                        if (format instanceof GarminFlightPlanFormat) {
                            GarminFlightPlanRoute garminFlightPlanRoute = (GarminFlightPlanRoute) route;
                            completeGarminFlightPlan(garminFlightPlanRoute);
                        }
                    }
                };
                new NavigationFormatParser().write(route, format, duplicateFirstPosition, true, parserCallback, files);
            }
            formatAndRoutesModel.setModified(false);
            recentFormatsModel.addFormat(format);
            countWrite(format);
            log.info(String.format("Saved: %s", targetsAsString));

            if (!exportSelectedRoute && format.isSupportsReading()) {
                if (openAfterSave) {
                    openPositionList(toUrls(files), getReadFormatsWithPreferredFormat(format));
                    log.info(String.format("Open after save: %s", files[0]));
                }
                if (confirmOverwrite) {
                    URL url = files[0].toURI().toURL();
                    String path = createReadablePath(url);
                    urlModel.setString(path);
                    recentUrlsModel.addUrl(url);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.severe(String.format("Error saving %s in %s: %s", files[0], format, t.getMessage()));

            showMessageDialog(r.getFrame(),
                    MessageFormat.format(RouteConverter.getBundle().getString("save-error"), urlModel.getShortUrl(), targetsAsString, t.getMessage()),
                    r.getFrame().getTitle(), ERROR_MESSAGE);
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    private static boolean checkReadFormat(NavigationFormat format) {
        return !((format instanceof HaicomLoggerFormat && !checkForFeature("csv-haicom", "Read Haicom Logger")));
    }

    private static boolean checkWriteFormat(NavigationFormat format) {
        return !((format instanceof GarminFlightPlanFormat && !checkForFeature("fpl-g1000", "Write Garmin Flight Plan")) ||
                (format instanceof GoRiderGpsFormat && !checkForFeature("rt-gorider", "Write GoRider GPS")));
    }

    private static boolean checkForFeature(String featureName, String featureDescription) {
        if (!hasFeature(featureName)) {
            final RouteConverter r = RouteConverter.getInstance();
            JLabel labelFeatureError = new JLabel(MessageFormat.format(RouteConverter.getBundle().getString("feature-not-available"), featureDescription));
            labelFeatureError.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    startMail(r.getFrame());
                }
            });
            showMessageDialog(r.getFrame(), labelFeatureError, r.getFrame().getTitle(), ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void completeGarminFlightPlan(GarminFlightPlanRoute garminFlightPlanRoute) {
        CompleteFlightPlanDialog dialog = new CompleteFlightPlanDialog(garminFlightPlanRoute);
        dialog.pack();
        dialog.restoreLocation();
        dialog.setVisible(true);
    }

    public void saveFile() {
        saveFile(new File(urlModel.getString()), formatAndRoutesModel.getFormat(), false, false, false);
    }

    public void saveAsFile() {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("save-file-dialog-title"));
        setWriteFormatFileFilters(chooser);
        chooser.setSelectedFile(createSelectedTarget());
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int save = chooser.showSaveDialog(RouteConverter.getInstance().getFrame());
        if (save != APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());
        if (selectedFormat == null)
            selectedFormat = formatAndRoutesModel.getFormat();
        setWriteFormatFileFilterPreference(selectedFormat);
        saveFile(selected, selectedFormat, false, true, !formatAndRoutesModel.getFormat().equals(selectedFormat));
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
            int confirm = showConfirmDialog(RouteConverter.getInstance().getFrame(),
                    RouteConverter.getBundle().getString("confirm-discard"),
                    urlModel.getShortUrl(), YES_NO_CANCEL_OPTION);
            switch (confirm) {
                case YES_OPTION:
                    saveFile();
                    break;
                case NO_OPTION:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean confirmOverwrite(String file) {
        int confirm = showConfirmDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString("save-confirm-overwrite"), file),
                RouteConverter.getInstance().getFrame().getTitle(), YES_NO_OPTION);
        return confirm != YES_OPTION;
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
        NavigationFormat format = formatAndRoutesModel.getFormat();
        boolean supportsMultipleRoutes = format instanceof MultipleRoutesFormat;
        boolean existsARoute = formatAndRoutesModel.getSize() > 0;
        boolean existsMoreThanOneRoute = formatAndRoutesModel.getSize() > 1;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;
        RouteCharacteristics characteristics = getCharacteristicsModel().getSelectedCharacteristics();

        comboBoxChoosePositionList.setEnabled(existsMoreThanOneRoute);

        RouteConverter r = RouteConverter.getInstance();
        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.enable("insert-positions", existsMoreThanOnePosition);
        actionManager.enable("delete-positions", existsMoreThanOnePosition);
        actionManager.enable("new-positionlist", supportsMultipleRoutes);
        actionManager.enable("rename-positionlist", existsARoute);
        actionManager.enable("convert-route-to-track", existsAPosition && characteristics.equals(Route));
        actionManager.enable("convert-track-to-route", existsAPosition && characteristics.equals(Track));
        actionManager.enable("delete-positionlist", existsMoreThanOneRoute);
        actionManager.enable("split-positionlist", supportsMultipleRoutes && existsARoute && existsMoreThanOnePosition);
        tableHeaderMenu.enable(existsMoreThanOnePosition);
        //noinspection ConstantConditions
        actionManager.enable("complete-flight-plan", existsAPosition && format instanceof GarminFlightPlanFormat);
        actionManager.enable("print-map", r.isMapViewAvailable() && existsAPosition);
        actionManager.enable("print-map-and-route", r.isMapViewAvailable() && existsAPosition && characteristics.equals(Route));
        actionManager.enable("print-profile", existsAPosition);
    }

    private int[] selectedPositions = null;

    private void handlePositionsUpdate() {
        int[] selectedRows = tablePositions.getSelectedRows();
        // avoid firing events of the selection hasn't changed
        if (Arrays.equals(this.selectedPositions, selectedRows))
            return;

        this.selectedPositions = selectedRows;

        boolean existsASelectedPosition = selectedPositions.length > 0;
        boolean allPositionsSelected = selectedPositions.length == tablePositions.getRowCount();
        boolean firstRowNotSelected = existsASelectedPosition && selectedPositions[0] != 0;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;
        boolean supportsMultipleRoutes = formatAndRoutesModel.getFormat() instanceof MultipleRoutesFormat;
        RouteCharacteristics characteristics = getCharacteristicsModel().getSelectedCharacteristics();

        buttonMovePositionToTop.setEnabled(firstRowNotSelected);
        buttonMovePositionUp.setEnabled(firstRowNotSelected);
        boolean lastRowNotSelected = existsASelectedPosition && selectedPositions[selectedPositions.length - 1] != tablePositions.getRowCount() - 1;
        buttonMovePositionDown.setEnabled(lastRowNotSelected);
        buttonMovePositionToBottom.setEnabled(lastRowNotSelected);

        RouteConverter r = RouteConverter.getInstance();
        ActionManager actionManager = r.getContext().getActionManager();
        actionManager.enable("cut", existsASelectedPosition);
        actionManager.enable("copy", existsASelectedPosition);
        actionManager.enable("delete", existsASelectedPosition);
        actionManager.enable("select-all", existsAPosition && !allPositionsSelected);
        findMenu(r.getFrame().getJMenuBar(), "position", "complete").setEnabled(existsASelectedPosition);
        actionManager.enable("add-coordinates", existsASelectedPosition);
        actionManager.enable("add-elevation", existsASelectedPosition);
        actionManager.enable("add-postal-address", existsASelectedPosition);
        actionManager.enable("add-populated-place", existsASelectedPosition);
        actionManager.enable("add-speed", existsASelectedPosition);
        actionManager.enable("add-time", existsASelectedPosition);
        actionManager.enable("add-number", existsASelectedPosition);
        actionManager.enable("split-positionlist", supportsMultipleRoutes && existsASelectedPosition);
        actionManager.enable("insert-positions", existsAPosition);
        actionManager.enable("delete-positions", existsAPosition);
        actionManager.enable("revert-positions", existsMoreThanOnePosition);
        tableHeaderMenu.enable(existsMoreThanOnePosition);
        actionManager.enable("print-map", r.isMapViewAvailable() && existsAPosition);
        actionManager.enable("print-map-and-route", r.isMapViewAvailable() && existsAPosition && characteristics.equals(Route));
        actionManager.enable("print-profile", existsAPosition);

        r.selectPositions(selectedPositions);
    }

    // helpers

    private File createSelectedSource() {
        File source = new File(urlModel.getString());
        source = findExistingPath(source);
        File path = new File(preferences.get(READ_PATH_PREFERENCE, ""));
        path = findExistingPath(path);

        if (path == null)
            return source;
        else if (source != null)
            return new File(path, source.getName());
        else
            return path;
    }

    private File createSelectedTarget() {
        File target = new File(urlModel.getString());
        target = findExistingPath(target);
        NavigationFormat format = formatAndRoutesModel.getFormat();
        File path = target != null ? target : new File(preferences.get(WRITE_PATH_PREFERENCE + format.getClass().getName(), ""));
        path = findExistingPath(path);
        if (path == null)
            path = new File("");

        String fileName = path.getName();
        //noinspection ConstantConditions
        if (format instanceof GoPal3RouteFormat)
            fileName = createGoPalFileName(fileName);
        return new File(calculateConvertFileName(new File(path.getParentFile(), fileName), "", format.getMaximumFileNameLength()));
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
        setFormatFileFilters(chooser, getReadFormatsSortedByName(),
                preferences.get(READ_FORMAT_PREFERENCE, Gpx11Format.class.getName()));
    }

    private void setReadFormatFileFilterPreference(NavigationFormat selectedFormat) {
        String preference = selectedFormat != null ? selectedFormat.getClass().getName() : "";
        preferences.put(READ_FORMAT_PREFERENCE, preference);
    }

    private void setWriteFormatFileFilters(JFileChooser chooser) {
        setFormatFileFilters(chooser, getWriteFormatsWithPreferredFormats(recentFormatsModel.getFormats()),
                preferences.get(WRITE_FORMAT_PREFERENCE, Gpx11Format.class.getName()));
    }

    private void setWriteFormatFileFilterPreference(NavigationFormat selectedFormat) {
        String preference = selectedFormat.getClass().getName();
        preferences.put(WRITE_FORMAT_PREFERENCE, preference);
    }

    private void logFormatUsage() {
        StringBuilder builder = new StringBuilder();
        for (NavigationFormat format : getFormatsSortedByName()) {
            int reads = preferences.getInt(READ_COUNT_PREFERENCE + format.getClass().getName(), 0);
            int writes = preferences.getInt(WRITE_COUNT_PREFERENCE + format.getClass().getName(), 0);
            if (reads > 0 || writes > 0)
                builder.append(String.format("\n%s, reads: %d, writes: %d", format.getName(), reads, writes));
        }
        log.info("Format usage:" + builder.toString());
    }

    private void count(String preferenceName) {
        int count = preferences.getInt(preferenceName, 0);
        preferences.putInt(preferenceName, count + 1);
    }

    private void countRead(NavigationFormat format) {
        count(READ_COUNT_PREFERENCE + format.getClass().getName());
    }

    private void countWrite(NavigationFormat format) {
        count(WRITE_COUNT_PREFERENCE + format.getClass().getName());
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
        tablePositions.getSelectionModel().setValueIsAdjusting(true);
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
        tablePositions.getSelectionModel().setValueIsAdjusting(false);
    }

    public int selectPositionsWithinDistanceToPredecessor(double distance) {
        int[] indices = getPositionsModel().getPositionsWithinDistanceToPredecessor(distance);
        selectPositions(indices);
        return indices.length;
    }

    public int[] selectAllButEveryNthPosition(int order) {
        int rowCount = getPositionsModel().getRowCount();
        int[] indices = allButEveryNthAndFirstAndLast(rowCount, order);
        selectPositions(indices);
        return new int[]{indices.length, rowCount - indices.length};
    }

    public int selectInsignificantPositions(double threshold) {
        int[] indices = getPositionsModel().getInsignificantPositions(threshold);
        selectPositions(indices);
        return indices.length;
    }

    public void clearSelection() {
        tablePositions.getSelectionModel().clearSelection();
    }

    public void renameRoute(String name) {
        formatAndRoutesModel.renamePositionList(name);
    }

    private void createUIComponents() {
        comboBoxChoosePositionList = new JComboBox() {
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                preferredSize.width = convertPanel.getPreferredSize().width - 300;
                return preferredSize;
            }
        };
        comboBoxChoosePositionList.setMinimumSize(new Dimension(-1, comboBoxChoosePositionList.getMinimumSize().height));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
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
        convertPanel.add(comboBoxChoosePositionList, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        private TransferHandler delegate;

        TableDragAndDropHandler(TransferHandler delegate) {
            this.delegate = delegate;
        }

        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(positionFlavor) || delegate.canImport(support);
        }

        private int[] toRows(List<NavigationPosition> positions) {
            int[] result = new int[positions.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getPositionsModel().getIndex(positions.get(i));
            }
            return result;
        }

        private void moveRows(int[] rows, TransferSupport support) {
            JTable table = getPositionsView();
            int index = support.getDropLocation() instanceof JTable.DropLocation ?
                    ((JTable.DropLocation) support.getDropLocation()).getRow() : MAX_VALUE;
            int rowCount = table.getModel().getRowCount();
            if (index < 0 || index > rowCount)
                index = rowCount;

            if (rows[0] > index) {
                getPositionsModel().up(rows, rows[0] - index);
                JTableHelper.selectPositions(table, index, index + rows.length - 1);
            } else {
                getPositionsModel().down(revert(rows), index - rows[0] - rows.length + 1);
                JTableHelper.selectPositions(table, index - rows.length + 1, index);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            Transferable transferable = support.getTransferable();
            try {
                if (support.isDataFlavorSupported(positionFlavor)) {
                    Object selection = transferable.getTransferData(positionFlavor);
                    if (selection != null) {
                        PositionSelection positionsSelection = (PositionSelection) selection;
                        int[] rows = toRows(positionsSelection.getPositions());
                        if (rows.length > 0) {
                            moveRows(rows, support);
                            return true;
                        }
                    }
                }
            } catch (UnsupportedFlavorException | IOException e) {
                // intentionally left empty
            }
            return delegate.importData(support);
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
