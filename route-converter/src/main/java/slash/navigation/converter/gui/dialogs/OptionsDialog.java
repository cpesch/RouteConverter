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

package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.common.helpers.APIKeyRegistry;
import slash.common.helpers.TimeZoneAndId;
import slash.common.helpers.TimeZoneAndIds;
import slash.navigation.babel.BabelFormat;
import slash.navigation.columbus.ColumbusV1000Device;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NumberPattern;
import slash.navigation.common.NumberingStrategy;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.helpers.MapViewImplementation;
import slash.navigation.converter.gui.helpers.RoutingServiceFacade;
import slash.navigation.converter.gui.models.FixMapMode;
import slash.navigation.converter.gui.renderer.*;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.googlemaps.GoogleMapsServer;
import slash.navigation.gui.Application;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.mapview.MapView;
import slash.navigation.routing.RoutingPreferencesModel;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Locale.*;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JFileChooser.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.LocaleHelper.*;
import static slash.common.io.Transfer.trim;
import static slash.navigation.common.DegreeFormat.*;
import static slash.navigation.common.NumberPattern.*;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.common.NumberingStrategy.Relative_Position_In_Current_Selection;
import static slash.navigation.common.UnitSystem.*;
import static slash.navigation.converter.gui.RouteConverter.*;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.*;
import static slash.navigation.converter.gui.models.FixMapMode.*;
import static slash.navigation.googlemaps.GoogleMapsServer.*;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;
import static slash.navigation.gui.helpers.UIHelper.createJFileChooser;

/**
 * Dialog to show options for the program.
 *
 * @author Christian Pesch
 */

