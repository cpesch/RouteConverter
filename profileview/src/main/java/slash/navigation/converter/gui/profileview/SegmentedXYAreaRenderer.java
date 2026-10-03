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

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.function.IntPredicate;

import static org.jfree.chart.plot.PlotOrientation.VERTICAL;

/**
 * An {@link XYAreaRenderer} that leaves a gap where an item starts a new segment,
 * e.g. the first position of a GPX track segment: the area of each segment is closed
 * down to the axis on its own, instead of one area across the whole series.
 *
 * The item index of the series must stay the row index of the positions, so a gap is
 * drawn by the renderer and not by additional items in the series. Series without a
 * segment start are drawn by {@link XYAreaRenderer} itself.
 *
 * @author Christian Pesch
 */

public class SegmentedXYAreaRenderer extends XYAreaRenderer {
    private final transient IntPredicate startsNewSegment;
    private transient GeneralPath segment;
    private transient double previousTransX;
    private transient boolean drawSegments;

    public SegmentedXYAreaRenderer(IntPredicate startsNewSegment) {
        super(AREA);
        this.startsNewSegment = startsNewSegment;
    }

    private boolean hasSegmentStart(XYDataset dataset, int series) {
        for (int item = 1; item < dataset.getItemCount(series); item++)
            if (startsNewSegment.test(item))
                return true;
        return false;
    }

    private static double valueOrZero(double value) {
        return Double.isNaN(value) ? 0.0 : value;
    }

    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info,
                         XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {
        if (item == 0)
            drawSegments = plot.getOrientation() == VERTICAL && getPlotArea() && hasSegmentStart(dataset, series);
        if (!drawSegments) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
            return;
        }
        if (!getItemVisible(series, item))
            return;

        int itemCount = dataset.getItemCount(series);
        double x1 = dataset.getXValue(series, item);
        double y1 = valueOrZero(dataset.getYValue(series, item));
        double transX1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
        double transY1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());
        double transZero = rangeAxis.valueToJava2D(0.0, dataArea, plot.getRangeAxisEdge());

        if (item == 0 || startsNewSegment.test(item)) {
            // close the previous segment at its last item before the next one starts
            if (item > 0)
                fillSegment(g2, dataArea, series, item - 1, previousTransX, transZero);
            segment = new GeneralPath();
            segment.moveTo(transX1, transZero);
        }
        segment.lineTo(transX1, transY1);
        previousTransX = transX1;

        if (item == itemCount - 1)
            fillSegment(g2, dataArea, series, item, transX1, transZero);

        updateCrosshairValues(crosshairState, x1, y1, plot.indexOf(dataset), transX1, transY1, VERTICAL);
        addHotspot(state.getEntityCollection(), dataArea, plot, domainAxis, rangeAxis, dataset, series, item,
                transX1, transY1, transZero);
    }

    private void fillSegment(Graphics2D g2, Rectangle2D dataArea, int series, int item, double transX, double transZero) {
        segment.lineTo(transX, transZero);
        segment.closePath();

        Paint paint = getUseFillPaint() ? lookupSeriesFillPaint(series) : getItemPaint(series, item);
        if (paint instanceof GradientPaint gradientPaint)
            paint = getGradientTransformer().transform(gradientPaint, dataArea);
        g2.setPaint(paint);
        g2.fill(segment);

        if (isOutline()) {
            g2.setStroke(lookupSeriesOutlineStroke(series));
            g2.setPaint(lookupSeriesOutlinePaint(series));
            g2.draw(segment);
        }
    }

    // the same hotspot XYAreaRenderer creates, so a click on the area still selects the item
    // and thus the position with the same index
    private void addHotspot(EntityCollection entities, Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis,
                            ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                            double transX1, double transY1, double transZero) {
        if (entities == null)
            return;

        int itemCount = dataset.getItemCount(series);
        int previous = Math.max(item - 1, 0), next = Math.min(item + 1, itemCount - 1);
        double transX0 = domainAxis.valueToJava2D(dataset.getXValue(series, previous), dataArea, plot.getDomainAxisEdge());
        double transY0 = rangeAxis.valueToJava2D(valueOrZero(dataset.getYValue(series, previous)), dataArea, plot.getRangeAxisEdge());
        double transX2 = domainAxis.valueToJava2D(dataset.getXValue(series, next), dataArea, plot.getDomainAxisEdge());
        double transY2 = rangeAxis.valueToJava2D(valueOrZero(dataset.getYValue(series, next)), dataArea, plot.getRangeAxisEdge());

        GeneralPath hotspot = new GeneralPath();
        hotspot.moveTo((transX0 + transX1) / 2.0, transZero);
        hotspot.lineTo((transX0 + transX1) / 2.0, (transY0 + transY1) / 2.0);
        hotspot.lineTo(transX1, transY1);
        hotspot.lineTo((transX1 + transX2) / 2.0, (transY1 + transY2) / 2.0);
        hotspot.lineTo((transX1 + transX2) / 2.0, transZero);
        hotspot.closePath();

        Area dataAreaHotspot = new Area(hotspot);
        dataAreaHotspot.intersect(new Area(dataArea));
        if (!dataAreaHotspot.isEmpty())
            addEntity(entities, dataAreaHotspot, dataset, series, item, 0.0, 0.0);
    }
}
