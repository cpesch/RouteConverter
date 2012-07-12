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

package slash.navigation.babel;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Filters illegal characters in GPX files from GPSBabel.
 *
 * @author Christian Pesch
 */
class IllegalCharacterFilterInputStream extends FilterInputStream {
    private static final Logger log = Logger.getLogger(IllegalCharacterFilterInputStream.class.getName());
    private static final int SPACE = 0x20;
    private static final int LF = 0xA;
    private static final int CR = 0xD;

    protected IllegalCharacterFilterInputStream(InputStream in) {
        super(in);
    }

    private boolean isIllegalCharacter(int character) {
        boolean illegal = character > 0 && character < SPACE && !(character == LF || character == CR);
        if (illegal)
            log.warning("Filtered illegal character " + character + " from GPSBabel GPX data");
        return illegal;
    }

    public int read() throws IOException {
        int character = super.read();
        if (isIllegalCharacter(character))
            return SPACE;
        return character;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        for (int i = off; i < off + len; i++) {
            byte character = b[i];
            if (isIllegalCharacter(character))
                b[i] = SPACE;
        }
        return result;
    }
}
