package slash.navigation.mapview.vtm;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;

public class VtmMapView extends GdxMapApp {

    public void createLayers() {
        TileSource bitmap = new BitmapTileSource("https://a.tile.thunderforest.com", 2, 19);
        TileSource file = new MapFileTileSource().setOption("file",
                new File("/Users/christian/.routeconverter/maps/androidmaps/germany/hamburg.map").getAbsolutePath());

        VectorTileLayer l = mMap.setBaseMap(bitmap);
        mMap.setTheme(VtmThemes.DEFAULT);

        mMap.layers().add(new BuildingLayer(mMap, l));
        mMap.layers().add(new LabelLayer(mMap, l));

        mMap.setMapPosition(53.55, 9.99, 1 << 17);
    }

    public static void main(String[] args) {
        GdxMapApp.init();
        GdxMapApp.run(new VtmMapView());
    }
}
