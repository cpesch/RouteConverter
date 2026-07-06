.. index:: Splash screens

.. _splash-screen:

Splash screens
==============

PortableApps.com applications usually have a splash screen. This file goes in
``App\AppInfo\splash.jpg`` and must be a JPEG image. The usual procedure is to
use the `black and white PortableApps.com Development Test Release splash screen
<http://portableapps.com/files/images/development/SplashDevelopmentTestRelease.jpg>`_
while an application is a :term:`Development Test` and then when it is deemed
ready to become a :term:`Pre-Release`, it will be given its own one ready for
becoming an :term:`official PortableApps.com release`.

Configuration values which affect splash screens are
:ini-key:`[Launch]:SplashTime` and :ini-key:`[Launch]:LaunchAppAfterSplash`.
