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

package slash.navigation.photo;

import org.apache.commons.imaging.common.RationalNumber;
import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.io.File;

/**
 * A position from Photo (.jpg) files with embedded EXIF metadata.
 */

public class PhotoPosition extends Wgs84Position {
    private TagState tagState;
    private NavigationPosition closestPositionForTagging;
    private String make, model;
    private RationalNumber exposure, fNumber, focal;
    private Integer width, height, flash, photographicSensitivity;

    public PhotoPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time,
                         String description, Object origin,

                         TagState tagState, NavigationPosition closestPositionForTagging,

                         String make, String model, Integer width, Integer height, RationalNumber fNumber,
                         RationalNumber exposure, Integer flash, RationalNumber focal) {
        super(longitude, latitude, elevation, speed, time, description, origin);
        this.tagState = tagState;
        this.closestPositionForTagging = closestPositionForTagging;
        this.make = make;
        this.model = model;
        this.width = width;
        this.height = height;
        this.fNumber = fNumber;
        this.exposure = exposure;
        this.flash = flash;
        this.focal = focal;
    }

    public PhotoPosition(TagState tagState, CompactCalendar time, String description, File file) {
        this(null, null, null, null, time, description, file, tagState, null, null, null, null, null, null, null, null, null);
    }

    public NavigationPosition getClosestPositionForTagging() {
        return closestPositionForTagging;
    }

    public void setClosestPositionForTagging(NavigationPosition closestPositionForTagging) {
        this.closestPositionForTagging = closestPositionForTagging;
    }

    public TagState getTagState() {
        return tagState;
    }

    public void setTagState(TagState tagState) {
        this.tagState = tagState;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public RationalNumber getfNumber() {
        return fNumber;
    }

    public void setfNumber(RationalNumber fNumber) {
        this.fNumber = fNumber;
    }

    public RationalNumber getExposure() {
        return exposure;
    }

    public void setExposure(RationalNumber exposure) {
        this.exposure = exposure;
    }

    public Integer getFlash() {
        return flash;
    }

    public void setFlash(Integer flash) {
        this.flash = flash;
    }

    public RationalNumber getFocal() {
        return focal;
    }

    public void setFocal(RationalNumber focal) {
        this.focal = focal;
    }

    public Integer getPhotographicSensitivity() {
        return photographicSensitivity;
    }

    public void setPhotographicSensitivity(Integer photographicSensitivity) {
        this.photographicSensitivity = photographicSensitivity;
    }
}
