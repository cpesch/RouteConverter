package slash.navigation.brouter;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.download.Checksum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;

public class BRouterSegmentsRequireDownloadTest {
    private static final long ONE_DAY = 24L * 60 * 60 * 1000;
    private static final CompactCalendar NOW = fromMillis(100L * ONE_DAY);

    private static Checksum checksum(long daysOld) {
        return new Checksum(fromMillis(NOW.getTimeInMillis() - daysOld * ONE_DAY), 10L, "sha1");
    }

    @Test
    public void emptyListDoesNotNeedDownload() {
        assertFalse(BRouter.segmentsRequireDownload(Collections.emptyList(), NOW, 7));
    }

    @Test
    public void listContainingNullNeedsDownload() {
        assertTrue(BRouter.segmentsRequireDownload(Arrays.asList(checksum(1), null), NOW, 7));
    }

    @Test
    public void singleTileOneDayOldWithMaxAgeSevenDoesNotNeedDownload() {
        assertFalse(BRouter.segmentsRequireDownload(Collections.singletonList(checksum(1)), NOW, 7));
    }

    @Test
    public void singleTileEightDaysOldWithMaxAgeSevenNeedsDownload() {
        assertTrue(BRouter.segmentsRequireDownload(Collections.singletonList(checksum(8)), NOW, 7));
    }

    @Test
    public void singleTileExactlySevenDaysOldWithMaxAgeSevenDoesNotNeedDownload() {
        assertFalse(BRouter.segmentsRequireDownload(Collections.singletonList(checksum(7)), NOW, 7));
    }

    @Test
    public void twoTilesSameDayTwoDaysOldDoesNotNeedDownload() {
        Checksum first = checksum(2);
        Checksum second = new Checksum(fromMillis(first.getLastModified().getTimeInMillis() + 1000L), 10L, "sha1");
        assertFalse(BRouter.segmentsRequireDownload(Arrays.asList(first, second), NOW, 7));
    }

    @Test
    public void twoTilesDifferentDaysBothFreshNeedsDownload() {
        assertTrue(BRouter.segmentsRequireDownload(Arrays.asList(checksum(0), checksum(1)), NOW, 7));
    }

    @Test
    public void singleTile100DaysOldWithMaxAgeZeroDoesNotNeedDownload() {
        assertFalse(BRouter.segmentsRequireDownload(Collections.singletonList(checksum(100)), NOW, 0));
    }

    @Test
    public void singleTile100DaysOldWithMaxAgeNegativeDoesNotNeedDownload() {
        assertFalse(BRouter.segmentsRequireDownload(Collections.singletonList(checksum(100)), NOW, -1));
    }

    @Test
    public void tileWithNullLastModifiedNeedsDownload() {
        Checksum noLastModified = new Checksum(null, 10L, "sha1");
        assertTrue(BRouter.segmentsRequireDownload(Collections.singletonList(noLastModified), NOW, 7));
    }
}
