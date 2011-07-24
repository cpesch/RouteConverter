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

/**
 * @author Malte Neumann Created on 25.06.2011 at 14:50:01
 *
 */
package slash.navigation.nmn;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import slash.common.io.CompactCalendar;
import slash.navigation.base.*;

/*
    Kodierung: Little Endian
    
    Dateibegin ff ff 00 00  
    Gefolgt 01 00 00 00 00 00 00 00 (1 als 64 bit)
    4 byte länge bis Ende. diese 4 byte inkludiert. Rest also Wert - 4
    4 byte 0 
    4 byte Länge der folgenden Nutzdaten
    Nutzdaten hier Datum 
    4 byte Anzahl Punkte
    4 byte int gesehen 0 und 1
    
    1..n Punkt 
      4 byte Bytelänge des folgenden Punktes. diese 4 byte nicht mitzählen
      8 byte 0 
      4 byte int Anzahl der folgenden Buchstaben
      n byte Text 
      4 byte int bisher nur 1
      4 byte int Anzahl der folgenden Datenpunkte mit 04
      8 byte unterschiedlichster Inhalt. Ab zweiten Punkt 0
      4 byte (int )Datentyp bisher gesehen: 0, 4. 0 immer nach Datentyp 4  
    
    Sind die anzahl der bytes gelesen. Kommt der nächste Punkt in identischem Aufbau

    
    Datentyp 4 (04 00 00 00)
        4 byte int 04 00 00 00
        8 byte int als Länge diese nicht mitzählen
        4 byte int Länge des Textes. Wenn 0, dann folgende trotzdem 4 Bytes. 
        n byte Text
        manchmal hier schon zu Ende.
        4 byte (int) bisher gefunden: 0, 1
        8 byte breite double
        8 byte länge double
               
        4 byte int bisher gefunden 2, 3, 
        
        4 byte int abhängig folgt bis zur Gesamtlänge - 8
            0x0: 4 byte int dann:
                 00 7B A4 3F: 
                    4 byte textlänge
                    n byte Text
                 05 00 00 00: 
                  5 byte text
            0x2: 8 byte ?
            0x7:    
            0x9: 4 byte Textlänge
               n byte Text  
            0x8: 4 byte Textlänge
               n byte Text            
            0x32: es folgt 
               4 byte Textlänge 
               n byte PLZ
            0x3C: es folgt
               4 byte Textlänge
               n byte Text
               4 byte 
               4 byte Textlänge
               n byte Text
               4 byte Textlänge
               n byte Text              
               
        8 byte ?     
 
 */


public class NavigonRouteFormat extends SimpleFormat<Wgs84Route> {

    @Override
    public String getName() {
        return "Navigon (*" + getExtension() + ")";
    }

    @Override
    public String getExtension() {
        return ".route";
    }

    @Override
    public int getMaximumPositionCount() {
        return 99;
    }

