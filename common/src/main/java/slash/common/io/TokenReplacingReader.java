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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * A reader that replaces tokens.
 *
 * Based on http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 *
 * @author Jakob Jenkov
 */

class TokenReplacingReader extends Reader {
    private PushbackReader pushbackReader;
    private TokenResolver tokenResolver;
    private StringBuilder tokenNameBuffer = new StringBuilder();
    private String tokenValue;
    private int tokenValueIndex;

    TokenReplacingReader(Reader source, TokenResolver resolver) {
        this.pushbackReader = new PushbackReader(source, 2);
        this.tokenResolver = resolver;
    }

    public int read(CharBuffer target) {
        throw new UnsupportedOperationException();
    }

    public int read() throws IOException {
        if (tokenValue != null) {
            if (tokenValueIndex < tokenValue.length()) {
                return tokenValue.charAt(tokenValueIndex++);
            }
            if (tokenValueIndex == tokenValue.length()) {
                tokenValue = null;
                tokenValueIndex = 0;
            }
        }

        int data = pushbackReader.read();
        if (data != '$')
            return data;

        data = pushbackReader.read();
        if (data != '{') {
            pushbackReader.unread(data);
            return '$';
        }
        tokenNameBuffer.delete(0, tokenNameBuffer.length());

        data = pushbackReader.read();
        while (data != '}') {
            tokenNameBuffer.append((char) data);
            data = pushbackReader.read();
        }

        tokenValue = tokenResolver.resolveToken(tokenNameBuffer.toString());
        if (tokenValue == null)
            tokenValue = "${" + tokenNameBuffer.toString() + "}";

        // token replaces to empty string
        else if (tokenValueIndex >= tokenValue.length())
            tokenValue = " ";

        return tokenValue.charAt(tokenValueIndex++);
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        int charsRead = -1;
        for (int i = 0; i < len; i++) {
            int nextChar = read();
            if (nextChar == -1) {
                break;
            }
            charsRead = i + 1;
            cbuf[off + i] = (char) nextChar;
        }
        return charsRead;
    }

    public void close() throws IOException {
        pushbackReader.close();
    }

    public long skip(long n) {
        throw new UnsupportedOperationException();
    }

    public boolean ready() throws IOException {
        return pushbackReader.ready();
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }
}
