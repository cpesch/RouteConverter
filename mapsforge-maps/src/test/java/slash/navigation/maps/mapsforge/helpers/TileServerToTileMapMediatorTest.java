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
package slash.navigation.maps.mapsforge.helpers;

import org.junit.Test;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.impl.TileMap;
import slash.navigation.maps.tileserver.TileServer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class TileServerToTileMapMediatorTest {

    @Test
    public void testEvents() throws InterruptedException {
        ItemTableModel<TileServer> sourceModel = new ItemTableModel<>(1);
        ItemTableModel<TileMap> destinationModel = new ItemTableModel<>(2);
        TileServerToTileMapMediator mediator = new TileServerToTileMapMediator(sourceModel, destinationModel);

        assertEquals(0, destinationModel.getRowCount());

        final boolean[] found = new boolean[1];
        found[0] = false;

        TableModelListener l = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                synchronized (found) {
                    found[0] = true;
                    found.notifyAll();
                }
            }
        };
        destinationModel.addTableModelListener(l);

        sourceModel.addOrUpdateItem(new TileServer("a", "b", "c", singletonList("d"),false, 0, 0, null));

        while (true) {
            synchronized (found) {
                if (found[0])
                    break;
                found.wait(100);
            }
        }

        destinationModel.removeTableModelListener(l);

        assertEquals(1, destinationModel.getRowCount());

        mediator.dispose();
    }

}
