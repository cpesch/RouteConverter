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

package slash.navigation.hgt;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A tile with elevation data.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class ElevationTile {
    private static final int NUMBER_OF_INTERVALS = 1200; // 1200 Intervals means 1201 positions per line and column
    private static final int INVALID_VALUE_LIMIT = -15000; // Won't interpolate below this elevation in Meters, guess is: -0x8000

    private RandomAccessFile elevationFile;

    public ElevationTile(RandomAccessFile elevationFile) {
        this.elevationFile = elevationFile;
    }

    /**
     * Calculate the elevation for the destination position according the
     * theorem on intersecting lines (Strahlensatz).
     *
     * @param dHeight12 the delta height/elevation of two sub tile positions
     * @param dLength12 the length of an sub tile interval (1 / NUMBER_OF_INTERVALS)
     * @param dDiff     the distance of the real point from the sub tile position
     * @return the delta elevation (relative to sub tile position)
     */
    private double calculateElevation(double dHeight12, double dLength12, double dDiff) {
        return (dHeight12 * dDiff) / dLength12;
    }

    public Double getElevationFor(Double longitude, Double latitude) throws IOException {
        if (elevationFile == null || longitude == null || latitude == null)
            return null;

        // cut off the decimal places
        int longitudeAsInt = longitude.intValue();
        int latitudeAsInt = latitude.intValue();

        if (longitude < 0) {                                        // If it's west longitude (negative value)
            longitudeAsInt = (longitudeAsInt - 1) * -1;             // Make a positive number (left edge)
            longitude = ((double) longitudeAsInt + longitude) + (double) longitudeAsInt; // Make positive double longitude (needed for later calculation)
        }

        if (latitude < 0) {                                        // If it's a south latitude (negative value)
            latitudeAsInt = (latitudeAsInt - 1) * -1;              // Make a positive number (bottom edge)
            latitude = ((double) latitudeAsInt + latitude) + (double) latitudeAsInt; // Make positive double latitude (needed for later calculation)
        }

        int nLonIndex = (int) ((longitude - (double) longitudeAsInt) * NUMBER_OF_INTERVALS); // Calculate the interval index for longitude
        int nLatIndex = (int) ((latitude - (double) latitudeAsInt) * NUMBER_OF_INTERVALS);   // Calculate the interval index for latitude

        if (nLonIndex >= NUMBER_OF_INTERVALS) {
            nLonIndex = NUMBER_OF_INTERVALS - 1;
        }

        if (nLatIndex >= NUMBER_OF_INTERVALS) {
            nLatIndex = NUMBER_OF_INTERVALS - 1;
        }

        double dOffLon = longitude - (double) longitudeAsInt;                    // The longitude value offset within a tile
        double dOffLat = latitude - (double) latitudeAsInt;                      // The latitude value offset within a tile

        double dLeftTop;                                            // The left top position of a sub tile
        double dLeftBottom;                                         // The left bottom position of a sub tile
        double dRightTop;                                           // The right top position of a sub tile
        double dRightBottom;                                        // The right bootm position of a sub tile
        int pos;                                                    // The index of the elevation into the hgt file

        pos = (((NUMBER_OF_INTERVALS - nLatIndex) - 1) * (NUMBER_OF_INTERVALS + 1)) + nLonIndex; // The index for the left top elevation
        elevationFile.seek(pos * 2);                                // We have 16-bit values for elevation, so multiply by 2
        dLeftTop = elevationFile.readShort();                       // Now read the left top elevation from hgt file

        pos = ((NUMBER_OF_INTERVALS - nLatIndex) * (NUMBER_OF_INTERVALS + 1)) + nLonIndex; // The index for the left bottom elevation
        elevationFile.seek(pos * 2);                                // We have 16-bit values for elevation, so multiply by 2
        dLeftBottom = elevationFile.readShort();                    // Now read the left bottom elevation from hgt file

        pos = (((NUMBER_OF_INTERVALS - nLatIndex) - 1) * (NUMBER_OF_INTERVALS + 1)) + nLonIndex + 1; // The index for the right top elevation
        elevationFile.seek(pos * 2);                                // We have 16-bit values for elevation, so multiply by 2
        dRightTop = elevationFile.readShort();                      // Now read the right top elevation from hgt file

        pos = ((NUMBER_OF_INTERVALS - nLatIndex) * (NUMBER_OF_INTERVALS + 1)) + nLonIndex + 1; // The index for the right bottom elevation
        elevationFile.seek(pos * 2);                                // We have 16-bit values for elevation, so multiply by 2
        dRightBottom = elevationFile.readShort();                   // Now read the right bottom top elevation from hgt file

        // if one of the read elevation values is not valid, we cannot interpolate
        if ((dLeftTop < INVALID_VALUE_LIMIT) || (dLeftBottom < INVALID_VALUE_LIMIT) ||
                (dRightTop < INVALID_VALUE_LIMIT) || (dRightBottom < INVALID_VALUE_LIMIT)) {
            return null;
        }

        // the delta between top lat value and requested latitude (offset within a sub tile)
        double dDeltaLon = dOffLon - (double) nLonIndex * (1.0 / (double) NUMBER_OF_INTERVALS);
        // the delta between left lon value and requested longitude (offset within a sub tile)
        double dDeltaLat = dOffLat - (double) nLatIndex * (1.0 / (double) NUMBER_OF_INTERVALS);

        // the interpolated elevation calculated from left top to left bottom
        double dLonHeightLeft = dLeftBottom - calculateElevation(dLeftBottom - dLeftTop, 1.0 / (double) NUMBER_OF_INTERVALS, dDeltaLat);
        // the interpolated elevation calculated from right top to right bottom
        double dLonHeightRight = dRightBottom - calculateElevation(dRightBottom - dRightTop, 1.0 / (double) NUMBER_OF_INTERVALS, dDeltaLat);

        // interpolate between the interpolated left elevation and interpolated right elevation
        double dElevation = dLonHeightLeft - calculateElevation(dLonHeightLeft - dLonHeightRight, 1.0 / (double) NUMBER_OF_INTERVALS, dDeltaLon);
        // round the interpolated elevation
        return dElevation + 0.5;
    }
}
