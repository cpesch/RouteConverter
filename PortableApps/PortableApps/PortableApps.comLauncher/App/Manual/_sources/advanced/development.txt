.. _development-builds:

================================================================
Working with development builds of the PortableApps.com Launcher
================================================================

If you want to test features of the PortableApps.com Launcher, or get
development builds in between releases, you'll need to get it and compile the
Generator. Here's how.

.. _hg:

The PortableApps.com Launcher source repository
===============================================

Development of the PortableApps.com Launcher takes place in a Mercurial_
repository at SourceForge_. The URL is
http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher/. To check
out ("clone") a copy of the repository, you will need Mercurial_ or TortoiseHg_.

To clone the repository with Mercurial,

.. code-block:: bash

   hg clone http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher
   cd launcher

(To use a different directory name, put the directory name at the end of the
``hg clone`` line after a space.)

To clone the repository with TortoiseHg, create a directory, right click on it
in Explorer and find the TortoiseHg "Clone..." option. Specify the path to clone
as ``http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher/``.

You can also get a copy of the latest version in this repository without
Mercurial in the bz2_, gzip_ or zip_ formats.

.. _Mercurial: http://mercurial.selenic.com
.. _SourceForge: http://sourceforge.net
.. _TortoiseHg: http://tortoisehg.bitbucket.org
.. _bz2: http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher/archive/tip.tar.bz2
.. _gzip: http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher/archive/tip.tar.gz
.. _zip: http://portableapps.hg.sourceforge.net/hgweb/portableapps/launcher/archive/tip.zip

.. _compile-pal-generator:

Compiling the PortableApps.com Launcher Generator
=================================================

The PortableApps.com Launcher Generator is written in :term:`NSIS` and so you
will need NSIS Portable to compile it (it has the necessary plug-ins included).

1. :ref:`Install the PortableApps.com Launcher <install>`. Instead of installing
   the PortableApps.com Launcher package, you can get a copy of the :ref:`source
   repository <hg>`.

2. Run NSIS Portable and compile ``Other\Source\GeneratorWizard.nsi``
   from the PortableApps.com Launcher source.

3. Upon success, the PortableApps.com Launcher Generator will be at
   ``PortableApps.comLauncherGenerator.exe``.

After that you can run the PortableApps.com Launcher Generator as normal.  If
the PortableApps.com Launcher Generator says that it can't find NSIS, edit
``Data\settings.ini`` to specify the path to makensis.exe.
