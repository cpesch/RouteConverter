.. ini-section:: [FileWriteN]

[FileWrite\ *N*]
================

For writing data to files. The values which must be set depend on the
:ini-key:`Type <[FileWriteN]:Type>` specified below.

.. ini-key:: [FileWriteN]:Type

Type
----

| Values: ``ConfigWrite``, ``INI``, ``Replace``, ``XML attribute``, ``XML text``
| Mandatory.

----

Specify the type of file writing which is to be used:

* ``ConfigWrite``: write arbitrary data to a file, the line on which to write
  being selected as one starting with the :ini-key:`Entry <[FileWriteN]:Entry>`.

* ``INI``: write a string to an INI file.

* ``Replace``: search for a string and replace it with another string in a file.
  This is particularly useful for updating drive letters and configuration paths
  (using :env:`%PAL:Drive% <PAL:Drive>` and :env:`%PAL:LastDrive%
  <PAL:LastDrive>`)

* ``XML attribute``: write an attribute to an XML file. Only available when
  :ini-key:`[Activate]:XML`\ =\ ``true``.

* ``XML text``: write a text node to an XML file. Only available when
  :ini-key:`[Activate]:XML`\ =\ ``true``.

.. ini-key:: [FileWriteN]:File

File
----

| Mandatory.
| |envsub|

----

Specify the file in which the modification will be made. This file must exist
before any writing will be done. If it does not exist, the ``[FileWriteN]``
section will be skipped.

.. ini-key:: [FileWriteN]:Entry

Entry
-----

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``ConfigWrite``.

----

The :ini-key:`Value <[FileWriteN]:Value>` will be written to a line starting
with this value, or if it is not found, at the end of the file. This should be
set to the text to search for at the start of a line. In an INI-style file, this
would be ``key=``, and in an XML file it might be ``'     <config
id="something">'``; note that you **must** include any leading
whitespace which will be in the file, and if there is any leading or trailing
whitespace you must quote the string with single (``'``) or double (``"``)
quotes.

.. ini-key:: [FileWriteN]:Section

Section
-------

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``INI``.

----

The INI section to write the value to.

.. ini-key:: [FileWriteN]:Key

Key
---

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``INI``.

----

The INI key to write the value to.

.. ini-key:: [FileWriteN]:Value

Value
-----

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``ConfigWrite``,
  ``INI``, ``XML attribute``, ``XML text``.
| |envsub|

----

The value which will be written to the file. If dealing with :ini-key:`Type
<[FileWriteN]:Type>`\ =\ ``ConfigWrite``, you should remember with things like
XML files that you will normally need to close the tag, for example
``%PAL:DataDir%\settings</config>``. In such cases you can also try using the
inbuilt XML support.

.. ini-key:: [FileWriteN]:Find

Find
----

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``Replace``.
| |envsub|

----

The string to search for.

.. ini-key:: [FileWriteN]:Replace

Replace
-------

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``Replace``.
| |envsub|

----

The string to replace the search string with. If, after environment variable
replacement, this is the same as the :ini-key:`Find <[FileWriteN]:Find>` string,
the replacement will be skipped (e.g. if you use it to update drive letters and
it's on the same letter).

.. ini-key:: [FileWriteN]:Attribute

Attribute
---------

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``XML attribute``
| |envsub|

----

The attribute which will be set inside the element identified by the given
XPath. See :ref:`xml` for more details.

.. ini-key:: [FileWriteN]:XPath

XPath
-----

| Mandatory for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``XML attribute``, ``XML text``.

----

Specify the XPath_ to find the place to write to. It is a good idea to make
sure that you have a solid understanding of how XPaths work and how to use them
before writing one.

For information about what this should look like, see :ref:`xml`.

.. _XPath: http://en.wikipedia.org/wiki/XPath

.. ini-key:: [FileWriteN]:CaseSensitive

CaseSensitive
-------------

| Values: ``true`` / ``false``
| Default: ``false``
| Applies for :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``ConfigWrite``, ``Replace``.
| Optional.

----

Case sensitive searches are somewhat faster than case-insensitive searches. If
you can do a case-sensitive ConfigWrite or find and replace, do.

Concerning drive letter updates, you can't guarrantee what case the drive letter
will be and so it will not normally be practical to do a case sensitive
replacement for drive letters.

.. ini-key:: [FileWriteN]:Encoding

Encoding
--------

| Values: auto / ``ANSI`` / ``UTF-16LE``
| Default: auto
| Applies to :ini-key:`Type <[FileWriteN]:Type>`\ =\ ``Replace``.
| Optional.

----

If you need to find and replace in a Unicode (UTF-16LE) file, but it is missing
the byte-order mark (BOM), set the encoding here as ``UTF-16LE``; if a file for
some reason has the UTF-16LE BOM, ``U+FFFE``, at the start of the file, but
isn't really UTF-16LE, set this to ``ANSI``. Otherwise, omit this value and the
encoding will be automatically detected by checking for the BOM.

For UTF-8 files, leave this value out. The value ``ANSI`` isn't really ANSI,
it's just "normal", which includes ANSI, UTF-8 and really anything which doesn't
use null bytes all over the place.

This value only works with the :ini-key:`Type <[FileWriteN]:Type>` ``Replace``;
both ``ConfigWrite`` and ``INI`` automatically detect the encoding of the file
(this requires that the file start with the UTF-16LE BOM, ``U+FFFE``).

.. versionchanged:: 2.1
   previously ``ConfigWrite`` was not able to write to UTF-16LE files.
