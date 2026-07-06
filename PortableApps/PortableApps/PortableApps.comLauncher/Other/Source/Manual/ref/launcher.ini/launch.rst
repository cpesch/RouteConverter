.. ini-section:: [Launch]

[Launch]
========

The ``[Launch]`` section provides details regarding the launching of the
application and surrounding details. It's also the "general" section for things
which aren't big enough to warrant their own section.

.. ini-key:: [Launch]:AppName

AppName
-------

| Value: ``App Name``
| Deprecated.

----

Specify the application name (non-portable version) here, for displaying in the
"already running" error box. If this is not specified, the value will be
obtained from the ``Name`` field in appinfo.ini, minus ``Portable`` or ``,
Portable Edition``. Please consider using the standard portable application
naming scheme ("*AppName* Portable") instead of specifying this value.

**Example:** for the program "App Name", this value would be ``App Name``
(though it should be unset and ``Name`` in AppInfo.ini should be set to ``App
Name Portable`` or ``App Name, Portable Edition``.) 

.. ini-key:: [Launch]:ProgramExecutable

ProgramExecutable
-----------------

| Mandatory.

----

Specify the program to be launched by the PortableApps.com Launcher here,
relative to the App directory of the portable application.

There is a special case for Java applications; after specifying
:ini-key:`[Activate]:Java`\ ``=require`` they can specify a value of
``java.exe`` or ``javaw.exe`` and it will be interpreted into the path to that
executable in the Java Runtime Environment.

**Example:** inside the portable application package, the executable to run is
at ``App\AppName\AppName.exe``, so, after removing the ``App\``,
:ini-key:`ProgramExecutable <[Launch]:ProgramExecutable>`\
``=AppName\AppName.exe``.

This is the lookup order for the various alternative forms of ProgramExecutable.
At each level if the condition is not met or the value is not set, it goes on to
the next.

* :ini-key:`[Launch]:ProgramExecutableWhenParameters64` (only if 64-bit and
  parameters are given)
* :ini-key:`[Launch]:ProgramExecutable64` (only if 64-bit)
* :ini-key:`[Launch]:ProgramExecutableWhenParameters` (only if parameters are
  given)
* :ini-key:`[Launch]:ProgramExecutable`

.. ini-key:: [Launch]:ProgramExecutableWhenParameters

ProgramExecutableWhenParameters
-------------------------------

| Optional.

----

Specify the program to be launched by the PortableApps.com Launcher when
command-line arguments are given here, relative to the App directory of the
portable application. This has the effect of overriding :ref:`ProgramExecutable
<[Launch]:ProgramExecutable>` when specified if command-line arguments are
given. If none are given, the value in ProgramExecutable will be used.

All rules about Java for :ini-key:`[Launch]:ProgramExecutable` hold with this
value.

This can be used for apps which launch a menu but the menu can be bypassed if
parameters are passed. One example of this is the NSIS menu - when the program
is launched, it is desirable for the menu, ``NSIS.exe``, to be shown, but if a
file is given to it in its command line, the NSIS compiler, ``makensisw.exe``,
should be launched, as the menu does not recognise file names given to it.

.. ini-key:: [Launch]:ProgramExecutable64

ProgramExecutable64
-------------------

| Optional.

.. versionadded:: 2.1

----

An override for :ref:`[Launch]:ProgramExecutable` when the portable app is
running on a 64-bit operating system. Typically a portable app should not
include a 64-bit version whether provided by the publisher or not, but if there
are significant benefits in having a 64-bit version, or it is required for
functionality (e.g. defragmentation or system information analysis), it may be
included.

All the rules of :ref:`[Launch]:ProgramExecutable` hold for this.

.. ini-key:: [Launch]:ProgramExecutableWhenParameters64

ProgramExecutableWhenParameters64
---------------------------------

| Optional.

.. versionadded:: 2.1

----

An override for :ref:`[Launch]:ProgramExecutableWhenParameters` when the
portable app is running on a 64-bit operating system. Typically a portable app
should not include a 64-bit version whether provided by the publisher or not,
but if there are significant benefits in having a 64-bit version, or it is
required for functionality (e.g. defragmentation or system information
analysis), it may be included.

All the rules of :ref:`[Launch]:ProgramExecutableWhenParameters` hold for this.

.. ini-key:: [Launch]:CommandLineArguments

CommandLineArguments
--------------------

| Optional.
| |envsub|

----

