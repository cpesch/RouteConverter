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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Finds {@link Downloadable}s for {@link BoundingBox}es from a given {@link DataSource}
 * and prefers {@link Downloadable}s that are already present.
 *
 * @author Christian Pesch
 */

class DownloadableFinder {
    private static final Logger log = Logger.getLogger(DownloadableFinder.class.getName());
    private final GraphManager graphManager;

    public DownloadableFinder(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    private List<GraphDescriptor> getGraphDescriptorsFor(MapDescriptor mapDescriptor) {
        List<GraphDescriptor> localDescriptors = graphManager.getLocalGraphDescriptors().stream()
                .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                .collect(toList());
        List<GraphDescriptor> remoteDescriptors = graphManager.getRemoteGraphDescriptors().stream()
                .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                .filter(GraphDescriptor::hasValidBoundingBox)
                .sorted(new GraphDescriptorComparator())
                .collect(toList());
        // if there is no other choice use the graphs with the invalid bounding boxes
        if(remoteDescriptors.isEmpty())
            remoteDescriptors = graphManager.getRemoteGraphDescriptors().stream()
                    .filter(graphDescriptor -> graphDescriptor.matches(mapDescriptor))
                    .sorted(new GraphDescriptorComparator())
                    .toList();

        List<GraphDescriptor> result = new ArrayList<>();
        result.addAll(localDescriptors);
        result.addAll(remoteDescriptors);
        return result;
    }

    List<GraphDescriptor> getGraphDescriptorsFor(Collection<MapDescriptor> mapDescriptors) {
        Set<GraphDescriptor> graphDescriptors = mapDescriptors.stream()
                .flatMap(mapDescriptor -> getGraphDescriptorsFor(mapDescriptor).stream())
                .collect(toSet());
        List<GraphDescriptor> result = new ArrayList<>(graphDescriptors.stream().toList());
        result.sort((d1, d2) -> {
            // prefer local files over downloads
            if(d1.getLocalFile() != null && d2.getLocalFile() == null)
                return -1;
            if(d1.getLocalFile() == null && d2.getLocalFile() != null)
                return 1;

            if(d1.equals(d2))
                return 0;
            if(!d1.hasValidBoundingBox())
                return -1;
            if(!d2.hasValidBoundingBox())
                return 1;
            return d1.getBoundingBox().contains(d2.getBoundingBox()) ? -1 : 1;
        });
        log.info(format("Found %d graph descriptors: %s for %d map descriptors: %s", result.size(), result, mapDescriptors.size(), mapDescriptors));
        return result;
    }
}
