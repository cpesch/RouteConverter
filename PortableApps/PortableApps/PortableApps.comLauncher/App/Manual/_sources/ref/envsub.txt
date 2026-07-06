.. index:: Environment variable substitutions

.. _ref-envsub:

================================
Environment variable substitions
================================

This document covers environment variable substition.

Many of the values in :ref:`launcher.ini <ref-launcher.ini>` are subject to
environment variable substitions. This works by taking the input string and
parsing environment variables, so that a chunk like ``%TEMP%`` will become
something like ``C:\Users\user\AppData\Roaming\Temp``. To make this more useful
in making applications portable, a number of extra environment variables are
provided. This document is primarily here to describe those values.

For the purposes of this page, the portable app in question is installed to
``X:\PortableApps\AppNamePortable`` and last ran from the path
``W:\Apps\AppNamePortable`` (the use of a different path is explained below).

These values can be used in any values marked "|envsub|". Other environment
variables can also be used, but any directory variables will not have the
alternative forms provided for these specific variables.

.. _ref-envsub-drive:

Drive variables
===============

These are a few variables for drive letter updating. In Live mode, these are the
location of the portable device from which the application was run, *not* the
application or data directory if :ini-key:`[LiveMode]:CopyApp` is set.

Note that you cannot guarrantee the case that this variable will be in. It may
be lower or upper case.

.. env:: PAL:Drive

PAL:Drive
---------

The drive letter including a colon from which the portable app is running.

**Example:** ``X:``

.. env:: PAL:LastDrive

PAL:LastDrive
-------------

The drive letter including a colon from which the portable app ran last.

**Example:** ``W:``

.. env:: PAL:DriveLetter

PAL:DriveLetter
---------------

The drive letter from which the portable app is running, without a colon. Useful
with :ini-section:`[FileWriteN]` find and replace where the colon is not
included in the path, e.g.  ``file:////%PAL:DriveLetter%/`` for ``file:////X/``
or even ``%PAL:DriveLetter%\:/`` for ``X\:/``.

If you wish your app to support UNC paths (see
:ini-key:`[Launch]:SupportsUNC`), you should not use this. For UNC paths, this
will be ``\`` (the first letter of the UNC path). Instead, you should figure
out something probably in the custom code or `ask for help <help>`_.

**Example:** ``X``

.. env:: PAL:LastDriveLetter

PAL:LastDriveLetter
-------------------

The drive letter from which the portable app ran last. Useful with
:ini-section:`[FileWriteN]` find and replace where the colon is not included in
the path, e.g.  ``file:////%PAL:LastDriveLetter%/`` for ``file:////X/`` or even
``%PAL:LastDriveLetter%\:/`` for ``X\:/``.

See above for comments on UNC paths.

**Example:** ``W``

.. _ref-envsub-directory:

Directory variables
===================

Each variable fitting into this category gets several extra environment
variables generated for it, for different forms they may be needed. For
example, when dealing directly with Windows paths must be separated by a
backslash (``\``), while with various other applications, for example
applications ported from Linux, a forward slash (``/``) is often needed, or
even a double backslash (``\\``), or something else.

.. _ref-envsub-java.util.prefs:

One complex example is with Java applications that use ``java.util.prefs`` to
store their settings; ``java.util.prefs`` stores settings in the registry, but
its path storage mechanism is unusual. The path gets stored with a forward
slash as the separator, and then all characters *other* than a colon and
lower-case letters are escaped with a slash (including the path separator), so
that a Windows path like ``X:\PortableApps\AppNamePortable`` will become
``/X:///Portable/Apps///App/Name/Portable``.

Each environment variable listed in this section is currently available in four
forms. For the environment variable listed as ``VARIABLE``, here are the
environment variables which will be available:

* ``%VARIABLE%`` -- directory separator is a backslash (``\``).
* ``%VARIABLE:ForwardSlash%`` -- directory separator is a forward slash (``/``).
* ``%VARIABLE:DoubleBackslash%`` -- directory separator is a double backslash (``\\``).
* ``%VARIABLE:java.util.prefs%`` -- path is in a format for reading with ``java.util.prefs`` (see above).

