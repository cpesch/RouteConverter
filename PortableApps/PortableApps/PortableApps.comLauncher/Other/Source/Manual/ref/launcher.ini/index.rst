.. index:: launcher.ini

.. _ref-launcher.ini:

launcher.ini
============

.. admonition:: What is launcher.ini?

   The PortableApps.com Launcher is a universal launcher; it can make almost
   anything portable, but it needs to be told what to do so that it can make an
   application portable. It gets this information from a file in the
   ``App\AppInfo\Launcher`` directory of a portable application package called
   ``AppNamePortable.ini``, where *AppNamePortable* is the base name of the
   launcher executable. To give this a solid name, we call it launcher.ini
   although it should in practice never be called this.

.. toctree::
   :maxdepth: 2

   launch
   activate
   livemode
   environment
   registry
   qt
   filewriten
   filesystem
   language
   services
