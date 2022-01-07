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
package slash.navigation.graphhopper;

import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.graphhopper.GraphManager.GraphDescriptorComparator;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Finds {@link Downloadable}s for {@link BoundingBox}es from a given {@link DataSource}
 * and prefers {@link Downloadable}s that are already present.
 *
 * @author Christian Pesch
 */

public class DownloadableFinder {
    private static final Logger log = Logger.getLogger(DownloadableFinder.class.getName());
    private final GraphManager graphManager;

    public DownloadableFinder(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    private List<GraphDescriptor> getGraphDescriptorsFor(MapDescriptor mapDescriptor) {
        List<GraphDescriptor> descriptors = graphManager.getLocalGraphDescriptors().stream()
                .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                .collect(toList());

        List<GraphDescriptor> remoteDescriptors = graphManager.getRemoteGraphDescriptors().stream()
                .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                .filter(GraphDescriptor::hasValidBoundingBox)
                .sorted(new GraphDescriptorComparator())
                .collect(toList());
        // if there is no other choice use the graphs with the invalid bounding boxes
        if(remoteDescriptors.size() == 0)
            remoteDescriptors = graphManager.getRemoteGraphDescriptors().stream()
                    .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                    .sorted(new GraphDescriptorComparator())
                    .collect(toList());
        descriptors.addAll(remoteDescriptors);
        return descriptors;
    }

    public List<GraphDescriptor> getGraphDescriptorsFor(Collection<MapDescriptor> mapDescriptors) {
        List<GraphDescriptor> result = mapDescriptors.stream()
                .flatMap(mapDescriptor -> getGraphDescriptorsFor(mapDescriptor).stream())
                .collect(toList());
        log.info(format("Found %d graph descriptors: %s for %d map descriptors: %s", result.size(), result, mapDescriptors.size(), mapDescriptors));
        return result;
    }
}
