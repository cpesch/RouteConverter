.. index:: XML

.. _xml:

Dealing with XML data
=====================

The PortableApps.com Launcher can deal with writing values to XML files and
reading values from XML files.

.. versionadded:: 2.1

Enabling XML support
--------------------

To enable XML support for your launcher, set :ini-key:`[Activate]:XML` to
``true``.

Sections that support XML
-------------------------

XML is supported by the PortableApps.com Launcher for reading and writing
configuration variables; for reading language strings, there is
:ini-section:`[LanguageFile]`. For writing any strings (language or otherwise),
there is :ini-section:`[FileWriteN]`.

Example
-------

FreeCol is one app which stores settings in an XML file. For language switching,
it has the :ini-section:`[LanguageFile]` section and a
:ini-section:`[FileWriteN]` section.

Here are those XML-specific parts of FreeCol configuration:

.. code-block:: ini

   [LanguageFile]
   File=%PAL:DataDir%\freecol\freecol\options.xml
   Type=XML attribute
   XPath=/clientOptions/languageOption[@id="model.option.languageOption"]
   Attribute=value

   [FileWrite1]
   File=%PAL:DataDir%\freecol\freecol\options.xml
   Type=XML attribute
   XPath=/clientOptions/languageOption[@id="model.option.languageOption"]
   Attribute=value
   Value=%PAL:LanguageCustom%

This will work on an XML file like this:

.. code-block:: xml

   <clientOptions>
       <!-- ... -->
       <languageOption
           id="model.option.languageOption"
           value="(language)"/>
       <!-- ... -->
   </clientOptions>


.. index:: Custom code; XML

Using XML in custom code
------------------------

It's possible that you'll need to deal with XML files in custom code; when
writing to an XML file, if you can, set an environment variable with
``${SetEnvironmentVariable}`` or ``${SetEnvironmentVariablesPath}`` and use the
environment variable in a normal :ini-section:`[FileWriteN]` section. However,
if this is not possible, you can use the write macros below. The NSIS code to
read or write is like this (XML must be activated, of course)::

   ${XMLReadAttrib} filename xpath attribute $output

   ${XMLReadText} filename xpath $output

   ${XMLWriteAttrib} filename xpath attribute value

   ${XMLWriteText} filename xpath value


.. index:: XML XPaths

.. _xml-xpaths:

XPaths in XML
-------------

XPaths are a way of selecting an element (or attributes, but we only deal with
elements here) in an XML tree.

From the FreeCol example above, the XPath is
``/clientOptions/languageOption[@id="model.option.languageOption"]``. This
matches an XML document with the root element ``<clientOptions>`` and directly
under it a ``languageOption`` element with the atttribute ``id`` set to
``model.option.languageOption``. In other words, the simplest XML tree which
could match that XPath is this:

.. code-block:: xml

   <clientOptions>
       <langugeOption id="model.option.languageOption"/>
   </clientOptions>

Note that when an attribute is being written to or read from, the XPath used
does **not** include the attribute. That is given as a separate value
(``Attribute``).

For more information about XPaths, see the `Wikipedia article on XPaths`_ and
links that it has.

.. _Wikipedia article on XPaths: http://en.wikipedia.org/wiki/XPath_1.0
