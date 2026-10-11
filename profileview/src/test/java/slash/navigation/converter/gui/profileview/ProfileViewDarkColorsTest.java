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

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileViewDarkColorsTest {
    private static final Color FOREGROUND = new Color(187, 187, 187);
    private static final Color BACKGROUND = new Color(60, 63, 65);
    private static final Color GRID = new Color(81, 81, 81);

    @Test
    void appliesColorsToPlotAndBothAxes() {
        XYPlot plot = new XYPlot(new XYSeriesCollection(), new NumberAxis("Distance"), new NumberAxis("Elevation"), new XYAreaRenderer());

        ProfileView.applyDarkColors(plot, FOREGROUND, BACKGROUND, GRID);

        assertEquals(BACKGROUND, plot.getBackgroundPaint());
        assertEquals(GRID, plot.getDomainGridlinePaint());
        assertEquals(GRID, plot.getRangeGridlinePaint());
        for (ValueAxis axis : new ValueAxis[]{plot.getDomainAxis(), plot.getRangeAxis()}) {
            assertEquals(FOREGROUND, axis.getLabelPaint());
            assertEquals(FOREGROUND, axis.getTickLabelPaint());
            assertEquals(FOREGROUND, axis.getAxisLinePaint());
            assertEquals(FOREGROUND, axis.getTickMarkPaint());
        }
    }
}
