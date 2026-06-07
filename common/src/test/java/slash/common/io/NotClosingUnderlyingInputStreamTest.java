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

package slash.common.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Tests for {@link NotClosingUnderlyingInputStream}.
 *
 * @author Christian Pesch
 */

public class NotClosingUnderlyingInputStreamTest {

    @Test
    public void testReadByte() throws IOException {
        byte[] data = {42};
        InputStream delegate = new ByteArrayInputStream(data);
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        assertEquals(42, ncis.read());
    }

    @Test
    public void testReadReturnsMinusOneAtEnd() throws IOException {
        InputStream delegate = new ByteArrayInputStream(new byte[0]);
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        assertEquals(-1, ncis.read());
    }

    @Test
    public void testAvailable() throws IOException {
        byte[] data = {1, 2, 3};
        InputStream delegate = new ByteArrayInputStream(data);
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        assertEquals(3, ncis.available());
    }

    @Test
    public void testCloseDoesNotCloseDelegateSoItCanStillBeRead() throws IOException {
        byte[] data = {10, 20};
        ByteArrayInputStream delegate = new ByteArrayInputStream(data);
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        ncis.close(); // should NOT close delegate
        // ByteArrayInputStream.read() after close still works (it ignores close)
        assertEquals(10, delegate.read());
    }

    @Test
    public void testCloseUnderlyingInputStream() throws IOException {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream delegate = new ByteArrayInputStream(new byte[]{1}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        ncis.closeUnderlyingInputStream();
        assertTrue(closed.get());
    }

    @Test
    public void testMarkSupported() {
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[]{1, 2});
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        assertEquals(delegate.markSupported(), ncis.markSupported());
    }

    @Test
    public void testMarkAndReset() throws IOException {
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[]{5, 6});
        NotClosingUnderlyingInputStream ncis = new NotClosingUnderlyingInputStream(delegate);
        ncis.mark(10);
        assertEquals(5, ncis.read());
        ncis.reset();
        assertEquals(5, ncis.read());
    }
}

