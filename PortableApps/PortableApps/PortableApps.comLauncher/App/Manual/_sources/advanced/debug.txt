.. index:: Debugging

.. _debug:

=======================================
Debugging the PortableApps.com Launcher
=======================================

To debug the PortableApps.com Launcher, you will need the normal prerequisites
for working with the PortableApps.com Launcher. See :ref:`install-launcher` for
details on that process.

Once you have a compile environment set up for the PortableApps.com Launcher,
you can recompile it with debugging flags turned on. All debug flags go in the
`Debug.nsh`_ file. This file should contain :ref:`debug flags <debug-flags>` as
listed below, like this::

   !define DEBUG_ALL

When you compile the Launcher with the Generator, it will find this file and
turn on debugging.

Remember to remove the debug file when doing release builds, or else people will
end up with a build with debugging enabled, which is unlikely to be what you
wanted.

Debug messages will be output to the screen in message boxes and to a file
``Data\debug.log`` in your package unless otherwise specified by
``DEBUG_OUTPUT`` below.

.. _debug-file:

``Debug.nsh``
=============

All modifications to the PortableApps.com Launcher involving debugging go in a
file ``Debug.nsh`` in the ``App\AppInfo\Launcher`` directory of a package.

**Note:** I am considering relocating this to ``App\AppInfo\Launcher\Debug.nsh``
before the release of 2.1. In a later release I plan on integrating it into the
user interface of a utility which will do all that the Generator does and more,
and then where it is won't matter so much.

.. versionchanged:: 2.1
   previously this file was ``Other\Source\PortableApps.comLauncherDebug.nsh``.
   The Generator will move this file to the new location if it exists.

.. _debug-flags:

Debug flags
-----------

Here is a list of the debug flags available. See above for how to enable them.

``DEBUG_ALL``
   Debug (almost) everything. For the sake of verbosity, the "About to execute
   segment" and "Finished executing segment" debug messages are not shown unless
   ``DEBUG_SEGWRAP`` is turned on.
   
   This is equivalent to ``DEBUG_GLOBAL`` and all
   ``DEBUG_SEGMENT_[segment name]`` flags being turned on.

``DEBUG_SEGWRAP``
   Show debug messages to announce when a :ref:`segment <segments>` is
   about to be executed and when it has finished.

``DEBUG_OUTPUT`` (values: ``file``, ``messagebox``, nothing)
   By default debugging will write its output to a file ``Data\debug.log`` in
   the portable application package and show a message box which pauses
   execution and allows you to terminate execution. If you want it to only log
   to a file, set this to ``file``, like this::
   
      !define DEBUG_OUTPUT file

   If you want to only show the message boxes, set this to ``messagebox``, like
   this::

      !define DEBUG_OUTPUT messagebox

   Any other value will cause debugging messages to not be shown. If you want
   both, leave this value out.

To debug only certain :ref:`segments <segments>`, there are more flags:

``DEBUG_GLOBAL``
   Debug outside all segments.

``DEBUG_SEGMENT_[segment name]``
   Debug the segment given by ``[segment name]``, e.g.
   ``DEBUG_SEGMENT_RunAsAdmin`` to debug the RunAsAdmin segment.
