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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.hex;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Helps to encode bytes, shorts, ints to hexadecimal encoded
 * digits or an Outputstream to an encoded Writer.
 *
 * @author Christian Pesch
 */

public class HexEncoder extends OutputStream {
    private final Writer writer;

    private HexEncoder(Writer writer) {
        this.writer = writer;
    }

    public void write(int anInt) throws IOException {
        writer.write(lowByte(anInt));
        writer.write(highByte(anInt));
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }


    private static final char[] encoding = new char[16];
    static {
        encoding[0] = '0';
        encoding[1] = '1';
        encoding[2] = '2';
        encoding[3] = '3';
        encoding[4] = '4';
        encoding[5] = '5';
        encoding[6] = '6';
        encoding[7] = '7';
        encoding[8] = '8';
        encoding[9] = '9';
        encoding[10] = 'A';
        encoding[11] = 'B';
        encoding[12] = 'C';
        encoding[13] = 'D';
        encoding[14] = 'E';
        encoding[15] = 'F';
    }

    private static char lowByte(int aByte) {
        return encoding[((aByte >> 4) & 0x0f)];
    }

    private static char highByte(int aByte) {
        return encoding[(aByte & 0x0f)];
    }

    public static String encodeByte(byte aByte) {
        char[] chars = new char[2];
        chars[0] = lowByte(aByte);
        chars[1] = highByte(aByte);
        return new String(chars);
    }

    private static String encodeBytes(byte[] bytes) {
        StringWriter writer = new StringWriter(2 * bytes.length);
        HexEncoder encoder = new HexEncoder(writer);
        try {
            encoder.write(bytes);
            encoder.close();
        } catch (IOException e) {
            throw new RuntimeException("No IOException is possible here");
        }
        return writer.toString();
    }

    private static String encodeShort(short aShort) {
        return encodeByte((byte) ((aShort >> 8) & 0x00ff)) +
                encodeByte((byte) (aShort & 0x00ff));
    }

    private static String encodeInt(int anInt) {
        return encodeShort((short) ((anInt >> 16) & 0x0000ffff)) +
                encodeShort((short) (anInt & 0x0000ffff));
    }

    private static String encodeLong(long aLong) {
        return encodeInt((int) ((aLong >> 32))) +
                encodeInt((int) (aLong));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(HexEncoder.class + " <int1> [<int2> ...]");
            System.exit(1);
        }

        for (String arg : args) {
            int decimal = Integer.parseInt(arg);
            System.out.println("Dec " + decimal + " is hex " + HexEncoder.encodeInt(decimal));
        }
        System.exit(0);
    }
}
