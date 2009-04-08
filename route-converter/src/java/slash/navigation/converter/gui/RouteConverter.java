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

package slash.navigation.converter.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.*;
import slash.navigation.babel.BabelException;
import slash.navigation.babel.BabelFormat;
import slash.navigation.catalog.domain.Route;
import slash.navigation.catalog.domain.RouteService;
import slash.navigation.catalog.model.CategoryTreeModel;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.catalog.model.RoutesListModel;
import slash.navigation.converter.gui.dnd.DnDHelper;
import slash.navigation.converter.gui.dnd.RouteSelection;
import slash.navigation.converter.gui.helper.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.helper.TableHeaderPopupMenu;
import slash.navigation.converter.gui.helper.TablePopupMenu;
import slash.navigation.converter.gui.helper.AbstractListDataListener;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.renderer.*;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.Constants;
import slash.navigation.gui.renderer.NavigationFormatListCellRenderer;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.KmlFormat;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

/**
 * A small graphical user interface for the route conversion.
 *
 * @author Christian Pesch
 */

public abstract class RouteConverter extends SingleFrameApplication {
    private static final Logger log = Logger.getLogger(RouteConverter.class.getName());
    private Preferences preferences = Preferences.userNodeForPackage(getClass());

    public static ResourceBundle getBundle() {
        return getInstance().getContext().getBundle();
    }

    static String getTitle() {
        Version version = Version.parseVersionFromManifest();
        return MessageFormat.format(getBundle().getString("title"), version.getVersion(), version.getBuildDate());
    }

    private static final String SOURCE_PREFERENCE = "source";
    private static final String SOURCE_FORMAT_PREFERENCE = "sourceFormat";
    private static final String TARGET_PREFERENCE = "target";
    private static final String TARGET_FORMAT_PREFERENCE = "targetFormat";
    private static final String ADD_POSITION_LONGITUDE_PREFERENCE = "addPositionLongitude";
    private static final String ADD_POSITION_LATITUDE_PREFERENCE = "addPositionLatitude";
    private static final String START_GOOGLE_EARTH_PREFERENCE = "startGoogleEarth";
    private static final String DUPLICATE_FIRST_POSITION_PREFERENCE = "duplicateFirstPosition";
    private static final String NUMBER_POSITION_NAMES_PREFERENCE = "numberPositionNames";
    private static final String SAVE_AS_ROUTE_TRACK_WAYPOINTS_PREFERENCE = "saveAsRouteTrackWaypoints";
    private static final String START_WITH_LAST_FILE_PREFERENCE = "startWithLastFile";
    private static final String SELECT_DUPLICATE_PREFERENCE = "selectDuplicate";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String DIVIDER_LOCATION_PREFERENCE = "dividerLocation";

    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";

