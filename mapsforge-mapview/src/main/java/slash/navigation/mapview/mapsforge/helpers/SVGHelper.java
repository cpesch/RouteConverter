package slash.navigation.mapview.mapsforge.helpers;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import org.mapsforge.core.graphics.GraphicUtils;

import java.awt.image.BufferedImage;
import java.io.Reader;
import java.net.URI;

import static com.kitfox.svg.app.beans.SVGIcon.AUTOSIZE_STRETCH;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Provides SVG helpers.
 *
 * @author Christian Pesch
 */

public class SVGHelper {
    public static BufferedImage getResourceBitmap(Reader reader, String name, float scaleFactor, float defaultSize, int width, int height, int percent) {
        SVGUniverse universe = SVGCache.getSVGUniverse();
        URI uri = universe.loadSVG(reader, name);
        SVGDiagram diagram = universe.getDiagram(uri);

        double scale = scaleFactor / Math.sqrt((diagram.getHeight() * diagram.getWidth()) / defaultSize);
        float[] bmpSize = GraphicUtils.imageSize(diagram.getWidth(), diagram.getHeight(), (float) scale, width, height, percent);

        SVGIcon icon = new SVGIcon();
        icon.setAntiAlias(true);
        icon.setAutosize(AUTOSIZE_STRETCH);
        icon.setPreferredSize(new java.awt.Dimension((int) bmpSize[0], (int) bmpSize[1]));
        icon.setSvgURI(uri);

        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);
        icon.paintIcon(null, bufferedImage.createGraphics(), 0, 0);
        return bufferedImage;
    }
}
