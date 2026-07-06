.. index:: Registry, Making things portable; registry

.. _registry:

=========================
Dealing with the registry
=========================

Many applications store their data in the registry; when making such an
application portable this data must be preserved.

Making registry keys portable
=============================

With the PortableApps.com Launcher it's easy to make a registry key that an
application uses portable.

First of all, when using any of the registry sections in your launcher
configuration file, you must set :ini-key:`[Activate]:Registry` to ``true``,
or else they **will not work**. If something doesn't seem to be working, check
that value first. You probably didn't set it.

When you have an application that uses a key like
``HKEY_CURRENT_USER\Software\AppName``, you can make that portable with a line
in the :ini-section:`[RegistryKeys]` section like this:

.. code-block:: ini

   [RegistryKeys]
   appname=HKCU\Software\AppName

The ``appname`` refers to the filename (without the ``.reg`` extension) where it
will be saved, inside the ``Data\settings`` directory of your package. This
means that you can update drive letters and things like that inside
``%PAL:DataDir%\settings\appname.reg``.

If the registry key exists when the launcher comes to load the portable data, it
will be backed up, and restored at the end, so that no data is lost.

.. _registry-cleanupifempty:

If the registry key you are wishing to save and make portable is deeper, like
``HKCU\Software\Publisher\AppName``, then you need to make sure that the
"Publisher" key is also deleted if it is empty, and not left behind. This also
uses the :ini-section:`[RegistryCleanupIfEmpty]` section.

.. code-block:: ini

   [RegistryKeys]
   appname=HKCU\Software\Publisher\AppName

   [RegistryCleanupIfEmpty]
   1=HKCU\Software\Publisher

This also applies for more complex subtrees. (Try to think of more useful file
names that "appname1" and "appname2" for your own applications.) In the example
below, note that order is important. If the key numbers of the
:ini-section:`[RegistryCleanupIfEmpty]` values were the other way round, it
would try to delete ``HKCU\Software\Publisher`` first, and find that it wasn't
empty (because it would have ``Type`` in it) and so it wouldn't be cleaned up.

.. code-block:: ini

   [RegistryKeys]
   appname1=HKCU\Software\Publisher\Type\AppName
   appname2=HKCU\Software\Publisher\AppNameConfig

   [RegistryCleanupIfEmpty]
   1=HKCU\Software\Publisher\Type
   2=HKCU\Software\Publisher

Read the :ref:`launcher.ini registry reference <ref-launcher.ini-registry>` and
the rest of this document for more information.

Hives
=====

The registry is divided up into "hives" for storing data. Here is a list of the
hives supported by the PortableApps.com Launcher:

* **HKEY_LOCAL_MACHINE** (**HKLM**) -- settings shared between users; requires
  administrative privileges to write to it
* **HKEY_CURRENT_USER** (**HKCU**) -- settings for the current user; requires no
  special permissions to write to it, but on restricted accounts certain methods
  of writing to it will not work.
* **HKEY_CLASSES_ROOT** (**HKCR**) -- a virtual hive constructed of an
  amalgamation of ``HKEY_LOCAL_MACHINE\Classes`` and
  ``HKEY_CURRENT_USER\Classes`` (``HKEY_CURRENT_USER\Classes`` takes
  precedence). In your launcher configuration you should use ``HKCU\Classes``
  for this value instead.

The official format for hives is the four-letter abbreviation (``HKLM`` or
``HKCU``) instead of the long name.

**A note on HKEY_USERS (HKU)**: programs like Regshot use the full path to
HKEY_CURRENT_USER, which includes the user ID. This means that anything like
``HKU\S-?-?-??-?????????-?????????-?????????-????`` (each ``?`` is a number)
should be used as ``HKCU``. There is also ``HKU\.DEFAULT`` which is the same as
far as portability is concerned.

Keys and values to ignore
=========================

Lots of cache-type data is stored in the registry and other Windows settings
which can be safely ignored when making a portable application. This section
will gradually grow with lists of such values which you can ignore when making
an application portable or when testing an application.

These keys are in HKCU:

* ``SessionInformation\ProgramCount``
* ``Software\Microsoft\Cryptography\RNG\Seed``
* ``Software\Microsoft\DirectDraw\MostRecentApplication``
* ``Software\Microsoft\DirectInput\MostRecentApplication``
* ``Software\Microsoft\SchedulingAgent``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\ComDlg32\LastVisitedMRU``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\ComDlg32\LastVisitedMRU``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\ComDlg32\OpenSaveMRU``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\UserAssist``
* ``Software\Microsoft\Windows\CurrentVersion\Explorer\UserAssist``
* ``Software\Microsoft\Windows\CurrentVersion\Group Policy``
* ``Software\Microsoft\Windows\ShellNoRoam\BagMRU``
* ``Software\Microsoft\Windows\ShellNoRoam\Bags``
* ``Software\Microsoft\Windows\ShellNoRoam\MUICache``

These keys are in HKLM:

* ``Software\Microsoft\Windows\CurrentVersion\Reliability``
* ``System\ControlSet001`` (equivalent to ``System\CurrentControlSet``)
* ``System\CurrentControlSet\Control\DeviceClasses``
* ``System\CurrentControlSet\Services\*\Enum``
* ``System\CurrentControlSet\Services\SharedAccess``
* ``System\CurrentControlSet\Services\swmidi``

If you come up with more keys that can be ignored, please :ref:`contact Chris
Morgan <help>`.

Specific registry keys
======================

Some registry keys have particular ways of dealing with them. These are listed
here.

``HKCU\Software\JavaSoft\Prefs``
--------------------------------

Keys in here are from Java applications which use :ref:`java-java.util.prefs`.
See that page for tips on dealing with those registry keys.

``HKCU\Software\Trolltech``
---------------------------

Keys in this key are from Qt applications. See :ref:`qt` for details on what to
do about them.

``HKLM\Software\Classes\CLSID\XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX``
--------------------------------------------------------------------

(Where ``X`` is a hexadecimal digit.)

These are DLL servers and need registering. For the moment, this will block an
app from being properly portable. Code to deal with this is due in version 2.2.

General handling of registry keys
=================================

The normal way of dealing with registry keys in the launcher configuration file
is with the
:ini-section:`[RegistryKeys]`,
:ini-section:`[RegistryValueWrite]`,
:ini-section:`[RegistryCleanupIfEmpty]`,
:ini-section:`[RegistryCleanupForce]` and
:ini-section:`[RegistryValueBackupDelete]` sections.

.. _registry-detecting-changes:

Detecting changes
=================

.. This should probably go in a document on making portable apps and just have a
   reference to it from here.

In making a portable app, unless you know exactly what it's going to be
changing, you should monitor things like the registry, to see what keys you may
need to deal with. The most popular tool in the PortableApps.com community for
doing this is Regshot_.

.. TODO: check if this is the right link to include; at the time of writing this
   I didn't have an internet connection.

.. _Regshot: http://sourceforge.net/projects/regshot

*This document is not complete*
