.. _examples-7-zip:

Worked Example: 7-Zip
=====================

The :ref:`AppID <paf-appinfo-appid>` of 7-Zip is ``7-ZipPortable``, and so the
launcher executable is named ``7-ZipPortable.exe`` and the launcher
configuration file is in ``App\AppInfo\Launcher\7-ZipPortable.ini``.

The main 7-Zip executable to be launched is ``7zFM.exe``, and it lives in
``App\7-Zip``.

7-Zip stores its settings in the registry, in
``HKEY_CURRENT_USER\Software\7-Zip``.  In previous releases of 7-Zip Portable,
it has stored this in Data\settings\7zip_portable.reg (this is represented as
:ini-section:`[RegistryKeys]`\ ``:7zip_portable=HKCU\Software\7-Zip``).

Paths saved in this 7zip_portable.reg file are normal paths with a
double-backslash directory separator and should be updated with a
:ini-section:`[FileWriteN]` section in this way.

7-Zip itself **can** be run from a read-only location (this is why
:ini-key:`[LiveMode]:CopyApp`\ =\ ``false``).

From the information above, here is a fully functional sample launcher file,
configured correctly for 7-Zip Portable:

.. code-block:: ini

   [Launch]
   ProgramExecutable=7-Zip\7zFM.exe

   [Activate]
   Registry=true

   [FileWrite1]
   Type=Replace
   File=%PAL:DataDir%\settings\7zip_portable.reg
   Find=%PAL:LastDrive%\\
   Replace=%PAL:Drive%\\

   [RegistryKeys]
   7zip_portable=HKEY_CURRENT_USER\Software\7-Zip

To outline what this actually does: in the registry,
``HKEY_CURRENT_USER\Software\7-Zip`` is backed up if necessary, and the contents
of Data\\settings\\\ **7zip_portable**\ .reg`` are inserted into the registry,
after updating the drive letter. Then App\\\ **7-Zip\7zFM.exe** is run. After it
is closed, the 7-Zip registry key is stored and deleted, and the backup of local
7-Zip settings is restored if it existed.
