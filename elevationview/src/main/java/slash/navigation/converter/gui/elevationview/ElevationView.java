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

package slash.navigation.converter.gui.elevationview;

import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.gui.Application;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * Displays the elevations of a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class ElevationView {
    private ChartPanel chartPanel;
    private XYPlot plot;
    private PositionsModel positionsModel;

    public ElevationView(PositionsModel positionsModel, final PositionsSelectionModel positionsSelectionModel) {
        this.positionsModel = positionsModel;
        XYSeriesCollection dataset = createDataset(positionsModel);
        JFreeChart chart = createChart(dataset);
        plot = createPlot(chart);
        chartPanel = new ChartPanel(chart, false, true, true, true, true);
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            public void chartMouseClicked(ChartMouseEvent e) {
                ChartEntity entity = e.getEntity();
                if (!(entity instanceof XYItemEntity))
                    return;
                int index = ((XYItemEntity) entity).getItem();
                positionsSelectionModel.setSelectedPositions(new int[]{index});
            }

            public void chartMouseMoved(ChartMouseEvent e) {
            }
        });
    }

    private XYSeriesCollection createDataset(PositionsModel model) {
        PatchedXYSeries series = new PatchedXYSeries("Elevation");
        new ElevationModel(model, series);
        return new XYSeriesCollection(series);
    }

    private static ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    private JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYAreaChart(
                null,
                getBundle().getString("distance-axis"),
                getBundle().getString("elevation-axis"),
                dataset,
                PlotOrientation.VERTICAL, false, true, false
        );
        chart.setBackgroundPaint(new JPanel().getBackground());
        return chart;
    }

    private XYPlot createPlot(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.setForegroundAlpha(0.65F);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        Font font = new JLabel().getFont();
        rangeAxis.setLabelFont(font);

        NumberAxis valueAxis = (NumberAxis) plot.getDomainAxis();
        valueAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        valueAxis.setLowerMargin(0.0);
        valueAxis.setUpperMargin(0.0);
        valueAxis.setLabelFont(font);

        XYItemRenderer renderer = plot.getRenderer();
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{2}m @ {1} Km",
                NumberFormat.getIntegerInstance(), NumberFormat.getIntegerInstance()));
        return plot;
    }

    public Component getComponent() {
        return chartPanel;
    }

    public void setSelectedPositions(int[] selectPositions) {
        plot.clearDomainMarkers();

        double[] distances = positionsModel.getRoute().getDistancesFromStart(selectPositions);
        for (double distance : distances) {
            plot.addDomainMarker(0, new ValueMarker(distance / 1000.0), Layer.FOREGROUND, false);
        }
        // make sure the protected fireChangeEvent() is called without any side effects
        plot.setWeight(plot.getWeight());
    }

    public void print() {
        chartPanel.createChartPrintJob();
    }
}
