.. _paf-icons:

3. Icons
========

Within the ``AppNamePortable\App\AppInfo`` directory, the icons used by the
PortableApps.com Installer and within the PortableApps.com Menu are located. The
icons are included in ICO and PNG format. The main icon is called
``appicon.ico``, ``appicon_16.png``, ``appicon_32.png`` and
``appicon_128.png``\*. If the application also uses multiple icons (as detailed
above), these additional icons are named as ``appicon1.ico``
(``appicon1_16.png`` and ``appicon1_32.png``), ``appicon2.ico``, etc. The
numbers correspond to Start1, Start2, etc within the Control section.

*\* Note that the 128px variant is optional and not required by the installer to
compile. However, publishers are encouraged to include this high-resolution icon
for upcoming features in the platform.*

The PNG icons are 16x16 and 32x32 respectively and are in True Color format with
alpha transparency.

The ICO file is in Windows ICO format and contain the following 6 required
formats as well as the optional Vista format if desired:

* 16px - 256 color (8-bit)
* 32px - 256 color (8-bit)
* 48px - 256 color (8-bit)
* 16px - True Color + Alpha (32-bit / XP format)
* 32px - True Color + Alpha (32-bit / XP format)
* 48px - True Color + Alpha (32-bit / XP format)
* 256px - True Color + Alpha PNG (32-bit PNG / Vista format) \*OPTIONAL

*\* The 256px alpha size is optional. It is used by Windows Vista to display
large and extra large icon sizes. Some publishers may wish to use include it for
completeness but end users won't normally see it.*

**ExtractIcon Note** - In packages that make use of the ExtractIcon feature
within appinfo.ini, the appicon.ico and PNG versions of the icon will not be
used and may be omitted. A generic appicon.ico will be included for backwards
compatibility.
