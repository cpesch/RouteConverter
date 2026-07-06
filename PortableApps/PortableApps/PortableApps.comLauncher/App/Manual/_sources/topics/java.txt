.. index:: Java, Making things portable; Java applications

.. _java:

=================================
Making Java applications portable
=================================

There are many different languages and frameworks which applications can be
written in, and they all have different ways of doing things. This document
deals with the considerations in making Java applications portable.

Turn on Java support
====================

If your application uses Java at all, whether it requires it or can merely use
it to benefit if it's there, you will need to turn on Java support in
:ref:`ref-launcher.ini`. To do this you will need to set the value
:ini-key:`[Activate]:Java`; see that page for details on how to turn on Java
support and what the different modes (``try`` and ``force``) mean.

The ``ProgramExecutable``
=========================

When you need to launch a Java application with ``java.exe`` or (more commonly)
``javaw.exe``, set :ini-key:`[Activate]:Java` to ``force`` and then you can set
:ini-key:`ProgramExecutable <[Launch]:ProgramExecutable>` to ``java.exe`` or ``javaw.exe`` and it
will be rewritten to the path to that Java binary.

Build systems and command line arguments
========================================

There are a few different ways of making and building Java applications, each of
which requires different techniques to handle portability.

Eclipse-based applications
--------------------------

These seem to normally accept a command-line argument to the executable
included, to specify the Java location to use. Try putting ``-vm
"%JAVA_HOME%\bin"`` into :ini-key:`CommandLineArguments
<[Launch]:CommandLineArguments>` (your :ini-key:`ProgramExecutable
<[Launch]:ProgramExecutable>` will not be ``java.exe`` or ``javaw.exe``).

.. _java-launch4j:

Launch4J-based applications
---------------------------

(This seems to be most applications.)

These apps run an executable, which discovers Java from the registry, and run
something which creates an executable in the JRE's directory, in a subdirectory
``launch4j-tmp``, which will run the Java application. Try running the base
application (with a local JRE installation) and then use a program such as
Microsoft SysInternals Process Explorer to look at the command line arguments
which it then gets started with. In Process Explorer, right-click on the
executable and click "Properties", and copy the execution string. Also note the
working directory.

The problem with bypassing ``AppName.exe`` and directly calling ``javaw.exe`` is
that :ini-key:`[Launch]:SingleAppInstance` no longer works correctly and must be
set to false. Instead, if mixing local and portable instances is not valid, set
:ini-key:`[Launch]:CloseEXE` to ``AppName.exe``, so that the local version,
still running ``AppName.exe``, will be required to close before the portable
application will start.

Others
------

If you come up against any other techniques or circumstances where other
techniques would be better, please suggest them.

Changing the settings location
==============================

Most Java applications store their settings in the location provided by the Java
``user.home`` constant. Fortunately for us, these values can be set in the
Java command line. Any such value can be written with the ``-D`` argument, like
this: ``-Duser.home="%PAL:DataDir%\settings"`` (remember that quotes are
normally a good idea for paths, just in case they include spaces).

.. _java-java.util.prefs:

``java.util.prefs``
===================

One way of storing settings in Java applications is with ``java.util.prefs``.
This stores its settings in the registry, in
``HKCU\Software\JavaSoft\Prefs\[package path]``, with the package path
backslash-separated, so that for com.company.application, the key is
``HKCU\Software\JavaSoft\Prefs\com\company\application``. With this you must
save the full path, and then prune the tree back to ``HKCU\Software`` as long as
it's empty; this means you will do it like this:

.. code-block:: ini

   [Activate]
   Registry=true

   [RegistryKeys]
   (file name)=HKCU\Software\JavaSoft\Prefs\com\company\application

   [RegistryCleanupIfEmpty]
   1=HKCU\Software\JavaSoft\Prefs\com\company
   2=HKCU\Software\JavaSoft\Prefs\com
   3=HKCU\Software\JavaSoft\Prefs
   4=HKCU\Software\JavaSoft

This example will vary depending on the name of the package which is using
``java.util.prefs``.

An Example
==========

This example is of a hypothetical application called Jest ('cos it's jest a test
app and written in Java too). It's written by a company called JestTech.

Jest stores some things in ``user.home``, but also uses ``java.util.prefs`` to
store its settings in the registry. It requires up to one GB of a certain type
of memory (the type that requires a command line argument ``-Xmx1024m``). Its
class path includes a couple of jar files in its own directory (in the portable
package they end up as App\\Jest\\lib\\foo.jar and App\\Jest\\lib\\bar.jar with the
main package being App\\Jest\\lib\\jest.jar), but because of the deployment method
used it requires ``javaws.jar`` from the JRE libraries. The Jest main class is
``com.jesttech.jest.Jest``. The normal distribution method in Windows is with
Launch4J in such a way that the executable is called Jest.exe (due to settings
being in the registry, this means that Jest.exe must be closed before we start
the portable version).

One last thing: there's an opportunity for convenience finding of documents by
setting, in the registry key ``HKCU\Software\JavaSoft\Prefs\com\jesttech\jest``,
the value ``docsdir``. It must be formatted in just the way ``java.util.prefs``
:ref:`likes it <ref-envsub-java.util.prefs>`.

Here's what we'd put into ``App\AppInfo\Launcher\JestPortable.ini``.

.. code-block:: ini

   [Launch]
   ProgramExecutable=javaw.exe
   CommandLineArguments=-Duser.home="%PAL:DataDir%\settings" -Xmx1024m -classpath "lib\Jest.jar;lib\foo.jar;lib\bar.jar;%JAVA_HOME%\lib\javaws.jar" com.jesttech.jest.Jest
   WorkingDirectory=%PAL:AppDir%\Jest
   CloseEXE=Jest.exe
   WaitForProgram=true
   WaitForOtherInstances=false

   [Activate]
   Java=require
   Registry=true

   [FileWrite1]
   Type=Replace
   File=%PAL:DataDir%\settings\jest.reg
   Find=%PAL:LastDrive%//
   Replace=%PAL:CurrentDrive%//

   [RegistryKeys]
   jest=HKCU\Software\JavaSoft\Prefs\com\jesttech\jest

   [RegistryCleanupIfEmpty]
   1=HKCU\Software\JavaSoft\Prefs\com\jesttech
   2=HKCU\Software\JavaSoft\Prefs\com
   3=HKCU\Software\JavaSoft\Prefs
   4=HKCU\Software\JavaSoft

   [RegistryValueWrite]
   HKCU\Software\JavaSoft\Prefs\com\jesttech\jest\docsdir=%PortableApps.comDocumentsDir:java.util.prefs%
