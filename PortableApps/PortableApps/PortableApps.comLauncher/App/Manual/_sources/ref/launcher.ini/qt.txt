.. ini-section:: [QtKeysCleanup]

[QtKeysCleanup]
===============

|inikeyint|

:ref:`Qt applications <qt>` tend to leave quite a lot of a mess behind them
which needs to be cleared up. This section deals with cleaning up keys like
these::

   HKCU\Software\Trolltech\OrganizationDefaults\Qt Factory Cache 4.6\com.trolltech.Qt.QImageIOHandlerFactoryInterface:\X:\PortableApps\AppNamePortable\App\AppName
   HKCU\Software\Trolltech\OrganizationDefaults\Qt Plugin Cache 4.6.false\X:\PortableApps\AppNamePortable\App\AppName

To get the value that you should store here, ignore the path at the end of the
registry and also ignore the ``HKCU\Software\Trolltech\OrganizationDefaults\``
at the start.

The format is numbered INI values, starting at 1.

**Example:**

.. code-block:: ini

   [QtKeysCleanup]
   1=Qt Factory Cache 4.6\com.trolltech.Qt.QImageIOHandlerFactoryInterface:
   2=Qt Plugin Cache 4.6.false