So, for the environment variable :env:`PAL:AppDir` with the value
``X:\PortableApps\AppNamePortable\App``, the following environment variables
will be available:

* ``%VARIABLE%`` -- ``X:\PortableApps\AppNamePortable\App``
* ``%VARIABLE:ForwardSlash%`` -- ``X:/PortableApps/AppNamePortable/App``
* ``%VARIABLE:DoubleBackslash%`` -- ``X:\\PortableApps\\AppNamePortable\\App``
* ``%VARIABLE:java.util.prefs%`` -- ``/X:///Portable/Apps///App/Name/Portable///App``

Now on to the environment variables themselves.

.. env:: PAL:AppDir

PAL:AppDir
----------

The path to the App directory which contains the portable app.

When Live mode is not enabled, this will be
``X:\PortableApps\AppNamePortable\App`` and when Live mode is enabled it will
be ``%TEMP%\AppNamePortableLive\App`` unless :ini-key:`[LiveMode]:CopyApp` is
set to ``false``.

.. env:: PAL:DataDir

PAL:DataDir
-----------

The path to the Data directory which contains the portable app's data.

When Live mode is not enabled, this will be
``X:\PortableApps\AppNamePortable\Data`` and when Live mode is enabled it will
be ``%TEMP%\AppNamePortableLive\Data``.

.. env:: JAVA_HOME

JAVA_HOME
---------

When Java is found, this is set to the location where it was found, not
including the "bin" directory or a filename like "javaw.exe". This will be the
Java Portable directory, e.g.  ``X:\PortableApps\CommonFiles\Java``, or some
local installation, e.g.  ``C:\Program Files\Java``.

This variable is only available with :ini-key:`[Activate]:Java` set to ``find``
or ``require``, but if it is ``find``, the path it is set to may not exist (for
``require`` the launcher will abort if Java is not found).

.. env:: PortableApps.comDocuments

PortableApps.comDocuments
-------------------------

The PortableApps.com Documents directory, normally ``X:\Documents``. There is no
guarrantee that this directory will exist.

.. env:: PortableApps.comPictures

PortableApps.comPictures
------------------------

The PortableApps.com Pictures directory, normally ``X:\Documents\Pictures``. There is no
guarrantee that this directory will exist.

.. env:: PortableApps.comMusic

PortableApps.comMusic
---------------------

The PortableApps.com Music directory, normally ``X:\Documents\Music``. There is no
guarrantee that this directory will exist.

.. env:: PortableApps.comVideos

PortableApps.comVideos
----------------------

The PortableApps.com Videos directory, normally ``X:\Documents\Videos``. There is no
guarrantee that this directory will exist.

.. env:: PAL:PortableAppsDir

PAL:PortableAppsDir
-------------------

The PortableApps.com PortableApps directory, normally ``X:\PortableApps``. To be
exact, this is the parent directory of the portable app package.

.. env:: PAL:PortableAppsBaseDir

PAL:PortableAppsBaseDir
-----------------------

The base of the PortableApps.com directory hierachy, where
``PAL:PortableAppsDir`` and ``PortableApps.comDocuments`` are usually at.

.. env:: PAL:LastPortableAppsBaseDir

PAL:LastPortableAppsBaseDir
---------------------------

The value of ``PAL:PortableAppsBaseDir`` from the previous run.

.. env:: USERPROFILE

USERPROFILE
-----------

A local variable for copying to and from, e.g. ``C:\Documents and
Settings\Username`` on XP and 2000, ``C:\Users\Username`` on Vista.

.. env:: ALLUSERSPROFILE

ALLUSERSPROFILE
---------------

A local variable for copying to and from, e.g. ``C:\Documents and Settings\All
Users`` on XP and 2000, ``C:\ProgramData`` on Vista.

Most apps will need to use :env:`ALLUSERSAPPDATA` instead of this because it
includes the "Application Data" part on the end for Windows 2000 and XP.

.. env:: ALLUSERSAPPDATA

ALLUSERSAPPDATA
---------------

