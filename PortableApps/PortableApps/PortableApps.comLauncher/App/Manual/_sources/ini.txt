.. _ini:

=========
INI files
=========

This document contains explanation of the INI file format.

The INI file format is a plain-text data storage format used primarily in
Microsoft Windows for storing configuration. It is used extensively by
PortableApps.com in various products.

* PortableApps.com Format: :ref:`App\\AppInfo\\appinfo.ini <paf-appinfo>`

* PortableApps.com Installer: :ref:`App\\AppInfo\\installer.ini <paf-installer>`

* PortableApps.com Launcher: :ref:`App\\AppInfo\\Launcher\\launcher.ini
  <ref-launcher.ini>`

In this documentation, fully qualified key names (those including the section
name) are written as ``[Section]:Key``, or if including the value, as
``[Section]:Key=Value``.

Basic syntax
============

INI files are divided into sections_, which contain lines of `keys and values`_.

Inline comments are not permitted, but comment lines are. Comment lines start
with a semicolon (``;``).

.. code-block:: ini

   [SectionOne]
   KeyOne=value
   KeyTwo="  value"

   [SectionTwo]
   ; Comments can go in like this
   KeyOne=more values

.. _ini-sections:

Sections
========

Section lines are indicated by a left square bracket at the start of the line,
the section name (arbitrary text) and then a right square bracket;
``[SectionName]``.  The convention with Windows and PortableApps.com is to use
CamelCase for section names.

.. _ini-keys:

Keys
====

Key/value lines are lines which are not `section lines`_. The key comes first,
by convention in CamelCase, then arbitrary whitespace (by convention omitted
entirely in the INI format), an equals sign (``=``), arbitrary whitespace
(omitted by convention), and then the value.  Thus the normal way is
``Key=value``. The value can be quoted; if its first and last characters are the
same, as a single or double quote, ``'`` or ``"``. If this is the case, the
quotes are removed. It is purely a character check, though, not a full quoted
string validation, so something like ``Key='It's going to work'`` will work
fine. If you escape the character as ``\'`` or anything like that, it will
remain like that, and will be read as ``It\'s going to work``. Regarding which
type of quotation mark to use, the author prefers in such situation to use the
quote style which is not used (or used less) in the string, so ``Key="It's going
to work"`` (read as ``It's going to work``) or ``Key='"quoted string"'`` (read
as ``"quoted string"``). This last point is most important for the
PortableApps.com Launcher; for example, it is mentioned in
:ini-key:`[Launch]:CommandLineArguments`, as quoting paths in that can be very
important for making sure apps will work in paths with spaces.

Using INI files
===============

In NSIS
-------

In :term:`NSIS`, there are two main commands for dealing with INI files;
``ReadINIStr`` to read values and ``WriteINIStr`` to write values. When writing
custom code for the PortableApps.com Installer or the PortableApps.com Launcher
you can use these. Also in the PortableApps.com Launcher a few specialty macros
are available; ``${ReadLauncherConfig}``, ``${ReadLauncherConfigWithDefault}``
and ``${ReadUserConfig}``. See :ref:`custom-code` for details on using them.

In general
----------

The Windows API provides a number of functions for dealing with INI files;
GetPrivateProfileString_ and WritePrivateProfileString_ are used for reading and
writing values. Other functions are available from that page; the intricate
details of them is beyond the scope of this documentation.

.. _`section lines`: Sections_
.. _`keys and values`: Keys_

.. _GetPrivateProfileString:
   http://msdn.microsoft.com/en-us/library/ms724353(v=VS.85).aspx

.. _WritePrivateProfileString:
   http://msdn.microsoft.com/en-us/library/ms725501(v=VS.85).aspx