    protected JPanel contentPane;
    private JSplitPane splitPane;
    private JPanel mapPanel;
    private MapView mapView;
    private static final GridConstraints MAP_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 2640), 0, true);

    private JTabbedPane tabbedPane;
    private JPanel convertPanel;
    private JPanel browsePanel;

    private FormatAndRoutesModel formatAndRoutesModel = new FormatAndRoutesModel();
    protected JTextField textFieldSource;
    private JLabel labelPositionLists;
    private JLabel labelPositions;
    private JLabel labelFormat;
    private JTable tablePositions;
    private JButton buttonOpenFile;
    private JButton buttonNewFile;
    private JButton buttonAppendFileToPositionList;
    private JComboBox comboBoxChoosePositionList;
    private JComboBox comboBoxChoosePositionListCharacteristics;
    private JButton buttonAddPositionList;
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
    private JCheckBox checkboxStartGoogleEarth;
    private JCheckBox checkboxDuplicateFirstPosition;
    private JCheckBox checkboxNumberPositionNames;
    private JCheckBox checkBoxSaveAsRouteTrackWaypoints;
    private JComboBox comboBoxChooseFormat;
    private JButton buttonSaveFile;

    private JPanel expertPanel;
    private JLabel labelBrowse;
    private JLabel labelMail;
    private JLabel labelCp;
    private JLabel labelCredit;
    private JComboBox comboBoxLocale;
    private JTextField textFieldBabelPath;
    private JButton buttonChooseGPSBabel;
    private JCheckBox checkBoxAutomaticUpdateCheck;
    private JCheckBox checkBoxStartWithLastFile;
    private JButton buttonPrintMap;
    private JButton buttonRenumberPositions;
    private JButton buttonCheckForUpdate;
    private JButton buttonTestDragListenerPort;

    private RouteService routeService = new RouteService(System.getProperty("catalog", "http://www.routeconverter.de/catalog/"));
    private RouteServiceOperator operator = new RouteServiceOperator(this);

    protected JTree treeCategories;
    protected JTable tableRoutes;
    private JButton buttonDeleteCategory;
    private JButton buttonAddCategory;
    private JButton buttonRenameCategory;
    private JButton buttonAddFile;
    private JButton buttonAddUrl;
    private JButton buttonRenameRoute;
    private JButton buttonDeleteRoute;
    private JButton buttonLogin;

    private String[] args;

    protected void initialize(String[] args) {
        DebugOutput.activate();
        this.args = args;
    }

    protected void startup() {
        log.info("Started " + getTitle() + " on " + Platform.getPlatform() + " with " + Platform.getJvm());
        show();
        createUpdater().implicitCheck(frame);
        parseArgs(args);
    }

    private void parseArgs(String[] args) {
        if (args.length > 0) {
            openUrls(Files.toUrls(args));
        } else if (getStartWithLastFilePreference()) {
            String source = preferences.get(SOURCE_PREFERENCE, "");
            openUrls(Files.toUrls(source));
        } else {
            onNewPositionList();
        }
    }

    private void show() {
        createFrame(getTitle(), "RouteConverter", contentPane, buttonOpenFile);
        prepareConvertPane();

        tabbedPane.addChangeListener(new ChangeListener() {
            private Map<Component, Runnable> lazyInitializers = new HashMap<Component, Runnable>();

            {
                lazyInitializers.put(expertPanel, new Runnable() {
                    public void run() {
                        prepareExpertPane();
                    }
                });
                lazyInitializers.put(browsePanel, new Runnable() {
                    public void run() {
                        prepareBrowsePane();
                    }
                });
            }

            public void stateChanged(ChangeEvent e) {
                Component selected = ((JTabbedPane) e.getSource()).getSelectedComponent();
                Runnable runnable = lazyInitializers.get(selected);
                if (runnable != null) {
                    lazyInitializers.remove(selected);

                    Constants.startWaitCursor(frame.getRootPane());
                    try {
                        runnable.run();
                    }
                    finally {
                        Constants.stopWaitCursor(frame.getRootPane());
                    }
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                openFrame(contentPane);
            }
        });

        if (MapView.isSupportedPlatform()) {
            mapPanel.setVisible(true);
            createMapView();
        } else {
            mapPanel.setVisible(false);
        }
    }

    private void prepareConvertPane() {
        new RouteFormatToJLabelAdapter(getFormatAndRoutesModel(), labelFormat, labelPositionLists);
        new PositionsCountToJLabelAdapter(getPositionsModel(), labelPositions);

        buttonOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOpenPositionList();
            }
        });

        buttonNewFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNewPositionList();
            }
        });

        buttonAppendFileToPositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAppend();
            }
        });

        buttonRenamePositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onRenamePositionList();
            }
        });

        buttonAddPositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAddPositionList();
            }
        });

        buttonRemovePositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BaseRoute selectedRoute = getFormatAndRoutesModel().getSelectedRoute();
                if (selectedRoute != null)
                    getFormatAndRoutesModel().removeRoute(selectedRoute);
            }
        });

        buttonSaveFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        buttonMovePositionToTop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getPositionsTable().getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().top(selectedRows);
                    reestablishPositionSelection(0);
                }
            }
        });

        buttonMovePositionUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getPositionsTable().getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().up(selectedRows);
                    reestablishPositionSelection(-1);
                }
            }
        });

        buttonAddPosition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                onAddPosition();
            }
        });

        buttonRemovePosition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onRemovePosition();
            }
        });

        buttonFilterPositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showFilter();
            }
        });

        buttonRevertPositionList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getPositionsModel().revert();
                clearPositionSelection();
            }
        });

        buttonMovePositionDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getPositionsTable().getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().down(selectedRows);
                    reestablishPositionSelection(+1);
                }
            }
        });

        buttonMovePositionToBottom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getPositionsTable().getSelectedRows();
                if (selectedRows.length > 0) {
                    getPositionsModel().bottom(selectedRows);
                    reestablishPositionSelection(0);
                }
            }
        });

        getFormatAndRoutesModel().addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                handleRoutesUpdate();
            }
        });

        getPositionsTable().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAddPosition();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        getPositionsTable().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onRemovePosition();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        getPositionsTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int[] selectedRows = getPositionsTable().getSelectedRows();
                if (e.getValueIsAdjusting() || selectedRows.length == 0)
                    return;
                boolean firstRowNotSelected = selectedRows[0] != 0;
                buttonMovePositionToTop.setEnabled(firstRowNotSelected);
                buttonMovePositionUp.setEnabled(firstRowNotSelected);
                boolean lastRowNotSelected = selectedRows[selectedRows.length - 1] != getPositionsTable().getRowCount() - 1;
                buttonMovePositionDown.setEnabled(lastRowNotSelected);
                buttonMovePositionToBottom.setEnabled(lastRowNotSelected);
                if (isMapViewAvailable())
                    mapView.setSelectedPositions(selectedRows);
            }
        });

        getPositionsTable().setModel(getPositionsModel());
        final PositionsTableColumnModel tableColumnModel = new PositionsTableColumnModel();
        getPositionsTable().setColumnModel(tableColumnModel);

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                handlePositionsUpdate();
            }
        });

        new TableHeaderPopupMenu(getPositionsTable().getTableHeader(), tableColumnModel);
        new TablePopupMenu(this);

        NavigationFormat[] formats = NavigationFormats.getWriteFormatsSortedByName();
        getFormatComboBox().setModel(new DefaultComboBoxModel(formats));
        getFormatComboBox().setRenderer(new NavigationFormatListCellRenderer());
        String preferredFormat = preferences.get(TARGET_FORMAT_PREFERENCE, Gpx11Format.class.getName());
        for (NavigationFormat format : formats) {
            if (format.getClass().getName().equals(preferredFormat))
                getFormatComboBox().setSelectedItem(format);
        }
        getFormatComboBox().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                handleFormatUpdate();
            }
        });

        checkboxStartGoogleEarth.setSelected(preferences.getBoolean(START_GOOGLE_EARTH_PREFERENCE, false));
        checkboxDuplicateFirstPosition.setSelected(preferences.getBoolean(DUPLICATE_FIRST_POSITION_PREFERENCE, false));
        checkboxNumberPositionNames.setSelected(preferences.getBoolean(NUMBER_POSITION_NAMES_PREFERENCE, false));
        checkBoxSaveAsRouteTrackWaypoints.setSelected(preferences.getBoolean(SAVE_AS_ROUTE_TRACK_WAYPOINTS_PREFERENCE, true));
        handleFormatUpdate();
        handleRoutesUpdate();
        handlePositionsUpdate();

        getPositionListComboBox().setModel(getFormatAndRoutesModel());
        getPositionListComboBox().setRenderer(new RouteListCellRenderer());
        getPositionListComboBox().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    getFormatAndRoutesModel().setSelectedItem(e.getItem());
            }
        });
        comboBoxChoosePositionListCharacteristics.setModel(getCharacteristicsModel());
        comboBoxChoosePositionListCharacteristics.setRenderer(new RouteCharacteristicsListCellRenderer());

        addDragAndDropToConvertPane();
    }

    private boolean isMapViewAvailable() {
        return mapView != null && mapView.isInitialized();
    }

    private void prepareExpertPane() {
        labelBrowse.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                createExternalPrograms().startBrowserForHomepage(frame);
            }
        });

        labelMail.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                createExternalPrograms().startBrowserForForum(frame);
            }
        });

        labelCredit.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                createExternalPrograms().startBrowserForHomepage(frame);
            }
        });

        labelCp.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                createExternalPrograms().startMail(frame);
            }
        });

        buttonChooseGPSBabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onChooseBabelPath();
            }
        });

        comboBoxLocale.setModel(new DefaultComboBoxModel(new Object[]{Locale.GERMANY, Locale.US, Locale.FRANCE, Constants.NL, Constants.ROOT_LOCALE}));
        comboBoxLocale.setRenderer(new LocaleListCellRenderer());
        comboBoxLocale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;
                Locale locale = (Locale) e.getItem();
                setLocale(locale);
            }
        });
        comboBoxLocale.setSelectedItem(Locale.getDefault());

        textFieldBabelPath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                BabelFormat.setBabelPathPreference(textFieldBabelPath.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        textFieldBabelPath.setText(BabelFormat.getBabelPathPreference());

        checkBoxAutomaticUpdateCheck.setSelected(createUpdater().isAutomaticUpdateCheck());
        checkBoxAutomaticUpdateCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createUpdater().setAutomaticUpdateCheck(checkBoxAutomaticUpdateCheck.isSelected());
            }
        });
        new CheckBoxPreferencesSynchronizer(checkBoxStartWithLastFile, preferences, START_WITH_LAST_FILE_PREFERENCE, true);

        buttonCheckForUpdate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createUpdater().explicitCheck(frame);
            }
        });

        buttonTestDragListenerPort.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isMapViewAvailable())
                    mapView.testDragListenerPort();
            }
        });
        buttonTestDragListenerPort.setEnabled(isMapViewAvailable());

        buttonPrintMap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isMapViewAvailable())
                    mapView.print();
            }
        });
        buttonPrintMap.setEnabled(isMapViewAvailable());

        buttonRenumberPositions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Constants.startWaitCursor(frame.getRootPane());
                try {
                    tabbedPane.setSelectedComponent(convertPanel);
                    getPositionsModel().renumberPositions();
                } finally {
                    Constants.stopWaitCursor(frame.getRootPane());
                }
            }
        });
    }

    private void prepareBrowsePane() {
        buttonAddCategory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final CategoryTreeNode selected = getSelectedTreeNode();
                final String name = JOptionPane.showInputDialog(frame,
                        MessageFormat.format(getBundle().getString("add-category-label"), selected.getName()),
                        frame.getTitle(), JOptionPane.QUESTION_MESSAGE);
                if (Conversion.trim(name) == null)
                    return;

                operator.executeOnRouteService(new RouteServiceOperator.Operation() {
                    public void run() throws IOException {
                        final CategoryTreeNode subCategory = selected.addSubCategory(name);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                treeCategories.expandPath(new TreePath(selected.getPath()));
                                treeCategories.scrollPathToVisible(new TreePath(subCategory.getPath()));
                            }
                        });
                    }
                });
            }
        });

        buttonRenameCategory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final CategoryTreeNode selected = getSelectedTreeNode();
                final String name = (String) JOptionPane.showInputDialog(frame,
                        MessageFormat.format(getBundle().getString("rename-category-label"), selected.getName()),
                        frame.getTitle(), JOptionPane.QUESTION_MESSAGE, null, null, selected.getName());
                if (Conversion.trim(name) == null)
                    return;

                operator.executeOnRouteService(new RouteServiceOperator.Operation() {
                    public void run() throws IOException {
                        selected.renameCategory(name);
                    }
                });
            }
        });

        buttonDeleteCategory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final List<CategoryTreeNode> categories = getSelectedTreeNodes();

                operator.executeOnRouteService(new RouteServiceOperator.Operation() {
                    public void run() throws IOException {
                        for (CategoryTreeNode category : categories) {
                            category.delete();
                        }
                    }
                });
            }
        });

        buttonAddFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAddFileToCatalog();
            }
        });

        buttonAddUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addUrlToCatalog(getSelectedTreeNode(), "");
            }
        });

        buttonRenameRoute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedRow = tableRoutes.getSelectedRow();
                if (selectedRow == -1)
                    return;

                final CategoryTreeNode categoryTreeNode = getSelectedTreeNode();
                if (categoryTreeNode == null)
                    return;

                final Route selected = getRoutesListModel().getRoute(selectedRow);
                String description = null;
                try {
                    description = (String) JOptionPane.showInputDialog(frame,
                            MessageFormat.format(getBundle().getString("rename-route-label"), selected.getName()),
                            frame.getTitle(), JOptionPane.QUESTION_MESSAGE, null, null, selected.getDescription());
                } catch (IOException e) {
                    operator.handleServiceError(e);
                }
                if (Conversion.trim(description) == null)
                    return;

                final String theDescription = description;
                operator.executeOnRouteService(new RouteServiceOperator.Operation() {
                    public void run() throws IOException {
                        // strange way to handle cache invalidations
                        categoryTreeNode.renameRoute(selected, theDescription);
                    }
                });
            }
        });

        buttonDeleteRoute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final int[] selectedRows = tableRoutes.getSelectedRows();
                if (selectedRows.length == 0)
                    return;

                final CategoryTreeNode category = getSelectedTreeNode();
                if (category == null)
                    return;

                operator.executeOnRouteService(new RouteServiceOperator.Operation() {
                    public void run() throws IOException {
                        for (int selectedRow : selectedRows) {
                            Route route = getRoutesListModel().getRoute(selectedRow);
                            // strange way to handle cache invalidations
                            category.deleteRoute(route);
                        }
                    }
                });
            }
        });

        buttonLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                operator.showLogin();
            }
        });

        treeCategories.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        treeCategories.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath treePath = e.getPath();
                selectTreePath(treePath);
            }
        });
        treeCategories.getModel().addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
                selectTreePath(treeCategories.getSelectionModel().getSelectionPath());
            }

            public void treeNodesInserted(TreeModelEvent e) {
            }

            public void treeNodesRemoved(TreeModelEvent e) {
            }

            public void treeStructureChanged(TreeModelEvent e) {
            }
        });
        treeCategories.setCellRenderer(new CategoryTreeCellRenderer());

        tableRoutes.setDefaultRenderer(Object.class, new RoutesTableCellRenderer());
        tableRoutes.setDragEnabled(true);
        tableRoutes.setTransferHandler(new TransferHandler() {
            public int getSourceActions(JComponent comp) {
                return MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                int[] selectedRows = tableRoutes.getSelectedRows();
                List<Route> selectedRoutes = new ArrayList<Route>();
                for (int selectedRow : selectedRows) {
                    Route route = getRoutesListModel().getRoute(selectedRow);
                    selectedRoutes.add(route);
                }
                return new RouteSelection(selectedRoutes);
            }
        });
        tableRoutes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int[] selectedRows = tableRoutes.getSelectedRows();
                if (e.getValueIsAdjusting() || selectedRows.length == 0)
                    return;

                Route route = getRoutesListModel().getRoute(selectedRows[0]);
                URL url;
                try {
                    url = route.getUrl();
                    if (url == null)
                        return;
                } catch (Throwable t) {
                    operator.handleServiceError(t);
                    return;
                }

                openPositionList(Arrays.asList(url));
            }
        });

        addDragAndDropToBrowsePane();

        new Thread(new Runnable() {
            public void run() {
                routeService.setAuthentication(getUserNamePreference(), getPasswordPreference());
                final CategoryTreeNode root = new CategoryTreeNode(routeService.getRootCategory());
                final CategoryTreeModel categoryTreeModel = new CategoryTreeModel(root);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        treeCategories.setModel(categoryTreeModel);
                        selectTreeNode(root);
                        /* TODO select path from previous start
                        CategoryTreeNode m = root.getSubCategory("Motorradfahren");
                        treeCategories.expandPath(new TreePath(new Object[]{m}));
                        treeCategories.setSelectionPath(new TreePath(new Object[]{m}));
                        CategoryTreeNode d = m.getSubCategory("Deutschland");
                        treeCategories.expandPath(new TreePath(new Object[]{m, d}));
                        CategoryTreeNode s = d.getSubCategory("Schleswig-Holstein");
                        treeCategories.expandPath(new TreePath(new Object[]{m, d, s}));
                        treeCategories.setSelectionPath(new TreePath(new Object[]{m, d, s}));
                        */
                    }
                });
            }
        }, "CategoryTreeCreator").start();
    }

    private void createMapView() {
        new Thread(new Runnable() {
            public void run() {
                // can do this outside of Swing
                mapView = new MapView(getPositionsModel(), getCharacteristicsModel());

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Throwable cause = mapView.getInitializationCause();
                        if (mapView.getCanvas() == null || cause != null) {
                            StringWriter stackTrace = new StringWriter();
                            cause.printStackTrace(new PrintWriter(stackTrace));
                            mapPanel.add(new JLabel(MessageFormat.format(RouteConverter.getBundle().getString("start-browser-error"), stackTrace.toString().replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);
                        } else {
                            mapPanel.add(mapView.getCanvas(), MAP_PANEL_CONSTRAINTS);
                        }

                        int location = preferences.getInt(DIVIDER_LOCATION_PREFERENCE, -1);
                        if (location > 0)
                            splitPane.setDividerLocation(location);
                        else
                            splitPane.setDividerLocation(300);

                        splitPane.addPropertyChangeListener(new PropertyChangeListener() {
                            private int location = 0;

                            public void propertyChange(PropertyChangeEvent e) {
                                if (!isMapViewAvailable())
                                    return;

                                if (e.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
                                    if (splitPane.getDividerLocation() != location) {
                                        location = splitPane.getDividerLocation();
                                        mapView.resize();
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }, "MapViewCreator").start();
    }

    CharacteristicsModel getCharacteristicsModel() {
        return getFormatAndRoutesModel().getCharacteristicsModel();
    }


    // start: public API for actions

    public JTable getPositionsTable() {
        return tablePositions;
    }

    public PositionsModel getPositionsModel() {
        return getFormatAndRoutesModel().getPositionsModel();
    }

    public JComboBox getFormatComboBox() {
        return comboBoxChooseFormat;
    }

    public JComboBox getPositionListComboBox() {
        return comboBoxChoosePositionList;
    }

    public FormatAndRoutesModel getFormatAndRoutesModel() {
        return formatAndRoutesModel;
    }

    // end: public API for actions

    NavigationFormat getFormat() {
        return (NavigationFormat) getFormatComboBox().getSelectedItem();
    }

    RoutesListModel getRoutesListModel() {
        return (RoutesListModel) tableRoutes.getModel();
    }


    private BaseNavigationPosition getCenter(int row) {
        BaseNavigationPosition position = getPositionsModel().getPosition(row);
        // if there is only one position or it is the first row,, create the new position close to it
        // if (row == 0 || getPositionsModel().getRowCount() == 1)
        if (row >= getPositionsModel().getRowCount() - 1)
            return Calculation.duplicateALittleNorth(position);
        // otherwhise center between given positions
        // BaseNavigationPosition second = getPositionsModel().getPosition(row - 1);
        BaseNavigationPosition second = getPositionsModel().getPosition(row + 1);
        if (!second.hasCoordinates() || !position.hasCoordinates())
            return null;
        return Calculation.center(Arrays.asList(second, position));
    }

    private void onAddPosition() {
        int[] selectedRows = getPositionsTable().getSelectedRows();
        // final int row = selectedRows.length > 0 ? selectedRows[0] : 0;
        int row = selectedRows.length > 0 ? selectedRows[0] : getPositionsTable().getRowCount();
        BaseNavigationPosition center = selectedRows.length > 0 ? getCenter(row) :
                // getPositionsModel().getRowCount() > 0 ? getCenter(0) : null;
                getPositionsModel().getRowCount() > 0 ? getCenter(getPositionsModel().getRowCount() - 1) : null;
        // getPositionsModel().add(row,
        final int insertRow = row > getPositionsModel().getRowCount() - 1 ? row : row + 1;

        Double longitude = center != null ? center.getLongitude() : getAddPositionLongitude();
        setAddPositionLongitude(longitude);
        Double latitude = center != null ? center.getLatitude() : getAddPositionLatitude();
        setAddPositionLatitude(latitude);

        getPositionsModel().add(insertRow, longitude, latitude,
                center != null && center.getTime() != null ? center.getTime() : Calendar.getInstance(),
                getBundle().getString("add-position-comment"));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Rectangle rectangle = getPositionsTable().getCellRect(insertRow, 1, true);
                getPositionsTable().scrollRectToVisible(rectangle);
                selectPositions(insertRow, insertRow);
            }
        });
    }

    public void onRemovePosition() {
        Constants.startWaitCursor(frame.getRootPane());
        try {
            int[] selectedRows = getPositionsTable().getSelectedRows();
            if (selectedRows.length > 0) {
                getPositionsModel().remove(selectedRows);
                final int row = selectedRows[0] > 0 ? selectedRows[0] - 1 : 0;
                if (getPositionsTable().getRowCount() > 0)
                    selectPositions(row, row);
            }
        } finally {
            Constants.stopWaitCursor(frame.getRootPane());
        }
    }

    public int selectDuplicatesWithinDistance(int distance) {
        clearPositionSelection();
        int[] indices = getPositionsModel().getDuplicatesWithinDistance(distance);
        for (int index : indices) {
            getPositionsTable().getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public int selectPositionsThatRemainingHaveDistance(int distance) {
        clearPositionSelection();
        int[] indices = getPositionsModel().getPositionsThatRemainingHaveDistance(distance);
        for (int index : indices) {
            getPositionsTable().getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public int selectAllButEveryNthPosition(int order) {
        clearPositionSelection();
        int rowCount = getPositionsModel().getRowCount();
        int[] indices = Range.allButEveryNthAndFirstAndLast(rowCount, order);
        for (int index : indices) {
            getPositionsTable().getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    public int selectInsignificantPositions(int threshold) {
        clearPositionSelection();
        int[] indices = getPositionsModel().getInsignificantPositions(threshold);
        for (int index : indices) {
            getPositionsTable().getSelectionModel().addSelectionInterval(index, index);
        }
        return indices.length;
    }

    private void selectPositions(final int index0, final int index1) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getPositionsTable().getSelectionModel().setSelectionInterval(index0, index1);
            }
        });
    }

    public void clearPositionSelection() {
        getPositionsTable().getSelectionModel().clearSelection();
    }

    private void reestablishPositionSelection(final int upOrDown) {
        final int minSelectedRow = getPositionsTable().getSelectionModel().getMinSelectionIndex();
        final int maxSelectedRow = getPositionsTable().getSelectionModel().getMaxSelectionIndex();
        if (minSelectedRow != -1 && maxSelectedRow != -1)
            selectPositions(minSelectedRow + upOrDown, maxSelectedRow + upOrDown);
    }

    protected CategoryTreeNode getSelectedTreeNode() {
        TreePath treePath = treeCategories.getSelectionPath();
        return (CategoryTreeNode) (treePath != null ?
                treePath.getLastPathComponent() : treeCategories.getModel().getRoot());
    }

    protected List<CategoryTreeNode> getSelectedTreeNodes() {
        TreePath[] treePaths = treeCategories.getSelectionPaths();
        List<CategoryTreeNode> treeNodes = new ArrayList<CategoryTreeNode>();
        for (TreePath treePath : treePaths) {
            treeNodes.add((CategoryTreeNode) treePath.getLastPathComponent());
        }
        return treeNodes;
    }

    private void selectTreePath(TreePath treePath) {
        CategoryTreeNode selected = (CategoryTreeNode) treePath.getLastPathComponent();
        selectTreeNode(selected);
    }

    private void selectTreeNode(CategoryTreeNode selected) {
        tableRoutes.setModel(selected.getRoutesListModel());
        TableCellRenderer routesHeaderRenderer = new RoutesTableCellHeaderRenderer();
        TableColumnModel routeColumns = tableRoutes.getColumnModel();
        for (int i = 0; i < routeColumns.getColumnCount(); i++) {
            TableColumn column = routeColumns.getColumn(i);
            column.setHeaderRenderer(routesHeaderRenderer);
            if (i == 0)
                column.setMaxWidth(50);
            if (i == 2) {
                column.setPreferredWidth(100);
                column.setMaxWidth(100);
            }
        }
    }

    private void handleFormatUpdate() {
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;
        boolean existsOneRoute = getFormatAndRoutesModel().getSize() == 1;
        boolean existsMoreThanOneRoute = getFormatAndRoutesModel().getSize() > 1;

        // TODO nobody seems to use this checkboxStartGoogleEarth.setVisible(getFormat() instanceof KmlFormat);
        checkboxStartGoogleEarth.setVisible(false);
        checkboxDuplicateFirstPosition.setVisible(getFormat() instanceof NmnFormat && !(getFormat() instanceof Nmn7Format));
        checkboxNumberPositionNames.setVisible(getFormat() instanceof TomTomRouteFormat);
        checkBoxSaveAsRouteTrackWaypoints.setVisible(supportsMultipleRoutes && existsOneRoute);

        buttonAddPositionList.setEnabled(supportsMultipleRoutes);
        // TODO check this later
        // TODO buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);

        preferences.put(TARGET_FORMAT_PREFERENCE, getFormat().getClass().getName());
    }

    private void handleRoutesUpdate() {
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsARoute = getFormatAndRoutesModel().getSize() > 0;
        boolean existsOneRoute = getFormatAndRoutesModel().getSize() == 1;
        boolean existsMoreThanOneRoute = getFormatAndRoutesModel().getSize() > 1;

        checkBoxSaveAsRouteTrackWaypoints.setVisible(supportsMultipleRoutes && existsOneRoute);

        getPositionListComboBox().setEnabled(existsMoreThanOneRoute);
        buttonRenamePositionList.setEnabled(existsARoute);
        buttonAddPositionList.setEnabled(supportsMultipleRoutes);
        // TODO check this later
        // TODO buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsARoute);
        buttonAppendFileToPositionList.setEnabled(existsARoute);
        buttonRemovePositionList.setEnabled(existsMoreThanOneRoute);
    }

    private void handlePositionsUpdate() {
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsAPosition = getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = getPositionsModel().getRowCount() > 1;

        buttonMovePositionToTop.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionUp.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionDown.setEnabled(existsMoreThanOnePosition);
        buttonMovePositionToBottom.setEnabled(existsMoreThanOnePosition);
        buttonRemovePosition.setEnabled(existsAPosition);
        buttonFilterPositionList.setEnabled(existsMoreThanOnePosition);
        buttonRevertPositionList.setEnabled(existsMoreThanOnePosition);

        // TODO check this later
        // TODO buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);
    }

    protected abstract void addDragAndDropToConvertPane();

    protected abstract void addDragAndDropToBrowsePane();

    protected void onDrop(List<File> files) {
        if (tabbedPane.getSelectedComponent().equals(convertPanel))
            openUrls(Files.toUrls(files.toArray(new File[files.size()])));
        else if (tabbedPane.getSelectedComponent().equals(browsePanel))
            addFilesToCatalog(getSelectedTreeNode(), files);
    }

    protected void onDrop(String string) {
        if (tabbedPane.getSelectedComponent().equals(convertPanel)) {
            String url = DnDHelper.extractUrl(string);
            try {
                openPositionList(Arrays.asList(new URL(url)));
            }
            catch (MalformedURLException e) {
                log.severe("Could not create URL from '" + url + "'");
            }
        } else if (tabbedPane.getSelectedComponent().equals(browsePanel))
            addUrlToCatalog(getSelectedTreeNode(), string);
    }

    private File createSelectedSource() {
        File source = new File(textFieldSource.getText());
        source = Files.findExistingPath(source);
        File path = new File(preferences.get(SOURCE_PREFERENCE, ""));
        path = Files.findExistingPath(path);
        if (path == null)
            return source;
        else if (source != null)
            return new File(path, source.getName());
        else
            return path;
    }

    private String getSource() {
        String source = textFieldSource.getText();
        return (source != null && source.length() > 0) ? source : "<null>";
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

    private String getTarget() {
        return TARGET_PREFERENCE + getFormat().getName();
    }

    private File createSelectedTarget() {
        File file = new File(getSource());
        File path = new File(preferences.get(getTarget(), ""));
        if (!path.exists())
            path = file.getParentFile();
        String fileName = file.getName();
        if (getFormat() instanceof GoPalRouteFormat)
            fileName = Files.createGoPalFileName(fileName);
        return new File(Files.calculateConvertFileName(new File(path, fileName), getFormat().getExtension(), getFormat().getMaximumFileNameLength()));
    }

    private File createUploadRoute() {
        File path = new File(preferences.get(UPLOAD_ROUTE_PREFERENCE, ""));
        return Files.findExistingPath(path);
    }


    private boolean confirmDiscard() {
        if (getFormatAndRoutesModel().isModified()) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    getBundle().getString("confirm-discard"),
                    frame.getTitle(), JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return false;
        }
        return true;
    }

    private boolean confirmOverwrite(String file) {
        int confirm = JOptionPane.showConfirmDialog(frame,
                MessageFormat.format(getBundle().getString("save-confirm-overwrite"), file),
                frame.getTitle(), JOptionPane.YES_NO_OPTION);
        return confirm != JOptionPane.YES_OPTION;
    }

    private void handleBabelError(final BabelException e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(frame, MessageFormat.format(getBundle().getString("babel-error"), e.getBabelPath()), frame.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void handleOpenError(final Exception e, final String path) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), Files.shortenPath(path), e.getMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        createExternalPrograms().startMail(frame);
                    }
                });
                JOptionPane.showMessageDialog(frame, labelOpenError, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void handleOpenError(final Exception e, final List<URL> urls) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), Files.printArrayToDialogString(urls.toArray(new URL[urls.size()])), e.getMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        createExternalPrograms().startMail(frame);
                    }
                });
                JOptionPane.showMessageDialog(frame, labelOpenError, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void handleUnsupportedFormat(final String path) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.severe("Unsupported format: " + path);
                JOptionPane.showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("unsupported-format"), Files.shortenPath(path)),
                        frame.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        });
    }


    protected void openUrls(List<URL> urls) {
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
            onNewPositionList();
            return;
        }

        if (copy.size() > 0) {
            openPositionList(copy);
        }
    }

    private void setReadFormatFileFilters(JFileChooser chooser) {
        chooser.resetChoosableFileFilters();
        String preferredFormat = preferences.get(SOURCE_FORMAT_PREFERENCE, "");
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
        preferences.put(SOURCE_FORMAT_PREFERENCE, preference);

    }

    // create this only once to make users choices stable at least for one program start
    private JFileChooser chooser;

    private synchronized JFileChooser getChooser() {
        if (chooser == null)
            chooser = Constants.createJFileChooser();
        return chooser;
    }

    private void onOpenPositionList() {
        if (!confirmDiscard())
            return;

        getChooser().setDialogTitle(getBundle().getString("open-source"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(false);
        int open = getChooser().showOpenDialog(frame);
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File selected = getChooser().getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        setReadFormatFileFilterPreference(getChooser());
        openPositionList(Files.toUrls(selected));
    }

    private void openPositionList(final List<URL> urls) {
        final URL url = urls.get(0);
        final String path = Files.createReadablePath(url);
        textFieldSource.setText(path);
        preferences.put(SOURCE_PREFERENCE, path);

        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.startWaitCursor(frame.getRootPane());
                                Gpx11Format gpxFormat = new Gpx11Format();
                                getFormatAndRoutesModel().setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                            }
                        });

                        final NavigationFileParser parser = new NavigationFileParser();
                        if (parser.read(url)) {
                            log.info("Opened: " + path);

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getFormatAndRoutesModel().setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                    getPositionListComboBox().setModel(getFormatAndRoutesModel());
                                }
                            });

                            if (urls.size() > 1) {
                                List<URL> append = new ArrayList<URL>(urls);
                                append.remove(0);
                                // this way the route is always marked as modified :-(
                                appendToPositionList(append);
                            }

                        } else
                            handleUnsupportedFormat(path);
                    }
                    finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.stopWaitCursor(frame.getRootPane());
                            }
                        });
                    }
                } catch (BabelException e) {
                    handleBabelError(e);
                } catch (Exception e) {
                    log.severe("Open error: " + e.getMessage());
                    e.printStackTrace();
                    handleOpenError(e, path);
                }
            }
        }, "Open").start();
    }

    private void onNewPositionList() {
        if (!confirmDiscard())
            return;

        Constants.startWaitCursor(frame.getRootPane());
        textFieldSource.setText(getBundle().getString("new-route"));
        Gpx11Format gpxFormat = new Gpx11Format();
        GpxRoute gpxRoute = new GpxRoute(gpxFormat);
        gpxRoute.setName(getBundle().getString("new-route"));
        getFormatAndRoutesModel().setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
        Constants.stopWaitCursor(frame.getRootPane());
    }

    private void onRenamePositionList() {
        RenameDialog renameDialog = createRenameDialog();
        renameDialog.pack();
        renameDialog.setLocationRelativeTo(frame);
        renameDialog.setVisible(true);
    }

    private void onAddPositionList() {
        getChooser().setDialogTitle(getBundle().getString("add-position-list-source"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(true);
        int open = getChooser().showOpenDialog(frame);
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File[] selected = getChooser().getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        setReadFormatFileFilterPreference(getChooser());
        addPositionList(Files.toUrls(selected));
    }

    private void addPositionList(final List<URL> urls) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.startWaitCursor(frame.getRootPane());
                            }
                        });

                        for (URL url : urls) {
                            final String path = Files.createReadablePath(url);

                            final NavigationFileParser parser = new NavigationFileParser();
                            if (parser.read(url)) {
                                log.info("Added route: " + path);

                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        try {
                                            getFormatAndRoutesModel().addRoutes(parser.getAllRoutes());
                                        } catch (IOException e) {
                                            log.severe("Open error: " + e.getMessage());
                                            handleOpenError(e, path);
                                        }
                                    }
                                });

                            } else {
                                log.severe("Unsupported format: " + path);
                                handleUnsupportedFormat(path);
                            }
                        }
                    }
                    finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.stopWaitCursor(frame.getRootPane());
                            }
                        });
                    }
                } catch (Exception e) {
                    log.severe("Add route error: " + e.getMessage());
                    handleOpenError(e, urls);
                }
            }
        }, "AddRoute").start();
    }


    private void onAppend() {
        getChooser().setDialogTitle(getBundle().getString("append-source"));
        setReadFormatFileFilters(getChooser());
        getChooser().setSelectedFile(createSelectedSource());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(true);
        int open = getChooser().showOpenDialog(frame);
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File[] selected = getChooser().getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        setReadFormatFileFilterPreference(getChooser());
        appendToPositionList(Files.toUrls(selected));
    }

    private void appendToPositionList(final List<URL> urls) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.startWaitCursor(frame.getRootPane());
                            }
                        });

                        for (URL url : urls) {
                            final String path = Files.createReadablePath(url);

                            final NavigationFileParser parser = new NavigationFileParser();
                            if (parser.read(url)) {
                                log.info("Appended: " + path);

                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        try {
                                            // if there is no file loaded: parseArgs()
                                            if (getFormatAndRoutesModel().getRoutes() == null) {
                                                textFieldSource.setText(path);
                                                getFormatAndRoutesModel().setRoutes(new FormatAndRoutes(parser.getFormat(), parser.getAllRoutes()));
                                                getPositionListComboBox().setModel(getFormatAndRoutesModel());
                                            } else {
                                                getPositionsModel().append(parser.getTheRoute());
                                            }
                                        } catch (IOException e) {
                                            log.severe("Open error: " + e.getMessage());
                                            handleOpenError(e, path);
                                        }
                                    }
                                });

                            } else {
                                log.severe("Unsupported format: " + path);
                                handleUnsupportedFormat(path);
                            }
                        }
                    }
                    finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Constants.stopWaitCursor(frame.getRootPane());
                            }
                        });
                    }
                } catch (Exception e) {
                    log.severe("Append error: " + e.getMessage());
                    handleOpenError(e, urls);
                }
            }
        }, "FileAppender").start();
    }

    private void onSave() {
        NavigationFormat format = getFormat();

        getChooser().setDialogTitle(getBundle().getString("save-target"));
        getChooser().resetChoosableFileFilters();
        getChooser().setFileFilter(new NavigationFormatFileFilter(format));
        getChooser().setSelectedFile(createSelectedTarget());
        getChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
        getChooser().setMultiSelectionEnabled(false);
        int save = getChooser().showSaveDialog(frame);
        if (save != JFileChooser.APPROVE_OPTION)
            return;

        File selected = getChooser().getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        saveFile(selected, format);
    }

    private void saveFile(File file, NavigationFormat format) {
        preferences.put(getTarget(), file.getParent());

        boolean duplicateFirstPosition = checkboxDuplicateFirstPosition.isSelected();
        NavigationFileParser parser = new NavigationFileParser();
        BaseRoute route = getFormatAndRoutesModel().getSelectedRoute();
        int fileCount = parser.getNumberOfFilesToWriteFor(route, format, duplicateFirstPosition);
        if (fileCount > 1) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    MessageFormat.format(getBundle().getString("save-confirm-split"), Files.shortenPath(getSource()),
                            route.getPositionCount(), format.getName(),
                            format.getMaximumPositionCount(), fileCount),
                    frame.getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
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
        try {
            try {
                Constants.startWaitCursor(frame.getRootPane());

                boolean saveAsRouteTrackWaypoints = checkBoxSaveAsRouteTrackWaypoints.isSelected();
                if (format.isSupportsMultipleRoutes() && (getFormatAndRoutesModel().getRoutes().size() > 1 || !saveAsRouteTrackWaypoints)) {
                    parser.write(getFormatAndRoutesModel().getRoutes(), (MultipleRoutesFormat) format, targets[0]);
                } else {
                    boolean numberPositionNames = checkboxNumberPositionNames.isSelected();
                    parser.write(route, format, duplicateFirstPosition, numberPositionNames, true, targets);
                }
                getFormatAndRoutesModel().setModified(false);
                log.info("Saved: " + targetsAsString);
            }
            finally {
                Constants.stopWaitCursor(frame.getRootPane());
            }

            if (format instanceof KmlFormat && checkboxStartGoogleEarth.isSelected()) {
                createExternalPrograms().startGoogleEarth(frame, targets);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Save error " + file + "," + format + ": " + e.getMessage());

            JOptionPane.showMessageDialog(frame,
                    MessageFormat.format(getBundle().getString("save-error"), Files.shortenPath(getSource()), targetsAsString, e.getMessage()),
                    frame.getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAddFileToCatalog() {
        CategoryTreeNode categoryTreeNode = getSelectedTreeNode();

        JFileChooser chooser = Constants.createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("add-file"));
        chooser.setSelectedFile(createUploadRoute());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(frame);
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        final File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        preferences.put(UPLOAD_ROUTE_PREFERENCE, selected[0].getPath());

        addFilesToCatalog(categoryTreeNode, Arrays.asList(selected));
    }

    private void addFileToCatalog(CategoryTreeNode categoryTreeNode, File file) {
        String path = Files.createReadablePath(file);
        String description = null;
        Double length = null;
        Constants.startWaitCursor(frame.getRootPane());
        try {
            NavigationFileParser parser = new NavigationFileParser();
            if (parser.read(file)) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
                if (route != null) {
                    description = RouteComments.createRouteDescription(route);
                    length = route.getLength();
                }
                showAddFileToCatalog(categoryTreeNode, description, length, file);
            } else
                handleUnsupportedFormat(path);
        } catch (Exception e) {
            log.severe("Cannot parse description from route " + path + ": " + e.getMessage());
            handleOpenError(e, Files.toUrls(file));
        }
        finally {
            Constants.stopWaitCursor(frame.getRootPane());
        }
    }

    protected void addFilesToCatalog(CategoryTreeNode category, List<File> files) {
        for (File file : files) {
            addFileToCatalog(category, file);
        }
    }

    protected void addUrlToCatalog(CategoryTreeNode category, String string) {
        showAddUrlToCatalog(category, DnDHelper.extractDescription(string), DnDHelper.extractUrl(string));
    }

    protected void onMove(final List<CategoryTreeNode> categories, final CategoryTreeNode parent) {
        operator.executeOnRouteService(new RouteServiceOperator.Operation() {
            public void run() throws IOException {
                for (CategoryTreeNode category : categories) {
                    category.moveCategory(parent);
                }
            }
        });
    }

    protected void onMove(final List<Route> routes, final CategoryTreeNode source, final CategoryTreeNode target) {
        operator.executeOnRouteService(new RouteServiceOperator.Operation() {
            public void run() throws IOException {
                for (Route route : routes) {
                    source.moveRoute(route, target);
                }
            }
        });
    }

    private void onChooseBabelPath() {
        JFileChooser chooser = Constants.createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-gpsbabel-path"));
        chooser.setSelectedFile(new File(BabelFormat.getBabelPathPreference()));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int open = chooser.showOpenDialog(frame);
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        textFieldBabelPath.setText(selected.getAbsolutePath());
    }

    protected void onExit() {
        if (!confirmDiscard())
            return;

        preferences.putBoolean(START_GOOGLE_EARTH_PREFERENCE, checkboxStartGoogleEarth.isSelected());
        preferences.putBoolean(DUPLICATE_FIRST_POSITION_PREFERENCE, checkboxDuplicateFirstPosition.isSelected());
        preferences.putBoolean(NUMBER_POSITION_NAMES_PREFERENCE, checkboxNumberPositionNames.isSelected());
        preferences.putBoolean(SAVE_AS_ROUTE_TRACK_WAYPOINTS_PREFERENCE, checkBoxSaveAsRouteTrackWaypoints.isSelected());
        preferences.putInt(DIVIDER_LOCATION_PREFERENCE, splitPane.getDividerLocation());

        if (mapView != null)
            mapView.dispose();
        closeFrame();

        log.info("Exited " + getTitle() + " on " + Platform.getPlatform() + " with " + Platform.getJvm());
        System.exit(0);
    }

    // Dialogs

    private void showFilter() {
        FilterDialog options = createFilterDialog();
        options.pack();
        options.setLocationRelativeTo(frame);
        options.setVisible(true);
    }

    private void showAddUrlToCatalog(CategoryTreeNode categoryTreeNode, String description, String url) {
        AddUrlDialog addUrlDialog = new AddUrlDialog(this, categoryTreeNode, description, url);
        addUrlDialog.pack();
        addUrlDialog.setLocationRelativeTo(frame);
        addUrlDialog.setVisible(true);
    }

    private void showAddFileToCatalog(CategoryTreeNode categoryTreeNode, String description, Double length, File file) {
        AddFileDialog addFileDialog = new AddFileDialog(this, categoryTreeNode, description, length, file);
        addFileDialog.pack();
        addFileDialog.setLocationRelativeTo(frame);
        addFileDialog.setVisible(true);
    }

    protected RenameDialog createRenameDialog() {
        return new RenameDialog(this);
    }

    protected abstract FilterDialog createFilterDialog();

    protected abstract ExternalPrograms createExternalPrograms();

    protected abstract Updater createUpdater();

    // Callbacks from dialogs

    void login(String userName, String password) {
        routeService.setAuthentication(userName, password);
        setUserNamePreference(userName, password);
    }

    void register(String userName, String password, String firstName, String lastName, String email) throws IOException {
        routeService.addUser(userName, password, firstName, lastName, email);
    }

    void addFile(final CategoryTreeNode category, final String description, final File file) {
        operator.executeOnRouteService(new RouteServiceOperator.Operation() {
            public void run() throws IOException {
                category.addRoute(description, file);
            }
        });
    }

    void addUrl(final CategoryTreeNode category, final String description, final String string) {
        operator.executeOnRouteService(new RouteServiceOperator.Operation() {
            public void run() throws IOException {
                category.addRoute(description, string);
            }
        });
    }

    // Preferences handling

    Double getAddPositionLongitude() {
        return preferences.getDouble(ADD_POSITION_LONGITUDE_PREFERENCE, -41.0);
    }

    void setAddPositionLongitude(double longitude) {
        preferences.putDouble(ADD_POSITION_LONGITUDE_PREFERENCE, longitude);
    }

    Double getAddPositionLatitude() {
        return preferences.getDouble(ADD_POSITION_LATITUDE_PREFERENCE, 41.0);
    }

    void setAddPositionLatitude(double latitude) {
        preferences.putDouble(ADD_POSITION_LATITUDE_PREFERENCE, latitude);
    }

    boolean getStartWithLastFilePreference() {
        return preferences.getBoolean(START_WITH_LAST_FILE_PREFERENCE, true);
    }

    int getSelectDuplicatePreference() {
        return preferences.getInt(SELECT_DUPLICATE_PREFERENCE, 5);
    }

    void setSelectDuplicatePreference(int selectDuplicatePreference) {
        preferences.putInt(SELECT_DUPLICATE_PREFERENCE, selectDuplicatePreference);
    }

    int getSelectByDistancePreference() {
        return preferences.getInt(SELECT_BY_DISTANCE_PREFERENCE, 1000);
    }

    void setSelectByDistancePreference(int selectByDistancePreference) {
        preferences.putInt(SELECT_BY_DISTANCE_PREFERENCE, selectByDistancePreference);
    }

    int getSelectByOrderPreference() {
        return preferences.getInt(SELECT_BY_ORDER_PREFERENCE, 5);
    }

    void setSelectByOrderPreference(int selectByOrderPreference) {
        preferences.putInt(SELECT_BY_ORDER_PREFERENCE, selectByOrderPreference);
    }

    int getSelectBySignificancePreference() {
        return preferences.getInt(SELECT_BY_SIGNIFICANCE_PREFERENCE, 20);
    }

    void setSelectBySignificancePreference(int selectBySignificancePreference) {
        preferences.putInt(SELECT_BY_SIGNIFICANCE_PREFERENCE, selectBySignificancePreference);
    }

    String getUserNamePreference() {
        return preferences.get(USERNAME_PREFERENCE, "");
    }

    String getPasswordPreference() {
        return new String(preferences.getByteArray(PASSWORD_PREFERENCE, new byte[0]));
    }

    void setUserNamePreference(String userNamePreference, String passwordPreference) {
        preferences.put(USERNAME_PREFERENCE, userNamePreference);
        preferences.putByteArray(PASSWORD_PREFERENCE, passwordPreference.getBytes());
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(341);
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(true);
        splitPane.setOpaque(true);
        splitPane.setResizeWeight(1.0);
        contentPane.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(880, 580), null, 0, false));
        splitPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        mapPanel = new JPanel();
        mapPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mapPanel.setMinimumSize(new Dimension(-1, -1));
        mapPanel.setPreferredSize(new Dimension(300, 560));
        splitPane.setLeftComponent(mapPanel);
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(3);
        splitPane.setRightComponent(tabbedPane);
        convertPanel = new JPanel();
        convertPanel.setLayout(new GridLayoutManager(12, 3, new Insets(3, 3, 0, 3), -1, -1));
        convertPanel.setMinimumSize(new Dimension(-1, -1));
        convertPanel.setPreferredSize(new Dimension(560, 560));
        tabbedPane.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("convert-tab"), convertPanel);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("source"));
        convertPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldSource = new JTextField();
        textFieldSource.setDragEnabled(true);
        textFieldSource.setText("");
        convertPanel.add(textFieldSource, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFile = new JButton();
        buttonOpenFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/open.png")));
        buttonOpenFile.setInheritsPopupMenu(false);
        buttonOpenFile.setText("");
        buttonOpenFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("open-tooltip"));
        convertPanel.add(buttonOpenFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        convertPanel.add(scrollPane1, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tablePositions = new JTable();
        tablePositions.setAutoCreateColumnsFromModel(false);
        tablePositions.setShowHorizontalLines(false);
        tablePositions.setShowVerticalLines(false);
        scrollPane1.setViewportView(tablePositions);
        buttonSaveFile = new JButton();
        buttonSaveFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/save.png")));
        buttonSaveFile.setText("");
        buttonSaveFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("save-tooltip"));
        convertPanel.add(buttonSaveFile, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel1, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRevertPositionList = new JButton();
        buttonRevertPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/revert.png")));
        buttonRevertPositionList.setText("");
        buttonRevertPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("revert-tooltip"));
        panel1.add(buttonRevertPositionList, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionUp = new JButton();
        buttonMovePositionUp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/up.png")));
        buttonMovePositionUp.setText("");
        buttonMovePositionUp.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-up-tooltip"));
        panel1.add(buttonMovePositionUp, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionDown = new JButton();
        buttonMovePositionDown.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/down.png")));
        buttonMovePositionDown.setText("");
        buttonMovePositionDown.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-down-tooltip"));
        panel1.add(buttonMovePositionDown, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToTop = new JButton();
        buttonMovePositionToTop.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/top.png")));
        buttonMovePositionToTop.setText("");
        buttonMovePositionToTop.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-top-tooltip"));
        panel1.add(buttonMovePositionToTop, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonMovePositionToBottom = new JButton();
        buttonMovePositionToBottom.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/bottom.png")));
        buttonMovePositionToBottom.setText("");
        buttonMovePositionToBottom.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("move-to-bottom-tooltip"));
        panel1.add(buttonMovePositionToBottom, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemovePosition = new JButton();
        buttonRemovePosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-position.png")));
        buttonRemovePosition.setText("");
        buttonRemovePosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remove-tooltip"));
        panel1.add(buttonRemovePosition, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonFilterPositionList = new JButton();
        buttonFilterPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/filter.png")));
        buttonFilterPositionList.setText("");
        buttonFilterPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-tooltip"));
        panel1.add(buttonFilterPositionList, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddPosition = new JButton();
        buttonAddPosition.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/add-position.png")));
        buttonAddPosition.setText("");
        buttonAddPosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-tooltip"));
        panel1.add(buttonAddPosition, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkboxStartGoogleEarth = new JCheckBox();
        this.$$$loadButtonText$$$(checkboxStartGoogleEarth, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("start-google-earth-after-save"));
        convertPanel.add(checkboxStartGoogleEarth, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkboxDuplicateFirstPosition = new JCheckBox();
        this.$$$loadButtonText$$$(checkboxDuplicateFirstPosition, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("duplicate-first-position"));
        convertPanel.add(checkboxDuplicateFirstPosition, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkboxNumberPositionNames = new JCheckBox();
        this.$$$loadButtonText$$$(checkboxNumberPositionNames, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("number-positions"));
        convertPanel.add(checkboxNumberPositionNames, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel2, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        convertPanel.add(panel6, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRemovePositionList = new JButton();
        buttonRemovePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/remove-route.png")));
        buttonRemovePositionList.setText("");
        buttonRemovePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remove-position-list-tooltip"));
        panel6.add(buttonRemovePositionList, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAppendFileToPositionList = new JButton();
        buttonAppendFileToPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/append-route.png")));
        buttonAppendFileToPositionList.setText("");
        buttonAppendFileToPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("append-tooltip"));
        panel6.add(buttonAppendFileToPositionList, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenamePositionList = new JButton();
        buttonRenamePositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/rename-route.png")));
        buttonRenamePositionList.setText("");
        buttonRenamePositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename-position-list-tooltip"));
        panel6.add(buttonRenamePositionList, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPositions = new JLabel();
        labelPositions.setHorizontalAlignment(2);
        labelPositions.setHorizontalTextPosition(2);
        labelPositions.setText("-");
        panel6.add(labelPositions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddPositionList = new JButton();
        buttonAddPositionList.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/add-route.png")));
        buttonAddPositionList.setText("");
        buttonAddPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-position-list-tooltip"));
        panel6.add(buttonAddPositionList, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPositionLists = new JLabel();
        labelPositionLists.setHorizontalAlignment(2);
        labelPositionLists.setHorizontalTextPosition(2);
        labelPositionLists.setText("-");
        convertPanel.add(labelPositionLists, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("positions"));
        convertPanel.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel7, new GridConstraints(1, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonNewFile = new JButton();
        buttonNewFile.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/new.png")));
        buttonNewFile.setInheritsPopupMenu(false);
        buttonNewFile.setText("");
        buttonNewFile.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("new-tooltip"));
        panel7.add(buttonNewFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxSaveAsRouteTrackWaypoints = new JCheckBox();
        checkBoxSaveAsRouteTrackWaypoints.setSelected(false);
        this.$$$loadButtonText$$$(checkBoxSaveAsRouteTrackWaypoints, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("save-as-route-track-waypoints"));
        convertPanel.add(checkBoxSaveAsRouteTrackWaypoints, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(panel8, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionList = new JComboBox();
        comboBoxChoosePositionList.setVisible(true);
        panel8.add(comboBoxChoosePositionList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChoosePositionListCharacteristics = new JComboBox();
        panel8.add(comboBoxChoosePositionListCharacteristics, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("characteristics"));
        panel8.add(label6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("position-list"));
        label7.setVisible(true);
        convertPanel.add(label7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        expertPanel = new JPanel();
        expertPanel.setLayout(new GridLayoutManager(6, 1, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("expert-tab"), expertPanel);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        expertPanel.add(panel9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("preferred-locale"));
        panel9.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxLocale = new JComboBox();
        panel9.add(comboBoxLocale, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("gpsbabel-path"));
        panel9.add(label9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldBabelPath = new JTextField();
        panel9.add(textFieldBabelPath, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseGPSBabel = new JButton();
        buttonChooseGPSBabel.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/select.png")));
        buttonChooseGPSBabel.setText("");
        buttonChooseGPSBabel.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-gpsbabel-path"));
        panel9.add(buttonChooseGPSBabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("start-with-last-file"));
        panel9.add(label10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxStartWithLastFile = new JCheckBox();
        checkBoxStartWithLastFile.setHorizontalAlignment(11);
        checkBoxStartWithLastFile.setSelected(false);
        checkBoxStartWithLastFile.setText("");
        panel9.add(checkBoxStartWithLastFile, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("automatic-update-check"));
        panel9.add(label11, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAutomaticUpdateCheck = new JCheckBox();
        checkBoxAutomaticUpdateCheck.setHorizontalAlignment(11);
        checkBoxAutomaticUpdateCheck.setHorizontalTextPosition(11);
        checkBoxAutomaticUpdateCheck.setText("");
        panel9.add(checkBoxAutomaticUpdateCheck, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        expertPanel.add(panel10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JPanel panel11 = new JPanel();
        expertPanel.add(panel11, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 2, new Insets(3, 3, 3, 3), -1, -1));
        expertPanel.add(panel12, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenumberPositions = new JButton();
        this.$$$loadButtonText$$$(buttonRenumberPositions, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("renumber-positions"));
        panel12.add(buttonRenumberPositions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPrintMap = new JButton();
        this.$$$loadButtonText$$$(buttonPrintMap, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("print-map"));
        panel12.add(buttonPrintMap, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        expertPanel.add(panel13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelBrowse = new JLabel();
        this.$$$loadLabelText$$$(labelBrowse, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-www"));
        panel13.add(labelBrowse, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelMail = new JLabel();
        this.$$$loadLabelText$$$(labelMail, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-mail"));
        panel13.add(labelMail, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelCp = new JLabel();
        labelCp.setForeground(UIManager.getColor("Label.background"));
        labelCp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/cp.png")));
        labelCp.setInheritsPopupMenu(true);
        labelCp.setOpaque(false);
        labelCp.setText("");
        panel13.add(labelCp, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelCredit = new JLabel();
        this.$$$loadLabelText$$$(labelCredit, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-credit"));
        panel13.add(labelCredit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        expertPanel.add(panel14, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCheckForUpdate = new JButton();
        this.$$$loadButtonText$$$(buttonCheckForUpdate, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-check-for-update"));
        panel14.add(buttonCheckForUpdate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonTestDragListenerPort = new JButton();
        buttonTestDragListenerPort.setText("Test Drag and Drop Communication");
        panel14.add(buttonTestDragListenerPort, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        browsePanel = new JPanel();
        browsePanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("browse-tab"), browsePanel);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        browsePanel.add(panel15, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAddCategory = new JButton();
        this.$$$loadButtonText$$$(buttonAddCategory, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add"));
        panel15.add(buttonAddCategory, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeleteCategory = new JButton();
        this.$$$loadButtonText$$$(buttonDeleteCategory, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete"));
        panel15.add(buttonDeleteCategory, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenameCategory = new JButton();
        this.$$$loadButtonText$$$(buttonRenameCategory, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename"));
        panel15.add(buttonRenameCategory, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("categories"));
        browsePanel.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routes"));
        browsePanel.add(label13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        browsePanel.add(panel16, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAddFile = new JButton();
        this.$$$loadButtonText$$$(buttonAddFile, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-route-by-file"));
        panel16.add(buttonAddFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel16.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonDeleteRoute = new JButton();
        this.$$$loadButtonText$$$(buttonDeleteRoute, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete"));
        panel16.add(buttonDeleteRoute, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenameRoute = new JButton();
        this.$$$loadButtonText$$$(buttonRenameRoute, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("rename"));
        panel16.add(buttonRenameRoute, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddUrl = new JButton();
        this.$$$loadButtonText$$$(buttonAddUrl, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("add-route-by-url"));
        panel16.add(buttonAddUrl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonLogin = new JButton();
        this.$$$loadButtonText$$$(buttonLogin, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("login"));
        buttonLogin.setVisible(false);
        panel16.add(buttonLogin, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        browsePanel.add(scrollPane2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableRoutes = new JTable();
        scrollPane2.setViewportView(tableRoutes);
        final JScrollPane scrollPane3 = new JScrollPane();
        browsePanel.add(scrollPane3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        treeCategories = new JTree();
        scrollPane3.setViewportView(treeCategories);
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
        return contentPane;
    }
}
