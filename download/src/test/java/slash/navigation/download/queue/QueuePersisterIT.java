package slash.navigation.download.queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.Action.Flatten;
import static slash.navigation.download.State.Downloading;
import static slash.navigation.download.State.Succeeded;

public class QueuePersisterIT {
    private final QueuePersister persister = new QueuePersister();
    private File queueFile, tempFile, fileTarget, fragmentTarget1, fragmentTarget2;

    @Before
    public void setUp() throws IOException {
        queueFile = createTempFile("queueFile", ".xml");
        tempFile = createTempFile("tempFile", ".xml");
        fileTarget = createTempFile("fileTarget", ".xml");
        fragmentTarget1 = createTempFile("fragmentTarget1", ".xml");
        fragmentTarget2 = createTempFile("fragmentTarget2", ".xml");
    }

    @After
    public void tearDown() {
        if (queueFile.exists())
            assertTrue(queueFile.delete());
        if (tempFile.exists())
            assertTrue(tempFile.delete());
        if (fileTarget.exists())
            assertTrue(fileTarget.delete());
        if (fragmentTarget1.exists())
            assertTrue(fragmentTarget1.delete());
        if (fragmentTarget2.exists())
            assertTrue(fragmentTarget2.delete());
    }

    @Test
    public void testSaveAndLoadNow() throws IOException {
        persister.save(queueFile, new ArrayList<>());

        List<Download> result = persister.load(queueFile);
        assertEquals(new ArrayList<Download>(), result);
    }

    @Test
    public void testSaveAndLoadDownloads() throws IOException {
        List<Download> downloads = new ArrayList<>();
        downloads.add(new Download("description", "url", Flatten, new FileAndChecksum(fileTarget, createChecksum()),
                asList(new FileAndChecksum(fragmentTarget1, createChecksum()), new FileAndChecksum(fragmentTarget2, createChecksum())),
                "etag", Downloading, tempFile));
        persister.save(queueFile, downloads);

        List<Download> result = persister.load(queueFile);
        assertEquals(downloads, result);
    }

    @Test
    public void testSaveAndLoadSucceededCopyUsesActualChecksum() throws IOException {
        Checksum expected = new Checksum(fromMillis(1780069700000L), 22336L, "oldSha1");
        Checksum actual = new Checksum(fromMillis(1780069800000L), 54774L, "newSha1");
        FileAndChecksum fileAndChecksum = new FileAndChecksum(fileTarget, expected);
        fileAndChecksum.setActualChecksum(actual);

        List<Download> downloads = new ArrayList<>();
        downloads.add(new Download("description", "url", Copy, fileAndChecksum, null,
                "etag", Succeeded, tempFile));
        persister.save(queueFile, downloads);

        List<Download> result = persister.load(queueFile);
        Checksum persisted = result.get(0).getFile().getExpectedChecksum();
        assertEquals(actual.getLastModified().getTimeInMillis(), persisted.getLastModified().getTimeInMillis());
        assertEquals(actual.getContentLength(), persisted.getContentLength());
        assertEquals(actual.getSHA1(), persisted.getSHA1());
    }

    private Checksum createChecksum() {
        return new Checksum(now(), 4711L, "sha1");
    }
}
