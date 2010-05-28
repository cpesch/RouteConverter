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
    private RandomAccessFile elevationFile;

    public ElevationTile(RandomAccessFile elevationFile) {
        this.elevationFile = elevationFile;
    }

    /**
     * Calculate the elevation for the destination position) according the
     * theorem on intersecting lines (Strahlensatz).
     *
     * @param dHeight12 the delta height/elevation of two sub tile positions
     * @param dLength12 the length of an sub tile interval (1 / 1200)
     * @param dDiff     the distance of the real point from the sub tile position
     * @return the delta elevation (relative to sub tile position)
     */
    private double calculateElevation(double dHeight12, double dLength12, double dDiff) {
        return (dHeight12 * dDiff) / dLength12;
    }

    public Integer getElevationFor(Double longitude, Double latitude) throws IOException {
        if (elevationFile == null)
            return null;

        double dElevation;
        double dLon = longitude;
        double dLat = latitude;
        int nLon = (int) dLon;                    // Cut off the decimal places
        int nLat = (int) dLat;                    // Cut off the decimal places
        int nAS = 1200;                            // 1200 Intervals (means 1201 positions per line and column)

        if (dLon < 0) {                                        // If it's west longitude (negative value)
            nLon = (nLon - 1) * -1;                            // Make a positive number (left edge)
            dLon = ((double) nLon + dLon) + (double) nLon;     // Make positive double longitude (needed for later calculation)
        }

        if (dLat < 0) {                                        // If it's a south latitude (negative value)
            nLat = (nLat - 1) * -1;                            // Make a positive number (bottom edge)
            dLat = ((double) nLat + dLat) + (double) nLat;    // Make positive double latitude (needed for later calculation)
        }

        int nLonIndex = (int) ((dLon - (double) nLon) * 1200.0); // Calculate the interval index for longitude
        int nLatIndex = (int) ((dLat - (double) nLat) * 1200.0); // Calculate the interval index for latitude

        if (nLonIndex >= 1200) {
            nLonIndex = 1200 - 1;
        }

        if (nLatIndex >= 1200) {
            nLatIndex = 1200 - 1;
        }

        double dOffLon = dLon - (double) nLon;                      // The lon value offset within a tile
        double dOffLat = dLat - (double) nLat;                      // The lat value offset within a tile

        double dLeftTop;                                            // The left top position of a sub tile
        double dLeftBottom;                                            // The left bottom position of a sub tile
        double dRightTop;                                            // The right top position of a sub tile
        double dRightBottom;                                        // The right bootm position of a sub tile
        int pos;                                                    // The index of the elevation into the hgt file

        pos = (((nAS - nLatIndex) - 1) * (nAS + 1)) + nLonIndex;    // The index for the left top elevation
        elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dLeftTop = elevationFile.readShort();                // Now read the left top elevation from hgt file

        pos = ((nAS - nLatIndex) * (nAS + 1)) + nLonIndex;            // The index for the left bottom elevation
        elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dLeftBottom = elevationFile.readShort();                // Now read the left bottom elevation from hgt file

        pos = (((nAS - nLatIndex) - 1) * (nAS + 1)) + nLonIndex + 1;// The index for the right top elevation
        elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dRightTop = elevationFile.readShort();                // Now read the right top elevation from hgt file

        pos = ((nAS - nLatIndex) * (nAS + 1)) + nLonIndex + 1;         // The index for the right bottom elevation
        elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dRightBottom = elevationFile.readShort();                // Now read the right bottom top elevation from hgt file

        if ((dLeftTop < 0) ||                                        // If one of the elevation values
                (dLeftBottom < 0) ||                                    // we read from
                (dRightTop < 0) ||                                        // the hgt file is
                (dRightBottom < 0)) {                                    // not valid
            return null;                                            // we can't interpolate
        }

        double dDeltaLat;         // The delta between top lat value and wanted lat (delta within a sub tile)
        double dDeltaLon;       // The delta between left lon value and wanted lon (delta within a sub tile)

        dDeltaLon = dOffLon - (double) nLonIndex * (1.0 / (double) nAS);   // The delta (offset) from left point to wanted point
        dDeltaLat = dOffLat - (double) nLatIndex * (1.0 / (double) nAS);   // The delta (offset) from bottom point to wanted point

        double dLonHeightLeft;    // The interpolated elevation calculated from left top to left bottom
        double dLonHeightRight;    // The interpolated elevation calculated from right top to right bottom

        dLonHeightLeft = dLeftBottom - calculateElevation(dLeftBottom - dLeftTop, 1.0 / (double) nAS, dDeltaLat);
        dLonHeightRight = dRightBottom - calculateElevation(dRightBottom - dRightTop, 1.0 / (double) nAS, dDeltaLat);

        // Interpolate between the interpolated left elevation and interpolated right elevation
        dElevation = dLonHeightLeft - calculateElevation(dLonHeightLeft - dLonHeightRight, 1.0 / (double) nAS, dDeltaLon);

        return (int) (dElevation + 0.5);        // Do a rounding of the calculated elevation
    }
}
