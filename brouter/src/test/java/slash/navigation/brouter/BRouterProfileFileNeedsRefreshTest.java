package slash.navigation.brouter;

import org.junit.Test;
import slash.navigation.download.Checksum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;

public class BRouterProfileFileNeedsRefreshTest {

    private static Checksum checksum(long lastModifiedMillis, long contentLength, String sha1) {
        return new Checksum(fromMillis(lastModifiedMillis), contentLength, sha1);
    }

    @Test
    public void localMatchesLatestChecksumDoesNotNeedRefresh() {
        Checksum older = checksum(1000L, 10L, "oldsha");
        Checksum latest = checksum(2000L, 20L, "newsha");
        List<Checksum> expected = Arrays.asList(older, latest);

        assertFalse(BRouter.profileFileNeedsRefresh("newsha", 20L, expected));
    }

    @Test
    public void localMatchesOlderChecksumNeedsRefresh() {
        Checksum older = checksum(1000L, 10L, "oldsha");
        Checksum latest = checksum(2000L, 20L, "newsha");
        List<Checksum> expected = Arrays.asList(older, latest);

        assertTrue(BRouter.profileFileNeedsRefresh("oldsha", 10L, expected));
    }

    @Test
    public void emptyExpectedChecksumsDoesNotNeedRefresh() {
        assertFalse(BRouter.profileFileNeedsRefresh("anysha", 10L, Collections.emptyList()));
        assertFalse(BRouter.profileFileNeedsRefresh("anysha", 10L, null));
    }

    @Test
    public void outOfOrderChecksumsPickLatestByLastModified() {
        Checksum latest = checksum(3000L, 30L, "latestsha");
        Checksum middle = checksum(2000L, 20L, "middlesha");
        Checksum oldest = checksum(1000L, 10L, "oldestsha");
        // deliberately out of chronological order in the list
        List<Checksum> expected = Arrays.asList(middle, latest, oldest);

        assertFalse(BRouter.profileFileNeedsRefresh("latestsha", 30L, expected));
        assertTrue(BRouter.profileFileNeedsRefresh("middlesha", 20L, expected));
    }
}

