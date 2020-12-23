package slash.navigation.maps.mapsforge.impl;

import org.mapsforge.map.reader.MapFile;

import java.io.File;

public class NotClosingMapFile extends MapFile {
    public NotClosingMapFile(File mapFile) {
        super(mapFile);
    }

    public void close() {
        // intentionally overwritten to allow for lazy initialization and a single initialization
        // together with MapsforgeFileMap
    }

    public void destroy() {
        super.close();
    }
}