If you need to pass any command line arguments to :ini-key:`ProgramExecutable
<[Launch]:ProgramExecutable>` to make it run or make it portable, specify them
here. Remember that if your program is running from a path with spaces, you may
need to put double quotation marks around the value, e.g. ``-d
"%PAL:DataDir%\settings"``. If you do so, you should put single or double
quotation marks around the whole string, like this:
:ini-key:`CommandLineArguments <[Launch]:CommandLineArguments>`\ ``='-d
"%PAL:DataDir%\settings"'``.

Concerning the significance of quoting strings and how it will be interpreted,
refer to :ref:`INI keys <ini-keys>`.

For Java applications, you will almost always need to specify parameters here.
See :ref:`java` for more information.

**Example:** the application being made portable accepts a
``--data-directory=`` command line argument to make it portable, but it does
*not* require the string to be quoted:
:ini-key:`CommandLineArguments <[Launch]:CommandLineArguments>`\
``=--data-directory=%PAL:DataDir%\settings``

.. ini-key:: [Launch]:WorkingDirectory

WorkingDirectory
----------------

| Optional.
| |envsub|

----

If the application must be run from a certain working directory, either to
store its settings there or so that it can find certain files critical to it,
set it here. If the reason is so that it can find files, you may be able to
circumvent this by placing the application's directory in the ``PATH``.  See
:ini-section:`[Environment]` for details on that technique.

If possible, avoid using this as it will make relative files passed through the
command line fail unless it is only a single file given (which will be
automatically corrected).

**Example:** ``%PAL:AppDir%\AppName``

.. ini-key:: [Launch]:MinOS

MinOS
-----

| Values: none / ``2000`` / ``XP`` / ``2003`` / ``Vista`` / ``2008`` / ``7`` / ``2008 R2``
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application requires a certain operating system to run, specify the
version here. The values provided above are in order of how they will be
considered, so if for example you use the value ``Vista``, it will tell the user
that it won't run on their operating system when they run it on Windows 2000,
Windows XP or Windows Server 2003.

There is no special value for Wine in Linux or Mac OS X; if it works, it works,
if it doesn't, it doesn't.

.. ini-key:: [Launch]:MaxOS

MaxOS
-----

| Values: none / ``2000`` / ``XP`` / ``2003`` / ``Vista`` / ``2008`` / ``7`` / ``2008 R2``
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application does not run above a certain version of Windows, specify that
version here. Be cautious in doing this as often support will be improved in a
later version of an application. The values provided above are in order of how
they will be considered, so if for example you use the value ``Vista``, it will
tell the user that it won't run on their operating system when they run it on
Windows Server 2008, Windows 7 and Windows Server 2008 R2.

There is no special value for Wine in Linux or Mac OS X; if it works, it works,
if it doesn't, it doesn't.

.. ini-key:: [Launch]:RunAsAdmin

RunAsAdmin
----------

| Values: ``force`` / ``try`` / ``compile-force`` / none
| Default: none
| Optional.

----

.. versionchanged:: 2.1
   added the ``compile-force`` value

Setting this to ``force`` or ``try`` causes the user to be prompted to run the
program as an administrator (or a UAC prompt on Windows Vista or Windows 7 when
UAC is turned on). If the user cannot elevate to admin or cancels the operation
or an error occurs, what happens next depends on the setting here.

If the value is ``force`` then the portable application will quit, telling the
user that it requires administrative privileges. General reasons for requiring
administrative privileges are:

* being dependent upon services or drivers
* requiring settings which are stored in HKEY_LOCAL_MACHINE

If the value is ``try`` then the user will be warned that some features of the
portable application will not work. The application would like administrative
privileges but they are not essential to the running of the application. General
reasons for requesting (but not requiring) administrative privileges are:

* having extra features available with such privileges (such as unblocking
  certain firewall features, or optional improvement services, maybe to speed
  things up)
* storing settings in HKEY_LOCAL_MACHINE, but in a way which you can use the
  application without it, so that it works but loses settings while on that
  machine.

It is worthwhile noting that just because an application stores its settings in
HKEY_LOCAL_MACHINE does not mean that you must ``force`` running as
administrator; it will often be valid to ``try`` instead, with the result that
portable settings will not be loaded and no settings will be saved. In such a
situation the recommended path of action is to contact the author of the
original program and request that they modify their application to store its
settings in HKEY_CURRENT_USER instead, which is probably where the settings
should be.

A special value, ``compile-force``, is available for cases when ``force``
doesn't work properly (there seem to be some cases with environment variables
not working properly). Use it only if ``force`` doesn't work, as it sets a flag
in the executable so that the operating system handles running as admin rather
than the launcher, so no friendly message is given to the user. Internally,
this is the NSIS line ``RequestExecutionLevel admin``. After setting this value
you will need to regenerate the launcher. Also note that due to the way it
works, the value ``compile-force`` is incompatible with the operating
system-specific overrides below.

