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

package slash.navigation.fit;

import com.garmin.fit.*;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;

import static com.garmin.fit.Fit.ProtocolVersion.V2_0;
import static java.lang.String.format;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Reads and writes Garmin FIT (.fit) files.
 *
 * @author Christian Pesch
 */

public class FitFormat extends SimpleFormat<Wgs84Route> {
    static final Logger log = Logger.getLogger(FitFormat.class.getName());

    public String getName() {
        return "Garmin FIT (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".fit";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsReading() {
        return true;
    }

    public boolean isSupportsWriting() {
        return true;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener(new MesgLogger());
        MesgParser parser = new MesgParser();
        parser.setName(context.getFile().getName());
        broadcaster.addListener((CourseMesgListener) parser);
        broadcaster.addListener((CoursePointMesgListener) parser);
        broadcaster.addListener((GpsMetadataMesgListener) parser);
        broadcaster.addListener((RecordMesgListener) parser);
        broadcaster.addListener((SegmentPointMesgListener) parser);

        // +1 since Decoder is reading until the buffer is completely processed plus one to allow for #reset()
        source.mark(source.available() + 1);
        if (!decode.checkFileIntegrity(source))
            throw new IllegalArgumentException(format("FIT integrity check failed: %s", context.getFile()));

        source.reset();
        try {
            decode.read(source, broadcaster);
        } catch (FitRuntimeException e) {
            // If a file with 0 data size in it's header has been encountered, attempt to keep processing the file
            if (decode.getInvalidFileDataSize()) {
                decode.nextFile();
                decode.read(source, broadcaster);
            } else {
                throw new IllegalArgumentException(format("Cannot decode fit file %s: %s", context.getFile(), getLocalizedMessage(e)));
            }
        }

        List<Wgs84Position> positions = parser.getPositions();
        if (positions.size() > 0)
            context.appendRoute(new Wgs84Route(this, parser.getCharacteristics(), parser.getName(), positions));
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        BufferEncoder encoder = new BufferEncoder(V2_0);
        List<Mesg> mesgs = new MesgCreator().createMesgs(route, getCreator(), startIndex, endIndex);
        encoder.write(mesgs);
        byte[] bytes = encoder.close();
        target.write(bytes);
    }
}

