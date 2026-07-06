.. _paf-appinfo:

2. AppInfo.ini (App Configuration)
==================================

The portable app makes available its configuration information to the
PortableApps.com Platform by way of the ``AppInfo`` details. Within the
``AppNamePortable\App`` directory, an ``AppInfo`` directory contains an
``appinfo.ini`` file as well as any icons used within the menu (explained in
:ref:`Section 3 <paf-icons>`). The ``appinfo.ini`` file consists of the
following:

.. code-block:: ini

   [Format]
   Type=PortableApps.comFormat
   Version=2.0

   [Details]
   Name=AppName Portable
   AppId=AppNamePortable
   Publisher=App Developer & PortableApps.com
   Homepage=PortableApps.com/AppNamePortable
   Category=Utilities
   Description=AppName Portable is a tool that does something.
   Language=Multilingual
   Trademarks=
   InstallType=

   [License]
   Shareable=true
   OpenSource=true
   Freeware=true
   CommercialUse=true
   EULAVersion=1

   [Version]
   PackageVersion=1.2.0.1
   DisplayVersion=1.2 Release 1

   [SpecialPaths]
   Plugins=NONE

   [Dependencies]
   UsesJava=false
   UsesDotNetVersion=

   [Control]
   Icons=1
   Start=AppNamePortable.exe
   ExtractIcon=App\AppName\AppName.exe

Within the ``appinfo.ini`` file, the entries are as follows:

Within the ``[Format]`` section:
--------------------------------

**Type** is the type of configuration file this is (only PortableApps.comFormat
is valid at this time).

**Version** is the version of this format the file is in (currently 2.0).

Within the ``[Details]`` section:
---------------------------------

**Name** is the name of your app as it appears in the PortableApps.com Menu

.. _paf-appinfo-appid:

**AppID** is the globally unique id for the application. Apps released by
PortableApps.com or directly by the publisher of the regular version of the
software will generally just be the name without spaces. Apps released by other
entities must be AppNamePortable-example.com where example.com is their company
domain. Apps released by individuals on PortableApps.com that aren't working
towards making it an official PortableApps.com release must be
AppNamePortable-username where username is the PortableApps.com username. AppIDs
may contain letters, numbers, periods ( . ), dashes ( - ), plus signs ( + ) and
underscores ( _ ).

**Publisher** is the name of the app publisher as it appears in a hover tip in
the next PortableApps.com Platform release and within the app details screen. If
you are repackaging an app written by someone else, they should also be listed.

**Homepage** is the homepage of the portable app (not the base app)

**Category** is the category that the application falls into within the
PortableApps.com Platform. Valid entries are: Accessibility, Development,
Education, Games, Graphics & Pictures, Internet, Music & Video, Office, Security
or Utilities. Only these \*exact* entries are supported and should be used
regardless of the default language of the base app (even if this is a German
application, it should still use the English translation of the category).

**Description** is a brief description of what the application is. Maximum of
512 characters.

**Language** is the language the app is available in. If the app is
multilingual, it should be specified as Multilingual. The language string must
be in a specific format. The following strings are available: Afrikaans,
Albanian, Arabic, Armenian, Basque, Belarusian, Bosnian, Breton, Bulgarian,
Catalan, Cibemba, Croatian, Czech, Danish, Dutch, Efik, English, Estonian,
Farsi, Finnish, French, Galician, Georgian, German, Greek, Hebrew, Hungarian,
Icelandic, Igbo, Indonesian, Irish, Italian, Japanese, Khmer, Korean, Kurdish,
Latvian, Lithuanian, Luxembourgish, Macedonian, Malagasy, Malay, Mongolian,
Norwegian, NorwegianNynorsk, Pashto, Polish, Portuguese, PortugueseBR, Romanian,
Russian, Serbian, SerbianLatin, SimpChinese, Slovak, Slovenian, Spanish,
SpanishInternational, Swahili, Swedish, Thai, TradChinese, Turkish, Ukranian,
Uzbek, Valencian, Vietnamese, Welsh, Yoruba.

