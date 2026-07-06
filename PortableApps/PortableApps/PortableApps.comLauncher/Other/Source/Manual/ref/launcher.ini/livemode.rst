.. ini-section:: [LiveMode]

[LiveMode]
==========

The ``[LiveMode]`` section provides details regarding how the portable
application should function when in Live mode, when in a read-only directory.

As specified here, certain things are copied to the local hard drive, and then
the application is run, and any things copied locally are deleted. This is
called running locally, or "Live mode".

.. ini-key:: [LiveMode]:CopyApp

CopyApp
-------

| Values: ``true`` / ``false``
| Default: ``true``
| Optional.

----

When run in Live mode, does the application need to be run from a writable
location? If yes, specify ``true``.

.. ini-key:: [LiveMode]:CopyData

CopyData
--------

| Obsolete.

----

This option has been removed due to issues with the :env:`%PAL:DataDir%
<PAL:DataDir>` replacement variable in file moving. Data will now always be
copied. This may change back in the future.

.. When run in Live mode, does the application data need to be run from a
   writable location? If yes, specify "true". It is worthwhile noting that most
   applications will need settings to be writable. If you have anything in the
   :ini-section:`[RegistryKeys]` section with drive letters, or anything in a
   :ini-section:`[FileWriteN]` section, you will need this set to ``true``
