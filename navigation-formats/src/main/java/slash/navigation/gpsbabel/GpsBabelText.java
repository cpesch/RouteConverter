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

    Translated to Java from GPSBabel csv_util.cc (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
    Copyright (C) 2002 Alex Mottram (geo_alexm at cox-internet.com)
    Copyright (C) 2002-2014 Robert Lipe, robertlipe+source@gpsbabel.org
*/

package slash.navigation.gpsbabel;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.navigation.base.GpxBackedNavigationFormat.isLegalXmlCharacter;
import static slash.navigation.gpsbabel.GpsBabelMath.ddmm2degrees;

/**
 * Text helpers translated from GPSBabel.
 */
public class GpsBabelText {
    private static final double UNSET = 999;
    private static final Pattern ATOF = Pattern.compile("^(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?");

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    // the five bytes Windows-1252 leaves undefined; they are mapped to the C1 control characters of the same value
    private static final String WINDOWS_1252_HOLES = "\u0081\u008D\u008F\u0090\u009D";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private GpsBabelText() {
    }

    /**
     * Removes a leading UTF-8 byte order mark ({@code EF BB BF}), which editors like Notepad and Excel write
     * and which no format expects in front of its first field. Other bytes are returned unchanged.
     */
    public static byte[] stripBom(byte[] bytes) {
        if (bytes.length >= UTF8_BOM.length && bytes[0] == UTF8_BOM[0] && bytes[1] == UTF8_BOM[1] && bytes[2] == UTF8_BOM[2])
            return Arrays.copyOfRange(bytes, UTF8_BOM.length, bytes.length);
        return bytes;
    }

    /**
     * Decodes the text of a file or field whose encoding is not declared: as UTF-8 when the bytes are valid
     * UTF-8, else as Windows-1252 (see {@link #decodeCp1252(byte[], int, int)}), which decodes every byte.
     * A leading UTF-8 byte order mark is removed. Pure ASCII decodes the same either way, and text in
     * Windows-1252 with characters above 0x7F is practically never valid UTF-8, so the guess is safe.
     */
    public static String decodeUtf8OrCp1252(byte[] bytes) {
        byte[] text = stripBom(bytes);
        CharsetDecoder decoder = UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(text)).toString();
        } catch (CharacterCodingException e) {
            return decodeCp1252(text, 0, text.length);
        }
    }

    /**
     * Decodes Windows-1252 without losing a byte: the five bytes the code page leaves undefined
     * (0x81, 0x8D, 0x8F, 0x90, 0x9D) become the characters U+0081 ... U+009D instead of U+FFFD,
     * so {@link #encodeCp1252(String)} writes them back unchanged.
     */
    public static String decodeCp1252(byte[] bytes, int offset, int length) {
        StringBuilder result = new StringBuilder(new String(bytes, offset, length, WINDOWS_1252));
        for (int i = 0; i < result.length(); i++)
            if (result.charAt(i) == '\uFFFD')
                result.setCharAt(i, (char) (bytes[offset + i] & 0xFF));
        return result.toString();
    }

    /**
     * Encodes text as Windows-1252, the counterpart of {@link #decodeCp1252(byte[], int, int)}: U+0081 ... U+009D
     * of the five undefined bytes are written as these bytes, every other character the code page cannot
     * represent becomes {@code ?}. Use {@link #canEncodeCp1252(String)} to find out whether that happens.
     */
    public static byte[] encodeCp1252(String text) {
        StringBuilder holes = new StringBuilder();
        String marked = markHoles(text, holes);
        byte[] result = marked.getBytes(WINDOWS_1252);
        if (holes.length() > 0) {
            int hole = 0;
            for (int i = 0; i < result.length && hole < holes.length(); i++)
                if (result[i] == 0x1A)
                    result[i] = (byte) holes.charAt(hole++);
        }
        return result;
    }

    /**
     * Whether {@link #encodeCp1252(String)} writes every character of the text, without a {@code ?} for a
     * character outside Windows-1252.
     */
    public static boolean canEncodeCp1252(String text) {
        CharsetEncoder encoder = WINDOWS_1252.newEncoder();
        String marked = markHoles(text, new StringBuilder());
        try {
            encoder.encode(CharBuffer.wrap(marked));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    // replaces the characters of the undefined bytes - and a literal SUB, so it cannot be mistaken
    // for one of them - by SUB (0x1A, the same byte in Windows-1252) and remembers what they were
    private static String markHoles(String text, StringBuilder holes) {
        StringBuilder result = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0x1A || WINDOWS_1252_HOLES.indexOf(c) != -1) {
                if (result == null)
                    result = new StringBuilder(text);
                result.setCharAt(i, (char) 0x1A);
                holes.append(c);
            }
        }
        return result == null ? text : result.toString();
    }

    /**
     * Makes a text fit into one field of a line oriented text format: every delimiter, TAB, CR, LF and every
     * character that is illegal in XML 1.0 (NUL, other C0 controls, U+FFFE, U+FFFF, unpaired surrogates) is
     * replaced by a space, so the record keeps its field count and its line, and neither RouteConverter nor
     * a strict XML parser stumbles over the character on the way back. A null text is an empty field.
     */
    public static String cleanField(String text, String delimiters) {
        if (text == null)
            return "";

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                result.append(c).append(text.charAt(++i));
                continue;
            }
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n')
                continue; // CR LF becomes the one space of the LF
            boolean replace = c == '\t' || c == '\r' || c == '\n' || !isLegalXmlCharacter(c) ||
                    (delimiters != null && delimiters.indexOf(c) != -1);
            result.append(replace ? ' ' : c);
        }
        return result.toString();
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }

    private static boolean isSpace(char c) {
        return c == ' ' || (c >= '\t' && c <= '\r');
    }

    /**
     * csv_util.cc csv_stringtrim(string, enclosure, 0): trims whitespace and pairs of enclosures.
     */
    private static String csvStringTrim(String string, String enclosure) {
        if (string.isEmpty())
            return string;
        int elen = enclosure == null ? 0 : enclosure.length();
        int p1 = 0;
        int p2 = string.length() - 1;
        while (p2 > p1 && isSpace(string.charAt(p2)))
            p2--;
        while (p1 < p2 && isSpace(string.charAt(p1)))
            p1++;
        if (elen > 0) {
            while ((p2 - p1 + 1) >= (elen * 2) &&
                    string.startsWith(enclosure, p1) &&
                    string.startsWith(enclosure, p2 - elen + 1)) {
                p2 -= elen;
                p1 += elen;
            }
        }
        return p2 < p1 ? "" : string.substring(p1, p2 + 1);
    }

    /**
     * csv_util.cc csv_lineparse: splits a line into its fields, honouring enclosures that may contain
     * the delimiter. Empty fields are kept. A delimiter of "\\w" splits at runs of blanks and tabs.
     */
    public static List<String> csvLineParse(String line, String delimiter, String enclosure) {
        List<String> result = new ArrayList<>();
        if (line == null)
            return result;

        boolean hyperWhitespaceDelimiter = "\\w".equals(delimiter);
        // "csv" format is "commaspace" but real data has just the comma
        if (", ".equals(delimiter))
            delimiter = ",";

        int dlen = delimiter != null && !hyperWhitespaceDelimiter ? delimiter.length() : 0;
        int elen = enclosure != null ? enclosure.length() : 0;
        int length = line.length();
        int p = 0;

        while (true) {
            int sp = p;
            boolean efound = false;
            boolean dfound = false;
            int enclosedepth = 0;

            while (p < length && !dfound) {
                if (elen > 0 && line.startsWith(enclosure, p)) {
                    efound = true;
                    p += elen;
                    if (enclosedepth > 0)
                        enclosedepth--;
                    else
                        enclosedepth++;
                    continue;
                }

                if (enclosedepth == 0) {
                    if (dlen > 0 && line.startsWith(delimiter, p)) {
                        dfound = true;
                    } else if (hyperWhitespaceDelimiter && isWhitespace(line.charAt(p))) {
                        dfound = true;
                        while (p < length && isWhitespace(line.charAt(p)))
                            p++;
                    } else {
                        p++;
                    }
                } else {
                    p++;
                }
            }

            String field = line.substring(sp, p);
            if (elen > 0 && efound)
                field = csvStringTrim(field, enclosure);
            result.add(field);

            if (!dfound)
                break;
            p += dlen;
        }
        return result;
    }

    private static double atof(String string, int start) {
        Matcher matcher = ATOF.matcher(string.substring(start));
        return matcher.find() ? Double.parseDouble(matcher.group()) : 0.0;
    }

    private static boolean isNumberCharacter(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == ',';
    }

    /**
     * csv_util.cc human_to_dec: converts a human readable latitude and/or longitude such as
     * "N 41° 09.12' W 085° 09.36'" or "41 9 5.652 N" to decimal degrees.
     *
     * @param which 0 - no preference, 1 - an unmarked value is the latitude, 2 - an unmarked value is the longitude
     * @return latitude and longitude, NaN for a part that was not found
     */
    public static double[] humanToDec(String text, int which) {
        double[] unk = {UNSET, UNSET, UNSET};
        double[] lat = {UNSET, UNSET, UNSET};
        double[] lon = {UNSET, UNSET, UNSET};
        int latsign = 0;
        int lonsign = 0;
        int unksign = 1;

        double[] numres = unk;
        int numind = 0;
        String input = text == null ? "" : text.replace(',', '.');
        int cur = 0;

        while (cur < input.length()) {
            char c = input.charAt(cur);
            switch (c) {
                case 'n':
                case 's':
                case 'N':
                case 'S':
                    if (unk[0] != UNSET) {
                        numind = 0;
                        System.arraycopy(unk, 0, lat, 0, 3);
                        unk = new double[]{UNSET, UNSET, UNSET};
                        numres = unk;
                    } else {
                        numres = lat;
                        numind = 0;
                        lat[0] = lat[1] = lat[2] = UNSET;
                    }
                    latsign = (c == 'n' || c == 'N') ? 1 : -1;
                    cur++;
                    break;
                case 'w':
                case 'e':
                case 'W':
                case 'E':
                    if (unk[0] != UNSET) {
                        numind = 0;
                        System.arraycopy(unk, 0, lon, 0, 3);
                        unk = new double[]{UNSET, UNSET, UNSET};
                        numres = unk;
                    } else {
                        numres = lon;
                        numind = 0;
                        lon[0] = lon[1] = lon[2] = UNSET;
                    }
                    lonsign = (c == 'e' || c == 'E') ? 1 : -1;
                    cur++;
                    break;
                case '-':
                    unksign = -1;
                    cur++;
                    break;
                default:
                    if (isNumberCharacter(c)) {
                        numres[numind] = atof(input, cur);
                        while (cur < input.length() && isNumberCharacter(input.charAt(cur)))
                            cur++;
                    } else {
                        if (numres[numind] != UNSET) {
                            numind++;
                            if (numind > 2) {
                                numres = unk;
                                numind = 0;
                            }
                        }
                        cur++;
                    }
                    break;
            }
        }

        if (lat[0] == UNSET && lon[0] == UNSET) {
            if (which == 1) {
                System.arraycopy(unk, 0, lat, 0, 3);
                latsign = unksign;
            } else if (which == 2) {
                System.arraycopy(unk, 0, lon, 0, 3);
                lonsign = unksign;
            }
        }
        return new double[]{toDegrees(lat, latsign), toDegrees(lon, lonsign)};
    }

    private static double toDegrees(double[] parts, int sign) {
        double result = Double.NaN;
        if (parts[0] != UNSET)
            result = parts[0];
        if (parts[1] != UNSET)
            result += parts[1] / 60.0;
        if (parts[2] != UNSET)
            result += parts[2] / 3600.0;
        if (result > 360)
            result = ddmm2degrees(result); // NMEA style
        if (sign != 0)
            result *= sign;
        return result;
    }
}
