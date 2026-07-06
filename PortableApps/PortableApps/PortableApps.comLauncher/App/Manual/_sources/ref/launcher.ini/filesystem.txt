.. ini-section:: [FilesMove]

[FilesMove]
===========

These are files for which to manage portability. They come in the form ``[file
name]=[target directory]``.

The *file name* is the location of the place where it is saved, relative to the
portable data directory (AppNamePortable\\Data).

The *target directory* is the full path to the directory the file is copied to
during the program execution. Do not include the file name. |envsub|

If the target directory already exists at the start of the process, it will be
backed up (to *target directory*\ \\\ *file name*-BackupBy\ *AppID*) and
restored at the end.

Concerning how the file should be situated in the Data directory, the directory
must exist or the file will not be saved. Thus, you are recommended to either
store files in the root of Data or in the ``settings`` directory. Other
directories may be suitable sometimes if the directory will be created by
``DefaultData``.

:ref:`Wildcards <wildcards>` are supported.

**Example:** ``settings\file.txt=%PAL:AppDir%\AppName``

.. versionchanged:: 2.1
   added support for wildcards

.. ini-section:: [DirectoriesMove]

[DirectoriesMove]
=================

These are directories for which to manage portability. They come in the form
``[directory]=[target location]``.

The *directory* is the location of the source directory, relative to the
portable data directory (AppNamePortable\\Data).

The *target location* includes the directory you want it to go to, so
``%PAL:DataDir%\[directory]\*.*`` gets copied to ``[target location]\*.*``.
|envsub|

If the target directory already exists at the start of the process, it will be
backed up (to *target location*-BackupBy\ *AppID*) and restored at the end.

If you do not wish to save the data but only want to keep a local version safe
and throw away any changes, set the source directory to ``-``, so you end up
with ``-=[target location]``. If you don't wish to back up local data, you can
use :ini-section:`[DirectoriesCleanupForce]`.

Concerning how the directory should be situated, the parent directory must
exist or the directory will not be saved. Thus, you are recommended to use a
top-level directory name. Contrary to some former recommendations, ``settings``
should **not** be used as the key, as the ``Data\settings`` directory is used
for storing registry keys in such a way that data will be lost if you move the
directory; it  may also be used for other purposes, so whether you use registry
support or not, you should avoid it.

:ref:`Wildcards <wildcards>` are supported.

**Example:** ``config=%APPDATA%\Pub\lisher\AppName``

.. versionchanged:: 2.1
   added support for wildcards

.. ini-section:: [DirectoriesCleanupIfEmpty]

[DirectoriesCleanupIfEmpty]
===========================

|inikeyint|

|envsub|

:ref:`Wildcards <wildcards>` are supported.

----

These are directories which get cleaned up after the application has run if they
are empty. This is useful if there is a tree which will be left behind, for
example, if something stores to ``%APPDATA%\Publisher\AppName``, when
``AppName`` is saved, ``Publisher`` will still be left, empty. Remove it with a
line in here.

**Example:** ``1=%APPDATA%\Publisher``

.. versionchanged:: 2.1
   added support for wildcards

.. ini-section:: [DirectoriesCleanupForce]

[DirectoriesCleanupForce]
=========================

|inikeyint|

|envsub|

:ref:`Wildcards <wildcards>` are supported.

----

These are directories which get removed after the application has run. This is
useful if there is a tree which will be left behind, for example, if something
stores temporary data which can be safely deleted in ``%APPDATA%\AppName\Temp``.
Remove it with a line in here.

If you need to back up the local directory so that it will not be ruined, you
can use :ini-section:`[DirectoriesMove]` with a key name of ``-``.

**Example:** ``1=%APPDATA%\Publisher``

.. versionchanged:: 2.1
   added support for wildcards
