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

package slash.navigation.converter.gui.profileview;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.models.*;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.text.MessageFormat.format;
import static java.text.NumberFormat.getIntegerInstance;
import static org.jfree.chart.axis.NumberAxis.createIntegerTickUnits;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static org.jfree.ui.Layer.FOREGROUND;
import static slash.navigation.converter.gui.profileview.XAxisMode.Distance;
import static slash.navigation.converter.gui.profileview.YAxisMode.Elevation;

/**
 * Displays the elevations of a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class ProfileView implements PositionsSelectionModel {
    protected static final Preferences preferences = Preferences.userNodeForPackage(ProfileView.class);
    private static final String X_GRID_PREFERENCE = "xGrid";
    private static final String Y_GRID_PREFERENCE = "yGrid";

    private LazyToolTipChartPanel chartPanel;
    private XYPlot plot;
    private PositionsModel positionsModel;
    private ProfileModel profileModel;

    public void initialize(PositionsModel positionsModel, final PositionsSelectionModel positionsSelectionModel,
                           final UnitSystemModel unitSystemModel, final ProfileModeModel profileModeModel) {
        this.positionsModel = positionsModel;
        PatchedXYSeries series = new PatchedXYSeries("Profile");
        this.profileModel = new ProfileModel(positionsModel, series, unitSystemModel.getUnitSystem(),
                profileModeModel.getXAxisMode(), profileModeModel.getYAxisMode());
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        unitSystemModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setUnitSystem(unitSystemModel.getUnitSystem());
            }
        });
        profileModeModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setProfileMode(profileModeModel.getXAxisMode(), profileModeModel.getYAxisMode());
            }
        });

        JFreeChart chart = createChart(dataset);
        plot = createPlot(chart);

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        for (XAxisMode mode : XAxisMode.values())
            actionManager.register("show-" + mode.name().toLowerCase(), new ToggleXAxisProfileModeAction(profileModeModel, mode));
        for (YAxisMode mode : YAxisMode.values())
            actionManager.register("show-" + mode.name().toLowerCase(), new ToggleYAxisProfileModeAction(profileModeModel, mode));
        // since JFreeChart is not very nice to extensions - constructors calling protected methods... ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD
        LazyToolTipChartPanel.profileModeModel = profileModeModel;
        chartPanel = new LazyToolTipChartPanel(chart, false, true, true, true, true);
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            public void chartMouseClicked(ChartMouseEvent e) {
                ChartEntity entity = e.getEntity();
                if (!(entity instanceof XYItemEntity))
                    return;
                int row = ((XYItemEntity) entity).getItem();
                positionsSelectionModel.setSelectedPositions(new int[]{row}, true);
            }

            public void chartMouseMoved(ChartMouseEvent e) {
            }
        });
        chartPanel.setMouseWheelEnabled(true);

        updateAxis();
    }

    private static ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    private JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYAreaChart(null, null, null, dataset, VERTICAL, false, true, false);
        chart.setBackgroundPaint(new JPanel().getBackground());
        return chart;
    }

    private XYPlot createPlot(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.setForegroundAlpha(0.65F);
        plot.setDomainGridlinesVisible(preferences.getBoolean(X_GRID_PREFERENCE, true));
        plot.setRangeGridlinesVisible(preferences.getBoolean(Y_GRID_PREFERENCE, true));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setStandardTickUnits(createIntegerTickUnits());
        Font font = new JLabel().getFont();
        rangeAxis.setLabelFont(font);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setStandardTickUnits(createIntegerTickUnits());
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setLabelFont(font);

        plot.getRenderer().setBaseToolTipGenerator(null);
        return plot;
    }

    public Component getComponent() {
        return chartPanel;
    }

    private void setUnitSystem(UnitSystem unitSystem) {
        profileModel.setUnitSystem(unitSystem);
        updateAxis();
    }

    private void setProfileMode(XAxisMode xAxisMode, YAxisMode yAxisMode) {
        profileModel.setProfileMode(xAxisMode, yAxisMode);
        updateAxis();
    }

    private void updateAxis() {
        UnitSystem unitSystem = profileModel.getUnitSystem();

        YAxisMode yAxisMode = profileModel.getYAxisMode();
        String xAxisUnit = yAxisMode.equals(Elevation) ? unitSystem.getElevationName() : unitSystem.getSpeedName();
        String xAxisKey = yAxisMode.equals(Elevation) ? "elevation-axis" : "speed-axis";
        plot.getRangeAxis().setLabel(format(getBundle().getString(xAxisKey), xAxisUnit));

        XAxisMode xAxisMode = profileModel.getXAxisMode();
        String yAxisUnit = xAxisMode.equals(Distance) ? unitSystem.getDistanceName() : "s";
        String yAxisKey = xAxisMode.equals(Distance) ? "distance-axis" : "time-axis";
        plot.getDomainAxis().setLabel(format(getBundle().getString(yAxisKey), yAxisUnit));

        chartPanel.setToolTipGenerator(new StandardXYToolTipGenerator(
                "{2} " + xAxisUnit + " @ {1} " + yAxisUnit, getIntegerInstance(), getIntegerInstance()) {
            public String generateLabelString(XYDataset dataset, int series, int item) {
                return super.generateLabelString(dataset, series, item).replaceAll("null", "?");
            }
        });
    }

    public void setSelectedPositions(int[] selectPositions, boolean replaceSelection) {
        if (replaceSelection)
            plot.clearDomainMarkers();

        if (profileModel.getXAxisMode().equals(Distance)) {
            double[] distances = positionsModel.getRoute().getDistancesFromStart(selectPositions);
            for (double distance : distances) {
                plot.addDomainMarker(0, new ValueMarker(profileModel.formatDistance(distance)), FOREGROUND, false);
            }
        } else {
            long[] times = positionsModel.getRoute().getTimesFromStart(selectPositions);
            for (long time : times) {
                plot.addDomainMarker(0, new ValueMarker(profileModel.formatTime(time)), FOREGROUND, false);
            }
        }

        // make sure the protected fireChangeEvent() is called without any side effects
        plot.setWeight(plot.getWeight());
    }

    public void print() {
        chartPanel.createChartPrintJob();
    }

    private static class ToggleXAxisProfileModeAction extends FrameAction {
        private final ProfileModeModel profileModeModel;
        private final XAxisMode xAxisMode;

        public ToggleXAxisProfileModeAction(ProfileModeModel profileModeModel, XAxisMode xAxisMode) {
            this.profileModeModel = profileModeModel;
            this.xAxisMode = xAxisMode;
        }

        public void run() {
            profileModeModel.setXAxisMode(xAxisMode);
        }
    }

    private static class ToggleYAxisProfileModeAction extends FrameAction {
        private final ProfileModeModel profileModeModel;
        private final YAxisMode yAxisMode;

        public ToggleYAxisProfileModeAction(ProfileModeModel profileModeModel, YAxisMode yAxisMode) {
            this.profileModeModel = profileModeModel;
            this.yAxisMode = yAxisMode;
        }

        public void run() {
            profileModeModel.setYAxisMode(yAxisMode);
        }
    }
}
