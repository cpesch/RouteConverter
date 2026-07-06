.. index:: 64-bit applications, Making things portable; 64-bit applications

.. _64-bit:

===================
64-bit applications
===================

Many applications now available come in 32-bit and 64-bit builds. In general at
PortableApps.com we don't include 64-bit versions of apps in our packages. Our
reasoning behind this is that almost all 32-bit apps work in 64-bit versions of
Windows, and there is normally little or no performance benefit from running a
native 64-bit app instead of its 32-bit build. So normally including a 64-bit
build as well as a 32-bit build would just add to the size of a package
(approximately doubling it) without any real benefit. For some more explanation
of our policy on 64-bit apps and what happens with them, see `64-bit Software:
Where It Fits Into Portable Apps`_.

There are however some cases where there is benefit in including 64-bit builds
of apps. The most common is for apps that utilise low-level functions in the
operating system which don't work in Wow64 (the 32-bit emulation part of a
64-bit version of Windows). For example, `JkDefrag Portable`_ includes both
32-bit and 64-bit builds of JkDefrag, because the defragmentation must be run
differently between the two architectures. The version to use is automatically
selected by the launcher.

Another app at PortableApps.com which includes both 32-bit and 64-bit builds is
`7-Zip Portable`_. A 32-bit build of 7-Zip does run on a 64-bit version of
Windows, but with the processor-intensive compression, there is a noticeable
difference in performance between the 32-bit build and the native 64-bit build:
on a 64-bit operating system, the 64-bit build compresses and decompresses
archives approximately 5-15% faster than the 32-bit build. However, recall that
the 64-bit version will not run on a 32-bit operating system; this is the reason
why the 64-bit version alone cannot comprise the app. Because the size
difference was fairly small (less than a couple of megabytes), and the benefits
were deemed to be significant enough, 7-Zip Portable is now provided with both
32-bit and 64-bit builds included, automatically selected based on the base
computer's architecture.

To support 64-bit apps, the PortableApps.com Launcher can select a different
program executable to run from the normal one. The configuration for this is in
the launcher.ini keys :ini-key:`[Launch]:ProgramExecutable64` and
:ini-key:`[Launch]:ProgramExecutableWhenParameters64`.

Some apps will have their 32-bit and 64-bit versions next to one another, for
example in ``App\AppName\AppName.exe`` and ``App\AppName\AppName64.exe``;
sometimes however they will be in different directories --
``App\AppName\AppName.exe`` and ``App\AppName64\AppName.exe``.

It is possible that an environment variable will be needed to specify
``%PAL:AppDir%\AppName`` and ``%PAL:AppDir%\AppName64``, depending on the
architecture, so that a configuration file can be updated. If this is required,
it can be done easily with :ref:`custom code <custom-code>`. A variable
``$Bits`` is provided which contains the value ``64`` for 64-bit computers and
``32`` for 32-bit computers. Here is how you might use it (note that it goes in
the :ref:`Init hook <segments-hooks>`; ``$Bits`` is set up in ``.onInit`` and so
it is ready for use)::

   ${SegmentFile}

   ${SegmentInit}
       ${If} $Bits = 64
           ${SetEnvironmentVariablesPath} FullAppDir $AppDirectory\AppName64
       ${Else}
           ${SetEnvironmentVariablesPath} FullAppDir $AppDirectory\AppName
       ${EndIf}
   !macroend

With this code, the environment variables ``FullAppDir``, and also the
additional environment variables ``FullAppDir:ForwardSlash``, :ref:`etc.
<ref-envsub-directory>` will be available for use in things like a
:ini-section:`[FileWriteN]` section.

.. versionadded:: 2.1

.. _`JkDefrag Portable`:
   http://portableapps.com/apps/utilities/jkdefrag_portable

.. _`7-Zip Portable`:
   http://portableapps.com/apps/utilities/7zip_portable

.. _`64-bit Software: Where It Fits Into Portable Apps`:
   http://portableapps.com/node/24371
