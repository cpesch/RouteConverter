.. _troubleshooting:

===============
Troubleshooting
===============

When something isn't working properly in the PortableApps.com Launcher, it's
probably a mistake on your part. Before reporting it as a bug, please try
going through this list of things first. If you can't work out what the issue
is, or if you still think you've found a bug, then you can try :ref:`asking
for help <help>`.

Check your launcher configuration
   Please go through all the lines in your :ref:`ref-launcher.ini` file and
   reread the documentation for the section or key to make sure that you're
   not doing something wrong by mistake.

Other issues
   Go through the :ref:`release-checklist`.

Registry
========

Registry things aren't happening
   Make sure you have :ini-key:`[Activate]:Registry` set to ``true``.

Java
====

General issues
   Read the :ref:`java` section, your issue may be covered there.

The base application says it can't find Java
   If the base application requires Java, :ini-key:`[Activate]:Java` must be
   set to ``require``. Not setting it, or setting it to ``try``

*This document is not complete (not that it ever will be)*
