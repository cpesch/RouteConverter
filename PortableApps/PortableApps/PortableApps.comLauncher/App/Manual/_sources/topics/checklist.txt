.. index:: Release checklist

.. _release-checklist:

=================
Release checklist
=================

Here is a checklist of some things to check over before releasing a portable
app. For any other issues, try going through :ref:`troubleshooting`. This also
makes a good checklist for testers.

Does it validate against the :ref:`paf`?
   It saves time later if it's in PortableApps.com Format to start with: you'll
   need to fix it afterwards anyway if you want to have it released.

Does it run?
   There's not much point in releasing a package which doesn't work. Try to test
   it (or at least run it) on as many computers and operating systems as you
   can, with the software installed locally and not, 32-bit and 64-bit, and so
   on.

Does it run from the PortableApps.com Platform?
   If not, a common cause of failure is having the wrong value in
   :ref:`appinfo.ini, [Control] <paf-appinfo-control>`.

Does it run by running the launcher executable directly?
   Many users don't use the PortableApps.com Platform. Thus it's a good idea to
   make sure that it works without it.

Does a different working directory break things?
   Try running the executable while the working directory is not its own
   directory -- for example, start Command Prompt and run the launcher
   executable without changing the directory. If there are any symptoms of
   malfunction, your app may need the working directory to be set to a special
   value (see :ini-key:`[Launch]:WorkingDirectory`).

Does it run from a path with spaces in it?
   If not, check in launcher.ini that you have quoted paths in values like
   :ini-key:`[Launch]:CommandLineArguments`.  If it's still not working and the
   base app doesn't work if you install it to a path with spaces, set
   :ini-key:`[Launch]:NoSpacesInPath`\ ``=true``.

Is data stored in the Data directory?
   Make sure data isn't stored in the App directory. This is a core part of the
   usefulness of the PortableApps.com Format and PortableApps.com Installer.

Does a fresh installation work?
   Run the installer and install to an empty directory. Then try running the app
   and make sure it works. If it doesn't, make sure that the app and data are
   properly separated.

Does nothing go wrong in upgrading from a previous version?
   Try running the installer and installing over another installation of the
   portable app. No settings should be lost by doing this, and the app should
   run properly. If anything goes wrong, you may need to use :ref:`custom
   installer code <paf-installer-custom>` to update paths, or you may just need
   to make sure data isn't stored in the App directory (see the above two
   points).

Does it run from a different drive letter?
   A key feature for portable applications is being able to run from different
   drive letters. If your app can't do that, you're in trouble.  A
   :ini-section:`[FileWriteN]` ``Replace`` section tends to be the way this is
   achieved.

Does it run if you move the directory?
   Prior to the PortableApps.com Launcher, little attention has been paid to
   supporting moving the directory (e.g. from C:\Users\user\Desktop to
   X:\PortableApps). It is not a requirement that you support directory moving,
   but it is generally fairly easy to do and is often already supported. It's
   recommended that you make sure that this works and set
   :ini-key:`[Launch]:DirectoryMoveOK` to ``yes``, or determine that it's too
   hard to fix and set it definitively to ``no`` which will explicitly block
   the user from running it and tell them it breaks things.

Does it run on different operating systems?
   Where possible you should test apps on multiple operating systems,
   especially one of 2000 or XP and one of Vista and 7, so that you get the two
   path schemes (e.g. ``C:\Documents and Settings\Username\Application Data``
   and  ``C:\Users\Username\AppData\Roaming``). Testing on 32- and 64-bit
   systems is also helpful where possible. Support for running in Linux with
   Wine is not mandatory but it can be good to know from the start if an app
   can run in it.

Other helpful things
====================

Language switching:
   If an app is multilingual, it's good if the launcher supports automatic
   language switching. Refer to :ref:`languages` for more details about
   implementing this.

Support for directory moving:
   While not mandatory, if you can make an app support moving the installation
   directory, it's good to. See "Does it run if you move the directory?" above.

Suggestions and improvements
============================

If you have any suggestions for improvements to this checklist (or any of the
rest of the manual), please :ref:`get in contact with Chris Morgan <help>`.
