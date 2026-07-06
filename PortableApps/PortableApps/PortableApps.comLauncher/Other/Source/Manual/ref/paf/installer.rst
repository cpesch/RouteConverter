.. _paf-installer:

5. PortableApps.com Installer (and installer.ini)
=================================================

All apps in PortableApps.com Format must use the most recent PortableApps.com
Installer available at `PortableApps.com/development`_. The installer gets its
configuration from the appinfo.ini file above as well as the optional
installer.ini file which also resides in the AppInfo directory. The
installer.ini file allows for more fine-grained control over the installation
process as well as additional options like optional sections. The installer.ini
consists of:

**Please note that this example is only included to illustrate the possible
options. It should not be included as-is in a project. For projects, an
installer.ini should be created from scratch using only the features needed.**

.. code-block:: ini

   [CheckRunning]
   CloseEXE=Custom.exe
   CloseName=AppName

   [Source]
   IncludeInstallerSource=false

   [MainDirectories]
   RemoveAppDirectory=true
   RemoveDataDirectory=false
   RemoveOtherDirectory=true

   [OptionalComponents]
   OptionalComponents=true
   MainSectionTitle=AppName Portable (English) [Required]
   MainSectionDescription=Install the portable app
   OptionalSectionTitle=Additional Languages
   OptionalSectionDescription=Add multilingual support for this app
   OptionalSectionSelectedInstallType=Multilingual
   OptionalSectionNotSelectedInstallType=English
   OptionalSectionPreSelectedIfNonEnglishInstall=true
   OptionalSectionInstalledWhenSilent=true
   OptionalDirectory1=
   OptionalFile1=

   [CopyLocalFiles]
   CopyLocalFiles=true
   CopyFromRegPath=HKLM\Software\AppName
   CopyFromRegKey=AppPath
   CopyFromRegRemoveDirectories=2
   CopyFromDirectory=%PROGRAMFILES%\AppName
   CopyToDirectory=App\AppName

   [DownloadFiles]
   DownloadURL=
   DownloadName=
   DownloadFilename=
   DownloadMD5=
   DownloadTo=
   AdditionalInstallSize=
   Extract1To=
   Extract1File=
   AdvancedExtract1To=
   AdvancedExtract1Filter=
   DoubleExtractFilename=
   DoubleExtract1To=
   DoubleExtract1Filter=

   [Languages]
   ENGLISH=true
   AFRIKAANS=true
   ALBANIAN=true
   ARABIC=true
   ARMENIAN=true
   BASQUE=true
   BELARUSIAN=true
   BOSNIAN=true
   BRETON=true
   BULGARIAN=true
   CATALAN=true
   CIBEMBA=true
   CROATIAN=true
   CZECH=true
   DANISH=true
   DUTCH=true
   EFIK=true
   ESPERANTO=true
   ESTONIAN=true
   FARSI=true
   FINNISH=true
   FRENCH=true
   GALICIAN=true
   GEORGIAN=true
   GERMAN=true
   GREEK=true
   HEBREW=true
   HUNGARIAN=true
   ICELANDIC=true
   IGBO=true
   INDONESIAN=true
   IRISH=true
   ITALIAN=true
   JAPANESE=true
   KHMER=true
   KOREAN=true
   KURDISH=true
   LATVIAN=true
   LITHUANIAN=true
   LUXEMBOURGISH=true
   MACEDONIAN=true
   MALAGASY=true
   MALAY=true
   MONGOLIAN=true
   NORWEGIAN=true
   NORWEGIANNYNORSK=true
   PASHTO=true
   POLISH=true
   PORTUGUESE=true
   PORTUGUESEBR=true
   ROMANIAN=true
   RUSSIAN=true
   SERBIAN=true
   SERBIANLATIN=true
   SIMPCHINESE=true
   SLOVAK=true
   SLOVENIAN=true
   SPANISH=true
   SPANISHINTERNATIONAL=true
   SWAHILI=true
   SWEDISH=true
   THAI=true
   TRADCHINESE=true
   TURKISH=true
   UKRAINIAN=true
   UZBEK=true
   VALENCIAN=true
   VIETNAMESE=true
   WELSH=true
   YORUBA=true

   [DirectoriesToPreserve]
   PreserveDirectory1=

   [DirectoriesToRemove]
   RemoveDirectory1=

   [FilesToPreserve]
   PreserveFile1=

   [FilesToRemove]
   RemoveFile1=

