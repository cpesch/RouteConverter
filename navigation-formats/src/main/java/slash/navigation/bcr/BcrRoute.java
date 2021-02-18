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

package slash.navigation.bcr;

import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.csv.CsvFormat;
import slash.navigation.csv.CsvPosition;
import slash.navigation.csv.CsvRoute;
import slash.navigation.excel.ExcelFormat;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.excel.ExcelRoute;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.bcr.BcrFormat.*;

/**
 * A Map &amp; Guide Tourenplaner Route (.bcr) route.
 *
 * @author Christian Pesch
 */

public class BcrRoute extends BaseRoute<BcrPosition, BcrFormat> {
    private List<BcrSection> sections;
    private List<BcrPosition> positions;

    public BcrRoute(BcrFormat format, List<BcrSection> sections, List<BcrPosition> positions) {
        super(format, Route);
        this.sections = sections;
        this.positions = positions;
    }

    public BcrRoute(BcrFormat format, String name, List<String> description, List<BcrPosition> positions) {
        this(format, new ArrayList<BcrSection>(), positions);
        sections.add(new BcrSection(CLIENT_TITLE));
        sections.add(new BcrSection(COORDINATES_TITLE));
        sections.add(new BcrSection(DESCRIPTION_TITLE));
        sections.add(new BcrSection(ROUTE_TITLE));
        setName(name);
        setDescription(description);
        findSection(CLIENT_TITLE).put("REQUEST", "TRUE");
    }

    List<BcrSection> getSections() {
        return sections;
    }

    BcrSection findSection(String title) {
        for (BcrSection section : getSections()) {
            if (title.equals(section.getTitle()))
                return section;
        }
        return null;
    }

    public String getName() {
        BcrSection section = findSection(CLIENT_TITLE);
        return section.get(ROUTE_NAME);
    }

    public void setName(String name) {
        BcrSection section = findSection(CLIENT_TITLE);
        section.put(ROUTE_NAME, name);
    }

    private int getDescriptionCount() {
        BcrSection client = findSection(CLIENT_TITLE);
        String countStr = client.get(DESCRIPTION_LINE_COUNT);
        if (countStr == null)
            return 0;
        return Integer.parseInt(countStr);
    }

    public List<String> getDescription() {
        List<String> descriptions = new ArrayList<>();
        BcrSection client = findSection(CLIENT_TITLE);
        int count = getDescriptionCount();
        for (int i = 0; i < count; i++) {
            descriptions.add(client.get(DESCRIPTION + (i + 1)));
        }
        return descriptions;
    }

    private void setDescription(List<String> description) {
        BcrSection client = findSection(CLIENT_TITLE);
        client.remove(DESCRIPTION_LINE_COUNT);

        Set<String> removeNames = new HashSet<>();
        for (String name : client.keySet()) {
            if (name.startsWith(DESCRIPTION))
                removeNames.add(name);
        }
        for (String name : removeNames) {
            client.remove(name);
        }

        if (description != null) {
            client.put(DESCRIPTION_LINE_COUNT, Integer.toString(description.size()));
            for (int i = 0; i < description.size(); i++) {
                client.put(DESCRIPTION + (i + 1), description.get(i));
            }
        }
    }

    public List<BcrPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, BcrPosition position) {
        positions.add(index, position);
    }

    public BcrPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return new BcrPosition(longitude, latitude, elevation, speed, time, description);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>(getPositions());
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected CsvRoute asCsvFormat(CsvFormat format) {
        List<CsvPosition> positions = new ArrayList<>();
        for (BcrPosition position : getPositions()) {
            positions.add(position.asCsvPosition());
        }
        return new CsvRoute(format, getName(), positions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>();
        ExcelRoute route = new ExcelRoute(format, getName(), excelPositions);
        for (BcrPosition position : getPositions()) {
            ExcelPosition excelPosition = route.createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), position.getDescription());
            excelPositions.add(excelPosition);
        }
        return route;
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<>();
        for (BcrPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<>();
        for (BcrPosition bcrPosition : positions) {
            gpxPositions.add(bcrPosition.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<>();
        for (BcrPosition bcrPosition : positions) {
            kmlPositions.add(bcrPosition.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<>();
        for (BcrPosition position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<>();
        for (BcrPosition bcrPosition : positions) {
            nmnPositions.add(bcrPosition.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), getName(), nmnPositions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<>();
        for (BcrPosition bcrPosition : positions) {
            TomTomPosition tomTomPosition = bcrPosition.asTomTomRoutePosition();
            // shortens description to better fit to Tom Tom Rider display
            String city = bcrPosition.getCity();
            String street = bcrPosition.getStreet();
            if (city != null)
                tomTomPosition.setDescription(city + (street != null && !BcrPosition.STREET_DEFINES_CENTER_SYMBOL.equals(street) ? "," + street : ""));
            tomTomPositions.add(tomTomPosition);
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BcrRoute route = (BcrRoute) o;

        return positions.equals(route.positions) && sections.equals(route.sections);
    }

    public int hashCode() {
        int result;
        result = sections.hashCode();
        result = 31 * result + positions.hashCode();
        return result;
    }
}
