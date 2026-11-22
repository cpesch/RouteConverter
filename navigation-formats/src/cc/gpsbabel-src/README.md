# GPSBabel source, vendored for the native-format port

These files are unmodified copies from the GPSBabel project
(https://github.com/GPSBabel/gpsbabel), licensed under the GNU General Public License,
version 2 or (at your option) any later version — see `COPYING` and each file's header.

They are **reference material only**: nothing here is compiled or packaged, and `src/cc` is outside every Maven source and resource root. They exist so the
factory builder, which has no web access, can read the C++ code it translates to Java while
RouteConverter replaces its GPSBabel-backed formats with native ones
(rc-meta spec 00083, `specs/00083-gpsbabel-replacement.md`).

Delete this directory together with the last `slash.navigation.babel.*Format` class.

| Directory | GPSBabel ref | Files | Used by |
|-----------|--------------|-------|---------|
| `master-9c1e336/` | master 9c1e336 (2026-11-12) | `geo.cc/.h`, `style/garmin_poi.style`, `ozi.cc/.h`, `igc.cc/.h`, `jeeps/gpsdatum.h`, `jeeps/gpsmath.cc`, `mtk_logger.cc/.h` | LOC, Garmin POI DB, OziExplorer, IGC, datum conversion, Holux M-241 and MTK logger binary (file-reading path only) |
| `1.8.0/` | tag `gpsbabel_1_8_0` | `tef_xml.cc`, `tomtom.cc`, `igo8.cc` | TEF, TomTom OV2, iGO8 (removed upstream in 1.9.0) |
| `1.7.0/` | tag `gpsbabel_1_7_0` | `pcx.cc`, `compegps.cc`, `csv_util.cc` (`csv_lineparse`, `human_to_dec`), `util.cc` (`si_round`, `month_lookup`, `ddmm2degrees`, `degrees2ddmm`), `mapsend.cc/.h`, `alan.cc` | Garmin PCX5, CompeGPS, Magellan MapSend, Alan Map 500 (removed upstream in 1.8.0) |
| `1.5.4/` | tag `gpsbabel_1_5_4` | `tef_xml.cc` | TEF `fix_notes()` as run by GPSBabel 1.5.4, the behaviour oracle |

Translated Java classes carry the copyright lines of the file they come from plus
`Translated to Java from GPSBabel <file> (<ref>) for RouteConverter, <date>; modified.`
