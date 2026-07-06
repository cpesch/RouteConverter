.. index:: Making things portable; Qt applications

.. _qt:

Making Qt applications portable
===============================

Applications which have been written with the Qt framework have certain
behaviours which affect portability and which must be dealt with. The main thing
with Qt is that it leaves a lot of mess behind it which must be cleaned up,
things which on a local installation are alright as they do caching and thus
slightly speed up subsequent execution, but for portable use they are no good.

The plugin cache and factory cache registry keys are the two main things which
need to be cleared up.

Currently the only thing of note here is the :ini-section:`[QtKeysCleanup]`
section in the launcher INI file. It is covered in that document.