You can override this value for specific operating systems with one or more of
the values below. Sometimes an application may require administrative privileges
on Vista and onwards but not on 2000, XP and 2003 (that is the usual division).
In such a situation, you should probably set this to ``force`` and use values
for 2000, XP and 2003, ``none``, for future compatibility. On Linux or Mac with
Wine, the user will always be reported as running as the administrator, and so
you don't need a special case for it.

.. ini-key:: [Launch]:RunAsAdmin2000

RunAsAdmin2000
--------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows 2000, you can use this value to override :ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdminXP

RunAsAdminXP
--------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows XP, you can use this value to override :ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdmin2003

RunAsAdmin2003
--------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows Server 2003, you can use this value to override
:ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdminVista

RunAsAdminVista
---------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows Vista, you can use this value to override
:ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdmin2008

RunAsAdmin2008
--------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows Server 2008, you can use this value to override
:ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdmin7

RunAsAdmin7
-----------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows 7, you can use this value to override :ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:RunAsAdmin2008R2

RunAsAdmin2008R2
----------------

| Values: ``force`` / ``try`` / none
| Default: none
| Optional.

.. versionadded:: 2.1

----

If the application needs or can benefit from administrative privileges on
Windows Server 2008 R2, you can use this value to override
:ini-key:`[Launch]:RunAsAdmin`.

.. ini-key:: [Launch]:CleanTemp

CleanTemp
---------

| Values: ``true`` / ``false``
| Default: ``true``
| Optional.

----

Many applications leave things in the user's "temporary" directory (called TEMP)
and don't clean them up. When not set (thus when set to ``true``), this value
assigns a contained TEMP directory to the application (in the format
``%TEMP%\AppNamePortableTemp``) which is removed after the application is
closed, thus not leaving anything behind.

If :ini-key:`WaitForProgram <[Launch]:WaitForProgram>` is set to ``false``, this
will still work, placing TEMP in ``Data\temp``, but this may slow down some
applications and may also clutter up the device while running. In this case the
directory will not be deleted upon program completion, but rather the next time
the application is started.

If you test the application you are making portable thoroughly and it never
leaves anything behind in TEMP, you can set this to ``false`` and the contained
temporary directory will not be created.

.. ini-key:: [Launch]:SinglePortableAppInstance

SinglePortableAppInstance
-------------------------

| Values: ``true`` / ``false``
| Default: ``false``
| Optional.

----

If you only wish one instance of the portable version of the application to be
run, set this to true. If it is set to true, if the launcher is started while
another copy of the launcher is already running, the second instance will abort
silently. If you wish to prevent a local and portable version of the application
from running concurrently, look at :ini-key:`SingleAppInstance
<[Launch]:SingleAppInstance>`.

.. ini-key:: [Launch]:SingleAppInstance

SingleAppInstance
-----------------

| Values: ``true`` / ``false``
| Default: ``true``
| Optional.

----

If you only wish one instance of the application, portable or local, to be run,
omit this value. If it is set to ``true`` or omitted, if the launcher is
started while a local copy of the application is already running, it will abort
with an error message. This value only affects running a portable instance
while a local instance is already running; if a second portable instance is
launched, this value this value will not affect it.  See
:ini-key:`SinglePortableAppInstance <[Launch]:SinglePortableAppInstance>` for
controlling that case.

If, however, it is permissible for a portable version of the application to run
concurrently with a local instance, you can set this to ``false``.

If the application stores settings in a local location like %APPDATA%, or in the
registry, then it is not correct to set this to ``false``. You should only set
it to ``false`` in such a case as when it stores its settings in the
executable's directory or some path specified by an environment variable or
command-line argument, and will not interfere with a local instance or vica
versa.

.. ini-key:: [Launch]:CloseEXE

CloseEXE
--------

| Values: ``another_optional_app.exe``
| Optional.

----

If you wish to specify another executable to require to be closed before the
portable application is started than the :ini-key:`ProgramExecutable
<[Launch]:ProgramExecutable>` entry, enter the file name in here. This is
particularly useful with Java applications which use Launch4J. See
:ref:`java-launch4j` for details on that.

.. ini-key:: [Launch]:SplashTime

SplashTime
----------

| Value: time to show splash screen in milliseconds
| Default: ``1500`` (1.5 seconds)
| Optional.

----

If an application takes a long time to start you may wish to have the splash
screen show for more than 1.5 seconds (1500ms). Specify the number of
milliseconds (as an integer) here to change from it the default 1500.

Use this value with extreme caution. No-one likes a splash screen staying on top
of their screen for a minute and a half, stopping them from seeing what they
were doing underneath.

