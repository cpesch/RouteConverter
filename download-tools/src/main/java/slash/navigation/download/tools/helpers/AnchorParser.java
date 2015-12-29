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
package slash.navigation.download.tools.helpers;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides anchor parsing from strings.
 *
 * @author Christian Pesch
 */

public class AnchorParser {
    public List<String> parseAnchors(String stringWithAnchors) throws IOException {
        final List<String> result = new ArrayList<>();

        Reader reader = new StringReader(stringWithAnchors);
        HTMLEditorKit.Parser parser = new ParserDelegator();
        parser.parse(reader, new HTMLEditorKit.ParserCallback() {
            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.A) {
                    Object link = a.getAttribute(HTML.Attribute.HREF);
                    if (link != null) {
                        result.add(String.valueOf(link));
                    }
                }
            }
        }, true);
        reader.close();
        return result;
    }
}
