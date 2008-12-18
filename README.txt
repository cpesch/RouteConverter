RouteConverter
==============

There are two build options for RouteConverter: ant and Maven.

ant
---
Allows to build both the Java 5 and Java 6 versions with the
stripped down and patched thirdparty software and is thus the
preferred way to build.

Set JAVA_HOME to a Java 6 SDK and call
> ant -f build/build.xml clean jar
for build/output/RouteConverter.jar which runs best on Windows 
with Java 6.

Set JAVA_HOME to a Java 5 SDK and call
> ant -f build/build.xml clean jar5
for several build/output/RouteConverterSOMETHING.jar's which try
to achieve the best experience on Linux, Mac or Windows with 
Java 5.

Maven
-----
Set JAVA_HOME to a Java 6 SDK and call
> mvn clean install 


To start the built RouteConverter call
> java -jar build/output/RouteConverter.jar
