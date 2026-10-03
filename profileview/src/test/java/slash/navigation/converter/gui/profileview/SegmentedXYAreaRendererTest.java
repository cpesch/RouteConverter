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
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;

import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static org.junit.jupiter.api.Assertions.*;

class SegmentedXYAreaRendererTest {
    private static final Color AREA = Color.RED;
    private static final Color BACKGROUND = Color.WHITE;
    private static final int WIDTH = 600, HEIGHT = 300;

    // two segments of four positions, recorded from 0..6 and from 30..36, nothing in between
    private static final double[] X = {0, 2, 4, 6, 30, 32, 34, 36};
    private static final double[] Y = {10, 11, 12, 13, 15, 16, 17, 18};

    private record Rendered(BufferedImage image, ChartRenderingInfo info, XYPlot plot) {
        int argbAt(double x, double y) {
            Rectangle2D dataArea = info.getPlotInfo().getDataArea();
            int px = (int) plot.getDomainAxis().valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
            int py = (int) plot.getRangeAxis().valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
            return image.getRGB(px, py);
        }
    }

    private static Rendered render(IntPredicate startsNewSegment) {
        XYSeries series = new XYSeries("profile");
        for (int i = 0; i < X.length; i++)
            series.add(X[i], Y[i]);
        JFreeChart chart = ChartFactory.createXYAreaChart(null, null, null, new XYSeriesCollection(series),
                VERTICAL, false, false, false);
        XYPlot plot = chart.getXYPlot();
        SegmentedXYAreaRenderer renderer = new SegmentedXYAreaRenderer(startsNewSegment);
        renderer.setSeriesPaint(0, AREA);
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(BACKGROUND);
        // createXYAreaChart() blends the area at 50%, use the plain color to compare pixels
        plot.setForegroundAlpha(1.0f);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getDomainAxis().setLowerMargin(0.0);
        plot.getDomainAxis().setUpperMargin(0.0);

        ChartRenderingInfo info = new ChartRenderingInfo();
        BufferedImage image = chart.createBufferedImage(WIDTH, HEIGHT, info);
        return new Rendered(image, info, plot);
    }

    @Test
    void leavesAGapWhereASegmentStarts() {
        Rendered rendered = render(item -> item == 4);

        // in the middle of the gap, just above the axis: nothing recorded, nothing filled
        assertEquals(BACKGROUND.getRGB(), rendered.argbAt(18, 1));
        // inside both segments the area is filled
        assertEquals(AREA.getRGB(), rendered.argbAt(3, 1));
        assertEquals(AREA.getRGB(), rendered.argbAt(33, 1));
    }

    @Test
    void fillsAcrossTheSeriesWithoutSegmentStarts() {
        Rendered rendered = render(item -> false);

        assertEquals(AREA.getRGB(), rendered.argbAt(18, 1));
    }

    // the item of an entity is the position row a click selects
    @Test
    void keepsOneEntityPerItem() {
        Rendered rendered = render(item -> item == 4);

        Set<Integer> items = new TreeSet<>();
        for (Object entity : rendered.info().getEntityCollection().getEntities())
            if (entity instanceof XYItemEntity itemEntity)
                items.add(itemEntity.getItem());
        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7), items);
    }
}
