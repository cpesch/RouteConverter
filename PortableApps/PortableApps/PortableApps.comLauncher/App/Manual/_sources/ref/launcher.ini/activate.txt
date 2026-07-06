.. ini-section:: [Activate]

[Activate]
==========

This section is for telling the launcher whether or not it should load certain
things, for example Java and registry support.

.. ini-key:: [Activate]:Registry

Registry
--------

| Values: ``true`` or ``false``
| Default: ``false``
| Optional.

----

If the base application uses the registry, set this to true. **You MUST set this
to true if you wish the registry sections to be parsed.** Otherwise they will
not be read at all.

.. ini-key:: [Activate]:Java

Java
----

| Values: none, ``find`` or ``require``
| Default: none
| Optional.

----

If the application can use Java but does not depend on its being available, set
this to ``find``, and a Java Runtime Environment (JRE) will be found if
available, and the environment variable :env:`%JAVA_HOME% <JAVA_HOME>` will
become available for use.

If the application is completely dependant on Java, set this to ``require``.
Then you may set :ini-key:`[Launch]:ProgramExecutable` to ``javaw.exe`` (normal
use) or ``java.exe`` (command line version which may be useful for testing and
debugging) and it will use Java.

**Caveat:** With ``require``, if you can possibly help it, set
:ini-key:`[Launch]:WaitForProgram` or :ini-key:`[Launch]:WaitForOtherInstances`
to false if you use java.exe or javaw.exe. Otherwise you'll run into problems
cleaning up if other Java applications get run, as it'll look for instances of
the application, and find javaw.exe, even though it's another application's
javaw.exe instance.

See :ref:`java` for more discussion about Java apps.

.. ini-key:: [Activate]:XML

XML
---

| Values: ``true`` or ``false``
| Default: ``false``
| Optional.

----

If you wish to get the language string from an XML file with
:ini-section:`[LanguageFile]` or write anything to an XML file with
:ini-section:`[FileWriteN]`, using their ``Type``\ s ``XML text`` or ``XML
attribute``, you will need to set this to ``true``. After setting this to true,
**you will need to regenerate the launcher**. This includes the NSIS plug-in,
which adds approximately 60KB to the file size of the generated launcher
executable.

Don't worry too much about the possibility of forgetting this; if you try to
use any XML features when the launcher has not been compiled with XML support,
it will warn you that you need to set this value to ``true``.

See :ref:`xml` for more information about XML support in the PortableApps.com
Launcher.
