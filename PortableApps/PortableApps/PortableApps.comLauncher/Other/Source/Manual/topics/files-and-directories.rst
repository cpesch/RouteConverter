.. _files-and-directories:

=====================
Files and directories
=====================

One of the most common things with making applications portable is moving
settings around. Ideally, an application is able to be made portable (or at
least almost portable) with a command-line option or an environment variable to
set its settings location, but most applications don't support this. (TODO: a
discussion of ways to improve base applications is in order.)

Until I write more content in here, you should refer to the documentation for
the :ini-section:`[FilesMove]`, :ini-section:`[DirectoriesMove]`,
:ini-section:`[DirectoriesCleanupIfEmpty]` and
:ini-section:`[DirectoriesCleanupForce]` sections of launcher.ini.

*This document is not complete*

.. TODO: finish this

.. index:: wildcards

.. _wildcards:

Support for wildcards
=====================

The PortableApps.com Launcher supports wildcard matching for several sections;
:ini-section:`[FilesMove]`, :ini-section:`[DirectoriesMove]`,
:ini-section:`[FileWriteN]`, :ini-section:`[DirectoriesCleanupIfEmpty]` and
:ini-section:`[DirectoriesCleanupForce]`.  Here are the rules of the wildcard
matching:

* ``*`` matches any number of characters
* ``?`` matches one character
* Wildcards can be included at any level, but currently only at one level

**Examples:**

* ``%PAL:DataDir%\File????.sw?`` is valid and will match in the Data directory
  files with names such as ``FileABCD.swp``, ``File27X4.swo``,
  ``File3_2!.sw_``, but it will not match things like ``FileABC.swp``,
  ``FileABCDE.swp``, ``FileABCD.sw`` or ``FileABCD.swap``.

* ``%PAL:DataDir%\*.ini`` is valid and will match in the Data directory files
  with names such as ``foo.ini`` and ``bar.ini``.

* ``%PAL:DataDir%\*-???.ini`` is valid and will match in the Data directory
  files with names such as ``foo-123.ini`` and ``abcdef-ghi.ini``, but not
  ``foo.ini``.

* ``%PAL:DataDir%\*\settings.ini`` is valid and will match all files called
  ``settings.ini`` in all subdirectories (one level deep, not recursively) of
  the Data directory.

* ``%PAL:DataDir%\*\*.ini`` is *not* valid as wildcards are included at
  multiple levels.

.. versionadded:: 2.1