**Trademarks** (optional) is any trademark notifications that should appear. For
example, HappyApp is a trademark of Acme, Inc.

**InstallType** (optional) is if you would like the app listed as a specific
install type within the menu. For some apps that are packaged by language (like
Mozilla Firefox), the language may be specified on this line. In installers with
optional components, this line is automatically updated by the installer based
on the details within installer.ini (see below). The InstallType will be shown
within the PortableApps.com Platform in the app's details.

Within the ``[License]`` section:
---------------------------------

(all values are either ``true`` or ``false``)

**Shareable** is whether the app is allowed to be copied from one drive to
another (without the ``Data`` directory)

**OpenSource** is whether the app is fully open source under an OSI approved
license

**Freeware** is whether the app is free (no cost)

**CommercialUse** is whether the app is allowed to be used in a commercial
environment

**EULAVersion** (optional) is used to indicate the version of the End User
License Agreement used if you include EULA.txt or EULA.rtf and require the user
to agree to a license to install. If you are using an EULA and omit this entry,
the default, 1, will be used.

Within the ``[Version]`` section:
---------------------------------

**PackageVersion** is the version of the package itself. This must be in 1.2.3.4
format with no other characters and must be incremented with each public
release.

**DisplayVersion** is the user-friendly version that is generally used to
describe the version. So, a released app may have a DisplayVersion of ``2.4
Revision 2`` but a PackageVersion of ``2.4.0.2``.

Within the optional ``[SpecialPaths]`` section:
-----------------------------------------------

**Plugins** (optional) is the path to an app's user-added plugins directory if
it is within the App directory (as it is with applications like Firefox). This
path is excluded when the installer calculates how much free space is needed for
an upgrade. If there is no plugins directory, this value should be omitted from
appinfo.ini.

Within the optional ``[Dependencies]`` section:
-----------------------------------------------

**UsesJava** (optional) specifies whether the portable app makes use of `Java
Portable`_. If needed, this value should be set to true. If not needed, it
should be omitted or set to false.

**UsesDotNetVersion** (optional) specifies which minimum version of the .NET
framework the application requires. If needed, this value should be set to the
minimum version the application requires (example: 1.1, 2.0, 3.0, 3.5). If not
needed, this value should be omitted.

*Please note that PortableApps.com does not currently accept .NET-based apps for
inclusion in our application listings. Most PCs "in the wild" do not have .NET
available, so portable apps that require .NET will not function on them.*

.. _paf-appinfo-control:

Within the ``[Control]`` section:
---------------------------------

**Icons** is the number of icons that the app has in the PortableApps.com Menu

**Start** is the command line to execute to start the app relative to the
``AppNamePortable`` directory. This will typically be ``AppNamePortable.exe``.

**ExtractIcon** (optional) is used if the app's main icon is not appicon.ico
within the AppInfo directory. This should only be used when legally required for
launchers in specific apps as it cause the application to be accessed more
slowly. When not needed it should be left out of appinfo.ini.

Sometimes, an application will have multiple icons, as is the case with
OpenOffice.org Portable. In this case, the last section of the appinfo.ini file
will look like:

.. code-block:: ini

   [Control]
   Icons=2
   Start=AppNamePortable.exe
   Start1=AppNamePortable.exe
   Name1=AppName Portable
   Start2=AppNamePortable2.exe
   Name2=AppName Portable Other Part

**Icons** is still the number of icons to be shown in the PortableApps.com Menu

**Start** is the command line to execute for the main application

**Start1** is the command line for the first icon (often the same as Start)

**Name1** is the name to show in the menu for the first icon

**Start2** is the command line for the second icon

**Name2** is the name to show in the menu for the second icon

Like the main icon, ExtractIcon1, ExtractIcon2, etc can be used where legally
required. These should not normally be used or included.

.. _`Java Portable`: http://portableapps.com/apps/utilities/java_portable
