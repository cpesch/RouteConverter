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

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import slash.navigation.converter.gui.models.ProfileModeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import static org.jfree.ui.RectangleEdge.BOTTOM;
import static slash.navigation.gui.helpers.JMenuHelper.createMenu;

/**
 * [@link ChartPanel} that allows for a cheaper display of tooltips.
 *
 * @author Christian Pesch
 */

public class LazyToolTipChartPanel extends ChartPanel {
    static ProfileModeModel profileModeModel;
    private XYToolTipGenerator toolTipGenerator;

    public LazyToolTipChartPanel(JFreeChart chart,
                                 boolean properties, boolean save, boolean print, boolean zoom, boolean tooltips) {
        super(chart,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
                DEFAULT_MINIMUM_DRAW_WIDTH,
                DEFAULT_MINIMUM_DRAW_HEIGHT,
                DEFAULT_MAXIMUM_DRAW_WIDTH,
                DEFAULT_MAXIMUM_DRAW_HEIGHT,
                DEFAULT_BUFFER_USED,
                properties,
                save,
                print,
                zoom,
                tooltips
        );
    }

    protected JPopupMenu createPopupMenu(boolean properties, boolean copy, boolean save, boolean print, boolean zoom) {
        JPopupMenu popupMenu = super.createPopupMenu(properties, copy, save, print, zoom);
        // remove Zoom in/out plus separator from default menu
        popupMenu.remove(6);
        popupMenu.remove(5);
        popupMenu.remove(4);
        JMenu xAxisMenu = createMenu("show-profile-x-axis");
        new XAxisModeMenu(xAxisMenu, profileModeModel);
        popupMenu.add(xAxisMenu, 0);
        JMenu yAxisMenu = createMenu("show-profile-y-axis");
        new YAxisModeMenu(yAxisMenu, profileModeModel);
        popupMenu.add(yAxisMenu, 1);
        popupMenu.add(new JPopupMenu.Separator(), 2);
        return popupMenu;
    }

    public void setToolTipGenerator(XYToolTipGenerator toolTipGenerator) {
        this.toolTipGenerator = toolTipGenerator;
    }

    public String getToolTipText(MouseEvent e) {
        return getTooltipAtPoint(e.getPoint());
    }

    protected String getTooltipAtPoint(Point point) {
        XYPlot plot = (XYPlot) getChart().getPlot();
        PlotRenderingInfo info = getChartRenderingInfo().getPlotInfo();
        double x0 = point.getX();
        double x1 = x0 - 2 * getScaleX();
        double x2 = x0 + 4 * getScaleX();

        ValueAxis domainAxis = plot.getDomainAxis();
        Rectangle2D screenArea = scale(info.getDataArea());
        double tx1 = domainAxis.java2DToValue(x1, screenArea, BOTTOM);
        double tx2 = domainAxis.java2DToValue(x2, screenArea, BOTTOM);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {
            XYDataset dataset = plot.getDataset(datasetIndex);
            for (int seriesIndex = 0; seriesIndex < dataset.getSeriesCount(); seriesIndex++) {
                int itemCount = dataset.getItemCount(seriesIndex);
                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                    double xValue = dataset.getXValue(seriesIndex, itemIndex);
                    if (tx1 < xValue && xValue < tx2)
                        return toolTipGenerator.generateToolTip(dataset, seriesIndex, itemIndex);
                }
            }
        }
        return null;
    }
}