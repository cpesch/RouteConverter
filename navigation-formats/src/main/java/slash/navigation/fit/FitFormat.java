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

import com.garmin.fit.CoursePointMesg;
import com.garmin.fit.CoursePointMesgListener;
import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.GpsMetadataMesg;
import com.garmin.fit.GpsMetadataMesgListener;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthMesgListener;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SegmentPointMesg;
import com.garmin.fit.SegmentPointMesgListener;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import static java.lang.String.format;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Reads and writes Garmin FIT (.fi) files.
 *
 * @author Christian Pesch
 */

public class FitFormat extends SimpleFormat<Wgs84Route> {
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
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws Exception {
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        Listener listener = new Listener();
        // TODO all listeners
        broadcaster.addListener((GpsMetadataMesgListener) listener);

        source.mark(source.available() + 1);
        if (!decode.checkFileIntegrity(source))
            throw new IllegalArgumentException(format("FIT integrity check failed: %s", context.getFile()));

        // +1 since Decoder is reading until the buffer is completely processed plus one to allow for #reset()
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

        // TODO process
    }

    private static class Listener implements /*MesgListener*/ CoursePointMesgListener, GpsMetadataMesgListener, RecordMesgListener, SegmentPointMesgListener {
        public void onMesg(Mesg mesg) {
            for (Field field : mesg.getFields())
                System.out.println("Field " + field.getName());
        }

        public void onMesg(CoursePointMesg mesg) {
            // position_lat
        }

        public void onMesg(GpsMetadataMesg mesg) {
            // timestamp
        }

        public void onMesg(RecordMesg mesg) {
            // speed
        }

        public void onMesg(SegmentPointMesg mesg) {
            System.out.println("Long " + mesg.getPositionLong() + " Lat " + mesg.getPositionLat() + "altitude " + mesg.getAltitude() + " index " + mesg.getMessageIndex());
        }
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}

