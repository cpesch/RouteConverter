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

import slash.common.io.Externalization;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A tile with elevation data.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class ElevationTile {
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String HGT_FILES_URL_PREFERENCE = "hgtFilesUrl";

    private static String getHgtFilesUrl() {
        return preferences.get(HGT_FILES_URL_PREFERENCE, "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/");
    }

    private RandomAccessFile elevationFile = null;
    private int nLon;
    private int nLat;
    private String strFullFilename = "";

    public ElevationTile(double longitude, double latitude) {

        this.nLon = (int) longitude;
        this.nLat = (int) latitude;

        String strFilename = String.format("%s%02d%s%03d.hgt", (latitude < 0) ? "S" : "N",
                (latitude < 0) ? ((this.nLat - 1) * -1) : this.nLat,
                (longitude < 0) ? "W" : "E",
                (longitude < 0) ? ((this.nLon - 1) * -1) : this.nLon);

        this.strFullFilename = String.format("%s/%s", Externalization.getTempDirectory(), strFilename);


        File hgtFile = new File(strFullFilename);

        if (!hgtFile.exists()) {
            readHgtFile(strFilename);
        }

        try {
            elevationFile = new RandomAccessFile(strFullFilename, "r");
        }
        catch (Exception e) {
            // TODO
        }
    }

    private void readHgtFile(String strFilename) {
        URL u;
        URLConnection uc = null;
        String contentType;
        int contentLength;
        String strFolder = "";

        try {
            String destinationname = String.format("%s/", Externalization.getTempDirectory());
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;

            for (int nIndex = 0; nIndex < 6; nIndex++) {
                switch (nIndex) {
                    case 0:
                        strFolder = "Eurasia";
                        break;
                    case 1:
                        strFolder = "North_America";
                        break;
                    case 2:
                        strFolder = "Australia";
                        break;
                    case 3:
                        strFolder = "South_America";
                        break;
                    case 4:
                        strFolder = "Africa";
                        break;
                    case 5:
                        strFolder = "Islands";
                        break;
                }

                u = new URL(String.format("%s/%s/%s.zip", getHgtFilesUrl(), strFolder, strFilename));
                System.out.println(u);
                uc = u.openConnection();
                contentType = uc.getContentType();
                contentLength = uc.getContentLength();

                if (contentType.startsWith("application/") &&
                        contentLength != -1) {
                    break;
                }
            }

            InputStream raw = uc.getInputStream();
            InputStream in = new BufferedInputStream(raw);

            zipinputstream = new ZipInputStream(in);
            zipentry = zipinputstream.getNextEntry();

            while (zipentry != null) { // for each entry to be extracted
                int n;
                FileOutputStream fileoutputstream;
                String entryName = zipentry.getName();
                File newFile = new File(entryName);
                String directory = newFile.getParent();

                if (directory == null) {
                    if (newFile.isDirectory()) {
                        break;
                    }
                }

                fileoutputstream = new FileOutputStream(destinationname + entryName);

                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);
                }

                fileoutputstream.close();
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

    /**
     * @param dHeight12 The delta height/elevation of two sub tile positions
     * @param dLength12 The length of an sub tile interval (1 / 1200)
     * @param dDiff     The distance of the real point from the sub tile position
     * @return The delta elevation (relative to sub tile position)
     * @brief Calculate the elevation (for the destination position) according the
     * theorem on intersecting lines (Strahlensatz).
     */
    private double calculateElevation(double dHeight12, double dLength12, double dDiff) {
        return (dHeight12 * dDiff) / dLength12;
    }

    public Integer getElevationFor(Double longitude, Double latitude) throws IOException {
        if (elevationFile == null)
            return null;

        double dElevation = 0;
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

        int nLonIndex = (int) ((dLon - (double) nLon) * 1200.0);    // Calculate the interval index for longitude
        int nLatIndex = (int) ((dLat - (double) nLat) * 1200.0); // Calculate the interval index for latitude
        double dOffLon = dLon - (double) nLon;                      // The lon value offset within a tile
        double dOffLat = dLat - (double) nLat;                      // The lat value offset within a tile

        double dLeftTop;                                            // The left top position of a sub tile
        double dLeftBottom;                                            // The left bottom position of a sub tile
        double dRightTop;                                            // The right top position of a sub tile
        double dRightBottom;                                        // The right bootm position of a sub tile
        int pos;                                                    // The index of the elevation into the hgt file

        pos = (((nAS - nLatIndex) - 1) * (nAS + 1)) + nLonIndex;    // The index for the left top elevation
        this.elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dLeftTop = this.elevationFile.readShort();                // Now read the left top elevation from hgt file

        pos = ((nAS - nLatIndex) * (nAS + 1)) + nLonIndex;            // The index for the left bottom elevation
        this.elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dLeftBottom = this.elevationFile.readShort();                // Now read the left bottom elevation from hgt file

        pos = (((nAS - nLatIndex) - 1) * (nAS + 1)) + nLonIndex + 1;// The index for the right top elevation
        this.elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dRightTop = this.elevationFile.readShort();                // Now read the right top elevation from hgt file

        pos = ((nAS - nLatIndex) * (nAS + 1)) + nLonIndex + 1;         // The index for the right bottom elevation
        this.elevationFile.seek(pos * 2);                            // We have 16-bit values for elevation, so multiply by 2
        dRightBottom = this.elevationFile.readShort();                // Now read the right bottom top elevation from hgt file

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