The entire installer.ini is optional. If it is omitted, the App and Other
directories will be replaced and the installer will either be a single language
(as specified in appinfo.ini) or multilingual and include all supported
languages. The source for the installer will not be included.

Within the optional ``[CheckRunning]`` section:
-----------------------------------------------

**CloseEXE** (optional) allows you to assign a custom EXE to check for when
upgrading. If the EXE is the same as that specified in the Control - Start
option in appinfo.ini, this entry should be omitted from the installer.ini. If
you don't want to check if anything is running you can set ``CloseEXE=NONE``
(use uppercase) but this should be done with caution as a user could try to
upgrade your app while it's running.

**CloseName** (optional) allows you to assign a different name to what will be
closed when upgrading. If the name is the same as the name of the portable app
as specified in appinfo.ini then this entry should be omitted from the
installer.ini.

Within the optional ``[Source]`` section:
-----------------------------------------

**IncludeInstallerSource** (optional) allows you to include the source to the
PortableApps.com Installer to be installed with your portable app by setting it
to true.

Within the optional ``[MainDirectories]`` section:
--------------------------------------------------

**RemoveAppDirectory, RemoveDataDirectory and RemoveOtherDirectory** (optional)
allow you to specify whether these directories will be removed or preserved when
upgrading by installing a new version of your app over an existing one. By
default, the App and Other directories are removed and the Data directory is
preserved. If you wish to use these defaults, this section of installer.ini
should be omitted. (Note that you can preserve specific directories and files
below)

Within the optional ``[OptionalComponents]`` section:
-----------------------------------------------------

**OptionalComponents** - when set to true, this enables the installer to have an
optional section. This is typically used to install additional languages within
an app.

**MainSectionTitle** (optional) specifies the name that will appear for the
first section of the installer. By default it will read "AppName Portable
(English) [Required]" with AppName Portable being read from the appinfo.ini.
This entry should be omitted if you are happy with the default.

**MainSectionDescription** (optional) specifies the description that will appear
for the first section of the installer. By default it will read "Install the
portable app". This entry should be omitted if you are happy with the default.

**OptionalSectionTitle** (optional) specifies the name that will appear for the
second/optional section of the installer. By default it will read "Additional
Languages". This entry should be omitted if you are happy with the default.

**OptionalSectionDescription** (optional) specifies the description that will
appear for the second/optional section of the installer. By default it will read
"Add multilingual support for this app". This entry should be omitted if you are
happy with the default.

**OptionalSectionSelectedInstallType** (optional) specifies the InstallType that
will be written to appinfo.ini and displayed in the PortableApps.com Platform if
the user installs the app with the optional section. By default it will read
"Multilingual". This entry should be omitted if you are happy with the default.

**OptionalSectionNotSelectedInstallType** (optional) specifies the InstallType
that will be written to appinfo.ini and displayed in the PortableApps.com
Platform if the user installs the app without the optional section. By default
it will read "English". This entry should be omitted if you are happy with the
default.

**OptionalSectionPreSelectedIfNonEnglishInstall** (optional) specifies whether
the optional section is selected by default if the user selected to run the
installer in a language other than English. The default is true. This entry
should be omitted if you are happy with the default.

**OptionalSectionInstalledWhenSilent** (optional) specifies whether or not the
optional section is installed when the installer is running in silent mode when
launched from the platform's app installer. This entry defaults to true when the
optional components are not additional languages.

**OptionalDirectory1** allows you to specify which directories are a part of the
optional section of the installer. OptionalDirectory1 and higher are available
for use. The path should be relative. So if you want the directory
App\\AppName\\locales part of the optional section of the installer, you'd set
``OptionalDirectory1=App\AppName\locales`` in this section.

**OptionalFile1** allows you to specify which specific files are a part of the
optional section of the installer. OptionalFile1 and higher are available for
use. The path should be relative. So if you want the files App\\AppName\\*.lang
part of the optional section of the installer, you'd set
``OptionalFile1=App\AppName\*.lang`` in this section.

