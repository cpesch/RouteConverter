.. _paf-layout:

1. Directory and File Layout
============================

The basic directory layout of each portable app consists of a main directory,
AppNamePortable which contains three directories: App, Data and Other.

::

   AppNamePortable
   + App
     + AppInfo
     + AppName
     + DefaultData
   + Data
   + Other
     + Help
       + Images
     + Source

**AppNamePortable**: contains the main application launcher, typically named
``AppNamePortable.exe`` and the main help file ``help.html``. No other files are
present in this directory by default.

**App**: contains all the binary and other files that make up the application
itself, usually within a directory called ``AppName``. The other directory
called ``AppInfo`` (discussed in :ref:`section 2 <paf-appinfo>`) contains the
configuration details for the PortableApps.com Platform as well as the icons
used within the menu. It may also contain the launcher.ini configuration file
used for the PortableApps.com Launcher. The third directory, ``DefaultData`` is
usually used as a container for the default files to be placed within the
``Data`` directory. Generally, the launcher, when run, will check if there is a
set of files within Data and, if not, will copy them from DefaultData. The next
release of the PortableApps.com Installer will do the same.

**Data**: contains all the user data for the application including settings,
configuration and other data that would usually be stored within APPDATA for a
locally installed application. The applications released by PortableApps.com
typically contain the settings in a ``settings`` subdirectory, profiles for
Mozilla apps in a ``profiles`` subdirectory. No application components (binary
files, etc) should be contained within the Data directory. The launcher or
application must be able to recreate the Data directory and all required files
within it if it is missing.

**Other**: contains files that don't fit into the other categories. The
additional images and other files used by ``help.html`` included in the main
``AppNamePortable`` are included in a ``Help`` subdirectory in the ``Other``
directory.  Images for the help file would be included in an ``Images``
subdirectory within the ``Help`` subdirectory.

Any source code or source code licensing as well as the source files for the
PortableApps.com Installer (if desired) are included within the Source
subdirectory. This may include the source for the ``AppNamePortable.exe``
launcher, a ``readme.txt`` file detailing the usage of the launcher, license
information and other files.
