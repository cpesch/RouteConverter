.. _glossary:

Glossary
========

Here you can find explanation for various terms used in this manual.

General
-------

.. glossary::

   PortableApps.com
      PortableApps.com provides free portable software. PortableApps.com is a
      trademark of Rare Ideas, LLC. The :term:`PortableApps.com Launcher` is a
      product of PortableApps.com.

   PortableApps.com Format
      The format in which PortableApps.com releases must be, providing a common
      arrangement for all PortableApps.com applications so that data can be
      backed up easily and the PortableApps.com Platform can easily find
      application details. See :ref:`paf` for details.

   PortableApps.com Launcher
      A universal launcher for running portable applications without needing to
      write code (this is what this documentation is for). It's configured by an
      :term:`INI` file.

   INI
      A plain-text data storage technique used primarily in Microsoft Windows
      for storing configuration. Used extensively by PortableApps.com. See
      :ref:`ini` for a discussion of the format along with details of how to use
      it.

   NSIS
      `Nullsoft Scriptable Installer System`_. A programming language generally
      used for installers but used at PortableApps.com for various products
      including the PortableApps.com Launcher. :term:`Unicode NSIS` is a Unicode
      branch of NSIS and is the preferred build of NSIS for usage at
      PortableApps.com. `NSIS Portable`_ is available as an official release
      from PortableApps.com, in ANSI and Unicode builds.

   Unicode NSIS
      A Unicode branch of :term:`NSIS`, available from scratchpaper.com_; a
      portable edition is available as `NSIS Portable`_ (Unicode). All new
      PortableApps.com projects use Unicode NSIS instead of ANSI NSIS builds.

   Environment variable
      An environment variable is a dynamic system variable. The PortableApps.com
      Launcher utilises environment variables to aid string replacement. See
      :ref:`ref-envsub` for details on the use in launcher.ini values and
      :ini-section:`[Environment]` for a section in launcher.ini which can be
      used to set environment variables.

   Splash screen
      An image which appears on the user's screen while an application is
      starting, generally to give an indication that something is happening.
      See :ref:`splash-screen` for more details on their use in PortableApps.com
      applications.

.. _`Nullsoft Scriptable Installer System`: http://nsis.sourceforge.net
.. _`NSIS Portable`: http://portableapps.com/apps/development/nsis_portable
.. _scratchpaper.com: http://scratchpaper.com

Releases
--------

.. glossary::

   Development Test
      A test release of a portable application, generally of alpha or beta
      quality. When a portable application is first developed, it bears this
      label; in appinfo.ini, its ``DisplayVersion`` should end with
      ``Development Test N``, where N is the Development Test release number.
      The Development Test release number starts at 1 and should go back down
      to 1 again if there is a new release of the base application. These
      releases may be very buggy and may also contain malware and so it is
      generally a good idea to scan them before testing.

   Pre-Release
      A release of a portable application after it has gone through the
      :term:`Development Test` stage, just before it becomes an :term:`official
      PortableApps.com release`. Generally stable and deemed ready for release
      after. In appinfo.ini, its ``DisplayVersion`` should end with
      ``Pre-Release N``, where N is the Pre-Release release number. The
      Pre-Release release number starts at 1 and should go back down to 1 again
      if there is a new release of the base application. After final testing,
      the application will be made an :term:`official PortableApps.com
      release`.

   Official PortableApps.com release
      Once an application has passed through testing as a :term:`Development
      Test` and as a :term:`Pre-Release`, it is released officialy at
      PortableApps.com as a supported portable application. It will have its
      own :term:`splash screen` (see :ref:`splash-screen` for details). The
      ``DisplayVersion`` in appinfo.ini should not have any appendage unless a
      :term:`revision` is released.

   Revision
      If an :term:`official PortableApps.com release` is found to have problems
      or new features must be added for compatibility with the PortableApps.com
      Platform, or for some other reason, a revision is released. The only
      difference in the released package is that the ``DisplayVersion`` in
      appinfo.ini will have ``Revision N`` added to it. The revision number
      starts at 2.
