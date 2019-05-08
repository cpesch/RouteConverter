package slash.navigation.mapview.mapsforge.overlays;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.view.MapView;

import static javax.swing.JOptionPane.showMessageDialog;

public class PoiMarker extends Marker {
    private final String title;
    private final MapView mapView;

    public PoiMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset, String title, MapView mapView) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);
        this.title = title;
        this.mapView = mapView;
    }

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        // GroupLayer does not have a position, layerXY is null
        layerXY = mapView.getMapViewProjection().toPixels(getPosition());
        if (contains(layerXY, tapXY)) {
            showMessageDialog(null, title);
            return true;
        }
        return false;
    }
}
