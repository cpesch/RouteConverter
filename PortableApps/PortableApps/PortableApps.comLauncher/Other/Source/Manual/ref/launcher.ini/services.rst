.. ini-section:: [ServiceN]

[Service\ *N*]
==============

Services and drivers which are required or recommended by your portable
application. There are various keys for each section which are listed below.

**Services are currently disabled in the PortableApps.com Launcher.**

.. ini-key:: [ServiceN]:Name

Name
----

| Mandatory.

----

The service name.

.. ini-key:: [ServiceN]:Path

Path
----

| Mandatory.

----

The execution string of the service.

.. ini-key:: [ServiceN]:Type

Type
----

| Values: ``service`` / ``driver-kernel`` / ``driver-file-system``
| Default: ``service``
| Optional.

----

Specify whether you are dealing with a service, a kernel driver or a file system
driver.

.. ini-key:: [ServiceN]:User

User
----

| Values: none / ``LocalService`` / ``NetworkService``
| Default: none
| Optional.

----

If the service needs to run as other than LocalSystem, i.e. as LocalService or
NetworkService, specify it here (omit the leading ``NT AUTHORITY\``).

.. ini-key:: [ServiceN]:Display

Display
-------

| Optional.

----

The short display name of the service.

.. ini-key:: [ServiceN]:Dependencies

Dependencies
------------

| Optional.

----

A comma-separated list of dependencies.

.. ini-key:: [ServiceN]:Description

Description
-----------

| Optional.

----

A description of what the service does for the "Services" Control Panel applet
(this should not be necessary).

.. ini-key:: [ServiceN]:IfExists

IfExists
--------

| Values: ``skip`` / ``replace``
| Default: ``skip``
| Optional.

----

If the service already exists, you can either skip it or replace it with the
portable version of the service (the original service will be restored
afterwards).

**This value currently has no effect** (except to show a warning that it won't
do what you expect if you specify ``replace``).
