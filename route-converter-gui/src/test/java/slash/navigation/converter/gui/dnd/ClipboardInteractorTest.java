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
package slash.navigation.converter.gui.dnd;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import javax.swing.SwingUtilities;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.dnd.ClipboardInteractor.isSupportedFlavor;
import static slash.navigation.converter.gui.dnd.PositionSelection.POSITION_FLAVOR;
import static slash.navigation.converter.gui.dnd.PositionSelection.STRING_FLAVOR;

/**
 * Tests for {@link ClipboardInteractor}.
 *
 * @author Christian Pesch
 */
public class ClipboardInteractorTest {

    private static List<NavigationPosition> positions(String... names) {
        List<NavigationPosition> result = new ArrayList<>();
        for (String name : names)
            result.add(new Wgs84Position(1.0, 2.0, null, null, null, name));
        return result;
    }

    private static void drainEventDispatchThread() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
        SwingUtilities.invokeAndWait(() -> {});
    }

    private static class StubClipboard extends Clipboard {
        private DataFlavor[] visibleFlavors = new DataFlavor[0];
        private IllegalStateException toThrow;

        StubClipboard() {
            super("stub");
        }

        void setVisibleFlavors(DataFlavor... flavors) {
            this.visibleFlavors = flavors;
        }

        void throwOnGetAvailableDataFlavors(IllegalStateException e) {
            this.toThrow = e;
        }

        @Override
        public DataFlavor[] getAvailableDataFlavors() {
            if (toThrow != null)
                throw toThrow;
            return visibleFlavors;
        }
    }

    private static class RecordingEnabler implements java.util.function.Consumer<Boolean> {
        private final List<Boolean> calls = new ArrayList<>();

        public void accept(Boolean enable) {
            calls.add(enable);
        }

        Boolean last() {
            return calls.isEmpty() ? null : calls.get(calls.size() - 1);
        }
    }

    @Test
    public void isSupportedFlavorRecognizesPositionAndStringFlavors() {
        assertTrue(isSupportedFlavor(new DataFlavor[]{POSITION_FLAVOR}));
        assertTrue(isSupportedFlavor(new DataFlavor[]{DataFlavor.stringFlavor}));
        assertFalse(isSupportedFlavor(new DataFlavor[]{}));
        assertFalse(isSupportedFlavor(new DataFlavor[]{DataFlavor.imageFlavor}));
    }

    @Test
    public void puttingSelectionIntoClipboardEnablesPaste() throws Exception {
        StubClipboard clipboard = new StubClipboard();
        RecordingEnabler enabler = new RecordingEnabler();
        ClipboardInteractor sut = new ClipboardInteractor(clipboard, enabler);

        sut.putIntoClipboard(new PositionSelection(positions("A", "B")));
        drainEventDispatchThread();

        assertEquals(Boolean.TRUE, enabler.last());
    }

    @Test
    public void lostOwnershipDoesNotDisablePasteWhenAnotherInstanceCopiesTheSameFormats() throws Exception {
        StubClipboard clipboard = new StubClipboard();
        RecordingEnabler enabler = new RecordingEnabler();
        ClipboardInteractor sut = new ClipboardInteractor(clipboard, enabler);

        sut.putIntoClipboard(new PositionSelection(positions("A", "B")));
        drainEventDispatchThread();
        assertEquals(Boolean.TRUE, enabler.last());

        // simulate the new owner being mid-write on Windows: the format list is briefly empty
        clipboard.setVisibleFlavors();

        ClipboardOwner otherOwner = (c, contents) -> { };
        Transferable otherSelection = new PositionSelection(positions("C"));
        clipboard.setContents(otherSelection, otherOwner);
        drainEventDispatchThread();

        assertFalse(enabler.calls.contains(Boolean.FALSE));
    }

    @Test
    public void refreshReflectsCurrentClipboardContents() {
        StubClipboard clipboard = new StubClipboard();
        RecordingEnabler enabler = new RecordingEnabler();
        ClipboardInteractor sut = new ClipboardInteractor(clipboard, enabler);

        clipboard.setVisibleFlavors();
        sut.refresh();
        assertEquals(Boolean.FALSE, enabler.last());

        clipboard.setVisibleFlavors(DataFlavor.stringFlavor);
        sut.refresh();
        assertEquals(Boolean.TRUE, enabler.last());
    }

    @Test
    public void refreshReturnsFalseWhenClipboardThrows() {
        StubClipboard clipboard = new StubClipboard();
        clipboard.throwOnGetAvailableDataFlavors(new IllegalStateException("clipboard busy"));
        RecordingEnabler enabler = new RecordingEnabler();
        ClipboardInteractor sut = new ClipboardInteractor(clipboard, enabler);

        sut.refresh();

        assertEquals(Boolean.FALSE, enabler.last());
    }
}
