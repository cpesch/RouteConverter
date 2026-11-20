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

package slash.navigation.converter.gui.models;

import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.converter.gui.helpers.RowMoves;
import slash.navigation.converter.gui.panels.PositionsModelCallbackImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.navigation.gui.events.Range.increment;

/**
 * Moving positions by more than one row - as drag and drop in the positions table does -
 * shifts them; the positions they pass keep their order (#401).
 */
public class PositionsModelImplMoveTest {
    private PositionsModelImpl model(String order) {
        PositionsModelImpl model = new PositionsModelImpl(new PositionsModelCallbackImpl(null));
        BaseRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        @SuppressWarnings("unchecked")
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < order.length(); i++)
            positions.add(new BcrPosition(i, i, 0, String.valueOf(order.charAt(i))));
        model.setRoute(route);
        return model;
    }

    private String order(PositionsModelImpl model) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < model.getRowCount(); i++)
            buffer.append(model.getPosition(i).getDescription());
        return buffer.toString();
    }

    private void assertUp(int[] rows, int delta, String expected) {
        PositionsModelImpl model = model("ABCDE");
        model.up(rows.clone(), delta);
        assertEquals(expected, order(model));
        model.down(increment(rows, -delta), delta);
        assertEquals("ABCDE", order(model));
    }

    private void assertDown(int[] rows, int delta, String expected) {
        PositionsModelImpl model = model("ABCDE");
        model.down(rows.clone(), delta);
        assertEquals(expected, order(model));
        model.up(increment(rows, delta), delta);
        assertEquals("ABCDE", order(model));
    }

    @Test
    public void testUpOneRowMoreThanOneStep() {
        assertUp(new int[]{3}, 2, "ADBCE");
        assertUp(new int[]{4}, 3, "AEBCD");
    }

    @Test
    public void testUpBlockMoreThanOneStep() {
        assertUp(new int[]{3, 4}, 2, "ADEBC");
        assertUp(new int[]{3, 4}, 3, "DEABC");
    }

    @Test
    public void testUpKeepsGaps() {
        assertUp(new int[]{2, 4}, 2, "CAEBD");
    }

    @Test
    public void testUpOneStepAsTheButtonsDo() {
        assertUp(new int[]{2}, 1, "ACBDE");
        assertUp(new int[]{1, 3}, 1, "BADCE");
    }

    @Test
    public void testRowsStackedAtTheEdgeStay() {
        PositionsModelImpl model = model("ABCDE");
        model.up(new int[]{0, 1}, 1);
        assertEquals("ABCDE", order(model));
        model.up(new int[]{0, 1, 3}, 2);
        assertEquals("ABDCE", order(model));
        model = model("ABCDE");
        model.down(new int[]{3, 4}, 1);
        assertEquals("ABCDE", order(model));
        model.down(new int[]{1, 3, 4}, 2);
        assertEquals("ACBDE", order(model));
    }

    @Test
    public void testDownOneRowMoreThanOneStep() {
        assertDown(new int[]{1}, 2, "ACDBE");
    }

    @Test
    public void testDownBlockMoreThanOneStep() {
        assertDown(new int[]{1, 0}, 2, "CDABE");
        assertDown(new int[]{0, 1}, 2, "CDABE");
    }

    private void assertMarkedAt(PositionsModelImpl model, int[] rows, int[] movedRows) {
        for (int i = 0; i < rows.length; i++)
            assertEquals(String.valueOf("ABCDE".charAt(rows[i])), model.getPosition(movedRows[i]).getDescription());
    }

    @Test
    public void testModelPutsTheMarkedRowsWhereRowMovesSays() {
        int[][] rowsCases = {{2}, {0, 1}, {0, 2}, {1, 3}, {2, 4}, {0, 1, 3}, {3, 4}};
        for (int[] rows : rowsCases) {
            for (int delta = 1; delta <= 3; delta++) {
                PositionsModelImpl model = model("ABCDE");
                model.up(rows.clone(), delta);
                assertMarkedAt(model, rows, RowMoves.up(rows, delta));

                model = model("ABCDE");
                model.down(rows.clone(), delta);
                assertMarkedAt(model, rows, RowMoves.down(rows, delta, 5));
            }
        }
    }

    @Test
    public void testDownOneStepAsTheButtonsDo() {
        assertDown(new int[]{2}, 1, "ABDCE");
    }

    // marking positions in the middle and moving them to the top or bottom keeps them
    // selected at the rows RowMoves says, e.g. marking B,C in ABCDE and going Top puts
    // them at rows 0,1 still selected, Bottom puts them at rows 3,4 still selected (#403)
    @Test
    public void testModelPutsTheMarkedRowsWhereRowMovesTopSays() {
        int[][] rowsCases = {{2}, {0, 1}, {0, 2}, {1, 3}, {2, 4}, {0, 1, 3}, {3, 4}};
        for (int[] rows : rowsCases) {
            PositionsModelImpl model = model("ABCDE");
            model.top(rows.clone());
            assertMarkedAt(model, rows, RowMoves.top(rows));
        }
    }

    @Test
    public void testModelPutsTheMarkedRowsWhereRowMovesBottomSays() {
        int[][] rowsCases = {{2}, {0, 1}, {0, 2}, {1, 3}, {2, 4}, {0, 1, 3}, {3, 4}};
        for (int[] rows : rowsCases) {
            PositionsModelImpl model = model("ABCDE");
            model.bottom(rows.clone());
            assertMarkedAt(model, rows, RowMoves.bottom(rows, 5));
        }
    }
}
