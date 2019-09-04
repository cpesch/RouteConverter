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

package slash.navigation.nmn;

import com.fasterxml.jackson.databind.ObjectMapper;
import slash.navigation.nmn.bindingcruiser.Root;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class NavigonCruiserUtil {
    private static ObjectMapper newMapper() {
        return new ObjectMapper();
    }

    public static Root unmarshal(InputStream inputStream) throws IOException {
        return newMapper().readValue(inputStream, Root.class);
    }

    public static void marshal(Root root, OutputStream outputStream) throws IOException {
        try {
            newMapper().writeValue(outputStream, root);
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }
}