    @Override
    public List<Wgs84Route> read(BufferedReader reader,
                                 CompactCalendar startDate, String encoding)
            throws IOException {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    /* writing doesn't work
    private byte[] encodePoint(Wgs84Position position, int positionNo){
        //Die Route besteht aus einem Punkt der mehrere weitere Unterpunktbeschreibungen hat.
        //Im Navigongerät werden dort weitere Informationen wir Übergeorgnete Stadt, Land.. 
        //gespeichert. Diese Informationen liegen nicht vor und werden daher auch nicht 
        //geschrieben.
        //Es wird für jeden Routenpunkt ein Unterpunkt erstellt.        
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(0);
        
        
        byteBuffer.putInt(0); //bytelength of whole point will be filled at the end
        byteBuffer.putLong(0); //8byte 0
        String posNoString = String.format("%02d", positionNo);
        byteBuffer.putInt(posNoString.getBytes().length); //4 byte textlength
        byteBuffer.put(posNoString.getBytes()); //text
        byteBuffer.putInt(1); // 4 byte always 1
        byteBuffer.putInt(1); //count following "04 00 00 00" Block. write only one 04 block in every waypoint
        if (positionNo == 0){//8 byte ?
            //Copied from example cleverParking.route
            byte unknownBytes[] = {(byte)0x60, (byte)0x81, (byte)0x83, (byte)0x05, 
                                   (byte)0x64, (byte)0x00, (byte)0x00, (byte)0x00};
            byteBuffer.put(unknownBytes);
        }
        else
            byteBuffer.putLong(0);
        
        
        byteBuffer.putInt(4); //starttag
        int positionStarttag = byteBuffer.position(); //save position to fill the bytelength at the end
        byteBuffer.putLong(0); //length of following data. filled at the end
        byteBuffer.putInt(position.getComment().getBytes().length);
        byteBuffer.put(position.getComment().getBytes());
        byteBuffer.putInt(0); //4 byte ?
        byteBuffer.putDouble(position.getLongitude().doubleValue());
        byteBuffer.putDouble(position.getLatitude().doubleValue());
        byteBuffer.putInt(2); //4 byte ??
        byteBuffer.putLong(0); //8 byte ??
        
        int pointByteLength = byteBuffer.position();
        //fill the bytelength fields
        byteBuffer.putInt(0, pointByteLength - 4);
        byteBuffer.putInt(positionStarttag, pointByteLength - positionStarttag - 8);
        
        byte[] result = new byte[pointByteLength];
        byteBuffer.position(0);
        byteBuffer.get(result);
        
        return result;
    }
    
    
    
    @Override
    public void write(Wgs84Route route, OutputStream target, int startIndex,
                      int endIndex) throws IOException {

        ByteArrayOutputStream tmpOutput = new ByteArrayOutputStream();
        
        //4 bytes always 0
        tmpOutput.write(new byte[] {0,0,0,0});
        //4byte Textlenght Date - we write no date
        tmpOutput.write(new byte[] {0,0,0,0});
        
        //4 byte Pointcount
        //max. 255 Points with this style 
        tmpOutput.write((byte) (endIndex - startIndex));
        tmpOutput.write(new byte[] {0,0,0});
        
        //4 byte int. Seen 0 and 1
        //write always 1
        tmpOutput.write(new byte[] {1,0,0,0});
        
        
        //write all waypoints to temp. Buffer. 
        //We need at the end the size of all position bytes
        int positionNo = 1;
        for (int i = startIndex; i < endIndex; i++){
            Wgs84Position position = route.getPosition(i);
            byte[] waypointBytes = encodePoint(position, positionNo++);
            tmpOutput.write(waypointBytes);
        }
        
        
        byte[] header = new byte[16];
        ByteBuffer headerBuffer = ByteBuffer.wrap(header);
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
        headerBuffer.position(0);
        
        headerBuffer.putInt(0xFFFF);// start byte
        headerBuffer.putLong(1); // 8 byte ?. always 1
        headerBuffer.putInt(tmpOutput.size() + 4);
        
        
        target.write(header);
        target.write(tmpOutput.toByteArray());
    } */

    @Override
    public <P extends BaseNavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics,
                                                                     String name,
                                                                     List<P> positions) {
        Wgs84Route newRoute = new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
        newRoute.setName(name);
        return newRoute;
    }

    @Override
    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    @Override
    public boolean isSupportsReading() {
        return true;
    }

    @Override
    public boolean isSupportsWriting() {
        return false;
    }

    
    private boolean checkHeader(InputStream source) throws IOException{
        byte[] fileHeaderContent = new byte[16];
        source.read(fileHeaderContent);
        
        
        ByteBuffer header = ByteBuffer.allocate(16);
        header.put(fileHeaderContent);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.position(0);
    
        if ((header.getInt() == 0xFFFF) && (header.getLong() == 1)){
            long fileSize = header.getInt();
            return (source.available() == fileSize - 4); 
        }
        return false;
    }
    
    private String getText(ByteBuffer byteBuffer){
        int textLen = byteBuffer.getInt();
        return getText(byteBuffer, textLen);
    }
    
    private String getText(ByteBuffer byteBuffer, int count){
        if (count > 0){
            byte[] text = new byte[count];
            for (int i = 0; i < text.length; i++)
                text[i] = byteBuffer.get();
            return new String(text);
        }
        return "";
    }
    
