# Release notes

## Next release

### New features

- Garmin POI (.gpi) files: the position list now takes the name of the POI collection stored in the file, for example BMW_Motorrad_v18, and saving a position list as .gpi stores its name there
- Google Maps saved lists (Favorites, Want to go, own lists) exported by Google Takeout as CSV can now be opened, also from German Takeout exports, as can the nearby places you follow (with their address, which helps when adding coordinates); they carry no coordinates, so add them with Position > Complete… > Coordinates (places whose link has coordinates get them right away)
- The offline map themes Biker, Dark, Indigo and Motorider that come with the map renderer can now be chosen next to Default and Osmarender
- Google Maps commute routes exported by Google Takeout (Commute routes.json, Routen für Pendelstrecke.json) can now be opened: their start and destination places become a waypoint list
- Offline MBTiles maps whose tiles are stored as WebP images can now be displayed; before, only PNG and JPEG MBTiles opened
- Position list > Manage… reorders, creates, renames, deletes and changes the type of the position lists of a file, for example the routes and tracks of a GPX file; it replaces the New, Rename and Delete buttons below the position list; the position list, its type and the Manage button now share one row, the map toggle moved to the Format row, and the Content line moved into the tool tip of the position list (#400)
- Tour Exchange Format (.tef) files can now be written. No program is known to import them; for Map&Guide use the BCR format (#386)
- MTK logger binary files (.bin) from iBlue 747, Qstarz BT-Q1000, Transystem and other MediaTek-based GPS loggers can be read
- Online maps and overlays show the attribution supplied by the map catalog, so new map providers can name their exact terms without a RouteConverter update; applied overlays now show their attribution next to the map's (#438)
- Pasting or opening a Google Maps directions link now imports the coordinates of every stop, any via-points dragged onto the route, and names the route after the travel mode shown in Google Maps, for example "Hamburg to Hannover (bicycle)" (#439)
- Obfuscated Garmin POI (.gpi) files, as commercial POI packages often come, can now be read; GPSBabel could not read them (#453)
- Garmin POI (.gpi) files in other code pages than Western European, for example Central European, Cyrillic, Greek or UTF-8, can now be read; GPSBabel refused them (#453)
- Pasting or opening a Google Maps shared list link (maps.app.goo.gl or the full placelists/list link) now imports it as a waypoint list, one waypoint per place, named after the list (#445)
- Garmin POI (.gpi) files: the address, phone number, e-mail and web address of a POI are now read into its description; before, they were lost on the way through GPSBabel
- Garmin POI (.gpi) files are read more leniently: Thai, Japanese, Chinese, Korean, Vietnamese and ASCII code pages and an unknown code page no longer decline the file, a string with the preferred language of the user is chosen from multi-language texts, texts of 32 KB and more, empty texts and description records with a plain text or a web address are read, and a broken POI or a file that ends early costs only that POI, not the whole file

### Changes

- Only Internet Shortcuts (.url files with an [InternetShortcut] section) are followed to the web address they point to, and only http and https ones; links in emails, README files and other texts are no longer followed, so reading or probing a file does not access the network any more. Followed addresses, as in KML network links, time out after 10 s to connect and 30 s without data, and are not read beyond 64 MB
- After reading a ZIP archive, the log lists the files and formats that took longest to read, to find out what makes large archives such as Google Takeout exports slow
- Reading a large ZIP archive such as a Google Takeout export is faster: the CSV formats give up after the header line when it names no position columns, instead of parsing every row of every health-data CSV
- Reading a large ZIP archive such as a Google Takeout export is faster: inside ZIP archives and when converting from the command line, files with an extension that names their content (.csv, .json, .gpx, .kml, .kmz, .tcx, .fit and photos as .jpg, .jpeg, .png, .heic) are only read as that kind of file and no longer tried as every other format; a misnamed file, for example a GPX file named .csv, may now be read as an empty list or not recognized there. Opening a file in the program, drag and drop and recent files still try all formats
- Position lists read from a file inside a ZIP archive are named after that file when it has no name of its own, for example gps_location_2024-01-09 in a Google Takeout export
- RouteConverter now runs on Java 25: the Windows, macOS, Debian and Flatpak downloads bundle it; the Linux jar needs Java 25 or later installed and says so on an older Java (#167)
- Geocaching.com/EasyGPS (.loc) files are read and written by RouteConverter itself and no longer need GPSBabel (#384)
- Garmin POI Database (.xcsv) files are read and written by RouteConverter itself and no longer need GPSBabel (#385)
- Tour Exchange Format (.tef) files are read by RouteConverter itself and no longer need GPSBabel (#386)
- TomTom POI (.ov2) files are read and written by RouteConverter itself and no longer need GPSBabel (#387)
- iGO8 Track (.trk) files are read and written by RouteConverter itself and no longer need GPSBabel (#388)
- Garmin PCX5 (.wpt) files are read and written by RouteConverter itself and no longer need GPSBabel (#389)
- CompeGPS Data route, track and waypoint files (.rte, .trk, .wpt) are read and written by RouteConverter itself and no longer need GPSBabel (#390)
- OziExplorer route, track and waypoint files (.rte, .plt, .wpt) are read and written by RouteConverter itself and no longer need GPSBabel (#391)
- FAI/IGC Flight Recorder (.igc) files are read and written by RouteConverter itself and no longer need GPSBabel (#392)
- Alan Map 500 Tracklog (.trl) and Waypoints and Routes (.wpr) files are read and written by RouteConverter itself and no longer need GPSBabel (#432)
- Holux M-241 Binary (.bin) files are read by RouteConverter itself and no longer need GPSBabel (#433)
- Magellan MapSend (.wpt) files are read and written by RouteConverter itself and no longer need GPSBabel (#431)
- National Geographic TOPO! 3 and 4 (.tpo) files are read by RouteConverter itself and no longer need GPSBabel; the format is now listed as National Geographic Topo 3/4 (#450)
- Garmin MapSource 6.x (.gdb) files are read and written by RouteConverter itself and no longer need GPSBabel (#451)
- Garmin MapSource 5.x (.mps) files are read and written by RouteConverter itself and no longer need GPSBabel (#452)
- Garmin POI (.gpi) files are read and written by RouteConverter itself and no longer need GPSBabel (#453)
- The Delete positions dialog now shows distance, threshold and speed in the unit system chosen under Extras > Options > Display (Metric / Statute / Nautic), instead of always in meters and km/h (#398)
- Every menu entry and submenu now has an icon (#400)
- Maps and overlays from the OpenStreetMap tile servers keep their tiles on disk across restarts, in the cache folder of the application directory, and download a tile again only once it is older than 7 days, as the OpenStreetMap tile usage policy asks; before, every start downloaded all tiles again
- Opening an offline map for the first time selects a theme made for that map's provider, for example Elevate for OpenAndroMaps and the Freizeitkarte theme for Freizeitkarte maps, if it is downloaded; a theme you chose for a map or provider is kept
- Position > Complete… > Coordinates also tries the name alone when a description like "name; note" finds nothing, and requests to the public Nominatim and Photon servers are spaced one second apart, as their usage policies ask
- A Google Takeout Records.json export of several hundred MB to a few GB now opens without running out of memory, skips inaccurate Wi-Fi/cell fixes, keeps speed and heading, and splits into one track per day and phone instead of merging several phones into one zig-zag track; a Semantic Location History export now gets one named track per trip, for example "2021-01-05 10:12 CYCLING", instead of merging a whole day into one track, and a visit without a name now falls back to its address (#441)

### Fixes

- OziExplorer track, route and waypoint files saved as UTF-8 by other programs, with or without a byte order mark, are now read with their umlauts and other non-Latin characters; before, a byte order mark made the file unreadable and UTF-8 text came out garbled
- Garmin POI (.gpi) files: positions with seven decimals are written and read back unchanged, where saving and reopening could move them by up to 9 mm; a text longer than 65 535 bytes is cut instead of written as a broken text
- FAI/IGC Flight Recorder (.igc) files: damaged lines, a byte order mark, a missing A record and a damaged task declaration no longer make the whole flight unreadable; a small step back in the fix times no longer moves the rest of the flight to the next day; no-fix positions at 0/0 and fixes with impossible coordinates are skipped; a task without turn points and a second task are read
- FAI/IGC Flight Recorder (.igc) files are written with all header records the FAI specification requires and the date as HFDTEDATE, fixes are rounded to whole seconds and meters, and altitudes outside -9999 to 99999 m no longer corrupt the fix record
- Holux M-241 and MTK logger binary (.bin) files: a damaged or missing byte costs one position instead of the rest of the 64 KB block, positions with impossible coordinates are skipped, a format change at a block start without a separate marker and an erased first header are read, a log that wrapped around in overwrite mode is read from its oldest position on, logs of the iBlue 747 and other loggers set to log only positions with a fix open again, and a log that cannot be read opens with an error that says what is wrong instead of as an empty position list
- Garmin MapSource 6.x (.gdb) files: a damaged record no longer makes the whole file unreadable, the other records are read. Waypoints of older MapSource 6 files keep their time when they have a street address or a leg time, routes with avoided areas or roads keep their notes, and umlauts and the euro sign of older files are read correctly. Saving a route with two points of the same name now writes a file that can be read back; positions are rounded to the nearest unit instead of cut off, so +180° stays +180°, and no longer depend on the display precision; large files are read and written much faster. Garmin MapSource 5.x (.mps) files are saved with the same rounding
- A damaged or truncated National Geographic TOPO! (.tpo) or Magellan MapSend (.wpt) file or a damaged iGO8 (.trk) file now opens with an error that says what is wrong, instead of as an empty position list; MapSend track files report that they cannot be read yet
- A damaged or truncated National Geographic TOPO! (.tpo), Magellan MapSend (.wpt) or iGO8 (.trk) file now opens with an error that says what is wrong, instead of as an empty position list
- Garmin MapSource 5.x (.mps) files: saving waypoints or route points with the same name at different places keeps all of them, named Name.1, Name.2 and so on, instead of dropping them or moving route points onto another waypoint; track times before 1970 or after 2038 are saved without time instead of as 2106; routes and tracks without name get the same name on every save. Reading no longer gives track points without time the time of the previous point, files of MapSource 3.6 and 4.0 and waypoints with a street address are read, and a file that ends early or has a damaged record keeps the records before it; a damaged file with nothing left to read opens with an error instead of as an empty position list
- A damaged or truncated National Geographic TOPO! (.tpo), Magellan MapSend (.wpt) or iGO8 (.trk) file now opens with an error that says what is wrong, instead of as an empty position list; MapSend track files report that they cannot be read yet
- A damaged or truncated Garmin MapSource 6.x (.gdb), Garmin POI (.gpi) or FAI/IGC Flight Recorder (.igc) file of which nothing can be read now opens with an error that says what is wrong, instead of as an empty position list; .gdb files of BaseCamp and HomePort name their file format version, which RouteConverter cannot read yet
- Random data, Magellan MapSend tracks and other binary files are no longer read as dozens of garbage tracks of a Wintec WBT-202 (.tes) file, and OziExplorer Event and Point files are no longer read as waypoints with mixed-up coordinates
- TOPO! files whose compass style "Red Without Arrow" was renamed, MapSend files with other version signatures and iGO8 tracks with points far apart are now read
- Garmin PCX5 (.wpt) files: descriptions of positions less than 10° from the equator no longer lose their first letter when saved and opened again; text in the elevation column, unknown track elevations and the Garmin "no date" 31-DEC-89 are read as unknown instead of 0 m, -99999999 m or 1989; lines of unknown record types and single damaged lines no longer make the whole file unreadable, positions with impossible coordinates are skipped, and dates with four digit years and times without seconds are read; positions whose names share their first six characters get unique names when saved, as Waypoi, Wayp.1, Wayp.2
- Tour Exchange Format (.tef) files: positions without elevation are saved without one instead of at 0 m; files whose item count is wrong or missing, with empty or comma decimal altitudes, with blank lines before the XML declaration or in Windows-1252 without encoding declaration are read; items without coordinates are skipped instead of placed at 0°/0°; a damaged file keeps the items before the damage, and a file without any readable item opens with an error
- iGO8 Track (.trk) files: the name of the position list is saved as the title of the track and read back as its name; a cut off file or one with a wrong size field keeps its complete points, as with GPSBabel; positions without time continue from the time before instead of jumping back to 1970, and times before 1970 or after 2106 no longer wrap around
- Magellan MapSend track files are read again as a track, as they were with GPSBabel, instead of as garbage tracks of another format; track points with impossible coordinates are skipped and impossible times are left empty
- Alan Map 500 files: waypoints with impossible coordinates are skipped instead of placed far off the map, impossible dates are read as no time instead of the year -1 or 30000, an unknown track elevation stays unknown instead of reading as -0.2 m, elevations above 6553 m and speeds above 163 km/h are saved as unknown instead of as wrong values, and saving a track of more than 2500 points splits it instead of failing
- A damaged National Geographic TOPO! (.tpo) file now opens with everything that is still readable, instead of not at all; a huge track is skipped instead of running out of memory, and symbols are named after their symbol style, for example Yellow Diamond, instead of SYM 1
- A Navigon Mobile Navigator link in a file saved as Unicode (UTF-16) text, as Outlook and Word save it, is now read; before, the file came out as garbage tracks
- ZIP archives written as a stream, such as Google Takeout exports, now open completely: files larger than 8 KB inside them were skipped; a damaged file inside a ZIP archive no longer ends reading the others, a partly downloaded archive is read up to where it breaks off, and files larger than 64 MB inside are skipped
- Photos in a Google Takeout ZIP archive are now placed from the JSON file Google writes next to each photo (IMG_1.jpg.json), since Google removes the GPS data from most photos; photos without any location no longer show up as position lists without coordinates
- A ZIP archive with several KML files no longer loses the positions of the first file and shows those of a later file twice
- A ZIP archive whose files all have the same format is now shown and saved in that format, for example Garmin POI (.gpi) or KML, instead of always as GPX 1.1; archives with mixed formats still use GPX 1.1
- A ZIP archive whose settings or README files carry no positions, such as a Google Takeout export, is now shown in the format of the files that do, instead of as GPX 1.1; a ZIP archive without any positions is reported as not readable instead of opening as an empty position list in a random format
- An offline MBTiles map that RouteConverter cannot display, such as one with vector tiles, no longer stays locked on Windows until RouteConverter exits
- Google Takeout saved places and reviews without location information no longer show up as positions at 0°/0° in the Atlantic when opened as GeoJSON; they come in as positions without coordinates (#440)
- CSV files whose column names are written in lower case, such as timestamp, latitude, longitude and altitude in the GPS location files of Fitbit and Google Health in Google Takeout, are now read with their coordinates, elevation and time; times like 2024-01-09T09:14:11Z are understood
- Google Takeout reviews (Reviews.json / Bewertungen.json) now show their star rating and review text in the description when opened as GeoJSON (#440)
- Formats read and written with GPSBabel work again when no GPSBabel path is set under Extras > Options: on Java 25 RouteConverter took its working directory for the GPSBabel program. (#419)
- OziExplorer route and waypoint files whose lines are padded with commas, as spreadsheet exports write them, and OziExplorer CE Route2 (.rt2) files are read again
- OziExplorer (.plt, .rte, .wpt, .rt2) and CompeGPS Data files with a datum under the name OziExplorer uses, for example Ord Srvy Grt Britn, Tokyo or Pulkovo 1942 (1), are now converted to WGS 84; files with a datum RouteConverter does not know open with WGS 84 coordinates instead of not at all. Positions near the poles and the date line no longer jump far away
- OziExplorer files: garbled, empty and out-of-range lines no longer add positions at 0°/0° or beyond the poles, absurd times and altitudes are dropped, times before 1970 are kept, files with old Mac line endings or an altitude unit other than feet or meters are read, and commas in names and descriptions are kept by writing and reading character 209 (Ñ), as OziExplorer does. Tracks are saved with altitudes to a tenth of a foot and without the name ComplimentsOfGPSBabel when they have none
- CompeGPS Data files: waypoint (.wpt) and route (.rte) files are saved with waypoints and route as CompeGPS and TwoNav expect them instead of as a track, so names and descriptions survive; an unknown altitude is saved as -9999 as TwoNav writes it. Reading no longer gives up or invents dates on odd or damaged dates and times, the placeholder dates of CompeGPS, TwoNav and Garmin are read as "no time", a waypoint named A or with blanks in its name is read correctly, blanks written as _ by TwoNav come back as blanks, and lines without coordinates are skipped instead of becoming positions at 0°/0°
- Top and Bottom in the positions table keep the moved positions selected (#403)
- Dragging positions to another row of the positions table no longer mixes up the order of the positions in between (#401)
- Switching a map overlay on or off while the map is redrawing no longer stops the map from drawing
- Online maps and overlays no longer show up twice in Karten > Online when a leftover file sits next to the downloaded map catalog (#395)
- RouteConverter started from the Debian package now shows the startup splash screen, like the Linux jar does (#394)
- The map selector and Karten > Online now show the name of each online map instead of its id and list the online maps alphabetically, also those from your own userservers.xml. Once after the update, the displayed online map falls back to OpenStreetMap and the Active ticks in Karten > Online return to their defaults (#417)
- Show Map Overlays no longer offers overlays that the map catalog marks as inactive, such as the discontinued OpenStreetMap Hillshading (#426)
- Google Timeline (.json) files are read again when the Android export's first segment is a trip memory, when the file starts with a byte-order mark, and Google Takeout "Location History" exports (Records.json and Semantic Location History) are now read as well (#429)
- Updates of the offline maps from alternativaslibres.org and osm.paws.cz are recognized: the nightly map catalog update now finds a map that is not the first file in its zip and reads map headers larger than 16 KB
- Opening a text or web page file with many web links no longer takes minutes: the Google Maps URL and GeoHack URL readers search such files much faster. This also speeds up opening ZIP archives such as Google Takeout exports, where a single README took them seconds
- OziExplorer route (.rte) files now keep the description of each route point, which was lost; waypoint files cut the name and the description separately, so a long name no longer eats the description, and saving no longer shortens the descriptions in the open position list
- Names and descriptions with commas, line breaks, tabs or characters that are not valid in XML no longer break the written OziExplorer, CompeGPS, Garmin PCX5, Garmin POI Database, TomTom POI, Tour Exchange Format and Magellan MapSend files: such a character is replaced by a space, so the file reads back with all its positions
- Magellan MapSend (.wpt) files whose names contain a NUL character are read up to that character
- TomTom POI (.ov2) files: points west of Greenwich or south of the equator are saved at their exact position, where they moved about 1 m towards it; files with UTF-8 names, with a damaged or padded end and with characters that Windows-1252 leaves undefined are read, the last ones written back unchanged; characters that cannot be saved are reported in the log
- Geocaching.com/EasyGPS (.loc) files are read more leniently: files with umlauts but without encoding declaration, UTF-16 files and files with control characters open, a damaged end keeps the waypoints before it, and waypoints with broken or missing coordinates are skipped instead of ending up at 0°/0°
- Garmin POI Database (.xcsv) files are read more leniently: names and descriptions in double quotes keep their commas, lines may end with a carriage return only, and a byte order mark, a header line, comment lines starting with # or ; and Windows-1252 text are understood

## 3.8 — 2026-11-15

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.8

### Highlights (EN)

RouteConverter 3.8 gets a fresh look on Windows and Linux: a modern light or dark theme that follows your system, with sharp text, icons and maps on high-resolution displays. View > POIs shows every bank, toilet or other category of points of interest on the map, and you can hide the other position lists of a file, such as the thousands of daily tracks of a Google Timeline export.

Most important fixes:

- Excel files keep sorted, reverted and moved positions when saved
- The crashes reported most often from 3.7 are fixed
- Windows: dates and times follow your regional settings again
- macOS: large maps no longer run out of memory

### Was ist neu (DE)

RouteConverter 3.8 bekommt unter Windows und Linux ein neues Gesicht: ein modernes helles oder dunkles Design passend zur Systemeinstellung, dazu scharfe Schrift, Symbole und Karten auf hochauflösenden Bildschirmen. Unter Ansicht > POIs zeigt die Karte alle Banken, Toiletten oder andere POI-Kategorien an, und die übrigen Positionslisten einer Datei lassen sich ausblenden – etwa die tausenden Tagestracks einer Google-Zeitachse.

Die wichtigsten Fehlerbehebungen:

- Excel-Dateien behalten sortierte, umgekehrte und verschobene Positionen beim Speichern
- Die am häufigsten gemeldeten Abstürze von 3.7 sind behoben
- Windows: Datum und Uhrzeit folgen wieder den Regionseinstellungen
- macOS: Bei großen Karten geht nicht mehr der Speicher aus

### New features

- Windows and Linux: modern FlatLaf look-and-feel, light or dark following the operating system setting; macOS keeps its native look. Options > General > Look and feel switches to Light, Dark or the old System look (requires restart) (#263)
- View > POIs… shows all POIs of the chosen categories on the map, such as every bank or toilet in view, from the offline POI data or the displayed offline map; they stay while you edit the route, show their name on hover, and the map's context menu adds one to the position list (#434)
- View > Show Other Position Lists and a button next to the position list switch off drawing the file's other position lists on the map, e.g. the thousands of daily tracks of a Google Timeline export
- CSV and Excel import: a title row above the actual column headers is skipped, and a single column holding "latitude,longitude" is read when there is no separate latitude/longitude column (#318)

### Changes

- Windows with display scaling above 100 %: text and icons follow the display scale again instead of being scaled twice, and the map is sharp instead of blurred; macOS Retina and Linux HiDPI get the sharp map too. The font size setting added in 3.7 is therefore removed from Options (#442, #449)
- macOS: RouteConverter may use up to a quarter of the Mac's memory instead of a fixed 1 GB, like on Windows, so large maps and long sessions no longer run out of memory (#352)
- Windows and Linux: sharper toolbar and menu icons on scaled displays; disabled menu items show greyed icons (#284)
- Linux on a scaled GNOME or KDE desktop: menus, map and icons follow the display scale, and dark mode follows the desktop setting
- Linux: the RouteConverter jar tells which Java version to install instead of failing silently on a Java that is too old
- The bundled Java is updated to 21.0.13 and no longer trusts the eMudhra emSign and Sectigo AAA Certificate Services roots that forged certificates for any host chain up to (CVE-2026-111943)

### Fixes

- Excel (.xls/.xlsx): sorting, reverting, moving and removing positions, Save as, Split, New position list and deleting a list are saved correctly; before, the saved file could keep the old order, lose positions, bring a deleted list back, leave a position without coordinates or gain an empty column from another file (#338, #339, #340, #341)
- Windows: dates, times and numbers follow the regional settings again — the 3.7 installer bundled a Java runtime without locale data (#415, #470)
- Map and theme selection lists: typing a letter or clicking the separator line no longer crashes the app; typing a letter jumps to the next map or theme whose name starts with it (#350)
- Fast mouse-wheel zooming on the map no longer crashes the app (#321)
- A position list with a latitude beyond ±90° or a longitude beyond ±180° no longer crashes the map; such positions are drawn at the edge of the map (#354)
- Typing a longitude, latitude, elevation, speed, date or time the positions table can't understand shows a message and keeps the old value instead of crashing; a date and time without seconds such as "2026-08-18 10:00" is accepted (#294)
- Maps dialog: selecting a map with no known extent, or a `.map` file that isn't a valid offline map (e.g. a failed download), no longer crashes the app; invalid files are no longer listed (#303, #323)
- Browse: selecting a route while the route list reloads no longer crashes the app (#351)
- Pressing the context-menu key in a table before any mouse click no longer crashes the app; the menu opens below the selected cell (#324)
- Map context menu: Delete works on the point you clicked and is enabled when it applies; in 3.7 it used an earlier click point (#441)
- Save as keeps typed file names that contain a dot, and suggested file names are no longer cut at 64 characters (#287)
- Adding or moving a position on the map snaps it to a road the selected travel mode may use: with a bike profile a position on a cycleway stays there instead of jumping to the road next to it
- Find places in the downloaded offline POI data works again: it finds matches with the current POI downloads, also while an online map is displayed, and ignores the case of umlauts — "überseering" finds "Überseering" (#276, #277, #278)
- Routing: when a route segment takes too long or no route is found, a notification says how many segments failed and with which routing service; segments without a route are drawn as straight lines (#317)
- Maps and Downloads dialogs: with a sorted table, selecting, displaying, stopping, restarting or removing acts on the row you selected, not another one (#333)
- Unchanged downloads such as the map and routing catalogs are no longer downloaded again on every start (#440)
- If the online map you used last has been removed from the catalog, RouteConverter tells you once that it switched to OpenStreetMap (#320)
- Undo/Redo: clicking into a cell of the positions table and leaving it unchanged, or typing something invalid, no longer adds a step or marks the file modified (#309, #311)
- Logs, error reports and crash reports no longer contain API keys, session tokens or other credentials, including those embedded in tile or routing-service URLs (#280, #296, #305)
- Automatic geocoding no longer calls Google when no Google API key is set (#288)
- A tile server API key holding a whole URL no longer throws an error for every map tile (#295)
- Windows: Paste no longer stays disabled after positions were copied in a second RouteConverter window (#360)
- macOS: opening a route no longer fits the map one zoom level too close (#274)
- macOS: the downloaded GPSBabel is made executable; before, formats read through GPSBabel failed when no other GPSBabel was installed
- Hover coordinates on the map show at start when the option is on, not only after switching it off and on (#443)
- Login dialog: the messages say they ask for the RouteConverter service account, not the forum account, and point to the Register tab; registering requires a password of at least 8 characters, matching what the server accepts (#319)
- The bundled Java reads GPX and KML files in Japanese, Chinese, Hebrew, Arabic and Thai encodings instead of garbling their names
- Linux Flatpak: starts on Wayland desktops, keeps settings and maps across restarts, and opens help, web pages and mail in the desktop's browser and mail program

### Upgrade notes

- Options: the font size setting from 3.7 is gone; text follows the Windows display scale. If you raised the font size in 3.7, check the display scale in Windows Settings instead.
- Windows and Linux start with the new FlatLaf look; Options > General > Look and feel > System restores the previous look.

## 3.7 — 2026-10-10

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.7

### Highlights (EN)

RouteConverter 3.7 reads Google Maps Timeline exports straight from your phone and adds the RTZ route format used by ship navigation systems. GPX track segments now survive reading and writing and show up as gaps on the map and in the elevation profile, and ascent/descent totals are no longer inflated by GPS elevation noise. You can select and delete pauses, set the font size of the user interface, and see on the map where maps, routing, elevation and POI data are available. On the fix side, the app starts again on macOS 15 and earlier, starts faster on Windows, follows your regional date and time settings, and stays responsive when you select thousands of positions. New ways to install: a Debian/Ubuntu package and a Homebrew cask.

### Was ist neu (DE)

RouteConverter 3.7 liest Google-Maps-Zeitachsen direkt vom Smartphone und kennt das RTZ-Routenformat der Schiffsnavigation. Segmente in GPX-Tracks bleiben beim Lesen und Schreiben erhalten und erscheinen als Lücke auf der Karte und im Höhenprofil; GPS-Höhenrauschen bläht die Summen für Anstieg und Abstieg nicht mehr auf. Neu sind außerdem: Pausen auswählen und löschen, eine einstellbare Schriftgröße für die Oberfläche und eine Kartenansicht, die zeigt, wo Karten, Routing-, Höhen- und POI-Daten verfügbar sind. Behoben: Auf macOS 15 und älter startet die App wieder, unter Windows startet sie schneller, Datum und Uhrzeit folgen den Regionseinstellungen, und auch bei tausenden ausgewählten Positionen bleibt sie flüssig. Neu zu installieren als Debian/Ubuntu-Paket und über Homebrew.

### New features

- Google Maps Timeline: reads the on-device exports from the Google Maps app (`location-history.json` / `Timeline.json`) without dropping points (#351)
- RTZ route exchange format (IEC 61174, used by ECDIS ship navigation): read for everyone, write for sponsors
- GPX track segments are preserved when reading and writing. The map and the elevation profile leave a gap where a segment ends, and the time across the gap no longer counts toward the duration
- Select and delete pause positions (speed 0), optionally shifting the times of the following positions so the track runs on without the pauses (#333)
- Font-size setting for the user interface; on Windows the default follows the display scaling (#335, #338)
- Coverage overlay (View menu): shows on the map where maps, routing, elevation and POI data are available
- Command line: new `convert` command with explicit source/target formats, a one-line JSON result and distinct exit codes
- New ways to install: `RouteConverterLinux.deb` for Debian/Ubuntu with bundled Java, a Homebrew cask for macOS (`brew install --cask cpesch/routeconverter/routeconverter`), and a `.sha256` checksum next to every download
- The update dialog opens a release page with context instead of the raw download directory, uses buttons instead of links, nudges harder on very old versions and shows an end-of-life notice for Java 8 and RouteConverter 2.x
- Support RouteConverter: About dialog and Help menu with Donate, a gentle reminder after 100 and 500 starts, and a dialog explaining sponsor features when you try one

### Changes

- Ascent and descent totals ignore elevation changes below 5 m, which suppresses GPS noise; totals are lower than before and closer to other tools (#409)

### Fixes

- macOS: the app runs on macOS 11 and later again. 3.6 through 3.6.5 were built against macOS 26 by mistake, so on macOS 15 (Sequoia) and earlier the icon appeared crossed out and greyed and the app would not start at all (#393, thanks Edgar Kraus)
- Dates and times now follow the system's region settings instead of a 12-hour, month-first format (#415, thanks xyzlabc)
- BRouter kept using routing data downloaded once and never refreshed it, so roads that had reopened since stayed blocked (Blue Ridge Parkway); routing data older than 7 days is now downloaded again. Offline routing also skips a downloaded area that has no roads for the route (#423, thanks Kumba42)
- Windows: the program no longer unpacks 60 MB on every start, which made startup slow (#372)
- Windows Portable: the launcher could not start the program (#373)
- Selecting or deselecting many positions froze the program; drawing large tracks, routes and waypoint lists on the map is faster (#356)
- Copy and paste garbled descriptions with umlauts and other non-ASCII characters (#340)
- "Complete coordinates" overwrote positions that already had coordinates (#339)
- Microsoft Flight Simulator flight plans (.pln): altitudes are read and written in feet, as MSFS stores them. Before, a cruise waypoint at FL360 showed as 36,000 m instead of 10,973 m, and .pln files written by earlier versions carry meters in the feet field — re-export them to fix their altitudes (#417)
- OutdoorActive maps showed no tiles (#395)
- The world-map background left a grey hole after startup (#376)
- BRouter defaulted to the slow "moped" profile (#341)
- Downloads: an interrupted download that was resumed could replace a good local file with a truncated one (#383), temporary files were left behind (#342), the download table could freeze, the remaining size counted only one routing area, and a timeout was reported as "offline"
- With Arabic or Persian language settings, times were written with Eastern Arabic digits, so other programs could not read the files
- With Turkish language settings, file types were not recognized
- Some large files (for example a 6.7 MB `.hst` track) were read as empty (regression in 3.6)
- Dragging a position on the map follows the cursor again and grabs it at the pin tip
- Deleting many positions at once is much faster
- Running out of memory while inserting a route with GraphHopper is reported instead of failing silently (#337)
- Failed elevation lookups are reported instead of skipped silently, and Google is available again as elevation fallback

### Known issues

- Windows with display scaling above 100%: text and icons are not rendered at native resolution; use the new font-size setting to enlarge the text (#343)

### Downloads

| OS | File | URL |
|---|---|---|
| Windows (includes JRE) | `RouteConverterWindows.exe` | https://releases.routeconverter.com/latest/RouteConverterWindows.exe |
| Windows (Portable, includes JRE) | `RouteConverterPortable.paf.exe` | https://releases.routeconverter.com/latest/RouteConverterPortable.paf.exe |
| Linux | `RouteConverterLinux.jar` | https://releases.routeconverter.com/latest/RouteConverterLinux.jar |
| Linux (Debian/Ubuntu package, includes JRE) | `RouteConverterLinux.deb` | https://releases.routeconverter.com/latest/RouteConverterLinux.deb |
| Mac (Intel x64) | `RouteConverterMac-x64-app.zip` | https://releases.routeconverter.com/latest/RouteConverterMac-x64-app.zip |
| Mac (Apple Silicon aarch64) | `RouteConverterMac-aarch64-app.zip` | https://releases.routeconverter.com/latest/RouteConverterMac-aarch64-app.zip |
| Mac (Homebrew) | `brew install --cask cpesch/routeconverter/routeconverter` | https://github.com/cpesch/homebrew-routeconverter |
| CmdLine | `RouteConverterCmdLine.jar` | https://releases.routeconverter.com/latest/RouteConverterCmdLine.jar |
| TimeAlbumPro (Win, includes JRE) | `TimeAlbumProWindows.exe` | https://releases.routeconverter.com/latest/TimeAlbumProWindows.exe |
| TimeAlbumPro (Linux) | `TimeAlbumProLinux.jar` | https://releases.routeconverter.com/latest/TimeAlbumProLinux.jar |
| TimeAlbumPro (Mac Intel x64) | `TimeAlbumProMac-x64-app.zip` | https://releases.routeconverter.com/latest/TimeAlbumProMac-x64-app.zip |
| TimeAlbumPro (Mac Apple Silicon aarch64) | `TimeAlbumProMac-aarch64-app.zip` | https://releases.routeconverter.com/latest/TimeAlbumProMac-aarch64-app.zip |
| Archive | All artefacts for 3.7 | https://releases.routeconverter.com/previous-releases/3.7/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

Every artefact has a `<file>.sha256` checksum next to it.

### Acknowledgements

- Bug reports: Edgar Kraus, Kumba42, xyzlabc, and the forum and support threads behind #333, #335, #356, #372, #395 and #409
- Translators: the Crowdin community

## 3.6 — 2026-08-23

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.6

### Highlights (EN)

RouteConverter 3.6 adds read and write support for the Columbus Fusion track format and the Columbus GNSS GPX extensions — device-local timestamps, waypoint/POI/parking tags, 3-axis acceleration, and real fix-quality/HDOP values that feed new position-list columns (Heading, HDOP, Quality, acceleration). The Browse panel gains distance and duration columns, and every position list stays visible on the map while you edit one. The bundled JavaHelp is replaced by web-based help, and selecting redundant positions on very large tracks is now near-instant and can be cancelled.

### New features

- Added Columbus Fusion track format (Type=GNSS, GNSS with satellite info, GNSS+IMU, IMU) with device-local timestamps, waypoint/POI/parking tags, and 3-axis acceleration
- Added Columbus GNSS GPX support: course/hdop extensions, correct km/h speed for current firmware, cb: namespace extensions and standard hdop for new firmware
- Added position list columns: Heading, HDOP, Quality, acceleration (ax/ay/az) — hidden by default, enable via the column header context menu
- Added Parking count and POI count to the track description (TimeAlbum Pro)
- Elevation, heading, HDOP and acceleration are shown with two decimal places
- NMEA: GGA fix quality and HDOP are now read and written; GPX fix element and Columbus Type 1 VALID column map to the quality attribute
- Added distance and duration columns to the Browse panel for comparing routes at a glance
- All position lists stay visible on the map while you edit one: the other routes, tracks and waypoints are drawn in a subdued, read-only shade
- Added a Mapbox access token field (Options → API Keys) so the Mapbox Satellite layer works with your own token
- Replaced the bundled JavaHelp with web-based help: F1 and the "?" buttons open the matching page in your browser
- Added "Departure time" (Times menu) to fill each position's arrival time from a departure time
- Added "Insert positions" at a fixed interval along a straight line
- Added a "Download POI data" checkbox with remaining-size display and support for the newest POI catalog (v4)
- Added support for current Google Maps URL formats (place links, ?q=, short links) when importing
- Added opt-in anonymous telemetry and crash diagnostics (off by default)
- macOS: files can be opened directly with RouteConverter ("Open with")

### Fixes

- Map view (and profile view) could permanently stop updating after appending several files in quick succession; a race between the background map-update queue and newly loaded files could misclassify a pure append as a mid-list edit, desyncing the map from the position list and crashing the update worker (TimeAlbum Pro)
- Opening a file while a long route was still being routed left the map empty for minutes: the discarded route kept calculating leg after leg on the single map-update thread and the new file's rendering had to wait behind it; replacing the route now cancels the in-flight rendering at the next leg
- Opening a route with thousands of positions froze the map for minutes: every straight-line segment was added to the map as a separate AWT event with its own redraw cycle; the straight-line preview is now added in a single batch
- A waypoint list containing a single position without coordinates showed no waypoint markers at all on the map; positions without coordinates are now skipped instead of suppressing every marker
- Selecting redundant positions on a track with tens of thousands of points is now near-instant and can be cancelled (Douglas-Peucker runs off the UI thread)
- F1 over an open menu now opens the help topic of the highlighted menu item instead of the focused panel
- A background position augmentation could crash when the position list shrank mid-run (IndexOutOfBoundsException)
- The world-map background is now shown under any displayed map, not only Mapsforge
- The online map is restored correctly on startup
- Mapsforge map popups (New/Delete) now trigger on press+release and use the correct focus context
- BRouter: fixed a crash when connecting a freshly added position, an unexpected fallback travel mode, and stale segment/lookup data that did not refresh
- Legacy ANSI-encoded KML/GPX files are now decoded correctly instead of showing mojibake
- OpenStreetMap tiles now send a policy-compliant User-Agent
- The color picker no longer janks and colors can be reset to their defaults
- The Java-update dialog no longer points to an outdated version
- Updated translations (including Chinese, French and Spanish)

## 3.5 — 2026-07-03

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.5

### Highlights (EN)

RouteConverter 3.5 is a trust-and-stabilisation release. The Windows
installers and the standalone Java artifacts are now **code-signed** via the
SignPath Foundation, so Windows SmartScreen and Defender stop warning about
them. The runtime moves to **Java 21** (bundled JRE and minimum requirement),
the macOS bundle now launches cleanly on Apple Silicon, and a broad round of
download-integrity, background-map, and file-format fixes lands. No new
end-user features this time — POI lookup arrived in 3.4.

### Was ist neu (DE)

RouteConverter 3.5 ist ein Vertrauens- und Stabilisierungs-Release. Die
Windows-Installer und die eigenständigen Java-Artefakte sind jetzt über die
SignPath Foundation **signiert**, sodass Windows SmartScreen und Defender
keine Warnung mehr zeigen. Die Laufzeit wechselt auf **Java 21** (gebündelte
JRE und Mindestanforderung), das macOS-Bundle startet nun sauber auf Apple
Silicon, und zahlreiche Korrekturen bei Download-Integrität, Hintergrundkarte
und Dateiformaten kommen hinzu. Keine neuen Endnutzer-Funktionen — POI-Suche
kam mit 3.4.

### New features

- Crash / init diagnostics: the unhandled-error dialog shows the full
  root-cause chain, background-map load failures are logged with a
  startup-complete marker, and the error-report log is cleared after a
  successful send.

### Changes

- **Code signing:** Windows installers and Java artifacts are signed via the
  SignPath Foundation (Authenticode / jar signature); the certificate is held
  in SignPath's HSM and CI never holds it.
- **Java 21:** minimum runtime and bundled JRE move from 17 to 21.
- Windows installers merge the former Bundle + OpenSource builds into one
  installer per product; the standalone macOS `.jar` is no longer published.
- Download and release links now point to `releases.routeconverter.com`.
- Stripped bundled-JRE module set extended (`jdk.net` for httpclient5 5.6,
  `jdk.management`) to prevent `NoClassDefFound` on the minimised runtime.

### Fixes

- macOS: the bundle self-dequarantines so the signed JRE launches on Apple
  Silicon (no more `Killed: 9`).
- Downloads: SHA-1 is treated as authoritative and mtime is ignored when the
  SHA-1 is known; a "locally later than remote" file no longer masks a SHA-1
  mismatch; an outdated file drops its ETag so the re-download is
  unconditional (#106).
- BRouter: outdated cached `.rd5` segments are removed (lookup v11).
- Background world map installs without racing map-view creation (fixes the
  intermittent blank background map).
- KML: geometry-less placemarks are skipped (Kml21 / Kml22Beta).
- GPX: OsmAnd extensions bind as global elements and the TrekBuddy
  `ObjectFactory` is registered (fixes extension binding failures).
- Reverted geojson-jackson 3.0 → 1.14 (3.0 broke GeoJSON read/write).
- gpsbabel Extract receives its fragments; no more spurious "null bytes" log
  when the content length is unknown.

### Known issues

(None reported at release time.)

### Upgrade notes

- **Java runtime requirement is now 21 or later** (was 17). The bundled
  installer ships a JRE 21; if you run the standalone jar on your own JVM,
  upgrade to Java 21+.
- Settings + saved routes carry over from 3.4 unchanged.

### Downloads

| OS | File | URL |
|---|---|---|
| Archive | All artefacts for 3.5 | https://releases.routeconverter.com/previous-releases/3.5/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

### Acknowledgements

- Full merged-PR list: see the GitHub Release auto-notes.

## 3.4 — 2026-06-07

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.4

### Highlights (EN)

RouteConverter 3.4 brings Mapsforge POI lookup directly into the map view —
the existing Online Services panel now includes Mapsforge / OpenAndroMaps
POI databases and a Mapsforge geocoding service in FindPlace. The Find
Places dialog has been rebuilt with a sortable table, queries every
configured geocoding service in parallel, and shows the category and type
of each POI in a dedicated column. Under the hood the build moves to
Java 21 and the third-party repository now lives at
`mvn.routeconverter.com`. Plus the usual round of fixes — timezone
handling for tracks with an offset, WBT-202 tracks that start at zero
distance, stale-download avoidance, and a number of smaller UI polish
items.

### Was ist neu (DE)

RouteConverter 3.4 holt das Mapsforge-POI-Lookup direkt in die Kartenansicht:
Die Online Services nutzen jetzt Mapsforge- und OpenAndroMaps-POI-Datenbanken
sowie einen Mapsforge-Geocoding-Dienst in FindPlace. Der Find-Places-Dialog
wurde neu aufgebaut — sortierbare Tabelle, parallele Abfrage aller
konfigurierten Geocoding-Dienste, eigene Spalte für Kategorie und Typ jedes
POIs. Unter der Haube läuft der Build mit Java 21, das Drittanbieter-Repo
liegt jetzt auf `mvn.routeconverter.com`. Dazu die üblichen Korrekturen:
Zeitzonen mit Offset, WBT-202-Tracks mit Null-Distanz am Anfang,
Stale-Download-Vermeidung und einige kleinere UI-Verbesserungen.

### New features

- Mapsforge / OpenAndroMaps POI lookup, integrated into the Online Services panel.
- Read Mapsforge POI databases.
- Mapsforge geocoding service for FindPlace.
- FindPlace dialog: JTable replaces JList, queries all geocoding services
  in parallel, shows every matched position.
- POI category and type shown in a dedicated column.
- Catalog tooling driven by `datasource <source>`: `ScanWebsite`,
  `MirrorCatalog`, `CatalogMirrorApp`.

### Changes

- Third-party Maven artefacts are now served from `mvn.routeconverter.com`
  (was the legacy SVN-hosted repo).
- "Find places" opens with Cmd-F on macOS.
- Default scan directory no longer includes the directory RouteConverter
  was started from (avoids accidental large scans when launched from a
  data dir).

### Fixes

- Tracks with a non-UTC timezone offset are no longer converted to the
  wrong UTC time (#99, thanks @lundefugl).
- WBT-202 tracks that start at zero distance no longer fail validation
  (#100, thanks @kimmerin — first contribution).
- NPE when positions have no coordinates.
- Stale downloads avoided.
- Theme styles preserved across theme switching.
- Infinite loops when collecting files.
- Duplicate menu entries.
- Comparator contract violation.
- Non-visible layers filtered out.
- Files.walk() wrapped in try-with-resources.
- Java 17 Windows pipeline stabilised.

### Known issues

(None reported at release time.)

### Upgrade notes

- Java runtime requirement stays at 17 or later (bundled installer
  ships a JRE).
- Settings + saved routes carry over from 3.3 unchanged.

### Downloads

| OS | File | URL |
|---|---|---|
| Archive | All artefacts for 3.4 | https://releases.routeconverter.com/previous-releases/3.4/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

### Acknowledgements

- @lundefugl (#99) — timezone fix.
- @kimmerin (#100, first contribution) — WBT-202 zero-distance validation fix.
- Full merged-PR list: see the GitHub Release auto-notes.
