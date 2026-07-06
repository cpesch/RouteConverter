.. _paf-portability:

6. Host PC Modifications & Portability
======================================

During use, a portable app is permitted to modify registry entries and files on
the local drive, however the registry and local files must be returned to their
pre-run state on exit. This may involve backing up and then restoring the
settings for a local copy (in either the registry or APPDATA) of an application
on start and exit. The portable app must continue to work (settings and
preferences maintained, language selection maintained) as the drive letter
changes as the device is moved between computers. The applications Most Recently
Used (MRU) file list should continue working as well.
