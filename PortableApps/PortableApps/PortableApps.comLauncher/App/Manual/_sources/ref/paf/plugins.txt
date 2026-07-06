.. _paf-plugins:

7. Plugin Installers
====================

In addition to standard installers, the PortableApps.com Installer can be used
for plugin installers to add files to a portable app. This is accomplished via a
file called plugininstaller.ini within the Other\\Source directory. This file
can contain all of the entries within the appinfo.ini and installer.ini files
described above combined into a single file. One addition to the file is within
the [Details] section where an entry called PluginName= is made. This should be
the name of the plugin, for example: Adobe Flash for Firefox Portable. The
[MainDirectories] removal options all default to false for plugin installers. If
an EULA is needed for the plugin, instead of EULA.txt or EULA.rtf, the files
PluginEULA.txt or PluginEULA.rtf should be used.

To create a plugin installer, create a directory layout similar to the portable
app that the plugin is used with including the App, App\\AppName, Data, Other,
etc directories. Then place only the files to be included in the plugin
installer in their appropriate location. The App\\AppInfo directory should be
empty as it is used only by the main app. Any custom code should be in a file
called PortableApps.comInstallerPluginCustom.nsh. Finally, create a file
plugininstaller.ini with the entries that would normally be in appinfo.ini and
installer.ini above and compile as normal.

Additionally, a CommonFiles installer, which will install to
X:\\PortableApps\\CommonFiles is possible by adding an entry
``PluginType=CommonFiles`` to the details section. This is for use with specific
plugins that are used by multiple apps (Java, for example) as designated by
PortableApps.com. In this case, The [MainDirectories] removal option for App is
set to true by default and will remove the entire
X:\\PortableApps\\CommonFiles\\AppID directory (which is usually desired for
CommonFiles plugins.