A local variable for copying to and from, e.g. ``C:\Documents and Settings\All
Users\Application Data`` on XP and 2000, ``C:\ProgramData`` on Vista.

This variable does not exist in Windows itself and is added by the
PortableApps.com Launcher. The difference between it and
:env:`ALLUSERSPROFILE` above is that on Windows 2000 and XP it includes
"Application Data" at the end. This is how it is normally used.

.. versionadded:: 2.1

.. env:: LOCALAPPDATA

LOCALAPPDATA
------------

A local variable for copying to and from, e.g. ``C:\Documents and
Settings\Username\Local Settings\Application Data`` on XP and 2000,
``C:\Users\Username\AppData\Local`` on Vista.

.. env:: APPDATA

APPDATA
-------

A local variable for copying to and from, e.g. ``C:\Documents and
Settings\Username\Application Data`` on XP and 2000,
``C:\Users\Username\AppData\Roaming`` on Vista.

.. env:: DOCUMENTS

DOCUMENTS
---------

A local variable for copying to and from, e.g. ``C:\Documents and
Settings\Username\My Documents`` on XP and 2000, ``C:\Users\Username\Documents``
on Vista.

.. env:: TEMP

TEMP
----

The temporary directory which the application will be given. If a contained
temporary directory is assigned, this will be the assigned one.

.. _ref-envsub-partial-directory:

Partial directory variables
===========================

For dealing with :ref:`moving packages <moving-package-directory>`, it's often
handy to be able to update a path without including the drive letter.  Like the
:ref:`directory variables above <ref-envsub-directory>`, these variables get the
additional environment variables generated for them in different forms.

As with :ref:`drive variables <ref-envsub-drive>`, the case of these variables
cannot be guarranteed. They may be lower or upper case.

.. env:: PAL:PackagePartialDir

PAL:PackagePartialDir
---------------------

The path, minus the drive letter and colon, from which the portable app is
running.

**Example:** ``\PortableApps\AppNamePortable``

.. env:: PAL:LastPackagePartialDir

PAL:LastPackagePartialDir
-------------------------

The path, minus the drive letter and colon, from which the portable app ran
last.

The first time a portable app is run, this will be the same as
:env:`PAL:PackagePartialDir` above. Thus in its normal use case as part of the
:ini-key:`Find <[FileWriteN]:Find>` value in a :ini-section:`[FileWriteN]`
section, it will be the same as the :ini-key:`Replace <[FileWriteN]:Replace>`
value and result in the replacement being skipped.

**Example:** ``\Apps\AppNamePortable``

.. _ref-envsub-language:

Language variables
==================

These variables are for language switching. Particularly of interest is
%PAL:LanguageCustom%.

A full table of all the values is available in :ref:`languages-values`.

.. env:: PortableApps.comLanguageCode

* **PortableApps.comLanguageCode** -- e.g. "en", "pt", "pt-br"

.. env:: PortableApps.comLocaleCode2

* **PortableApps.comLocaleCode2** -- e.g. "en", "pt", "pt"

.. env:: PortableApps.comLocaleCode3

* **PortableApps.comLocaleCode3** -- e.g. "eng", "por", "por"

.. env:: PortableApps.comLocaleglibc

* **PortableApps.comLocaleglibc** -- e.g. "en_US", "pt", "pt_BR"

.. env:: PortableApps.comLocaleID

* **PortableApps.comLocaleID** -- e.g. "1033", "2070", "1046"

.. env:: PortableApps.comLocaleWinName

* **PortableApps.comLocaleWinName** -- e.g. "LANG_ENGLISH", "LANG_PORTUGUESE",
  "LANG_PORTUGUESEBR"

.. env:: PortableApps.comLocaleName

* **PortableApps.comLocaleName** -- e.g. "English", "Portuguese", "PortugueseBR"
  (note: these will be upper case until implemented in the PortableApps.com
  Platform)

.. env:: PAL:LanguageCustom

* **PAL:LanguageCustom** -- a custom variable constructed in the
  :ini-section:`[Language]` and :ini-section:`[LanguageStrings]` sections.
