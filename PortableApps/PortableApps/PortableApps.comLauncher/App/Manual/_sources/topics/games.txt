.. index:: Games, Making things portable; games

.. _games:

Making games portable
=====================

Here are some tips concerning making games portable.

**Full screen, resolution-changing games:** set
:ini-key:`[Launch]:LaunchAppAfterSplash`\ =\ ``true``, otherwise when the
splash screen stops the game may lose focus and the resolution will change back
to the system's original value.
