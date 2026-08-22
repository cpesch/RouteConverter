package slash.navigation.mapview.mapsforge.helpers;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import org.mapsforge.core.graphics.GraphicUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
    public static BufferedImage getResourceBitmap(Reader reader, String name, float scaleFactor, float defaultSize, int width, int height, int percent) throws IOException {
        SVGUniverse universe = SVGCache.getSVGUniverse();
        URI uri = universe.loadSVG(reader, name);
        SVGDiagram diagram = universe.getDiagram(uri);
        if (diagram == null || diagram.getRoot() == null) {
            removeDocument(name);
            throw new IOException("Cannot load SVG " + name);
        }

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

    /**
     * Removes a document from the {@link SVGUniverse} cache, so that a later load of the same name
     * reads it again. A failed load leaves a half loaded document behind - only a SAXParseException
     * removes it - which makes every later load of that name fail, too.
     */
    public static void removeDocument(String name) {
        SVGUniverse universe = SVGCache.getSVGUniverse();
        URI uri = universe.getStreamBuiltURI(name);
        if (uri != null)
            universe.removeDocument(uri);
    }
}
