.. index:: Custom code

.. _custom-code:

===========
Custom code
===========

The PortableApps.com Launcher is a very powerful launcher and is able to make
almost any software portable. But sometimes, there's something more needed which
just isn't supported by it. In such situations, the PortableApps.com Launcher
still has you covered: you don't need to write an entire custom launcher in
:term:`NSIS` (or another language) just because of that. You can still use the
power of the PortableApps.com Launcher for almost all the launcher, and just
write a small bit of custom code to do the last things which it can't do itself.

If your problem is that you need to disable a segment, because it's doing
something which makes your job impossible, first of all please :ref:`try to find
a better way <help>` as you're probably not doing it how you should. If that
fails, you can `disable hooks and segments`_.

.. _`disable hooks and segments`: `Disabling hooks and segments`_

.. _custom-code-file:

``Custom.nsh``
==============

All modifications to the PortableApps.com Launcher which do not involve
:ref:`debugging <debug>` go in a file ``Custom.nsh`` in the ``Other\Source``
directory of a package.

.. versionchanged:: 2.1
   previously this file was ``Other\Source\PortableApps.comLauncherCustom.nsh``.
   The Generator will move this file to the new location if it exists.

.. _custom-code-segment:

Writing a custom segment
========================

If there is something which you need to do in a launcher which is not possible
in the PortableApps.com Launcher, you can write :term:`NSIS` code for it
yourself but still use the general framework and power of the PortableApps.com
Launcher by writing a custom segment.

To write a custom segment for your application, use the `Custom.nsh`_ file
mentioned above.  You can look at :ref:`other segments <segments>` for guidance
on how to write a segment. This is the general structure for a segment:

::

   ${SegmentFile}

   Var [variables]

   ${Segment[hook]}
       ...
   !macroend

   ${Segment[hook]}
       ...
   !macroend

   ...

1. The first line of the file is ``${SegmentFile}``.

2. Next comes any variables which may be required. Normally no variables will be
   required but some segments need variables.

2. After this comes the hooks. Each hook is implemented like this::

      ${Segment[hook]}
          [segment contents]
      !macroend

   A list of available hooks is available :ref:`here <segments-hooks>`.

3. A segment can use custom macros and Functions if it is desired, but they
   should be clearly identified as part of the segment. The general convention
   is to prefix a segment-specific macro or function with *_segment name_* so
   that the macro "Start" in the segment FilesMove became ``_FilesMove_Start``.
   Such macros and functions as these should come above the variable
   definitions, immediately after the ``${SegmentFile}`` line.

.. _custom-code-disable:

Disabling hooks and segments
============================

If you ever need to disable a segment or hook, you can do so. In general though
if you can possibly avoid doing it you should; you can very easily break the
PortableApps.com Launcher by disabling certain things. In general I would
recommend that you :ref:`ask <help>` before doing it to see if there is a better
way.

All of these changes apply to `Custom.nsh`_.

To disable an inbuilt hook in a segment::

     ${DisableHook} Segment Hook

To disable all hooks in an inbuilt segment (in short, to disable the segment)::

     ${DisableSegment} Segment

.. _custom-code-execute:

Overriding the execution step
=============================

If you need to replace the execution step for an app, you can do so. Avoid doing
it if you can as it will make the app not behave like most apps. Try
:ref:`asking for help <help>` before you do it, as there may be a better way.

To override the Execute function completely, put into
`Custom.nsh`_ code like this::

     ${OverrideExecute}
         ...
     !macroend

You would be well advised to take a look at the Execute function in the
PortableApps.com Launcher before doing this,
``Other\Source\PortableApps.comLauncher.nsi``.

Additional features
===================

In addition to all standard NSIS features, there are a few extra macros
available in the PortableApps.com Launcher which custom code can use.

LogicLib is used extensively in the PortableApps.com Launcher, so LogicLib.nsh
is already included. You can use LogicLib code structures without any extra
code.

Reading values from launcher.ini
--------------------------------

Instead of using ``ReadINIStr`` for reading from the :ref:`launcher.ini file
<ref-launcher.ini>`, you can use ``${ReadLauncherConfig}`` or
``${ReadLauncherConfigWithDefault}``.

``${ReadLauncherConfig}`` is for reading a value from the launcher.ini file; it
uses ``ReadINIStr`` internally, so the same rules apply; to check if a value did
not exist, and is not merely empty, you can use ``${If} ${Errors}`` etc.
Remember to ``ClearErrors`` before depending on the error flag. Here is the
syntax for ReadLauncherConfig::

   ${ReadLauncherConfig} $0 Section Key

``${ReadLauncherConfigWithDefault}`` is as above, but if a value does not exist,
a default value is put in::

   ${ReadLauncherConfigWithDefault} $0 Section Key Default

Allowing extra user configuration
---------------------------------

``${ReadUserConfig}`` is used to get user configuration from the
``AppNamePortable.ini`` file in the root of the package (next to
``AppNamePortable.exe``) which contains values like DisableSplashScreen and
AdditionalParameters. Additional values for user configuration can be introduced
with this::

   ${ReadUserConfig} $0 KeyName

Then the user will be able to have a line in that file like this:

.. code-block:: ini

   KeyName=value

For boolean values (true or false, yes or no), the convention is to make the
allowed values "true" or "false".

Additions like this should be documented in help.html.

.. versionchanged:: 2.1
   ``${ReadUserOverrideConfig}`` became ``${ReadUserConfig}``
