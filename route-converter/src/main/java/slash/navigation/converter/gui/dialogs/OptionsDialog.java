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
import slash.navigation.converter.gui.renderer.DegreeFormatListCellRenderer;
import slash.navigation.converter.gui.renderer.ElevationServiceListCellRenderer;
import slash.navigation.converter.gui.renderer.FixMapModeListCellRenderer;
import slash.navigation.converter.gui.renderer.GeocodingServiceListCellRenderer;
import slash.navigation.converter.gui.renderer.GoogleMapsServerListCellRenderer;
import slash.navigation.converter.gui.renderer.LocaleListCellRenderer;
import slash.navigation.converter.gui.renderer.MapViewListCellRenderer;
import slash.navigation.converter.gui.renderer.NumberPatternListCellRenderer;
import slash.navigation.converter.gui.renderer.NumberingStrategyListCellRenderer;
import slash.navigation.converter.gui.renderer.RoutingServiceListCellRenderer;
import slash.navigation.converter.gui.renderer.TimeZoneAndIdListCellRenderer;
import slash.navigation.converter.gui.renderer.TravelModeListCellRenderer;
import slash.navigation.converter.gui.renderer.UnitSystemListCellRenderer;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.googlemaps.GoogleMapsServer;
import slash.navigation.gui.Application;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.mapview.MapView;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Arrays.asList;
import static java.util.Locale.CHINA;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.ITALY;
import static java.util.Locale.JAPAN;
import static java.util.Locale.ROOT;
import static java.util.Locale.US;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.LocaleHelper.ARABIA;
import static slash.common.helpers.LocaleHelper.CROATIA;
import static slash.common.helpers.LocaleHelper.CZECH;
import static slash.common.helpers.LocaleHelper.DENMARK;
import static slash.common.helpers.LocaleHelper.NEDERLANDS;
import static slash.common.helpers.LocaleHelper.NORWAY_BOKMAL;
import static slash.common.helpers.LocaleHelper.POLAND;
import static slash.common.helpers.LocaleHelper.PORTUGAL;
import static slash.common.helpers.LocaleHelper.RUSSIA;
import static slash.common.helpers.LocaleHelper.SERBIA;
import static slash.common.helpers.LocaleHelper.SLOVAKIA;
import static slash.common.helpers.LocaleHelper.SPAIN;
import static slash.common.helpers.LocaleHelper.UKRAINE;
import static slash.common.io.Transfer.trim;
import static slash.navigation.common.DegreeFormat.Degrees;
import static slash.navigation.common.DegreeFormat.Degrees_Minutes;
import static slash.navigation.common.DegreeFormat.Degrees_Minutes_Seconds;
import static slash.navigation.common.NumberPattern.Description_Only;
import static slash.navigation.common.NumberPattern.Number_Directly_Followed_By_Description;
import static slash.navigation.common.NumberPattern.Number_Only;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.common.NumberingStrategy.Relative_Position_In_Current_Selection;
import static slash.navigation.common.UnitSystem.Metric;
import static slash.navigation.common.UnitSystem.Nautic;
import static slash.navigation.common.UnitSystem.Statute;
import static slash.navigation.converter.gui.RouteConverter.AUTOMATIC_UPDATE_CHECK_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.getBundle;
import static slash.navigation.converter.gui.RouteConverter.getPreferences;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForGeonamesUserName;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForGoogleApiKey;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForThunderforestApiKey;
import static slash.navigation.converter.gui.models.FixMapMode.Automatic;
import static slash.navigation.converter.gui.models.FixMapMode.No;
import static slash.navigation.converter.gui.models.FixMapMode.Yes;
import static slash.navigation.googlemaps.GoogleMapsServer.China;
import static slash.navigation.googlemaps.GoogleMapsServer.Ditu;
import static slash.navigation.googlemaps.GoogleMapsServer.International;
import static slash.navigation.googlemaps.GoogleMapsServer.Uzbekistan;
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
                ARABIA, CHINA, CZECH, DENMARK, GERMANY, US, SPAIN, FRANCE, CROATIA,
                ITALY, JAPAN, NEDERLANDS, NORWAY_BOKMAL, POLAND, PORTUGAL, RUSSIA, SLOVAKIA, SERBIA, UKRAINE,
                ROOT
        });
        localeModel.setSelectedItem(Application.getInstance().getLocale());
        comboBoxLocale.setModel(localeModel);
        comboBoxLocale.setRenderer(new LocaleListCellRenderer());
        comboBoxLocale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                Locale locale = (Locale) e.getItem();
                Application.getInstance().setLocale(locale);
            }
        });

        List<MapViewImplementation> mapViews = r.getAvailableMapViews();
        ComboBoxModel<MapViewImplementation> mapViewModel =
                new DefaultComboBoxModel<>(mapViews.toArray(new MapViewImplementation[0]));
        mapViewModel.setSelectedItem(r.getMapViewPreference());
        comboBoxMapService.setModel(mapViewModel);
        comboBoxMapService.setRenderer(new MapViewListCellRenderer());
        comboBoxMapService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                MapViewImplementation mapView = MapViewImplementation.class.cast(e.getItem());
                r.setMapView(mapView);
                handleMapServiceUpdate();
            }
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
        comboBoxGoogleMapsServer.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                GoogleMapsServer googleMapsServer = GoogleMapsServer.class.cast(e.getItem());
                r.getGoogleMapsServerModel().setGoogleMapsServer(googleMapsServer);
            }
        });

        ComboBoxModel<FixMapMode> fixMapModeModel = new DefaultComboBoxModel<>(new FixMapMode[]{
                Automatic, Yes, No
        });
        fixMapModeModel.setSelectedItem(r.getFixMapModeModel().getFixMapMode());
        comboBoxFixMapMode.setModel(fixMapModeModel);
        comboBoxFixMapMode.setRenderer(new FixMapModeListCellRenderer());
        comboBoxFixMapMode.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED) {
                    return;
                }
                FixMapMode fixMapMode = FixMapMode.class.cast(e.getItem());
                r.getFixMapModeModel().setFixMapMode(fixMapMode);
            }
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

        checkBoxShowCoordinates.setSelected(r.getShowCoordinates().getBoolean());
        checkBoxShowCoordinates.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getShowCoordinates().setBoolean(checkBoxShowCoordinates.isSelected());
            }
        });

        checkBoxShowWaypointDescription.setSelected(r.getShowWaypointDescription().getBoolean());
        checkBoxShowWaypointDescription.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getShowWaypointDescription().setBoolean(checkBoxShowWaypointDescription.isSelected());
            }
        });
        checkBoxShowWaypointDescription.setEnabled(r.isMapViewAvailable() && !r.getMapView().isDownload());

        DefaultComboBoxModel<RoutingService> routingServiceModel = new DefaultComboBoxModel<>();
        for (RoutingService service : r.getRoutingServiceFacade().getRoutingServices()) {
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
                RoutingService service = RoutingService.class.cast(e.getItem());
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
                r.getRoutingServiceFacade().setTravelMode(travelMode);
            }
        });
        checkBoxAvoidFerries.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().setAvoidFerries(checkBoxAvoidFerries.isSelected());
            }
        });
        checkBoxAvoidHighways.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().setAvoidHighways(checkBoxAvoidHighways.isSelected());
            }
        });
        checkBoxAvoidTolls.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.getRoutingServiceFacade().setAvoidTolls(checkBoxAvoidTolls.isSelected());
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
                NumberPattern numberPattern = NumberPattern.class.cast(e.getItem());
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
                NumberingStrategy numberingStrategy = NumberingStrategy.class.cast(e.getItem());
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
                ElevationService service = ElevationService.class.cast(e.getItem());
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
                GeocodingService service = GeocodingService.class.cast(e.getItem());
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
                UnitSystem unitSystem = UnitSystem.class.cast(e.getItem());
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
                DegreeFormat degreeFormat = DegreeFormat.class.cast(e.getItem());
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
                TimeZoneAndId timeZoneAndId = TimeZoneAndId.class.cast(e.getItem());
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

        colorChooserRoute.setColor(r.getRouteColorModel().getColor());
        reducePanels(colorChooserRoute);
        colorChooserRoute.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                r.getRouteColorModel().setColor(colorChooserRoute.getColor());
            }
        });
        colorChooserTrack.setColor(r.getTrackColorModel().getColor());
        reducePanels(colorChooserTrack);
        colorChooserTrack.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                r.getTrackColorModel().setColor(colorChooserTrack.getColor());
            }
        });

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
        comboBoxFixMapMode.setEnabled(!download);
        comboBoxGoogleMapsServer.setEnabled(!download);
    }

    private void handleRoutingServiceUpdate() {
        RoutingServiceFacade routingServiceFacade = RouteConverter.getInstance().getRoutingServiceFacade();
        RoutingService service = routingServiceFacade.getRoutingService();
        textFieldRoutingServicePath.setEnabled(service.isDownload());
        textFieldRoutingServicePath.setText(service.isDownload() ? service.getPath() : "");
        buttonChooseRoutingServicePath.setEnabled(service.isDownload());
        checkBoxAvoidFerries.setEnabled(service.isSupportAvoidFerries());
        checkBoxAvoidFerries.setSelected(routingServiceFacade.isAvoidFerries());
        checkBoxAvoidHighways.setEnabled(service.isSupportAvoidHighways());
        checkBoxAvoidHighways.setSelected(routingServiceFacade.isAvoidHighways());
        checkBoxAvoidTolls.setEnabled(service.isSupportAvoidTolls());
        checkBoxAvoidTolls.setSelected(routingServiceFacade.isAvoidTolls());
        updateTravelModes();
    }

    private void updateTravelModes() {
        RoutingServiceFacade serviceFacade = RouteConverter.getInstance().getRoutingServiceFacade();
        RoutingService service = serviceFacade.getRoutingService();
        MutableComboBoxModel<TravelMode> travelModeModel = new DefaultComboBoxModel<>();
        for (TravelMode travelMode : service.getAvailableTravelModes()) {
            travelModeModel.addElement(travelMode);
        }
        travelModeModel.setSelectedItem(serviceFacade.getTravelMode());
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
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel1.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        contentPane.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("general-options-tab"), panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("preferred-locale"));
        panel3.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxLocale = new JComboBox();
        panel3.add(comboBoxLocale, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("automatic-update-check"));
        panel3.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAutomaticUpdateCheck = new JCheckBox();
        checkBoxAutomaticUpdateCheck.setHorizontalAlignment(11);
        checkBoxAutomaticUpdateCheck.setHorizontalTextPosition(11);
        panel3.add(checkBoxAutomaticUpdateCheck, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("program-options"));
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
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("columbus-v1000"));
        panel5.add(label4, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("columbus-v1000-timezone-set-to"));
        panel5.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonV1000LocalTime = new JRadioButton();
        this.$$$loadButtonText$$$(radioButtonV1000LocalTime, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("columbus-v1000-local-time"));
        panel5.add(radioButtonV1000LocalTime, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonV1000UTC = new JRadioButton();
        this.$$$loadButtonText$$$(radioButtonV1000UTC, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("columbus-v1000-utc"));
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
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-options"));
        panel7.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-times-with-timezone"));
        panel8.add(label7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxTimeZone = new JComboBox();
        panel8.add(comboBoxTimeZone, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-measures-with-system-of-unit"));
        panel8.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxUnitSystem = new JComboBox();
        panel8.add(comboBoxUnitSystem, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-degrees-with-format"));
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
        this.$$$loadLabelText$$$(label10, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("numbering-options"));
        panel10.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel10.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("number-pattern"));
        panel10.add(label11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxNumberPattern = new JComboBox();
        panel10.add(comboboxNumberPattern, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("numbering-strategy"));
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
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-options-tab"), panel12);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(7, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel12.add(panel13, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("recenter-after-zooming"));
        panel13.add(label13, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxRecenterAfterZooming = new JCheckBox();
        panel13.add(checkBoxRecenterAfterZooming, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-coordinates"));
        panel13.add(label14, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowCoordinates = new JCheckBox();
        panel13.add(checkBoxShowCoordinates, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-waypoint-description"));
        panel13.add(label15, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowWaypointDescription = new JCheckBox();
        panel13.add(checkBoxShowWaypointDescription, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel13.add(panel14, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBoxShowAllPositionsAfterLoading = new JCheckBox();
        panel13.add(checkBoxShowAllPositionsAfterLoading, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-all-positions-after-loading"));
        panel13.add(label16, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-options"));
        panel13.add(label17, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel13.add(separator5, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel12.add(spacer5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(8, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel12.add(panel15, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        this.$$$loadLabelText$$$(label18, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-engine"));
        panel15.add(label18, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel15.add(separator6, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        this.$$$loadLabelText$$$(label19, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-service"));
        panel15.add(label19, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxMapService = new JComboBox();
        panel15.add(comboBoxMapService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label20 = new JLabel();
        this.$$$loadLabelText$$$(label20, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-path"));
        panel15.add(label20, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldMapsPath = new JTextField();
        panel15.add(textFieldMapsPath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseMapsPath = new JButton();
        buttonChooseMapsPath.setHideActionText(true);
        buttonChooseMapsPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseMapsPath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-map-path"));
        panel15.add(buttonChooseMapsPath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label21 = new JLabel();
        this.$$$loadLabelText$$$(label21, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("google-maps-server"));
        panel15.add(label21, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxGoogleMapsServer = new JComboBox();
        panel15.add(comboBoxGoogleMapsServer, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxFixMapMode = new JComboBox();
        panel15.add(comboBoxFixMapMode, new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label22 = new JLabel();
        this.$$$loadLabelText$$$(label22, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("fix-map-mode"));
        panel15.add(label22, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel15.add(panel16, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label23 = new JLabel();
        this.$$$loadLabelText$$$(label23, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("theme-path"));
        panel15.add(label23, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldThemesPath = new JTextField();
        panel15.add(textFieldThemesPath, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseThemesPath = new JButton();
        buttonChooseThemesPath.setHideActionText(true);
        buttonChooseThemesPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseThemesPath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-theme-path"));
        panel15.add(buttonChooseThemesPath, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("colors-options-tab"), panel17);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel17.add(panel18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel18.add(spacer6, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        colorChooserRoute = new JColorChooser();
        panel18.add(colorChooserRoute, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label24 = new JLabel();
        this.$$$loadLabelText$$$(label24, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("route-color"));
        panel18.add(label24, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        colorChooserTrack = new JColorChooser();
        panel18.add(colorChooserTrack, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label25 = new JLabel();
        this.$$$loadLabelText$$$(label25, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("track-color"));
        panel18.add(label25, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(3, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-services-options-tab"), panel19);
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel19.add(panel20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label26 = new JLabel();
        this.$$$loadLabelText$$$(label26, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-service"));
        panel20.add(label26, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxRoutingService = new JComboBox();
        panel20.add(comboBoxRoutingService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label27 = new JLabel();
        this.$$$loadLabelText$$$(label27, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-service-path"));
        panel20.add(label27, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldRoutingServicePath = new JTextField();
        panel20.add(textFieldRoutingServicePath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseRoutingServicePath = new JButton();
        buttonChooseRoutingServicePath.setHideActionText(true);
        buttonChooseRoutingServicePath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseRoutingServicePath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-routing-service-path"));
        panel20.add(buttonChooseRoutingServicePath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label28 = new JLabel();
        this.$$$loadLabelText$$$(label28, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-engine"));
        panel20.add(label28, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator7 = new JSeparator();
        panel20.add(separator7, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel20.add(panel21, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel19.add(spacer7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(7, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel19.add(panel22, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label29 = new JLabel();
        this.$$$loadLabelText$$$(label29, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-options"));
        panel22.add(label29, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator8 = new JSeparator();
        panel22.add(separator8, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label30 = new JLabel();
        this.$$$loadLabelText$$$(label30, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("travel-mode"));
        panel22.add(label30, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxTravelMode = new JComboBox();
        panel22.add(comboboxTravelMode, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label31 = new JLabel();
        this.$$$loadLabelText$$$(label31, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-highways"));
        panel22.add(label31, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidHighways = new JCheckBox();
        panel22.add(checkBoxAvoidHighways, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label32 = new JLabel();
        this.$$$loadLabelText$$$(label32, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-tolls"));
        panel22.add(label32, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidTolls = new JCheckBox();
        panel22.add(checkBoxAvoidTolls, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel22.add(panel23, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label33 = new JLabel();
        this.$$$loadLabelText$$$(label33, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-ferries"));
        panel22.add(label33, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidFerries = new JCheckBox();
        panel22.add(checkBoxAvoidFerries, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("paths-services-options-tab"), panel24);
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel24.add(panel25, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label34 = new JLabel();
        this.$$$loadLabelText$$$(label34, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("elevation-options"));
        panel25.add(label34, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator9 = new JSeparator();
        panel25.add(separator9, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label35 = new JLabel();
        this.$$$loadLabelText$$$(label35, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("elevation-service"));
        panel25.add(label35, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxElevationService = new JComboBox();
        panel25.add(comboBoxElevationService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label36 = new JLabel();
        this.$$$loadLabelText$$$(label36, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("elevation-service-path"));
        panel25.add(label36, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldElevationServicePath = new JTextField();
        panel25.add(textFieldElevationServicePath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseElevationServicePath = new JButton();
        buttonChooseElevationServicePath.setHideActionText(true);
        buttonChooseElevationServicePath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseElevationServicePath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-elevation-service-path"));
        panel25.add(buttonChooseElevationServicePath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel25.add(panel26, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel24.add(panel27, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(4, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel24.add(panel28, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label37 = new JLabel();
        this.$$$loadLabelText$$$(label37, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("gpsbabel-options"));
        panel28.add(label37, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator10 = new JSeparator();
        panel28.add(separator10, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label38 = new JLabel();
        this.$$$loadLabelText$$$(label38, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("gpsbabel-path"));
        panel28.add(label38, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldBabelPath = new JTextField();
        panel28.add(textFieldBabelPath, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseBabelPath = new JButton();
        buttonChooseBabelPath.setHideActionText(true);
        buttonChooseBabelPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/16/open-action.png")));
        buttonChooseBabelPath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-gpsbabel-path"));
        panel28.add(buttonChooseBabelPath, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel28.add(panel29, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new GridLayoutManager(4, 3, new Insets(3, 3, 3, 3), -1, -1));
        panel24.add(panel30, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label39 = new JLabel();
        this.$$$loadLabelText$$$(label39, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("geocoding-options"));
        panel30.add(label39, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator11 = new JSeparator();
        panel30.add(separator11, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label40 = new JLabel();
        this.$$$loadLabelText$$$(label40, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("geocoding-service"));
        panel30.add(label40, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxGeocodingService = new JComboBox();
        panel30.add(comboBoxGeocodingService, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel30.add(panel31, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        panel24.add(panel32, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textFieldGoogleApiKey = new JTextField();
        panel32.add(textFieldGoogleApiKey, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textFieldThunderforestApiKey = new JTextField();
        panel32.add(textFieldThunderforestApiKey, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        labelGoogleApiKey = new JLabel();
        this.$$$loadLabelText$$$(labelGoogleApiKey, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("google-maps-api-key"));
        panel32.add(labelGoogleApiKey, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelThunderforestApiKey = new JLabel();
        this.$$$loadLabelText$$$(labelThunderforestApiKey, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("thunderforest-api-key"));
        panel32.add(labelThunderforestApiKey, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelGeonamesUserName = new JLabel();
        this.$$$loadLabelText$$$(labelGeonamesUserName, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("geonames-user-name"));
        panel32.add(labelGeonamesUserName, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldGeonamesUserName = new JTextField();
        panel32.add(textFieldGeonamesUserName, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JSeparator separator12 = new JSeparator();
        panel32.add(separator12, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label41 = new JLabel();
        this.$$$loadLabelText$$$(label41, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("api-key-options"));
        panel32.add(label41, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
