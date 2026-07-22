package slash.navigation.common;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PendingOpenUrlsTest {
    private static URL url(String path) {
        try {
            return new URL("file:///" + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void bufferedBeforeReleaseIsReturnedInArrivalOrder() {
        PendingOpenUrls pending = new PendingOpenUrls();
        assertTrue(pending.addOrDefer(asList(url("a.gpx"), url("b.gpx"))));

        List<URL> result = pending.release(emptyList());

        assertEquals(asList(url("a.gpx"), url("b.gpx")), result);
    }

    @Test
    public void releaseMergesInitialArgsBeforeBufferedAndDedupes() {
        PendingOpenUrls pending = new PendingOpenUrls();
        assertTrue(pending.addOrDefer(asList(url("shared.gpx"), url("odoc.gpx"))));

        List<URL> result = pending.release(asList(url("argv.gpx"), url("shared.gpx")));

        assertEquals(asList(url("argv.gpx"), url("shared.gpx"), url("odoc.gpx")), result);
    }

    @Test
    public void releaseWithNothingBufferedAndNoInitialArgsReturnsEmptyList() {
        PendingOpenUrls pending = new PendingOpenUrls();

        List<URL> result = pending.release(emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void addOrDeferAfterReleaseReturnsFalseAndDoesNotBuffer() {
        PendingOpenUrls pending = new PendingOpenUrls();
        pending.release(emptyList());

        assertFalse(pending.addOrDefer(asList(url("late.gpx"))));

        // a subsequent release must not resurrect the "late" url, proving it was not buffered
        assertTrue(pending.release(emptyList()).isEmpty());
    }

    @Test
    public void twoAddOrDeferCallsBeforeReleaseAccumulateInOrder() {
        PendingOpenUrls pending = new PendingOpenUrls();
        assertTrue(pending.addOrDefer(asList(url("first.gpx"))));
        assertTrue(pending.addOrDefer(asList(url("second.gpx"))));

        List<URL> result = pending.release(emptyList());

        assertEquals(asList(url("first.gpx"), url("second.gpx")), result);
    }
}
