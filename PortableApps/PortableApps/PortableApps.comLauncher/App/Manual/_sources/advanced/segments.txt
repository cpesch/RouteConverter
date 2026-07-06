.. _segments:

Segments
========

The PortableApps.com Launcher source code is divided up into lots of "segments",
each of which can run a number of "hooks". This aids with code separation of
different pieces of functionality, by grouping code by what it does rather than
when it executes, providing a synergistically value-adding, mutually-beneficial
strategic partnership between the developer and... and... well, whatever's left.
(A more useful workflow anyway. I'll leave the marketing talk to the marketers.)

.. admonition:: Why did you write that nonsense?

   A few proposals have been put forward about such things as this, but as usual
   analysts have been unable to agree on the issue.
   
   The simple answer is that we software developers have a quirky sense of
   humour.  How else could you explain things like the recursive acronyms that
   all developers so love? At times it can get dull, just writing a program
   which does what it's meant to do and that's all, and so developers make time
   to put in what are commonly known as "easter eggs": hidden functionality
   which they generally find amusing and which break the monotony of writing
   good software.
   
   In particular here the point is writing documentation. Writing this
   documentation for the PortableApps.com Launcher is taking far longer than the
   actual writing of the code did in the first place (orders of magnitude
   longer). And so at times I decide to put strange things in for the fun of it.

   It's just one of those illogicalities of software developers.

.. _segments-hooks:

Hooks
-----

Here is a list of the hooks which can be executed:

* ``.onInit``: things which must go in the NSIS ``.onInit`` function (see the
  `NSIS documentation`_ for details about ``.onInit``)
* ``Init``: load data into variables, abort the launcher if necessary, and do
  anything else of a "starting up" nature".
* ``Pre``: do things to make the application portable which must always be
  done, whether the launcher is dealing with a primary or secondary instance of
  the application.
* ``PrePrimary``: actions to make the application portable which should only be
  run with a primary instance of an application.
* ``PreSecondary``:  actions to make the application portable which should only
  be run with a secondary or subsequent instance of an application. I haven't
  yet thought of an instance when this would be useful but there could be.
* ``PreExec``: just before the program gets executed, there's an opportunity to
  do something here. Try to use the ``Pre`` hook instead.
* ``PreExecPrimary``: ``PreExec`` for primary instances.
* ``PreExecSecondary``: ``PreExec`` for secondary and subsequent instances.
* ``Post``: clean up the application and handle restoration of settings and
  related things in here.
* ``PostPrimary``: ``Post`` for primary instances.
* ``PostSecondary``: ``Post`` for secondary and subsequent instances.
* ``Unload``: unload plug-ins and clean up traces from the launcher itself.

.. _`NSIS documentation`: http://nsis.sourceforge.net/Docs/Chapter4.html#4.7.2.1.2

.. _segments-disable:

Customisations
--------------

If you ever need to disable a segment or hook, you can do so. In general though
if you can possibly avoid doing it you should; you can very easily break the
PortableApps.com Launcher by disabling certain things. See
:ref:`custom-code-segment` for details.

.. _segments-list:

List of core segments
---------------------

Here is the current list of segments included in the PortableApps.com Launcher:

* **Core:** various core functionality
* **DirectoriesCleanup:** :ini-section:`[DirectoriesCleanupIfEmpty]` and
  :ini-section:`[DirectoriesCleanupForce]`
* **DirectoriesMove:** :ini-section:`[DirectoriesMove]`
* **DirectoryMoving:** coping with moving the portable app package --
  :ini-key:`[Launch]:DirectoryMoveOK`
* **DriveLetter:** :ref:`ref-envsub-drive`
* **Environment:** :ini-section:`[Environment]`
* **ExecString:** constructing the string for execution
* **FileWrite:** :ini-section:`[FileWriteN]`
* **FilesMove:** :ini-section:`[FilesMove]`
* **InstanceManagement:** managing multiple instances of portable apps
* **Java:** :ref:`guess <java>`
* **Language:** launcher language selection for message boxes and language
  switching (see :ref:`languages`)
* **OperatingSystem:** :ini-key:`[Launch]:MinOS` and :ini-key:`[Launch]:MaxOS`
* **Qt:** :ini-section:`[QtKeysCleanup]` (see also :ref:`qt`)
* **RefreshShellIcons:** :ini-key:`[Launch]:RefreshShellIcons`
* **Registry:** :ini-key:`[Activate]:Registry` and helper utilities for
  other Registry segments. See also :ref:`registry` for this and the
  other Registry segments listed here.
* **RegistryCleanup:** :ini-section:`[RegistryCleanupIfEmpty]` and
  :ini-section:`[RegistryCleanupForce]`
* **RegistryKeys:** :ini-section:`[RegistryKeys]`
* **RegistryValueBackupDelete:** :ini-section:`[RegistryValueBackupDelete]`
* **RegistryValueWrite:** :ini-section:`[RegistryValueWrite]`
* **RunAsAdmin:** :ini-key:`[Launch]:RunAsAdmin` et al.
* **RunLocally:** live mode support (mainly a user feature, but slightly
  configurable with :ini-section:`[LiveMode]`)
* **Services:** services (currently not functional and disabled)
* **Settings:** Management of ``Data\settings`` and copying default data from
  ``App\DefaultData`` to the ``Data`` directory
* **SplashScreen:** :ref:`splash-screen`
* **Temp:** management of the TEMP directory (mainly controlled by
  :ini-key:`[Launch]:CleanTemp`)
* **Variables:** internal functions for providing environment variables and most
  of the :ref:`ref-envsub-directory`
* **WorkingDirectory:** :ini-key:`[Launch]:WorkingDirectory`