**Optional Section Note**: You must use either OptionalDirectory1 or
OptionalFile1 to specify files for inclusion in the optional section of the
installer if you have one.

Within the optional ``[CopyLocalFiles]`` section:
-------------------------------------------------

This section is used to copy files in from a local installation of an
application.

**CopyLocalFiles** is used to indicate that this section is enabled. It should
be set to true.

**CopyFromRegPath** is used when the path to the local files is indicated within
a key in the registry. Generally, this will be in the form of
HKLM\\Software\\AppName.

**CopyFromRegKey** is used in conjunction with ``CopyFromRegPath``. It indicates
the Key within the registry path above that should be used.

**CopyFromRegRemoveDirectories** is used to indicate the number of directories
to strip from the Key read in to arrive at the directory that should be copied.
If the Key indicates a path to a file rather than a directory, it should be
increased by one. For example, if the Key generally points to C:\\Program
Files\\AppName\\bin\\AppName.exe and you wish to copy all the files in
C:\\Program Files\\AppName, it would be set to 2: one to remove the file name
AppName.exe and one to remove the 'bin' directory from the path.

**CopyFromDirectory** is used to indicate the local directory to copy into the
portable app. If used in conjunction with the registry entries above, it will be
used as a fallback if the registry entry is missing or doesn't point to a valid
path. This entry is normally in the form ``%PROGRAMFILES%\AppName``. Several
environment variables are available including: %PROGRAMFILES%, %COMMONFILES%,
%DESKTOP%, %WINDIR%, %SYSDIR%, %APPDATA%, %LOCALAPPDATA% and %TEMP%.

**CopyToDirectory** indicates the relative path within the portable app that the
files will be copied to. This is usually in the form ``App\AppName``. If the
directory does not exist, it will be created.

Within the optional ``[DownloadFiles]`` section:
------------------------------------------------

This section is used to download and optionally extract files from the internet.

**DownloadURL** specifies the URL to the file that will be downloaded. It is
normally in the form http://example.com/path/filename

**DownloadName** is the name that will be displayed while the file is
downloaded. This must be a valid DOS name and should not include special
characters like :, ", \\, etc.

**DownloadFilename** is the name of the file that will be used while it is
worked with locally. This should normally be the same as the filename from the
DownloadURL. It is normally in the form filename.exe or filename.zip.

**DownloadMD5** is used to specify the MD5 hash of the file downloaded. This
allows the installer to verify that the file has not changed since the installer
was created. Use of this entry is *highly* recommended.

**DownloadTo** is optionally used if the downloaded file should just be copied
into the portable app as-is. The entry is normally in the form ``App\AppName``.
This entry is not to be used with the extraction entries that follow.

**AdditionalInstallSize** is used to specify the size of the files that will be
added to the files contained within the installer. The entry should be a number
only and be in KB

**Extract1To** and **Extract1File** are used for simple extraction of files from
ZIP files only. The Extract#To entries should specify the relative path to where
the files will go within the installed portable app (typically App\\AppName).
The Extract#File is used to specify the name of the file to extract. No
wildcards are permitted. Up to 10 entries in the form Extract1To/Extract1File,
Extract2To/Extract2File may be made. Extract#To supports the use of ``<ROOT>``
to indicate the app's root directory.

**AdvancedExtract1To** and **AdvancedExtract1Filter** are used for more advanced
extraction from ZIP files as well as many installer EXEs. The AdvancedExtract#To
entries should specify the relative path to where the files will go within the
installed portable app (typically App\\AppName). The AdvancedExtract#Filter
entries are used to specify a filter for the files to be extracted and are in
the same format used by 7-zip. Some examples include *.txt for all text files, *
for all files, *a* for files that contain the letter a, Src\\*.cpp for all cpp
files within the src directory, etc. ** can be used to indicate all files in the
archive recursively (including sub-directories). Up to 10 entries can be made.
AdvancedExtract#To supports the use of ``<ROOT>`` to indicate the app's root
directory.

