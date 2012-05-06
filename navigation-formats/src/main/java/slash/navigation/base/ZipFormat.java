package slash.navigation.base;

import slash.common.io.CompactCalendar;
import slash.common.io.Files;
import slash.common.io.NotClosingUnderlyingInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;

public class ZipFormat extends BaseNavigationFormat<BaseRoute> {
    private static final Logger log = Logger.getLogger(ZipFormat.class.getName());
    static {
        System.setProperty("sun.zip.encoding", "default");
    }

    public String getName() {
        return "ZIP Archive (" + getExtension() + ")";
    }

    public String getExtension() {
        return ".zip";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        throw new UnsupportedOperationException();
    }

    public boolean isWritingRouteCharacteristics() {
        throw new UnsupportedOperationException();
    }

    public <P extends BaseNavigationPosition> BaseRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<BaseRoute> parserContext) throws Exception {
        ZipInputStream zip = new ZipInputStream(source);
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                int size = (int) entry.getSize() + 1;
                NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(zip, size));
                buffer.mark(size);
                List<NavigationFormat> formats = getReadFormatsPreferredByExtension(Files.getExtension(entry.getName()));
                parserContext.parse(buffer, size, startDate, formats);
                zip.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.fine("Error reading invalid zip entry from " + source + ": " + e.getMessage());
            // TODO parserContext.addException();
        } finally {
            try {
                zip.close();
            } catch (IOException e) {
                log.fine("Error closing zip from " + source + ": " + e.getMessage());
            }
        }
    }

    public void write(BaseRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
