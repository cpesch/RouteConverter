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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Removes distrusted CA roots from the cacerts of a jlinked JRE and gates
 * that none remain.
 *
 * eMudhra (emSign) intermediates with guessable keys (CVE-2026-111943,
 * 2026-10) let anyone mint TLS certificates for any host. Temurin's cacerts
 * still trusts their emSign roots, so the bundled JRE would accept a forged
 * certificate for api.routeconverter.com. Mozilla dropped the roots; drop
 * them here too until the Temurin build in pom.xml ships without them.
 *
 * The store is read with a null password, like the JRE's default trust
 * manager does, and written back password-less (as the JDK ships it): an
 * encrypted store would load as empty at runtime and break every TLS call.
 *
 * Usage:
 *   CacertsStrip <cacerts>            strip, rewrite, then check
 *   CacertsStrip --check <cacerts>... check only
 * Exits 1 if a distrusted root remains or a store holds suspiciously few roots.
 */
public class CacertsStrip {
    private static final String[] DISTRUSTED = {"emsign", "emudhra"};
    private static final int MINIMUM_ROOTS = 50;

    public static void main(String[] args) throws Exception {
        boolean checkOnly = args.length > 0 && args[0].equals("--check");
        int first = checkOnly ? 1 : 0;
        if (args.length <= first) {
            System.err.println("usage: CacertsStrip [--check] <cacerts>...");
            System.exit(2);
        }

        boolean failed = false;
        for (int i = first; i < args.length; i++) {
            File file = new File(args[i]);
            if (!checkOnly)
                strip(file);
            failed |= !check(file);
        }
        if (failed)
            System.exit(1);
    }

    private static KeyStore load(File file) throws Exception {
        return KeyStore.getInstance(file, (char[]) null);
    }

    private static boolean isDistrusted(KeyStore keyStore, String alias) throws Exception {
        StringBuilder text = new StringBuilder(alias);
        Certificate certificate = keyStore.getCertificate(alias);
        if (certificate instanceof X509Certificate)
            text.append(' ').append(((X509Certificate) certificate).getSubjectX500Principal().getName());
        String lower = text.toString().toLowerCase(Locale.ROOT);
        for (String distrusted : DISTRUSTED)
            if (lower.contains(distrusted))
                return true;
        return false;
    }

    private static void strip(File file) throws Exception {
        KeyStore keyStore = load(file);
        List<String> removed = new ArrayList<>();
        for (String alias : Collections.list(keyStore.aliases())) {
            if (isDistrusted(keyStore, alias)) {
                keyStore.deleteEntry(alias);
                removed.add(alias);
            }
        }
        if (removed.isEmpty()) {
            System.out.println("    OK: no distrusted roots in " + file);
            return;
        }

        System.setProperty("keystore.pkcs12.certProtectionAlgorithm", "NONE");
        System.setProperty("keystore.pkcs12.macAlgorithm", "NONE");
        try (OutputStream out = new FileOutputStream(file)) {
            keyStore.store(out, null);
        }
        for (String alias : removed)
            System.out.println("    REMOVED: " + alias);
    }

    private static boolean check(File file) throws Exception {
        KeyStore keyStore = load(file);
        List<String> remaining = new ArrayList<>();
        int roots = 0;
        for (String alias : Collections.list(keyStore.aliases())) {
            if (!keyStore.isCertificateEntry(alias))
                continue;
            roots++;
            if (isDistrusted(keyStore, alias))
                remaining.add(alias);
        }

        for (String alias : remaining)
            System.out.println("::error::distrusted root still trusted in " + file + ": " + alias);
        if (roots < MINIMUM_ROOTS)
            System.out.println("::error::" + file + " holds only " + roots + " roots, the JRE could not verify TLS");
        if (remaining.isEmpty() && roots >= MINIMUM_ROOTS)
            System.out.println("    OK: " + file + " holds " + roots + " roots, none distrusted");
        return remaining.isEmpty() && roots >= MINIMUM_ROOTS;
    }
}
