.. _ref-launcher.ini-registry:

.. ini-section:: [RegistryKeys]

[RegistryKeys]
==============

``[RegistryKeys]`` deals with registry keys for which to manage
portability. They come in the form ``file name=registry key location``; the
*file name* is the location of the place where it is saved, which is in
Data\\settings\\\ *file name*.reg. If you wish to have drive letters updated
inside this, look at :ini-section:`[FileWriteN]` with :ini-key:`Type
<[FileWriteN]:Type>`\ =\ ``Replace``.

If you do not wish to save the data of the registry key to a file but only
want to keep it safe and throw away any changes, set the "file name" to ``-``,
so you end up with ``-=registry key location``.

Some more description of how this works and what it can be used to do is
available in :ref:`Dealing with the registry <registry>`.

**Example:** ``appname_portable=HKCU\Software\AppName``

.. ini-section:: [RegistryValueWrite]

[RegistryValueWrite]
====================

These are registry values to set for running the portable application, useful
for things like disabling file association writing. They come in the form
``HK??\*\Key\Value=REG_[TYPE]:[value]``; the key name is the place in the
registry, with the value appended to it after a final slash, and the value is of
the form REG\_\ *TYPE*:*value*; *REG_TYPE:* is optional, and defaults to
``REG_SZ`` (a string).

**Note:** if there is any possibility that the value does contain a colon,
write the type explicitly.

**Examples:** ``HKCU\Software\AppName\Key\Value=REG_DWORD:16``,
``HKCU\Software\AppName\Key\Value2=REG_SZ:%PAL:DataDir%``

.. ini-section:: [RegistryCleanupIfEmpty]

[RegistryCleanupIfEmpty]
========================

|inikeyint|

----

These are registry keys which get cleaned up after the application has run if
they are empty. This is useful if there is a tree which will be left behind, for
example, if something stores to HKEY_CURRENT_USER\\Software\\Publisher\\AppName,
when AppName is saved, Publisher will still be left, empty. Remove it with a
line in here.

Some more description of how this works and what it can be used to do is
available in :ref:`Dealing with the registry <registry-cleanupifempty>`.

**Example:** ``1=HKCU\Software\Publisher``

.. ini-section:: [RegistryCleanupForce]

[RegistryCleanupForce]
======================

|inikeyint|

----

These are registry keys which get removed after the application has run. This is
useful if there is a tree of useless information which will be left behind, for
example, if something stores temporary data to
HKEY_CURRENT_USER\\Software\\AppName\\Temp. Remove it with a line in here.

**Example:** ``1=HKCU\Software\Publisher``

.. ini-section:: [RegistryValueBackupDelete]

[RegistryValueBackupDelete]
===========================

|inikeyint|

----

These are registry values which get backed up before hand and restored later,
but any value which may have been set while the portable application is running
will be deleted. This can be useful for "dead" values which serve no purpose and
so there is no point in saving them anywhere.

**Example:** ``1=HKCU\Software\Publisher\AppName\Value``
