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

package slash.navigation.elevation;

import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;

import java.io.IOException;
import java.util.List;

/**
 * Interface for a service that looks up the elevation for a given position.
 *
 * @author Christian Pesch
 */

public interface ElevationService {
    String getName();
    boolean isDownload();
    boolean isOverQueryLimit();
    String getPath();
    void setPath(String path);

    Double getElevationFor(double longitude, double latitude) throws IOException;

    void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload);
    long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes);
    void downloadElevationData(List<BoundingBox> boundingBoxes);
}
