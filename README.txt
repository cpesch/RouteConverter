RouteConverter
==============

Two Integrated Development Environments (IDE) are supported: 
* Intellij IDEA and 
* Eclipse.

Intellij IDEA
-------------
See http://www.routeconverter.de/forum/thread-306.html how to get 
a license. Choose "Open Project..." with the root pom.xml. 

Eclipse
-------
Choose "Import..." and "Existing Projects into Workspace" and the
root directory. 

Two build options are supported: 
* ant and 
* Maven.

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
> mvn clean package
If you're using Maven 2.0.9 which suffers from issue 
http://jira.codehaus.org/browse/MNG-4032 call
> mvn clean install

To start the built RouteConverter call
> java -jar build/output/RouteConverter.jar