**DoubleExtractFilename** is used when a downloaded file contains an archive
within an archive. The DoubleExtractFilename should be set to the name of the
archive inside the archive. For example, if you are downloading a file called
setup.exe which contains a file data.zip that has the files needed within it,
DoubleExtractFilename would be set to data.zip. The **DoubleExtract#To** and
**DoubleExtract#Filter** are performed on the extracted archive and are in the same
format as AdvancedExtract1To and AdvancedExtract1Filter above. Up to 10 entries
may be used. DoubleExtract#To supports the use of ``<ROOT>`` to indicate the app's
root directory.

Within the optional ``[Languages]`` section:
--------------------------------------------

Each entry is used to specify whether that language is available as a user is
installing the portable app and appinfo.ini is set to Multilingual. If this
section is omitted, all languages are included. If this section is included,
ENGLISH= is required. All other languages are optional and default to false.

Within the optional ``[DirectoriesToPreserve]`` section:
--------------------------------------------------------

This section specifies directories that will be preserved even if a given
directory (App, Data, Other) is set to be removed on an upgrade. Up to 10
entries in the form of PreserveDirectory1, PreserveDirectory2, etc are
available. Each should be in the relative paths within the app. If you wish to
preserve the directory App\\AppName\\plugins, it would be entered as
``PreserveDirectory1=App\AppName\plugins`` within this section. If no
directories need preserving, this section should be omitted.

Within the optional ``[DirectoriesToRemove]`` section:
------------------------------------------------------

This section specifies directories that will be removed even if a given
directory (App, Data, Other) is set not to be removed on an upgrade. Up to 10
entries in the form of RemoveDirectory1, RemoveDirectory2, etc are available.
Each should be in the relative paths within the app. If you wish to remove the
directory App\\AppName\\locales, it would be entered as
``RemoveDirectory1=App\AppName\locales`` within this section. If no directories
need removing, this section should be omitted.

Within the optional ``[FilesToPreserve]`` section:
--------------------------------------------------

This section specifies files that will be preserved even if a given directory
(App, Data, Other) is set to be removed on an upgrade. Up to 10 entries in the
form of PreserveFile1, PreserveFile2, etc are available. Each should be in the
relative paths within the app. If you wish to preserve the files
App\\AppName\\*.hlp, it would be entered as ``PreserveFile1=App\AppName\*.hlp``
within this section. If no files need preserving, this section should be
omitted.

Within the optional ``[FilesToRemove]`` section:
------------------------------------------------

This section specifies files that will be removed even if a given directory
(App, Data, Other) is set not to be removed on an upgrade. Up to 10 entries in
the form of RemoveFile1, RemoveFile2, etc are available. Each should be in the
relative paths within the app. If you wish to remove the files
App\\AppName\\*.lang, it would be entered as ``RemoveFile1=App\AppName\*.lang``
within this section. If no files need removing, this section should be omitted.

An **End User License Agreement (EULA)** or other licensing file can be
displayed in the PortableApps.com Installer by including an EULA.txt or EULA.rtf
file in the Other\\Source directory. The PortableApps.com Installer will
automatically locate it and configure it for use.

.. _paf-installer-custom:

**Custom Code** may be included with your installer by including a file called
PortableApps.comInstallerCustom.nsh within the Other\\Source directory. This
file is coded in NSIS and can include 3 macros: CustomCodePreInstall (which is
run before installation), CustomCodePostInstall (which is run after
installation) and CustomCodeOptionalCleanup (which is run at the beginning of
installation if the optional section of an installer is not selected, intended
for use in app upgrades when the existing app may have had the optional section
included). In addition to the standard NSIS functions, the following NSIS
features are available: ConfigRead, ConfigReadS, ConfigWrite, ConfigWriteS,
GetParent, GetRoot, VersionCompare and the LogicLib features of NSIS.

The PortableApps.com Installer code itself should not be altered directly within
the confines of it being a PortableApps.com Installer. As always, the source
code is available under the GPL and may be freely modified and used in other
GPL-licensed works.

Every release of an app in PortableApps.com Format must use the current
PortableApps.com Installer. If a larger application is being compiled that has a
longer development and testing time, and a new version of the PortableApps.com
Installer is released during testing of a release the version of the installer
the app is currently using may be kept provided that the new Installer version
is less than 30 days old on the day the application using the older version is
released.

.. _`PortableApps.com/development`: http://portableapps.com/development
