.. _examples-scribus:

Worked Example: Scribus
=======================

The :ref:`AppID <paf-appinfo-appid>` of Scribus is ``ScribusPortable``,
and so the launcher executable is named ``ScribusPortable.exe`` and the
launcher configuration file is in
``App\AppInfo\Launcher\ScribusPortable.ini``.

The main Scribus executable to be launched is ``Scribus.exe``, and it lives in
``App\Scribus``. Thus the :ini-key:`ProgramExecutable` will be
``Scribus\Scribus.exe``.

With a little bit of guesswork and/or looking around, it can be seen that
Scribus stores its settings in the filesystem, in the path specified by the
environment variable ``HOME`` if it is found. This is a "safe" environment
variable to set, and so there will be an :ini-section:`[Environment]` pair
``HOME=%PAL:DataDir%`` to get it to store its settings in the Data directory.

In this directory another directory is created, ``.scribus``, and in that a
file ``scribus13.rc`` which contains the Scribus settings. Among various other
settings, an MRU (Most Recently Used) file list is maintained, which (I think)
can store paths with a backslash separator and a forward slash separator -- so
there may be paths in like ``X:\Documents\...`` and ``X:/Documents/...``.  To
update the drive letter in this file is the work of two
:ini-section:`[FileWriteN]` sections (one to do each slash style).

One last thing: Ghostscript is required, and will be included in the
``App\Ghostscript`` directory. For Scribus to be able to detect this, it will
need to be added with an :ini-section:`[Environment]` pair.

From the information above, here is a fully functional sample launcher file,
configured correctly for Scribus Portable:

.. code-block:: ini

   [Launch]
   ProgramExecutable=Scribus\Scribus.exe
   WaitForProgram=false
   
   [FileWrite1]
   Type=replace
   File=%PAL:DataDir%\.scribus\scribus13.rc
   Find=%PAL:LastDrive%\
   Replace=%PAL:Drive%\
   
   [FileWrite2]
   Type=replace
   File=%PAL:DataDir%\.scribus\scribus13.rc
   Find=%PAL:LastDrive%/
   Replace=%PAL:Drive%/
   
   [Environment]
   PATH=%PAL:AppDir%\Ghostscript;%PATH%
   HOME=%PAL:DataDir%

To outline what this actually does: it inserts the App\\Ghostscript directory
into the start of the environment variable PATH, sets the environment variable
HOME to the Data directory, updates drive letters in
Data\\.scribus\\scribus13.rc, then runs ``App\Scribus\Scribus.exe``. Without
waiting for it (this is because there is nothing which needs tidying up
afterwards), the launcher quits.

If Scribus were to change so that any cleanup was needed afterwards, the
:ini-key:`[Launch]:WaitForProgram` line would need to be removed.
