.. _examples:

Examples
========

Here are some fully worked examples for the PortableApps.com Launcher.

.. toctree::

   scribus
   7-zip

*More worked examples will be written some time. For the moment you can look at
applications which have been made portable with the PortableApps.com Launcher
and work out how they work manually.*

.. _apps-using-pal:

Apps using the PortableApps.com Launcher
========================================

When learning to use the PortableApps.com Launcher, referring to various
existing apps which already use the PortableApps.com Launcher can be very
helpful.

Here is a list of some of the apps which have been officially released at
PortableApps.com which use the PortableApps.com Launcher and notes on special
features that they use.

For the moment, this list is manually updated and a very long distance away
from being comprehensive. There are plans to automatically scrape information
about all apps and make this page a comprehensive reference to usage of
features. With only one or two exceptions, all the apps released since December
2010 (over 120 apps) use the PortableApps.com Launcher, and many of the older
apps are updated to use the PortableApps.com Launcher as new releases come out.

`AssaultCube <http://portableapps.com/apps/games/assaultcube_portable>`_
------------------------------------------------------------------------

* Running a batch file instead of an executable (including using
  :ini-key:`[Launch]:HideCommandLineWindow`)
* :ini-key:`[Launch]:LaunchAppAfterSplash`
* Waiting for multiple executables

`Audacity <http://portableapps.com/apps/music_video/audacity_portable>`_
------------------------------------------------------------------------

* Writing INI strings including using paths with double backslashes
* Updating drive letters
* Moving a directory
* Language switching including :ini-section:`[LanguageFile]` language
  preservation and :ini-section:`[LanguageStrings]` mappings
* Single portable app instance but multiple app instances

`Console <http://portableapps.com/apps/utilities/console_portable>`_
--------------------------------------------------------------------

* Updating drive letters
* Moving a file
* Setting environment variables

`Converber <http://portableapps.com/apps/utilities/converber_portable>`_
------------------------------------------------------------------------

* Moving files
* Full automatic language switching

`Finance Explorer`_
-------------------

* Moving files

.. _Finance Explorer:
   http://portableapps.com/apps/office/finance_explorer_portable

`Free UPX <http://portableapps.com/apps/utilities/free_upx_portable>`_
----------------------------------------------------------------------

* Updating drive letters
* Moving a file

`gVim <http://portableapps.com/apps/development/gvim_portable>`_
----------------------------------------------------------------

* Allowing multiple instances of portable and non-portable to mix
* Not needing to wait for the program to finish
* Command line arguments
* Environment variables
* Language switching
* Updating drive letters

IrfanView_
----------

* Allowing directory moving
* Throw-away registry keys
* Updating drive letters and configuration files

.. _IrfanView:
   http://portableapps.com/apps/graphics_pictures/irfanview_portable

`Marble <http://portableapps.com/apps/education/marble_portable>`_
------------------------------------------------------------------

* Working directory
* Registry key
* Pruning registry tree
* Moving a directory
* Qt registry key cleanup

`NSIS <http://portableapps.com/apps/development/nsis_portable>`_
----------------------------------------------------------------

* Multiple executables (including
  :ini-key:`[Launch]:ProgramExecutableWhenParameters`)
* Registry key
* Updating drive letters

`OpenTTD <http://portableapps.com/apps/games/openttd_portable>`_
----------------------------------------------------------------

* Working directory
* Launch app after splash (can be fullscreen)
* No need for clean TEMP
* Not waiting for the app
* Full automatic language switching.

`Opera <http://portableapps.com/apps/internet/opera_portable>`_
---------------------------------------------------------------

* Registry key
* Writing INI strings
* Updating drive letters
* Moving a directory
* Language switching including :ini-section:`[LanguageFile]` language
  preservation and :ini-section:`[LanguageStrings]` mappings

`Paul's Extreme Sound Stretch`_
-------------------------------

* Working directory
* Moving a directory
* Updating drive letters

.. _Paul's Extreme Sound Stretch:
   http://portableapps.com/apps/music_video/paul_stretch_portable

`PChat <http://portableapps.com/apps/internet/pchat_portable>`_
---------------------------------------------------------------

* Language switching
* Environment variables
* Command line arguments

`Regshot <http://portableapps.com/apps/utilities/regshot_portable>`_
--------------------------------------------------------------------

* Working directory
* Allowing directory moving
* Moving a file
* Updating drive letter and full package path

`SMPlayer <http://portableapps.com/apps/music_video/smplayer_portable>`_
------------------------------------------------------------------------

* Command line arguments
* Support enabled for :ini-key:`directory moving <[Launch]:DirectoryMoveOK>`
  (though not yet released)
* Writing INI strings
* Updating drive letters
* Moving a directory
* Language switching including :ini-section:`[LanguageFile]` language
  preservation and :ini-section:`[LanguageStrings]` mappings

`SQLite Database Browser`_
--------------------------

* Working directory (and that's all - a good example of keeping it simple)

.. _SQLite Database Browser:
   http://portableapps.com/apps/development/sqlite_database_browser_portable

`WAtomic <http://portableapps.com/apps/games/watomic_portable>`_
----------------------------------------------------------------

* Launch app after splash
* Moving files
* Pruning a directory if empty

`WinDjView <http://portableapps.com/apps/office/windjview_portable>`_
---------------------------------------------------------------------

* Allowing directory moving
* Registry key
* Registry value writing
* Pruning registry tree
* Drive letter updating
* Full automatic language switching

`Zaz <http://portableapps.com/apps/games/zaz_portable>`
-------------------------------------------------------

* Working directory
* Launch app after splash
* Moving a directory
* Full automatic language switching