public class OptionsDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTabbedPane tabbedPane1;
    private JComboBox<Locale> comboBoxLocale;
    private JComboBox<GoogleMapsServer> comboBoxGoogleMapsServer;
    private JComboBox<FixMapMode> comboBoxFixMapMode;
    private JComboBox<MapViewImplementation> comboBoxMapService;
    private JTextField textFieldMapsPath;
    private JButton buttonChooseMapsPath;
    private JTextField textFieldThemesPath;
    private JButton buttonChooseThemesPath;
    private JTextField textFieldBabelPath;
    private JButton buttonChooseBabelPath;
    private JCheckBox checkBoxAutomaticUpdateCheck;
    private JCheckBox checkBoxAvoidFerries;
    private JCheckBox checkBoxAvoidHighways;
    private JCheckBox checkBoxAvoidTolls;
    private JCheckBox checkBoxShowAllPositionsAfterLoading;
    private JCheckBox checkBoxRecenterAfterZooming;
    private JCheckBox checkBoxShowCoordinates;
    private JCheckBox checkBoxShowWaypointDescription;
    private JComboBox<RoutingService> comboBoxRoutingService;
    private JTextField textFieldRoutingServicePath;
    private JButton buttonChooseRoutingServicePath;
    private JComboBox<ElevationService> comboBoxElevationService;
    private JTextField textFieldElevationServicePath;
    private JButton buttonChooseElevationServicePath;
    private JComboBox<TravelMode> comboboxTravelMode;
    private JComboBox<NumberPattern> comboboxNumberPattern;
    private JComboBox<NumberingStrategy> comboBoxNumberingStrategy;
    private JComboBox<UnitSystem> comboBoxUnitSystem;
    private JComboBox<TimeZoneAndId> comboBoxTimeZone;
    private JComboBox<DegreeFormat> comboBoxDegreeFormat;
    private JRadioButton radioButtonV1000LocalTime;
    private JRadioButton radioButtonV1000UTC;
    private JButton buttonClose;
    private JColorChooser colorChooserRoute;
    private JColorChooser colorChooserTrack;
    private JColorChooser colorChooserWaypoint;
    private JComboBox<GeocodingService> comboBoxGeocodingService;
    private JTextField textFieldGoogleApiKey;
    private JTextField textFieldThunderforestApiKey;
    private JTextField textFieldGeonamesUserName;
    private JLabel labelGoogleApiKey;
    private JLabel labelThunderforestApiKey;
    private JLabel labelGeonamesUserName;

    public OptionsDialog() {
        super(RouteConverter.getInstance().getFrame(), "options");
        setTitle(getBundle().getString("options-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        ComboBoxModel<Locale> localeModel = new DefaultComboBoxModel<>(new Locale[]{
                ARABIA, BRAZIL, CATALAN, CHINA, CZECH, DENMARK, GERMANY, US, SPAIN, FRANCE, CROATIA,
                ITALY, JAPAN, KOREA, HUNGARY, NEDERLANDS, NORWAY_BOKMAL, POLAND, PORTUGAL, RUSSIA,
                SLOVAKIA, SERBIA, UKRAINE, ROOT
        });
        localeModel.setSelectedItem(Application.getInstance().getLocale());
        comboBoxLocale.setModel(localeModel);
        comboBoxLocale.setRenderer(new LocaleListCellRenderer());
        comboBoxLocale.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            Locale locale = (Locale) e.getItem();
            Application.getInstance().setLocale(locale);
        });

        List<MapViewImplementation> mapViews = r.getAvailableMapViews();
        ComboBoxModel<MapViewImplementation> mapViewModel =
                new DefaultComboBoxModel<>(mapViews.toArray(new MapViewImplementation[0]));
        mapViewModel.setSelectedItem(r.getMapViewPreference());
        comboBoxMapService.setModel(mapViewModel);
        comboBoxMapService.setRenderer(new MapViewListCellRenderer());
        comboBoxMapService.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            MapViewImplementation mapView = (MapViewImplementation) e.getItem();
            r.setMapView(mapView);
            handleMapServiceUpdate();
        });
        handleMapServiceUpdate();
        textFieldMapsPath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent de) {
                MapView service = r.getMapView();
                if (service.isDownload())
                    try {
                        service.setMapsPath(textFieldMapsPath.getText());
                    } catch (IOException e) {
                        r.getContext().getNotificationManager().showNotification(MessageFormat.format(
                                getBundle().getString("scan-error"), getLocalizedMessage(e)), null);
                    }
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        buttonChooseMapsPath.addActionListener(new FrameAction() {
            public void run() {
                chooseMapPath();
            }
        });
        textFieldThemesPath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent de) {
                MapView service = r.getMapView();
                if (service.isDownload())
                    try {
                        service.setThemesPath(textFieldThemesPath.getText());
                    } catch (IOException e) {
                        r.getContext().getNotificationManager().showNotification(MessageFormat.format(
                                getBundle().getString("scan-error"), getLocalizedMessage(e)), null);
                    }
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        buttonChooseThemesPath.addActionListener(new FrameAction() {
            public void run() {
                chooseThemePath();
            }
        });

        ComboBoxModel<GoogleMapsServer> googleMapsServerModel = new DefaultComboBoxModel<>(new GoogleMapsServer[]{
                International, China, Ditu, Uzbekistan
        });
        googleMapsServerModel.setSelectedItem(r.getGoogleMapsServerModel().getGoogleMapsServer());
        comboBoxGoogleMapsServer.setModel(googleMapsServerModel);
        comboBoxGoogleMapsServer.setRenderer(new GoogleMapsServerListCellRenderer());
        comboBoxGoogleMapsServer.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            GoogleMapsServer googleMapsServer = (GoogleMapsServer) e.getItem();
            r.getGoogleMapsServerModel().setGoogleMapsServer(googleMapsServer);
        });

        ComboBoxModel<FixMapMode> fixMapModeModel = new DefaultComboBoxModel<>(new FixMapMode[]{
                Automatic, Yes, No
        });
        fixMapModeModel.setSelectedItem(r.getMapPreferencesModel().getFixMapModeModel().getFixMapMode());
        comboBoxFixMapMode.setModel(fixMapModeModel);
        comboBoxFixMapMode.setRenderer(new FixMapModeListCellRenderer());
        comboBoxFixMapMode.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            FixMapMode fixMapMode = (FixMapMode) e.getItem();
            r.getMapPreferencesModel().getFixMapModeModel().setFixMapMode(fixMapMode);
        });

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
        buttonChooseBabelPath.addActionListener(new FrameAction() {
            public void run() {
                chooseBabelPath();
            }
        });

        labelGoogleApiKey.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForGoogleApiKey(OptionsDialog.this);
            }
        });
        // first setText then adding the listener to avoid #restartMapView() on initialization
        textFieldGoogleApiKey.setText(APIKeyRegistry.getInstance().getAPIKeyPreference("google"));
        textFieldGoogleApiKey.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                APIKeyRegistry.getInstance().setAPIKeyPreference("google", trimApiKey(textFieldGoogleApiKey.getText()));
                restartMapView();
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        labelThunderforestApiKey.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForThunderforestApiKey(OptionsDialog.this);
            }
        });
        // first setText then adding the listener to avoid #restartMapView() on initialization
        textFieldThunderforestApiKey.setText(APIKeyRegistry.getInstance().getAPIKeyPreference("thunderforest"));
        textFieldThunderforestApiKey.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                APIKeyRegistry.getInstance().setAPIKeyPreference("thunderforest", trimApiKey(textFieldThunderforestApiKey.getText()));
                restartMapView();
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        labelGeonamesUserName.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForGeonamesUserName(OptionsDialog.this);
            }
        });
        textFieldGeonamesUserName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                APIKeyRegistry.getInstance().setAPIKeyPreference("geonames", trimApiKey(textFieldGeonamesUserName.getText()));
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        textFieldGeonamesUserName.setText(APIKeyRegistry.getInstance().getAPIKeyPreference("geonames"));

        new CheckBoxPreferencesSynchronizer(checkBoxAutomaticUpdateCheck, getPreferences(), AUTOMATIC_UPDATE_CHECK_PREFERENCE, true);

        checkBoxShowAllPositionsAfterLoading.setSelected(r.getShowAllPositionsAfterLoading().getBoolean());
        checkBoxShowAllPositionsAfterLoading.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getShowAllPositionsAfterLoading().setBoolean(checkBoxShowAllPositionsAfterLoading.isSelected());
            }
        });

        checkBoxRecenterAfterZooming.setSelected(r.getRecenterAfterZooming().getBoolean());
        checkBoxRecenterAfterZooming.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRecenterAfterZooming().setBoolean(checkBoxRecenterAfterZooming.isSelected());
            }
        });

        checkBoxShowCoordinates.setSelected(r.getMapPreferencesModel().getShowCoordinatesModel().getBoolean());
        checkBoxShowCoordinates.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getMapPreferencesModel().getShowCoordinatesModel().setBoolean(checkBoxShowCoordinates.isSelected());
            }
        });

        checkBoxShowWaypointDescription.setSelected(r.getMapPreferencesModel().getShowWaypointDescriptionModel().getBoolean());
        checkBoxShowWaypointDescription.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getMapPreferencesModel().getShowWaypointDescriptionModel().setBoolean(checkBoxShowWaypointDescription.isSelected());
            }
        });
        checkBoxShowWaypointDescription.setEnabled(r.isMapViewAvailable() && !r.getMapView().isDownload());

        DefaultComboBoxModel<RoutingService> routingServiceModel = new DefaultComboBoxModel<>();
        for (RoutingService service : r.getRoutingServiceFacade().getRoutingPreferencesModel().getRoutingServices()) {
            routingServiceModel.addElement(service);
        }
        routingServiceModel.setSelectedItem(r.getRoutingServiceFacade().getRoutingService());
        comboBoxRoutingService.setModel(routingServiceModel);
        comboBoxRoutingService.setRenderer(new RoutingServiceListCellRenderer());
        comboBoxRoutingService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                RoutingService service = (RoutingService) e.getItem();
                r.getRoutingServiceFacade().setRoutingService(service);
                handleRoutingServiceUpdate();
            }
        });

        textFieldRoutingServicePath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                RoutingService service = r.getRoutingServiceFacade().getRoutingService();
                if (service.isDownload()) {
                    service.setPath(textFieldRoutingServicePath.getText());
                }
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        buttonChooseRoutingServicePath.addActionListener(new FrameAction() {
            public void run() {
                chooseRoutingServicePath();
            }
        });
        handleRoutingServiceUpdate();

        comboboxTravelMode.setRenderer(new TravelModeListCellRenderer());
        comboboxTravelMode.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                TravelMode travelMode = (TravelMode) e.getItem();
                r.getRoutingServiceFacade().getRoutingPreferencesModel().setTravelMode(travelMode);
            }
        });
        checkBoxAvoidFerries.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().getRoutingPreferencesModel().setAvoidFerries(checkBoxAvoidFerries.isSelected());
            }
        });
        checkBoxAvoidHighways.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().getRoutingPreferencesModel().setAvoidHighways(checkBoxAvoidHighways.isSelected());
            }
        });
        checkBoxAvoidTolls.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().getRoutingPreferencesModel().setAvoidTolls(checkBoxAvoidTolls.isSelected());
            }
        });

        ComboBoxModel<NumberPattern> numberPatternModel = new DefaultComboBoxModel<>(new NumberPattern[]{
                Description_Only, Number_Only, Number_Directly_Followed_By_Description, Number_Space_Then_Description
        });
        numberPatternModel.setSelectedItem(r.getNumberPatternPreference());
        comboboxNumberPattern.setModel(numberPatternModel);
        comboboxNumberPattern.setRenderer(new NumberPatternListCellRenderer());
        comboboxNumberPattern.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                NumberPattern numberPattern = (NumberPattern) e.getItem();
                r.setNumberPatternPreference(numberPattern);
            }
        });

        ComboBoxModel<NumberingStrategy> numberingStrategyModel = new DefaultComboBoxModel<>(new NumberingStrategy[]{
                Absolute_Position_Within_Position_List, Relative_Position_In_Current_Selection
        });
        numberingStrategyModel.setSelectedItem(r.getNumberingStrategyPreference());
        comboBoxNumberingStrategy.setModel(numberingStrategyModel);
        comboBoxNumberingStrategy.setRenderer(new NumberingStrategyListCellRenderer());
        comboBoxNumberingStrategy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                NumberingStrategy numberingStrategy = (NumberingStrategy) e.getItem();
                r.setNumberingStrategyPreference(numberingStrategy);
            }
        });

        DefaultComboBoxModel<ElevationService> elevationServiceModel = new DefaultComboBoxModel<>();
        for (ElevationService service : r.getElevationServiceFacade().getElevationServices()) {
            elevationServiceModel.addElement(service);
        }
        elevationServiceModel.setSelectedItem(r.getElevationServiceFacade().getElevationService());
        comboBoxElevationService.setModel(elevationServiceModel);
        comboBoxElevationService.setRenderer(new ElevationServiceListCellRenderer());
        comboBoxElevationService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                ElevationService service = (ElevationService) e.getItem();
                r.getElevationServiceFacade().setElevationService(service);
                handleElevationServiceUpdate();
            }
        });

        textFieldElevationServicePath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                ElevationService service = r.getElevationServiceFacade().getElevationService();
                if (service.isDownload()) {
                    service.setPath(textFieldElevationServicePath.getText());
                }
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        buttonChooseElevationServicePath.addActionListener(new FrameAction() {
            public void run() {
                chooseElevationServicePath();
            }
        });
        handleElevationServiceUpdate();

        DefaultComboBoxModel<GeocodingService> geocodingServiceModel = new DefaultComboBoxModel<>();
        for (GeocodingService service : r.getGeocodingServiceFacade().getGeocodingServices()) {
            geocodingServiceModel.addElement(service);
        }
        geocodingServiceModel.setSelectedItem(r.getGeocodingServiceFacade().getGeocodingService());
        comboBoxGeocodingService.setModel(geocodingServiceModel);
        comboBoxGeocodingService.setRenderer(new GeocodingServiceListCellRenderer());
        comboBoxGeocodingService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                GeocodingService service = (GeocodingService) e.getItem();
                r.getGeocodingServiceFacade().setGeocodingService(service);
            }
        });

        ComboBoxModel<UnitSystem> unitSystemModel = new DefaultComboBoxModel<>(new UnitSystem[]{
                Metric, Nautic, Statute
        });
        unitSystemModel.setSelectedItem(r.getUnitSystemModel().getUnitSystem());
        comboBoxUnitSystem.setModel(unitSystemModel);
        comboBoxUnitSystem.setRenderer(new UnitSystemListCellRenderer());
        comboBoxUnitSystem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                UnitSystem unitSystem = (UnitSystem) e.getItem();
                r.getUnitSystemModel().setUnitSystem(unitSystem);
            }
        });

        ComboBoxModel<DegreeFormat> degreeFormatModel = new DefaultComboBoxModel<>(new DegreeFormat[]{
                Degrees, Degrees_Minutes, Degrees_Minutes_Seconds
        });
        degreeFormatModel.setSelectedItem(r.getUnitSystemModel().getDegreeFormat());
        comboBoxDegreeFormat.setModel(degreeFormatModel);
        comboBoxDegreeFormat.setRenderer(new DegreeFormatListCellRenderer());
        comboBoxDegreeFormat.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                DegreeFormat degreeFormat = (DegreeFormat) e.getItem();
                r.getUnitSystemModel().setDegreeFormat(degreeFormat);
            }
        });

        TimeZoneAndIds timeZoneAndIds = TimeZoneAndIds.getInstance();
        ComboBoxModel<TimeZoneAndId> timeZoneModel = new DefaultComboBoxModel<>(timeZoneAndIds.getTimeZones());
        timeZoneModel.setSelectedItem(timeZoneAndIds.getTimeZoneAndIdFor(r.getTimeZone().getTimeZone()));
        comboBoxTimeZone.setModel(timeZoneModel);
        comboBoxTimeZone.setRenderer(new TimeZoneAndIdListCellRenderer());
        comboBoxTimeZone.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                TimeZoneAndId timeZoneAndId = (TimeZoneAndId) e.getItem();
                r.getTimeZone().setTimeZone(timeZoneAndId.getTimeZone());
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(radioButtonV1000LocalTime);
        group.add(radioButtonV1000UTC);
        radioButtonV1000LocalTime.setSelected(ColumbusV1000Device.getUseLocalTimeZone());
        radioButtonV1000UTC.setSelected(!ColumbusV1000Device.getUseLocalTimeZone());
        radioButtonV1000LocalTime.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                ColumbusV1000Device.setUseLocalTimeZone(radioButtonV1000LocalTime.isSelected());
            }
        });

        colorChooserRoute.setColor(r.getMapPreferencesModel().getRouteColorModel().getColor());
        reducePanels(colorChooserRoute);
        colorChooserRoute.getSelectionModel().addChangeListener(e -> r.getMapPreferencesModel().getRouteColorModel().setColor(colorChooserRoute.getColor()));
        colorChooserTrack.setColor(r.getMapPreferencesModel().getTrackColorModel().getColor());
        reducePanels(colorChooserTrack);
        colorChooserTrack.getSelectionModel().addChangeListener(e -> r.getMapPreferencesModel().getTrackColorModel().setColor(colorChooserTrack.getColor()));
        colorChooserWaypoint.setColor(r.getMapPreferencesModel().getWaypointColorModel().getColor());
        reducePanels(colorChooserWaypoint);
        colorChooserWaypoint.getSelectionModel().addChangeListener(e -> r.getMapPreferencesModel().getWaypointColorModel().setColor(colorChooserWaypoint.getColor()));

        setMnemonic(buttonClose, "close-mnemonic");
        buttonClose.addActionListener(new DialogAction(this) {
            public void run() {
                close();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        contentPane.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                close();
            }
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private String trimApiKey(String string) {
        string = trim(string);
        return string != null ? string.replaceAll("\\s+", "") : null;
    }

    private void restartMapView() {
        RouteConverter r = RouteConverter.getInstance();
        r.setMapView(r.getMapViewPreference());
    }

    private static final Set<String> REMOVEABLE_COLOR_PANELS = new HashSet<>(
            asList("Swatches", "HSV", "HSL", "CMYK",
                    // German Mac OS X has different names
                    "Muster",
                    // French locale has different names
                    "Echantillons", "TSV", "TSL",
                    // Chinese locale has different names
                    "\u6837\u672c(S)", "HSV(H)", "HSL(L)"
            )
    );

    private void reducePanels(JColorChooser chooser) {
        chooser.setPreviewPanel(new JPanel());
        for (AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
            String displayName = panel.getDisplayName();
            if (REMOVEABLE_COLOR_PANELS.contains(displayName)) {
                chooser.removeChooserPanel(panel);
            }
        }
    }

    private void handleMapServiceUpdate() {
        MapView service = RouteConverter.getInstance().getMapView();
        boolean download = service != null && service.isDownload();
        textFieldMapsPath.setEnabled(download);
        textFieldMapsPath.setText(download ? service.getMapsPath() : "");
        buttonChooseMapsPath.setEnabled(download);
        textFieldThemesPath.setEnabled(download);
        textFieldThemesPath.setText(download ? service.getThemesPath() : "");
        buttonChooseThemesPath.setEnabled(download);
        comboBoxGoogleMapsServer.setEnabled(!download);
    }

    private void handleRoutingServiceUpdate() {
        RoutingServiceFacade facade = RouteConverter.getInstance().getRoutingServiceFacade();
        RoutingPreferencesModel preferences = facade.getRoutingPreferencesModel();
        RoutingService service = facade.getRoutingService();
        textFieldRoutingServicePath.setEnabled(service.isDownload());
        textFieldRoutingServicePath.setText(service.isDownload() ? service.getPath() : "");
        buttonChooseRoutingServicePath.setEnabled(service.isDownload());
        checkBoxAvoidFerries.setEnabled(service.isSupportAvoidFerries());
        checkBoxAvoidFerries.setSelected(preferences.isAvoidFerries());
        checkBoxAvoidHighways.setEnabled(service.isSupportAvoidHighways());
        checkBoxAvoidHighways.setSelected(preferences.isAvoidHighways());
        checkBoxAvoidTolls.setEnabled(service.isSupportAvoidTolls());
        checkBoxAvoidTolls.setSelected(preferences.isAvoidTolls());
        updateTravelModes();
    }

    private void updateTravelModes() {
        RoutingServiceFacade facade = RouteConverter.getInstance().getRoutingServiceFacade();
        RoutingPreferencesModel preferences = facade.getRoutingPreferencesModel();
        RoutingService service = facade.getRoutingService();
        MutableComboBoxModel<TravelMode> travelModeModel = new DefaultComboBoxModel<>();
        List<TravelMode> availableTravelModes = service.getAvailableTravelModes();
        availableTravelModes.sort(comparing(TravelMode::getName));
        for (TravelMode travelMode : availableTravelModes) {
            travelModeModel.addElement(travelMode);
        }
        travelModeModel.setSelectedItem(preferences.getTravelMode());
        comboboxTravelMode.setModel(travelModeModel);
    }

    private void handleElevationServiceUpdate() {
        ElevationService service = RouteConverter.getInstance().getElevationServiceFacade().getElevationService();
        textFieldElevationServicePath.setEnabled(service.isDownload());
        textFieldElevationServicePath.setText(service.isDownload() ? service.getPath() : "");
        buttonChooseElevationServicePath.setEnabled(service.isDownload());
    }

    private void chooseMapPath() {
        RouteConverter r = RouteConverter.getInstance();
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-map-path"));
        chooser.setSelectedFile(new File(r.getMapView().getMapsPath()));
        chooser.setFileSelectionMode(DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(r.getFrame());
        if (open != APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0) {
            return;
        }

        textFieldMapsPath.setText(selected.getAbsolutePath());
    }

    private void chooseThemePath() {
        RouteConverter r = RouteConverter.getInstance();
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-theme-path"));
        chooser.setSelectedFile(new File(r.getMapView().getThemesPath()));
        chooser.setFileSelectionMode(DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(r.getFrame());
        if (open != APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0) {
            return;
        }

        textFieldThemesPath.setText(selected.getAbsolutePath());
    }

    private void chooseBabelPath() {
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-gpsbabel-path"));
        chooser.setSelectedFile(new File(BabelFormat.getBabelPathPreference()));
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0) {
            return;
        }

        textFieldBabelPath.setText(selected.getAbsolutePath());
    }

    private void chooseRoutingServicePath() {
        RouteConverter r = RouteConverter.getInstance();
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-routing-service-path"));
        chooser.setSelectedFile(new File(r.getRoutingServiceFacade().getRoutingService().getPath()));
        chooser.setFileSelectionMode(DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(r.getFrame());
        if (open != APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0) {
            return;
        }

        textFieldRoutingServicePath.setText(selected.getAbsolutePath());
    }

    private void chooseElevationServicePath() {
        RouteConverter r = RouteConverter.getInstance();
        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("choose-elevation-service-path"));
        chooser.setSelectedFile(new File(r.getElevationServiceFacade().getElevationService().getPath()));
        chooser.setFileSelectionMode(DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(r.getFrame());
        if (open != APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0) {
            return;
        }

        textFieldElevationServicePath.setText(selected.getAbsolutePath());
    }

    private void close() {
        dispose();
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(3, 3, 0, 3), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "close"));
        panel1.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        contentPane.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "general-options-tab"), panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "preferred-locale"));
        panel3.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxLocale = new JComboBox();
        panel3.add(comboBoxLocale, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "automatic-update-check"));
        panel3.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAutomaticUpdateCheck = new JCheckBox();
        checkBoxAutomaticUpdateCheck.setHorizontalAlignment(11);
        checkBoxAutomaticUpdateCheck.setHorizontalTextPosition(11);
        panel3.add(checkBoxAutomaticUpdateCheck, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "program-options"));
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 4, new Insets(3, 3, 3, 3), -1, -1));
        panel2.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "columbus-v1000"));
        panel5.add(label4, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "columbus-v1000-timezone-set-to"));
        panel5.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonV1000LocalTime = new JRadioButton();
        this.$$$loadButtonText$$$(radioButtonV1000LocalTime, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "columbus-v1000-local-time"));
        panel5.add(radioButtonV1000LocalTime, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonV1000UTC = new JRadioButton();
        this.$$$loadButtonText$$$(radioButtonV1000UTC, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "columbus-v1000-utc"));
        panel5.add(radioButtonV1000UTC, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel5.add(separator2, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(4, 1, new Insets(3, 3, 3, 3), -1, -1));
        panel2.add(panel7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel7.add(separator3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "display-options"));
        panel7.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "display-times-with-timezone"));
        panel8.add(label7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxTimeZone = new JComboBox();
        panel8.add(comboBoxTimeZone, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "display-measures-with-system-of-unit"));
        panel8.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxUnitSystem = new JComboBox();
        panel8.add(comboBoxUnitSystem, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "display-degrees-with-format"));
        panel8.add(label9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxDegreeFormat = new JComboBox();
        panel8.add(comboBoxDegreeFormat, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel7.add(panel9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel2.add(panel10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "numbering-options"));
        panel10.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel10.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "number-pattern"));
        panel10.add(label11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxNumberPattern = new JComboBox();
        panel10.add(comboboxNumberPattern, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "numbering-strategy"));
        panel10.add(label12, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxNumberingStrategy = new JComboBox();
        panel10.add(comboBoxNumberingStrategy, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel10.add(separator4, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel10.add(panel11, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(3, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "map-options-tab"), panel12);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(7, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel12.add(panel13, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "recenter-after-zooming"));
        panel13.add(label13, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxRecenterAfterZooming = new JCheckBox();
        panel13.add(checkBoxRecenterAfterZooming, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "show-coordinates"));
        panel13.add(label14, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowCoordinates = new JCheckBox();
        panel13.add(checkBoxShowCoordinates, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "show-waypoint-description"));
        panel13.add(label15, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowWaypointDescription = new JCheckBox();
        panel13.add(checkBoxShowWaypointDescription, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel13.add(panel14, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBoxShowAllPositionsAfterLoading = new JCheckBox();
        panel13.add(checkBoxShowAllPositionsAfterLoading, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "show-all-positions-after-loading"));
        panel13.add(label16, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "display-options"));
        panel13.add(label17, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel13.add(separator5, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel12.add(spacer5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(8, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel12.add(panel15, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        this.$$$loadLabelText$$$(label18, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "map-engine"));
        panel15.add(label18, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel15.add(separator6, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        this.$$$loadLabelText$$$(label19, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "map-service"));
        panel15.add(label19, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxMapService = new JComboBox();
        panel15.add(comboBoxMapService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label20 = new JLabel();
        this.$$$loadLabelText$$$(label20, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "map-path"));
        panel15.add(label20, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldMapsPath = new JTextField();
        panel15.add(textFieldMapsPath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseMapsPath = new JButton();
        buttonChooseMapsPath.setHideActionText(true);
        buttonChooseMapsPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseMapsPath.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "choose-map-path"));
        panel15.add(buttonChooseMapsPath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label21 = new JLabel();
        this.$$$loadLabelText$$$(label21, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "google-maps-server"));
        panel15.add(label21, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxGoogleMapsServer = new JComboBox();
        panel15.add(comboBoxGoogleMapsServer, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxFixMapMode = new JComboBox();
        panel15.add(comboBoxFixMapMode, new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label22 = new JLabel();
        this.$$$loadLabelText$$$(label22, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "fix-map-mode"));
        panel15.add(label22, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel15.add(panel16, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label23 = new JLabel();
        this.$$$loadLabelText$$$(label23, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "theme-path"));
        panel15.add(label23, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldThemesPath = new JTextField();
        panel15.add(textFieldThemesPath, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseThemesPath = new JButton();
        buttonChooseThemesPath.setHideActionText(true);
        buttonChooseThemesPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseThemesPath.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "choose-theme-path"));
        panel15.add(buttonChooseThemesPath, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "colors-options-tab"), panel17);
        final JTabbedPane tabbedPane2 = new JTabbedPane();
        panel17.add(tabbedPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(2, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane2.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "route-tab"), panel18);
        colorChooserRoute = new JColorChooser();
        panel18.add(colorChooserRoute, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label24 = new JLabel();
        this.$$$loadLabelText$$$(label24, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "route-color"));
        panel18.add(label24, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel18.add(spacer6, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane2.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "track-tab"), panel19);
        final Spacer spacer7 = new Spacer();
        panel19.add(spacer7, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        colorChooserTrack = new JColorChooser();
        panel19.add(colorChooserTrack, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label25 = new JLabel();
        this.$$$loadLabelText$$$(label25, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "track-color"));
        panel19.add(label25, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane2.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "waypoint-tab"), panel20);
        final Spacer spacer8 = new Spacer();
        panel20.add(spacer8, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        colorChooserWaypoint = new JColorChooser();
        panel20.add(colorChooserWaypoint, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label26 = new JLabel();
        this.$$$loadLabelText$$$(label26, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "waypoint-color"));
        panel20.add(label26, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(3, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routing-services-options-tab"), panel21);
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel21.add(panel22, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label27 = new JLabel();
        this.$$$loadLabelText$$$(label27, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routing-service"));
        panel22.add(label27, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxRoutingService = new JComboBox();
        panel22.add(comboBoxRoutingService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label28 = new JLabel();
        this.$$$loadLabelText$$$(label28, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routing-service-path"));
        panel22.add(label28, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldRoutingServicePath = new JTextField();
        panel22.add(textFieldRoutingServicePath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseRoutingServicePath = new JButton();
        buttonChooseRoutingServicePath.setHideActionText(true);
        buttonChooseRoutingServicePath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseRoutingServicePath.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "choose-routing-service-path"));
        panel22.add(buttonChooseRoutingServicePath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label29 = new JLabel();
        this.$$$loadLabelText$$$(label29, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routing-engine"));
        panel22.add(label29, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator7 = new JSeparator();
        panel22.add(separator7, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel22.add(panel23, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        panel21.add(spacer9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(7, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel21.add(panel24, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label30 = new JLabel();
        this.$$$loadLabelText$$$(label30, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routing-options"));
        panel24.add(label30, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator8 = new JSeparator();
        panel24.add(separator8, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label31 = new JLabel();
        this.$$$loadLabelText$$$(label31, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "travel-mode"));
        panel24.add(label31, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxTravelMode = new JComboBox();
        panel24.add(comboboxTravelMode, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label32 = new JLabel();
        this.$$$loadLabelText$$$(label32, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "avoid-highways"));
        panel24.add(label32, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidHighways = new JCheckBox();
        panel24.add(checkBoxAvoidHighways, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label33 = new JLabel();
        this.$$$loadLabelText$$$(label33, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "avoid-tolls"));
        panel24.add(label33, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidTolls = new JCheckBox();
        panel24.add(checkBoxAvoidTolls, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel24.add(panel25, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label34 = new JLabel();
        this.$$$loadLabelText$$$(label34, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "avoid-ferries"));
        panel24.add(label34, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidFerries = new JCheckBox();
        panel24.add(checkBoxAvoidFerries, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "paths-services-options-tab"), panel26);
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel26.add(panel27, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label35 = new JLabel();
        this.$$$loadLabelText$$$(label35, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "elevation-options"));
        panel27.add(label35, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator9 = new JSeparator();
        panel27.add(separator9, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label36 = new JLabel();
        this.$$$loadLabelText$$$(label36, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "elevation-service"));
        panel27.add(label36, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxElevationService = new JComboBox();
        panel27.add(comboBoxElevationService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label37 = new JLabel();
        this.$$$loadLabelText$$$(label37, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "elevation-service-path"));
        panel27.add(label37, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldElevationServicePath = new JTextField();
        panel27.add(textFieldElevationServicePath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseElevationServicePath = new JButton();
        buttonChooseElevationServicePath.setHideActionText(true);
        buttonChooseElevationServicePath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseElevationServicePath.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "choose-elevation-service-path"));
        panel27.add(buttonChooseElevationServicePath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel27.add(panel28, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel26.add(panel29, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new GridLayoutManager(4, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel26.add(panel30, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label38 = new JLabel();
        this.$$$loadLabelText$$$(label38, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "gpsbabel-options"));
        panel30.add(label38, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator10 = new JSeparator();
        panel30.add(separator10, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label39 = new JLabel();
        this.$$$loadLabelText$$$(label39, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "gpsbabel-path"));
        panel30.add(label39, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldBabelPath = new JTextField();
        panel30.add(textFieldBabelPath, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseBabelPath = new JButton();
        buttonChooseBabelPath.setHideActionText(true);
        buttonChooseBabelPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseBabelPath.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "choose-gpsbabel-path"));
        panel30.add(buttonChooseBabelPath, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel30.add(panel31, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new GridLayoutManager(4, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel26.add(panel32, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label40 = new JLabel();
        this.$$$loadLabelText$$$(label40, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "geocoding-options"));
        panel32.add(label40, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator11 = new JSeparator();
        panel32.add(separator11, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label41 = new JLabel();
        this.$$$loadLabelText$$$(label41, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "geocoding-service"));
        panel32.add(label41, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxGeocodingService = new JComboBox();
        panel32.add(comboBoxGeocodingService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel33 = new JPanel();
        panel33.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel32.add(panel33, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel34 = new JPanel();
        panel34.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel26.add(panel34, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textFieldGoogleApiKey = new JTextField();
        panel34.add(textFieldGoogleApiKey, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textFieldThunderforestApiKey = new JTextField();
        panel34.add(textFieldThunderforestApiKey, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        labelGoogleApiKey = new JLabel();
        this.$$$loadLabelText$$$(labelGoogleApiKey, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "google-maps-api-key"));
        panel34.add(labelGoogleApiKey, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelThunderforestApiKey = new JLabel();
        this.$$$loadLabelText$$$(labelThunderforestApiKey, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "thunderforest-api-key"));
        panel34.add(labelThunderforestApiKey, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelGeonamesUserName = new JLabel();
        this.$$$loadLabelText$$$(labelGeonamesUserName, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "geonames-user-name"));
        panel34.add(labelGeonamesUserName, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldGeonamesUserName = new JTextField();
        panel34.add(textFieldGeonamesUserName, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JSeparator separator12 = new JSeparator();
        panel34.add(separator12, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label42 = new JLabel();
        this.$$$loadLabelText$$$(label42, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "api-key-options"));
        panel34.add(label42, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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