.. ini-key:: [Launch]:LaunchAppAfterSplash

LaunchAppAfterSplash
--------------------

| Values: ``true`` / ``false``
| Default: ``false``
| Optional.

----

With full-screen, resolution-changing applications, running the application
while the splash screen is active can confuse the program. If you observe this
behaviour in your application, set this to true. (Otherwise avoid it as it may
slow down program start-up.)

.. ini-key:: [Launch]:WaitForProgram

WaitForProgram
--------------

| Values: ``true`` / ``false``
| Default: ``true``
| Optional.

----

If you don't need the launcher to wait for the conclusion of the application,
set this to false. Note that you should only do this if you do not have registry
entries to handle or files to move, for example if you can redirect all settings
with command-line arguments or environment variables.

This option is ignored when running locally.

.. ini-key:: [Launch]:WaitForOtherInstances

WaitForOtherInstances
---------------------

| Values: ``true`` / ``false``
| Default: ``true``
| Optional.

----

If the application is single-instance (i.e. if you run another copy of it it
won't run but will activate the first one), and the application can't restart
itself, you can set this to false. If the application can restart itself at all,
e.g. Firefox can, DO NOT set this to false, or else clean-up will start while
the application is still running, which won't be good for it.

.. ini-key:: [Launch]:WaitForEXE

WaitForEXE\ *N*
---------------

| Value: ``another_optional_app.exe``
| Optional.

----

If the program that you run is a launcher program which launches another
executable, and you need to wait for that as well as (or instead of) the
original program, specify its file name here, as ``WaitForEXE1``\ =\
``whatever.exe``.  If you need more than one, use ``WaitForEXE2``,
``WaitForEXE3``, etc.

.. ini-key:: [Launch]:RefreshShellIcons

RefreshShellIcons
-----------------

| Values: ``before`` / ``after`` / ``both`` / none
| Default: none
| Optional.

----

If the application does any registering of file type extensions which you handle
or clean up, to make the new icon appear or to stop the portable one appearing,
set this to one of the values. If it is just cleaning up at the end, ``after``
should be enough, but if you handle it with a :ini-section:`RegistryKeys` value,
you will need ``both``.

.. ini-key:: [Launch]:HideCommandLineWindow

HideCommandLineWindow
---------------------

| Values: ``true`` / ``false``
| Default: ``false``
| Optional.

----

If the application produces a command line window which you wish to hide (common
in some open source games), you can set this to true to hide it.

.. _moving-package-directory:

.. TODO: later the label moving-package-directory should be moved to something
   in topics or somewhere else, covering the general stuff more.

.. ini-key:: [Launch]:DirectoryMoveOK

DirectoryMoveOK
---------------

| Values: ``yes`` / ``warn`` / ``no``
| Default: ``warn``
| Optional.

.. versionadded:: 2.1

----

All portable apps should be designed to cope with changing drive letters (e.g.
moving from ``X:\PortableApps\AppNamePortable`` to
``Y:\PortableApps\AppNamePortable``), but with some portable apps it's not
practical to support moving the directory the package is stored in (e.g. moving
from ``C:\Users\User\Desktop\AppNamePortable`` to
``X:\PortableApps\AppNamePortable``).

If you have developed the package well, directory moves will either not matter
or be compensated for with :ini-section:`[FileWriteN]` sections using things
like the :env:`%PAL:LastPackagePartialDir% <PAL:LastPackagePartialDir>` and
:env:`%PAL:PackagePartialDir% <PAL:PackagePartialDir>` environment variables.

When many people make portable apps, they don't consider what will happen when a
user moves the directory, and so it may or may not work. Also historically it
was not considered important at all and so no effort was normally put into
making it work - it was unsupported behaviour. This is why the default is
``warn``.

.. ini-key:: [Launch]:NoSpacesInPath

NoSpacesInPath
--------------

| Values: ``true`` / ``false``
| Default: ``false``
| Optional.

----

If the application will not function if you try to run it in a directory with
spaces in the path, you can set this to true to provide a useful error message
to the user in this situation.

.. ini-key:: [Launch]:SupportsUNC

SupportsUNC
-----------

| Values: ``yes`` / ``warn`` / ``no``
| Default: ``warn``
| Optional.

.. versionadded:: 2.2

----

Sets if the launcher supports being run from an UNC path (i.e. in the form
``\\server\share\etc``).

Before this can validly be set to ``yes``, you should test your portable app
and make sure that it really does support UNC paths correctly. Some apps (or
their portability wrapper) may corrupt data when not specially designed to cope
with UNC paths, because of which the default is ``warn``. When thus set to
``warn`` or omitted, the user will be notified that it may not work correctly
and given a chance to exit.
