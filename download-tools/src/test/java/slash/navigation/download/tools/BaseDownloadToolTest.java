/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.download.tools;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Unit tests for the datasources-credential handling added for GitHub #161:
 * a {@code --password-file} reader and the {@code DATASOURCES_USERNAME}/
 * {@code DATASOURCES_PASSWORD} environment-variable fallback in the setters.
 *
 * @author Christian Pesch
 */
public class BaseDownloadToolTest {

    private static File writeTemp(String content) throws IOException {
        File file = File.createTempFile("datasources-password", ".txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        file.deleteOnExit();
        return file;
    }

    @Test
    public void readsPasswordFileTrimmingTrailingNewline() throws IOException {
        File file = writeTemp("s3cret\n");
        try {
            assertEquals("s3cret", new BaseDownloadTool().readDataSourcesPasswordFile(file));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Test
    public void readsPasswordFileTrimmingSurroundingWhitespace() throws IOException {
        File file = writeTemp("  s3cret  ");
        try {
            assertEquals("s3cret", new BaseDownloadTool().readDataSourcesPasswordFile(file));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Test
    public void explicitCredentialsArePreservedAndCompleteTheServerCheck() {
        BaseDownloadTool tool = new BaseDownloadTool();
        tool.setDataSourcesServer("https://api.example.com");
        tool.setDataSourcesUserName("bob");
        tool.setDataSourcesPassword("pw");
        assertTrue(tool.hasDataSourcesServer());
    }

    @Test
    public void nullCredentialsFallBackToEnvironment() {
        // Calling the setters with null exercises the System.getenv() fallback
        // branch. In CI the DATASOURCES_* vars are unset, so the fallback resolves
        // to null and the server check stays incomplete; guarded so a dev machine
        // that happens to export them doesn't fail the assertion.
        assumeTrue(System.getenv("DATASOURCES_USERNAME") == null);
        assumeTrue(System.getenv("DATASOURCES_PASSWORD") == null);

        BaseDownloadTool tool = new BaseDownloadTool();
        tool.setDataSourcesServer("https://api.example.com");
        tool.setDataSourcesUserName(null);
        tool.setDataSourcesPassword(null);
        assertFalse(tool.hasDataSourcesServer());
    }
}
