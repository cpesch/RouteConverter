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

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * As a pipe reads from input and writes to output.
 *
 * @author Christian Pesch
 */

public class InputOutput {
    public static void copy(InputStream input, OutputStream output) throws IOException {
        try {
            IOUtils.copy(input, output);
        } finally {
            try {
                closeQuietly(input);
            } finally {
                closeQuietly(output);
            }
        }
    }

    public static void copy(Reader reader, Writer writer) throws IOException {
        try {
            IOUtils.copy(reader, writer);
        } finally {
            try {
                closeQuietly(reader);
            } finally {
                closeQuietly(writer);
            }
        }
    }

    public static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }
}