package slash.navigation.maps.mapsforge.mbtiles;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

public class RendererJob extends Job {
    private final DatabaseRenderer databaseRenderer;
    private final int hashCodeValue;

    public RendererJob(Tile tile, DatabaseRenderer databaseRenderer, boolean isTransparent) {
        super(tile, isTransparent);
        this.databaseRenderer = databaseRenderer;
        this.hashCodeValue = calculateHashCode();
    }

    public DatabaseRenderer getDatabaseRenderer() {
        return databaseRenderer;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (!(obj instanceof RendererJob)) {
            return false;
        }
        RendererJob other = (RendererJob) obj;
        if (!this.getDatabaseRenderer().equals(other.getDatabaseRenderer())) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return this.hashCodeValue;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getDatabaseRenderer().hashCode();
        return result;
    }
}
