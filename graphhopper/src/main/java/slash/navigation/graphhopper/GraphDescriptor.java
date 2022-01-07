package slash.navigation.graphhopper;

import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.File;

import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.Files.removeExtension;
import static slash.navigation.graphhopper.PbfUtil.existsGraphDirectory;
import static slash.navigation.graphhopper.PbfUtil.lookupGraphDirectory;

class GraphDescriptor {
    private static final Logger log = Logger.getLogger(GraphDescriptor.class.getName());
    private final GraphManager.GraphType graphType;
    private final java.io.File localFile;
    private final File remoteFile;
    private BoundingBox boundingBox = null;

    GraphDescriptor(GraphManager.GraphType graphType, java.io.File localFile, File remoteFile) {
        this.graphType = graphType;
        this.localFile = localFile;
        this.remoteFile = remoteFile;
    }

    private String removeMapDirectoryPrefix(String identifier) {
        int index = identifier.indexOf('/');
        return index != -1 ? identifier.substring(index + 1) : identifier;
    }

    private boolean matchesIdentifier(String identifier) {
        return remoteFile != null && removeExtension(remoteFile.getUri()).equals(identifier) ||
                localFile != null && lookupGraphDirectory(localFile).getAbsolutePath().endsWith(identifier);
    }

    private boolean matchesIdentifier(MapDescriptor mapDescriptor) {
        String identifier = removeExtension(mapDescriptor.getIdentifier());
        // try to find europe/germany from europe/germany.map, mapsforge/europe/germany.map and europe/germany.zip
        return matchesIdentifier(identifier) || matchesIdentifier(removeMapDirectoryPrefix(identifier));
    }

    private boolean matchesBoundingBox(MapDescriptor mapDescriptor) {
        BoundingBox fileBoundingBox = getBoundingBox();
        return fileBoundingBox != null && fileBoundingBox.contains(mapDescriptor.getBoundingBox());
    }

    public boolean matches(MapDescriptor mapDescriptor) {
        return matchesIdentifier(mapDescriptor) || matchesBoundingBox(mapDescriptor);
    }

    public GraphManager.GraphType getGraphType() {
        return graphType;
    }

    public java.io.File getLocalFile() {
        return localFile;
    }

    public File getRemoteFile() {
        return remoteFile;
    }

    public boolean hasGraphDirectory() {
        return graphType.equals(GraphManager.GraphType.Directory) ||
                graphType.equals(GraphManager.GraphType.PBF) && existsGraphDirectory(getLocalFile());
    }

    public boolean hasValidBoundingBox() {
        /* basically it's a good idea to choose the graph with the smallest enclosing bounding box
           unfortunately, files like https://download.geofabrik.de/north-america/us/alaska-latest.osm.pbf
           claim too large bounding boxes */
        return getBoundingBox() == null ||
                !(getBoundingBox().getNorthEast().getLongitude() >= 179.9999 ||
                        getBoundingBox().getSouthWest().getLongitude() <= -179.9999);
    }

    public BoundingBox getBoundingBox() {
        if (boundingBox == null) {
            if(remoteFile != null)
                boundingBox = remoteFile.getBoundingBox();
            else if (localFile != null && localFile.isFile()) {
                try {
                    boundingBox = PbfUtil.extractBoundingBox(localFile);
                } catch (IOException e) {
                    log.warning(format("Cannot extract bounding box from %s: %s", localFile, e.getLocalizedMessage()));
                }
            }
        }
        return boundingBox;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphDescriptor that = (GraphDescriptor) o;

        if (getGraphType() != that.getGraphType()) return false;
        if (getLocalFile() != null ? !getLocalFile().equals(that.getLocalFile()) : that.getLocalFile() != null)
            return false;
        if (getRemoteFile() != null ? !getRemoteFile().equals(that.getRemoteFile()) : that.getRemoteFile() != null)
            return false;
        return getBoundingBox() != null ? getBoundingBox().equals(that.getBoundingBox()) : that.getBoundingBox() == null;
    }

    public int hashCode() {
        int result = getGraphType().hashCode();
        result = 31 * result + (getLocalFile() != null ? getLocalFile().hashCode() : 0);
        result = 31 * result + (getRemoteFile() != null ? getRemoteFile().hashCode() : 0);
        result = 31 * result + (getBoundingBox() != null ? getBoundingBox().hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[graphType=" + graphType +
                ", localFile=" + localFile +
                ", remoteFile=" + remoteFile +
                ", boundingBox=" + boundingBox + "]";
    }
}
