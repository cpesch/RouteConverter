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

    Datum and ellipsoid tables and the Molodensky transform are translated to Java
    from GPSBabel jeeps/gpsdatum.h and jeeps/gpsmath.cc (master 9c1e336) for
    RouteConverter, 2026; modified.
    Copyright (C) 1999 Alan Bleasby, Copyright (C) 2024 Robert Lipe and the GPSBabel
    developers, licensed under the GNU General Public License, version 2 or (at your
    option) any later version.
*/

package slash.navigation.common;

/**
 * Converts coordinates from the datums known to GPSBabel (OziExplorer, CompeGPS) to WGS 84.
 *
 * @author Alan Bleasby, Robert Lipe, GPSBabel jeeps
 */

public class Datums {
    private static final class Ellipse {
        private final String name;
        private final double a;
        private final double invf;

        private Ellipse(String name, double a, double invf) {
            this.name = name;
            this.a = a;
            this.invf = invf;
        }
    }

    private static final class Datum {
        private final String name;
        private final int ellipse;
        private final double dx, dy, dz;

        private Datum(String name, int ellipse, double dx, double dy, double dz) {
            this.name = name;
            this.ellipse = ellipse;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    private static final class Alias {
        private final String alias;
        private final int datum;

        private Alias(String alias, int datum) {
            this.alias = alias;
            this.datum = datum;
        }
    }

    // index of "WGS84" in ELLIPSES
    private static final int WGS84_ELLIPSE = 26;
    // index of "WGS 84" in DATUMS
    private static final int WGS84_DATUM = 118;
    // above this latitude the longitude is not shifted
    private static final double POLAR_LATITUDE = 89.99;

    private static final Ellipse[] ELLIPSES = {
            new Ellipse("Airy 1830", 6377563.396, 299.3249646),
            new Ellipse("Airy 1830 Modified", 6377340.189, 299.3249646),
            new Ellipse("Australian National", 6378160.000, 298.25),
            new Ellipse("Bessel 1841 (Namibia)", 6377483.865, 299.1528128),
            new Ellipse("Bessel 1841", 6377397.155, 299.1528128),
            new Ellipse("Clarke 1866", 6378206.400, 294.9786982),
            new Ellipse("Clarke 1880", 6378249.145, 293.465),
            new Ellipse("Everest (India 1830)", 6377276.345, 300.8017),
            new Ellipse("Everest (Sabah Sarawak)", 6377298.556, 300.8017),
            new Ellipse("Everest (India 1956)", 6377301.243, 300.8017),
            new Ellipse("Everest (Malaysia 1969)", 6377295.664, 300.8017),
            new Ellipse("Everest (Malay & Sing)", 6377304.063, 300.8017),
            new Ellipse("Everest (Pakistan)", 6377309.613, 300.8017),
            new Ellipse("Modified Fischer 1960", 6378155.000, 298.3),
            new Ellipse("Helmert 1906", 6378200.000, 298.3),
            new Ellipse("Hough 1960", 6378270.000, 297.0),
            new Ellipse("Indonesian 1974", 6378160.000, 298.247),
            new Ellipse("International 1924", 6378388.000, 297.0),
            new Ellipse("Krassovsky 1940", 6378245.000, 298.3),
            // GPSBabel's table holds the semi-minor axis instead of the inverse flattening for GRS67 and GRS75;
            // no datum uses them yet
            new Ellipse("GRS67", 6378160.000, 298.247167427),
            new Ellipse("GRS75", 6378140.000, 298.257),
            new Ellipse("GRS80", 6378137.000, 298.257222101),
            new Ellipse("S. American 1969", 6378160.000, 298.25),
            new Ellipse("WGS60", 6378165.000, 298.3),
            new Ellipse("WGS66", 6378145.000, 298.25),
            new Ellipse("WGS72", 6378135.000, 298.26),
            new Ellipse("WGS84", 6378137.000, 298.257223563),
            new Ellipse("Clarke 1880 (Benoit)", 6378300.789, 293.466),
    };

    private static final Datum[] DATUMS = {
            /* 000 */ new Datum("Adindan", 6, -166, -15, 204),
            /* 001 */ new Datum("AFG", 18, -43, -163, 45),
            /* 002 */ new Datum("Ain-El-Abd", 17, -150, -251, -2),
            /* 003 */ new Datum("Alaska-NAD27", 5, -5, 135, 172),
            /* 004 */ new Datum("Alaska-Canada", 6, -9, 151, 185),
            /* 005 */ new Datum("Anna-1-Astro", 2, -491, -22, 435),
            /* 006 */ new Datum("ARC 1950 Mean", 6, -143, -90, -294),
            /* 007 */ new Datum("ARC 1960 Mean", 6, -160, -8, -300),
            /* 008 */ new Datum("Asc Island 58", 17, -207, 107, 52),
            /* 009 */ new Datum("Astro B4", 17, 114, -116, -333),
            /* 010 */ new Datum("Astro Beacon E", 17, 145, 75, -272),
            /* 011 */ new Datum("Astro pos 71/4", 17, -320, 550, -494),
            /* 012 */ new Datum("Astro stn 52", 17, 124, -234, -25),
            /* 013 */ new Datum("Australia Geo 1984", 2, -134, -48, 149),
            /* 014 */ new Datum("Bahamas NAD27", 6, -4, 154, 178),
            /* 015 */ new Datum("Bellevue IGN", 17, -127, -769, 472),
            /* 016 */ new Datum("Bermuda 1957", 6, -73, 213, 296),
            /* 017 */ new Datum("Bukit Rimpah", 4, -384, 664, -48),
            /* 018 */ new Datum("Camp_Area_Astro", 17, -104, -129, 239),
            /* 019 */ new Datum("Campo_Inchauspe", 17, -148, 136, 90),
            /* 020 */ new Datum("Canada_Mean(NAD27)", 5, -10, 158, 187),
            /* 021 */ new Datum("Canal_Zone_(NAD27)", 5, 0, 125, 201),
            /* 022 */ new Datum("Canton_Island_1966", 17, 298, -304, -375),
            /* 023 */ new Datum("Cape", 6, -136, -108, -292),
            /* 024 */ new Datum("Cape_Canaveral_mean", 5, -2, 150, 181),
            /* 025 */ new Datum("Carribean NAD27", 5, -7, 152, 178),
            /* 026 */ new Datum("Carthage", 6, -263, 6, 431),
            /* 027 */ new Datum("Cent America NAD27", 5, 0, 125, 194),
            /* 028 */ new Datum("Chatham 1971", 17, 175, -38, 113),
            /* 029 */ new Datum("Chua Astro", 17, -134, 229, -29),
            /* 030 */ new Datum("Corrego Alegre", 17, -206, 172, -6),
            /* 031 */ new Datum("Cuba NAD27", 5, -9, 152, 178),
            /* 032 */ new Datum("Cyprus", 17, -104, -101, -140),
            /* 033 */ new Datum("Djakarta(Batavia)", 4, -377, 681, -50),
            /* 034 */ new Datum("DOS 1968", 17, 230, -199, -752),
            /* 035 */ new Datum("Easter lsland 1967", 17, 211, 147, 111),
            /* 036 */ new Datum("Egypt", 17, -130, -117, -151),
            /* 037 */ new Datum("European 1950", 17, -87, -96, -120),
            /* 038 */ new Datum("European 1950 mean", 17, -87, -98, -121),
            /* 039 */ new Datum("European 1979 mean", 17, -86, -98, -119),
            /* 040 */ new Datum("Finnish Nautical", 17, -78, -231, -97),
            /* 041 */ new Datum("Gandajika Base", 17, -133, -321, 50),
            /* 042 */ new Datum("Geodetic Datum 49", 17, 84, -22, 209),
            /* 043 */ new Datum("Ghana", 26, 0, 0, 0),
            /* 044 */ new Datum("Greenland NAD27", 5, 11, 114, 195),
            /* 045 */ new Datum("Guam 1963", 5, -100, -248, 259),
            /* 046 */ new Datum("Gunung Segara", 4, -403, 684, 41),
            /* 047 */ new Datum("Gunung Serindung 1962", 26, 0, 0, 0),
            /* 048 */ new Datum("GUX1 Astro", 17, 252, -209, -751),
            /* 049 */ new Datum("Herat North", 17, -333, -222, 114),
            /* 050 */ new Datum("Hjorsey 1955", 17, -73, 46, 86),
            /* 051 */ new Datum("Hong Kong 1963", 17, -156, -271, -189),
            /* 052 */ new Datum("Hu-Tzu-Shan", 17, -634, -549, -201),
            /* 053 */ new Datum("Indian", 9, 289, 734, 257),
            /* 054 */ new Datum("Iran", 17, -117, -132, -164),
            /* 055 */ new Datum("Ireland 1965", 1, 506, -122, 611),
            /* 056 */ new Datum("ISTS 073 Astro 69", 17, 208, -435, -229),
            /* 057 */ new Datum("Johnston Island 61", 17, 191, -77, -204),
            /* 058 */ new Datum("Kandawala", 7, -97, 787, 86),
            /* 059 */ new Datum("Kerguelen Island", 17, 145, -187, 103),
            /* 060 */ new Datum("Kertau 48", 11, -11, 851, 5),
            /* 061 */ new Datum("L.C. 5 Astro", 5, 42, 124, 147),
            /* 062 */ new Datum("La Reunion", 17, 94, -948, -1262),
            /* 063 */ new Datum("Liberia 1964", 6, -90, 40, 88),
            /* 064 */ new Datum("Luzon", 5, -133, -77, -51),
            /* 065 */ new Datum("Mahe 1971", 6, 41, -220, -134),
            /* 066 */ new Datum("Marco Astro", 17, -289, -124, 60),
            /* 067 */ new Datum("Masirah Is. Nahrwan", 6, -247, -148, 369),
            /* 068 */ new Datum("Massawa", 4, 639, 405, 60),
            /* 069 */ new Datum("Merchich", 6, 31, 146, 47),
            /* 070 */ new Datum("Mexico NAD27", 5, -12, 130, 190),
            /* 071 */ new Datum("Midway Astro 61", 17, 912, -58, 1227),
            /* 072 */ new Datum("Mindanao", 5, -133, -79, -72),
            /* 073 */ new Datum("Minna", 6, -92, -93, 122),
            /* 074 */ new Datum("Montjong Lowe", 26, 0, 0, 0),
            /* 075 */ new Datum("Nahrwan", 6, -231, -196, 482),
            /* 076 */ new Datum("Naparima BWI", 17, -2, 374, 172),
            /* 077 */ new Datum("North America 83", 21, 0, 0, 0),
            /* 078 */ new Datum("N. America 1927 mean", 5, -8, 160, 176),
            /* 079 */ new Datum("Observatorio 1966", 17, -425, -169, 81),
            /* 080 */ new Datum("Old Egyptian", 14, -130, 110, -13),
            /* 081 */ new Datum("Old Hawaiian_mean", 5, 89, -279, -183),
            /* 082 */ new Datum("Old Hawaiian Kauai", 5, 45, -290, -172),
            /* 083 */ new Datum("Old Hawaiian Maui", 5, 65, -290, -190),
            /* 084 */ new Datum("Old Hawaiian Oahu", 5, 56, -284, -181),
            /* 085 */ new Datum("Oman", 6, -346, -1, 224),
            /* 086 */ new Datum("OSGB36", 0, 375, -111, 431),
            /* 087 */ new Datum("Pico De Las Nieves", 17, -307, -92, 127),
            /* 088 */ new Datum("Pitcairn Astro 67", 17, 185, 165, 42),
            /* 089 */ new Datum("S. Am. 1956 mean(P)", 17, -288, 175, -376),
            /* 090 */ new Datum("S. Chilean 1963 (P)", 17, 16, 196, 93),
            /* 091 */ new Datum("Puerto Rico", 5, 11, 72, -101),
            /* 092 */ new Datum("Pulkovo 1942", 18, 28, -130, -95),
            /* 093 */ new Datum("Qornoq", 17, 164, 138, -189),
            /* 094 */ new Datum("Quatar National", 17, -128, -283, 22),
            /* 095 */ new Datum("Rome 1940", 17, -225, -65, 9),
            /* 096 */ new Datum("S-42(Pulkovo1942)", 18, 28, -121, -77),
            /* 097 */ new Datum("S.E.Asia_(Indian)", 7, 173, 750, 264),
            /* 098 */ new Datum("SAD-69/Brazil", 22, -60, -2, -41),
            /* 099 */ new Datum("Santa Braz", 17, -203, 141, 53),
            /* 100 */ new Datum("Santo (DOS)", 17, 170, 42, 84),
            /* 101 */ new Datum("Sapper Hill 43", 17, -355, 16, 74),
            /* 102 */ new Datum("Schwarzeck", 3, 616, 97, -251),
            /* 103 */ new Datum("Sicily", 17, -97, -88, -135),
            /* 104 */ new Datum("Sierra Leone 1960", 26, 0, 0, 0),
            /* 105 */ new Datum("S. Am. 1969 mean", 22, -57, 1, -41),
            /* 106 */ new Datum("South Asia", 13, 7, -10, -26),
            /* 107 */ new Datum("Southeast Base", 17, -499, -249, 314),
            /* 108 */ new Datum("Southwest Base", 17, -104, 167, -38),
            /* 109 */ new Datum("Tananarive Obs 25", 17, -189, -242, -91),
            /* 110 */ new Datum("Thai/Viet (Indian)", 7, 214, 836, 303),
            /* 111 */ new Datum("Timbalai 1948", 7, -689, 691, -45),
            /* 112 */ new Datum("Tokyo mean", 4, -128, 481, 664),
            /* 113 */ new Datum("Tristan Astro 1968", 17, -632, 438, -609),
            /* 114 */ new Datum("United Arab Emirates", 6, -249, -156, 381),
            /* 115 */ new Datum("Viti Levu 1916", 6, 51, 391, -36),
            /* 116 */ new Datum("Wake Eniwetok 60", 15, 101, 52, -39),
            /* 117 */ new Datum("WGS 72", 25, 0, 0, 5),
            /* 118 */ new Datum("WGS 84", 26, 0, 0, 0),
            /* 119 */ new Datum("Yacare", 17, -155, 171, 37),
            /* 120 */ new Datum("Zanderij", 17, -265, 120, -358),
            /* 121 */ new Datum("Sweden", 4, 424.3, -80.5, 613.1),
            /* 122 */ new Datum("GDA 94", 21, 0, 0, 0),
            /* 123 */ new Datum("CH-1903", 4, 674, 15, 405),
            /* 124 */ new Datum("Palestine 1923", 27, -235, -85, 264),
            /* 125 */ new Datum("ITM (Israeli New)", 21, -48, 55, -52),
    };

    private static final Alias[] ALIASES = {
            new Alias("Australian GDA94", 122),
            new Alias("Australian Geocentric 1994 (GDA94)", 122),
            new Alias("GDA94", 122),
            new Alias("GDA-94", 122),
            new Alias("CH1903", 123),
            new Alias("CH 1903", 123),
            new Alias("European 1950 (Spain and Portugal)", 38),
            new Alias("Geodetic Datum 1949", 42),
            new Alias("NAD27 Alaska", 3),
            new Alias("NAD27 Bahamas", 14),
            new Alias("NAD27 Canada", 4),
            new Alias("NAD27 Canal Zone", 21),
            new Alias("NAD27 Caribbean", 25),
            new Alias("NAD27 Central", 27),
            new Alias("NAD27 CONUS", 78),
            new Alias("NAD27 Cuba", 31),
            new Alias("NAD27 Greenland", 44),
            new Alias("NAD27 Mexico", 70),
            new Alias("NAD83", 77),
            new Alias("NAD 83", 77),
            new Alias("NAD-83", 77),
            new Alias("OSGB 36", 86),
            new Alias("OSGB-36", 86),
            new Alias("Wake-Eniwetok 1960", 116),
            new Alias("WGS72", 117),
            new Alias("WGS-72", 117),
            new Alias("WGS84", 118),
            new Alias("WGS-84", 118),
            new Alias("Israeli", 124),
            new Alias("D_Israel_new", 125),

            // the names OziExplorer uses for datums that GPSBabel knows under another name with the same
            // ellipsoid and the same shift; Ozi names of datums GPSBabel lacks are left out
            new Alias("Afgooye", 1),
            new Alias("Ain el Abd 1970", 2),
            new Alias("Anna 1 Astro 1965", 5),
            new Alias("Arc 1950", 6),
            new Alias("Arc 1960", 7),
            new Alias("Ascension Island 1958", 8),
            new Alias("Astro B4 Sorol Atoll", 9),
            new Alias("Astro Beacon 1945", 10),
            new Alias("Astro DOS 71/4", 11),
            new Alias("Astronomic Stn 1952", 12),
            new Alias("Australian Geodetic 1984", 13),
            new Alias("Bellevue (IGN)", 15),
            new Alias("Campo Inchauspe", 19),
            new Alias("Canton Astro 1966", 22),
            new Alias("Cape Canaveral", 24),
            new Alias("Djakarta (Batavia)", 33),
            new Alias("Easter Island 1967", 35),
            new Alias("European 1950 (Mean France)", 37),
            new Alias("European 1979", 39),
            new Alias("Finland Hayford", 40),
            new Alias("GUX 1 Astro", 48),
            new Alias("Hartebeeshoek94", 118),
            new Alias("Indian Thailand", 110),
            new Alias("ISTS 073 Astro 1969", 56),
            new Alias("Johnston Island", 57),
            new Alias("Kertau 1948", 60),
            new Alias("Luzon Mindanao", 72),
            new Alias("Luzon Philippines", 64),
            new Alias("Midway Astro 1961", 71),
            new Alias("Nahrwn Masirah Ilnd", 67),
            new Alias("Nahrwn Saudi Arbia", 75),
            new Alias("Nahrwn United Arab", 114),
            new Alias("Ord Srvy Grt Britn", 86),
            new Alias("Pitcairn Astro 1967", 88),
            new Alias("Prov So Amrican 1956", 89),
            new Alias("Prov So Chilean 1963", 90),
            new Alias("Pulkovo 1942 (1)", 92),
            new Alias("Pulkovo 1942 (2)", 92),
            new Alias("Qatar National", 94),
            new Alias("Reunion", 62),
            new Alias("S42", 96),
            new Alias("Sao Braz", 99),
            new Alias("Sapper Hill 1943", 101),
            new Alias("South American 1969", 105),
            new Alias("Tokyo", 112),
    };

    /**
     * Looks up the index of a datum by alias or name, both case-insensitive, ignoring leading and
     * trailing blanks.
     *
     * @param name the name of the datum
     * @return the index of the datum or -1 if unknown
     */
    public static int lookupIndex(String name) {
        if (name == null)
            return -1;
        name = name.trim();

        for (Alias alias : ALIASES) {
            if (alias.alias.equalsIgnoreCase(name))
                return alias.datum;
        }
        for (int i = 0; i < DATUMS.length; i++) {
            if (DATUMS[i].name.equalsIgnoreCase(name))
                return i;
        }
        return -1;
    }

    public static String name(int index) {
        return DATUMS[index].name;
    }

    /**
     * Transforms a position from the given datum to WGS 84.
     *
     * @return {latitude, longitude, height}
     */
    public static double[] toWgs84(double latitude, double longitude, double height, int datumIndex) {
        if (datumIndex == WGS84_DATUM ||
                !Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(height))
            return new double[]{latitude, longitude, height};

        Datum datum = DATUMS[datumIndex];
        Ellipse source = ELLIPSES[datum.ellipse];
        Ellipse destination = ELLIPSES[WGS84_ELLIPSE];
        double[] result = molodensky(latitude, longitude, height, source.a, source.invf,
                destination.a, destination.invf, datum.dx, datum.dy, datum.dz);
        // the longitude shift divides by the cosine of the latitude and explodes near the poles, where the
        // longitude hardly matters: keep it there
        if (Math.abs(latitude) > POLAR_LATITUDE)
            result[1] = longitude;
        result[1] = wrapLongitude(result[1]);
        return result;
    }

    private static double wrapLongitude(double longitude) {
        if (longitude >= -180.0 && longitude <= 180.0)
            return longitude;
        double wrapped = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        // keep the eastern edge of the range instead of jumping to the western one
        return wrapped == -180.0 && longitude > 0.0 ? 180.0 : wrapped;
    }

    private static double[] molodensky(double sphi, double slam, double sh, double sa, double sif,
                                       double da2, double dif, double dx, double dy, double dz) {
        double sf = 1.0 / sif;
        double df2 = 1.0 / dif;

        double esq = 2.0 * sf - Math.pow(sf, 2.0);
        double bda = 1.0 - sf;
        sphi = Math.toRadians(sphi);
        slam = Math.toRadians(slam);

        double da = da2 - sa;
        double df = df2 - sf;

        double phis = Math.sin(sphi);
        double phic = Math.cos(sphi);
        double lams = Math.sin(slam);
        double lamc = Math.cos(slam);

        double n = sa / Math.sqrt(1.0 - esq * Math.pow(phis, 2.0));

        double tmp = (1.0 - esq) / Math.pow((1.0 - esq * Math.pow(phis, 2.0)), 1.5);
        double m = sa * tmp;

        tmp = df * ((m / bda) + n * bda) * phis * phic;
        double tmp2 = da * n * esq * phis * phic / sa;
        tmp2 += ((-dx * phis * lamc - dy * phis * lams) + dz * phic);
        double dphi = (tmp2 + tmp) / (m + sh);

        double dlambda = (-dx * lams + dy * lamc) / ((n + sh) * phic);

        double dheight = dx * phic * lamc + dy * phic * lams + dz * phis - da * (sa / n) +
                df * bda * n * phis * phis;

        return new double[]{Math.toDegrees(sphi + dphi), Math.toDegrees(slam + dlambda), sh + dheight};
    }
}
