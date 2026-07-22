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

package slash.navigation.gui.actions;

import org.junit.Test;
import slash.navigation.gui.SimpleDialog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingletonDialogActionTest {

    private static class CountingDialogAction extends SingletonDialogAction {
        private final SimpleDialog dialog;
        int createDialogCount;

        private CountingDialogAction(SimpleDialog dialog) {
            this.dialog = dialog;
        }

        protected SimpleDialog createDialog() {
            createDialogCount++;
            return dialog;
        }
    }

    @Test
    public void testReusesShowingDialogAndRebuildsAfterItWasClosed() {
        SimpleDialog dialog = mock(SimpleDialog.class);
        // dialog reports it is showing after the first open, then closed before the third
        when(dialog.isShowing()).thenReturn(true, false);
        CountingDialogAction action = new CountingDialogAction(dialog);

        // first open: no cached dialog yet, so it is built and shown
        action.run();
        assertEquals(1, action.createDialogCount);
        verify(dialog, times(1)).showWithPreferences();

        // second open while still showing: bring the existing dialog to front, do not build a second one
        action.run();
        assertEquals(1, action.createDialogCount);
        verify(dialog, times(1)).showWithPreferences();

        // third open after it was closed (isShowing() == false): build a fresh dialog again
        action.run();
        assertEquals(2, action.createDialogCount);
        verify(dialog, times(2)).showWithPreferences();
    }
}
