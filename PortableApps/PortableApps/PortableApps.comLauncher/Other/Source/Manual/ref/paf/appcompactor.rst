.. _paf-appcompactor:

4. PortableApps.com AppCompactor and AppCompactor.ini
=====================================================

The PortableApps.com AppCompactor is used to shrink software to better fit on
smaller removable devices and run quicker over bandwidth-limited removable
media. Normally, the AppCompactor is entirely automatic in settings and just
needs to be run on the AppNamePortable directory of the portable app.
Occasionally, some software is incompatible with the AppCompactor and its
automated routines cannot detect the incompatibility. Additionally, sometimes
additional files of a specific app should be compressed and the AppCompactor
doesn't normally handle them. In these cases, the AppCompactor.ini may be used.

The AppCompactor.ini resides in the AppInfo directory alongside the AppInfo.ini
and Installer.ini. An example of the file follows:

.. code-block:: ini

   [PortableApps.comAppCompactor]
   FilesExcluded=msvcm90.dll|msvcp90.dll|mscvr90.dll
   AdditionalExtensionsExcluded=pyd|irc
   AdditionalExtensionsIncluded=example|beta

Within the ``[PortableApps.comAppCompactor]`` section:
------------------------------------------------------

*Note: If you do not plan on using the PortableApps.com AppCompactor to shrink
your app or do not need to make changes to its standard compression setup, this
section should be omitted.*

**FilesExcluded** are any files you would like excluded from an AppCompactor run
separated by pipes ``|``.

**AdditionalExtensionsExcluded** is any additional types of files (extensions)
that you would like to exclude from the compaction separated by pipes ``|``.

**AdditionalExtensionsIncluded** is any additional types of files (extensions)
that you would like to include in the compaction separated by pipes ``|``.

**CompressionFileSizeCutOff** is the cutoff point at which to ignore files for
compression in bytes. The default is 4096, meaning files 4K in size and smaller
are ignored.