    @Override
    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        if (checkHeader(source)){
            //copy whole file to a bytebuffer
            byte[] fileContentArray = new byte[source.available()];
            source.read(fileContentArray);
            
            ByteBuffer fileContent = ByteBuffer.wrap(fileContentArray);
            fileContent.order(ByteOrder.LITTLE_ENDIAN);
            fileContent.position(0);

            
            Wgs84Route route = createRoute(RouteCharacteristics.Route,
                                           "", 
                                           new ArrayList<BaseNavigationPosition>());
            List<Wgs84Route> resultRouteList = new ArrayList<Wgs84Route>();
            resultRouteList.add(route);
            
            // 4 byte Anzahl Punkte
            fileContent.getInt();
            
            //Erstellungsdatum
            getText(fileContent);
            
            int expectedWaypointCount = fileContent.getInt();

            fileContent.getInt(); // gesehen 0, 1

            while (fileContent.position() < fileContent.capacity() - 4){
                Wgs84Position waypoint = readWaypoint(fileContent);
                if (waypoint != null)
                    route.getPositions().add(waypoint);
            }
            
            if (route.getPositionCount() == expectedWaypointCount)
                return resultRouteList;
            return null;
        }
        return null;
    }
    
    protected Wgs84Position readWaypoint(ByteBuffer fileContent){
        //4 Byte length
        int trackPointLength = fileContent.getInt();
        int trackPointEndPosition = trackPointLength + fileContent.position();
        
        //8 Byte 0. unknown
        fileContent.position(fileContent.position() + 8);
        
        //4 byte länge + Text
        getText(fileContent);
        
        //4 byte int 
        fileContent.getInt();
        
        //4 byte int Anzahl der folgenden Datenpunkte mit 04
        fileContent.getInt();
        
        //8 byte ?
        fileContent.getInt();
        fileContent.getInt();
        
        Wgs84Position waypoint = null;
        int countBlock_04 = 0;
        while (fileContent.position() < trackPointEndPosition){ 
            int blockType = fileContent.getInt();
            if (blockType == 4){
                waypoint = readBlocktype_04(fileContent, waypoint, countBlock_04++);
                //Nur die ersten beiden Blöcke lesen. Danach kommt nur noch Bundesland, Land und unbekanntes
                if (countBlock_04 > 2)
                    fileContent.position(trackPointEndPosition);
            }
            else if (blockType == 0){
                //?? Immer am Ende des Bereichs -> alles überspringen.
                //is unterschiedlich lang
                fileContent.position(trackPointEndPosition);
            }
        }
        return waypoint;
    }
    
    protected Wgs84Position readBlocktype_04(ByteBuffer byteBuffer, 
                                         Wgs84Position positionPoint,
                                         int segmentCount){
        /*
        4 byte int 04 00 00 00
        8 byte int als Länge diese nicht mitzählen
        4 byte int Länge des Textes. Wenn 0, dann folgende trotzdem 4 Bytes. 
        n byte Text
        manchmal hier schon zu Ende.
        4 byte (int) bisher gefunden: 0, 1. manchmal auch nur 3 byte??
        8 byte breite double
        8 byte länge double
               
        4 byte int bisher gefunden 2, 3, 
        
        4 byte int abhängig folgt bis zur Gesamtlänge - 8
            0x0: 4 byte int dann:
                 00 7B A4 3F: 
                    4 byte textlänge
                    n byte Text
                 05 00 00 00: 
                  5 byte text
            0x2: 8 byte ?
            0x7:    
            0x9: 4 byte Textlänge
               n byte Text  
            0x8: 4 byte Textlänge
               n byte Text            
            0x32: es folgt 
               4 byte Textlänge 
               n byte PLZ
            0x3C: es folgt
               4 byte Textlänge
               n byte Text
               4 byte 
               4 byte Textlänge
               n byte Text
               4 byte Textlänge
               n byte Text              
               
        8 byte ?
        */
        double longitude = 0;
        double latitude = 0;
        String waypointDescription = null;
        
        
        long blockLength = byteBuffer.getLong();
        int startPosition = byteBuffer.position();
        
        int firstTextLen = byteBuffer.getInt();
        
        waypointDescription = getText(byteBuffer, firstTextLen);

        //unknown 4 byte
        if (byteBuffer.position() < startPosition + blockLength - 8){
            byteBuffer.getInt(); // 0 oder 1
        }
        
        if (byteBuffer.position() < startPosition + blockLength - 8){
            longitude = byteBuffer.getDouble();
            latitude = byteBuffer.getDouble();
        
            //unknow 4 byte
            byteBuffer.getInt();
        }
        
        while (byteBuffer.position() < startPosition + blockLength - 8){
            int dataType = byteBuffer.getInt();
            switch (dataType){
                case 0x0:
                    if (byteBuffer.position() < startPosition + blockLength - 8){
                        int textLen = byteBuffer.getInt();
                        if (textLen > 0xFFFF)
                            textLen = byteBuffer.getInt();
                        getText(byteBuffer, textLen);
                    }
                    break;
                case 0x2:
                    //8 byte ??
                    byteBuffer.getLong();
                    break;
                case 0x5: 
                    //unkown 5 bytes
                    for (int i = 0; i < 5; i++)
                        byteBuffer.get();
                    break;
                case 0x8:
                    getText(byteBuffer);
                    break;
                case 0x9: 
                    getText(byteBuffer);
                    break;
                case 0x32: //=50
                    getText(byteBuffer); //PLZ
                    break;
                case 0x3C: //=60
                    getText(byteBuffer);
                    byteBuffer.getInt();
                    getText(byteBuffer);
                    getText(byteBuffer);
                    break;
            }
        }
        
        //2x4 byte unknown
        byteBuffer.getInt();
        byteBuffer.getInt();
        
        Wgs84Position resultPoint;
        if (positionPoint == null){
            resultPoint = new Wgs84Position(new Double(longitude), new Double(latitude), 
                                            null, null, null, waypointDescription);
        }
        else if ((segmentCount == 1) && (! waypointDescription.equals(positionPoint.getComment()))){
            resultPoint = positionPoint;
            resultPoint.setComment(waypointDescription + ' ' + resultPoint.getComment());
        }
        else
            resultPoint = positionPoint;
            
        return resultPoint;
    }

    @Override
    public void write(Wgs84Route route, PrintWriter writer, int startIndex,
                      int endIndex) throws IOException {
        throw new UnsupportedOperationException();        
    }
}
