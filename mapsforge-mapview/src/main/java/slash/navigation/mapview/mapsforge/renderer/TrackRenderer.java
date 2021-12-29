package slash.navigation.mapview.mapsforge.renderer;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.lines.Line;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.navigation.mapview.MapViewConstants.TRACK_LINE_WIDTH_PREFERENCE;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asRGBA;

/**
 * Renders a track for a {@link List} of {@link PairWithLayer} for the {@link MapsforgeMapView}.
 *
 * @author Christian Pesch
 */

public class TrackRenderer {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);

    private final MapsforgeMapView mapView;
    private final ColorModel trackColorModel;
    private final GraphicFactory graphicFactory;

    public TrackRenderer(MapsforgeMapView mapView, ColorModel trackColorModel, GraphicFactory graphicFactory) {
        this.mapView = mapView;
        this.trackColorModel = trackColorModel;
        this.graphicFactory = graphicFactory;
    }

    public synchronized void renderTrack(List<PairWithLayer> pairWithLayers, final Runnable invokeAfterRenderingRunnable) {
        try {
            drawTrack(pairWithLayers);
        }
        finally {
            invokeAfterRenderingRunnable.run();
        }
    }

    private void drawTrack(List<PairWithLayer> pairWithLayers) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(asRGBA(trackColorModel));
        paint.setStrokeWidth(preferences.getInt(TRACK_LINE_WIDTH_PREFERENCE, 2));
        int tileSize = mapView.getTileSize();

        List<PairWithLayer> withLayers = new ArrayList<>();
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            if (!pairWithLayer.hasCoordinates())
                continue;

            Line line = new Line(mapView.asLatLong(pairWithLayer.getFirst()), mapView.asLatLong(pairWithLayer.getSecond()), paint, tileSize);
            pairWithLayer.setLayer(line);
            withLayers.add(pairWithLayer);

            Double distance = pairWithLayer.getFirst().calculateDistance(pairWithLayer.getSecond());
            Long time = pairWithLayer.getFirst().calculateTime(pairWithLayer.getSecond());
            pairWithLayer.setDistanceAndTime(new DistanceAndTime(distance, time));
        }
        mapView.addObjectsWithLayer(withLayers);
    }
}
