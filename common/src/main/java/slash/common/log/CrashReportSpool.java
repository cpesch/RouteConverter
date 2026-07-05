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

package slash.common.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;

/**
 * A small on-disk spool of structured crash reports. A captured crash is written
 * here as one JSON file before the report dialog is offered, so a crash before the
 * dialog can be shown (or before login) is not lost and can be offered on the next
 * successful launch. The spool is capped so it cannot grow without bound; the oldest
 * files are deleted first.
 *
 * @author Christian Pesch
 */

public class CrashReportSpool {
    static final int MAXIMUM_FILES = 10;
    private static final String CRASH_REPORTS_DIRECTORY = "crash-reports";
    private static final String FILE_PREFIX = "crash-";
    private static final String FILE_SUFFIX = ".json";
    private static final AtomicLong sequence = new AtomicLong();

    private final File directory;

    public CrashReportSpool(File directory) {
        this.directory = directory;
    }

    public static CrashReportSpool createDefault() {
        return new CrashReportSpool(new File(getTemporaryDirectory(), CRASH_REPORTS_DIRECTORY));
    }

    public File write(String json) throws IOException {
        ensureDirectory(directory);
        String name = FILE_PREFIX + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date()) +
                String.format("-%04d", sequence.incrementAndGet() % 10000) + FILE_SUFFIX;
        File file = new File(directory, name);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8)) {
            writer.write(json);
        }
        cap();
        return file;
    }

    public List<File> list() {
        File[] files = directory.listFiles((dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
        if (files == null)
            return new ArrayList<>();
        List<File> result = new ArrayList<>(asList(files));
        result.sort(comparing(File::getName));
        return result;
    }

    public File newest() {
        List<File> files = list();
        return files.isEmpty() ? null : files.get(files.size() - 1);
    }

    public String read(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0)
                    builder.append("\n");
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public boolean delete(File file) {
        return file.delete();
    }

    void cap() {
        List<File> files = list();
        for (int i = 0; i < files.size() - MAXIMUM_FILES; i++)
            delete(files.get(i));
    }
}
