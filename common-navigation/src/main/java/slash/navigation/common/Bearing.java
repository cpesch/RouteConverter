/*
 * $Id: Bearing.java,v 1.24 2006/11/18 19:03:12 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slash.navigation.common;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static slash.common.io.Transfer.roundMeterToMillimeterPrecision;

/**
 * Computes the distance, azimuth, and back azimuth between
 * two lat-lon positions on the Earth's surface. Reference ellipsoid is the WGS-84.
 *
 * Modified to return meters with millimeter precision
 *
 * @author Unidata Development Team, modified by Christian Pesch
 */

public class Bearing {

    /**
     * the azimuth, degrees, 0 = north, clockwise positive
     */
    private double azimuth;

    /**
     * the back azimuth, degrees, 0 = north, clockwise positive
     */
    private double backazimuth;

    /**
     * separation in meters
     */
    private double distance;

    /**
     * Earth radius in meters
     */
    public static final double EARTH_RADIUS = 6378137.0;

    /**
     * Some constant
     */
    private static final double F = 1.0 / 298.257223563;

    /**
     * epsilon
     */
    private static final double EPS = 0.5E-13;

    /**
     * constant R
     */
    private static final double R = 1.0 - F;

    /**
     * conversion for degrees to radians
     */
    private static final double rad = toRadians(1.0);

    /**
     * conversion for radians to degrees
     */
    private static final double deg = toDegrees(1.0);

    public Bearing(double azimuth, double backazimuth, double distance) {
        this.azimuth = azimuth;
        this.backazimuth = backazimuth;
        this.distance = distance;
    }

    /**
     * Get the azimuth in degrees, 0 = north, clockwise positive
     *
     * @return azimuth in degrees
     */
    public double getAngle() {
        return azimuth;
    }

    /**
     * Get the back azimuth in degrees, 0 = north, clockwise positive
     *
     * @return back azimuth in degrees
     */
    public double getBackAzimuth() {
        return backazimuth;
    }

    /**
     * Get the distance in meters
     *
     * @return distance in m
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Computes distance (in meters), azimuth (degrees clockwise positive
     * from North, 0 to 360), and back azimuth (degrees clockwise positive
     * from North, 0 to 360), from latitude-longituide point pt1 to
     * latitude-longituide pt2.<p>
     * Algorithm from U.S. National Geodetic Survey, FORTRAN program "inverse,"
     * subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
     * See http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
     * <P>Original documentation:
     * <br>SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
     * <br>MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
     * <br>EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
     * <br>STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
     * </P>
     * Reference ellipsoid is the WGS-84 ellipsoid.
     * <br>See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
     *
     * Requires close to 1.4 E-5 seconds wall clock time per call
     * on a 550 MHz Pentium with Linux 7.2.
     *
     * @param longitude1 Lon of point 1
     * @param latitude1 Lat of point 1
     * @param longitude2 Lon of point 2
     * @param latitude2 Lat of point 2
     * @return a Bearing object with distance (in meters), azimuth from
     *         pt1 to pt2 (degrees, 0 = north, clockwise positive)
     */
    public static Bearing calculateBearing(double longitude1, double latitude1,
                                           double longitude2, double latitude2) {
        if ((latitude1 == latitude2) && (longitude1 == longitude2))
            return new Bearing(0, 0, 0);

        // Algorithm from National Geodetic Survey, FORTRAN program "inverse,"
        // subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
        // http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
        // Conversion to JAVA from FORTRAN was made with as few changes as possible
        // to avoid errors made while recasting form, and to facilitate any future
        // comparisons between the original code and the altered version in Java.
        // Original documentation:
        // SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
        // MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
        // EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
        // STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
        // A IS THE SEMI-MAJOR AXIS OF THE REFERENCE ELLIPSOID
        // F IS THE FLATTENING (NOT RECIPROCAL) OF THE REFERNECE ELLIPSOID
        // LATITUDES GLAT1 AND GLAT2
        // AND LONGITUDES GLON1 AND GLON2 ARE IN RADIANS POSITIVE NORTH AND EAST
        // FORWARD AZIMUTHS AT BOTH POINTS RETURNED IN RADIANS FROM NORTH
        //
        // Reference ellipsoid is the WGS-84 ellipsoid.
        // See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
        // FAZ is forward azimuth in radians from pt1 to pt2;
        // BAZ is backward azimuth from point 2 to 1;
        // S is distance in meters.
        //
        // Conversion to JAVA from FORTRAN was made with as few changes as possible
        // to avoid errors made while recasting form, and to facilitate any future
        // comparisons between the original code and the altered version in Java.
        //
        // IMPLICIT REAL*8 (A-H,O-Z)
        // COMMON/CONST/PI,RAD
        // COMMON/ELIPSOID/EARTH_RADIUS,F
        double GLAT1 = rad * latitude1;
        double GLAT2 = rad * latitude2;
        double TU1 = R * sin(GLAT1) / cos(GLAT1);
        double TU2 = R * sin(GLAT2) / cos(GLAT2);
        double CU1 = 1. / sqrt(TU1 * TU1 + 1.);
        double SU1 = CU1 * TU1;
        double CU2 = 1. / sqrt(TU2 * TU2 + 1.);
        double S = CU1 * CU2;
        double BAZ = S * TU2;
        double FAZ = BAZ * TU1;
        double GLON1 = rad * longitude1;
        double GLON2 = rad * longitude2;
        double X = GLON2 - GLON1;
        double D, SX, CX, SY, CY, Y, SA, C2A, CZ, E, C;
        int count = 0;
        do {
            SX = sin(X);
            CX = cos(X);
            TU1 = CU2 * SX;
            TU2 = BAZ - SU1 * CU2 * CX;
            SY = sqrt(TU1 * TU1 + TU2 * TU2);
            CY = S * CX + FAZ;
            Y = atan2(SY, CY);
            SA = S * SX / SY;
            C2A = -SA * SA + 1.;
            CZ = FAZ + FAZ;
            if (C2A > 0.) {
                CZ = -CZ / C2A + CY;
            }
            E = CZ * CZ * 2. - 1.;
            C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
            D = X;
            X = ((E * CY * C + CZ) * SY * C + Y) * SA;
            X = (1. - C) * X * F + GLON2 - GLON1;
            if(count++ > 100000)
                return new Bearing(0, 0, 0);
            //IF(DABS(D-X).GT.EPS) GO TO 100
        } while (abs(D - X) > EPS);

        FAZ = atan2(TU1, TU2);
        BAZ = atan2(CU1 * SX, BAZ * CX - SU1 * CU2) + PI;
        X = sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
        X = (X - 2.) / X;
        C = 1. - X;
        C = (X * X / 4. + 1.) / C;
        D = (0.375 * X * X - 1.) * X;
        X = E * CY;
        S = 1. - E - E;
        S = ((((SY * SY * 4. - 3.) * S * CZ * D / 6. - X) * D / 4. + CZ) * SY * D + Y) * C * EARTH_RADIUS * R;

        double azimuth = FAZ * deg;   // radians to degrees
        if (azimuth < 0.0) {
            azimuth += 360.0;  // reset azs from -180 to 180 to 0 to 360
        }
        double backazimuth = BAZ * deg;  // radians to degrees; already in 0 to 360 range
        return new Bearing(azimuth, backazimuth, roundMeterToMillimeterPrecision(S));
    }
}

