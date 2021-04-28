[![Build Status](https://travis-ci.com/cpesch/RouteConverter.svg?branch=master)](https://travis-ci.com/cpesch/RouteConverter)
[![Test Coverage](https://codecov.io/gh/cpesch/RouteConverter/branch/master/graph/badge.svg)](https://codecov.io/gh/cpesch/RouteConverter)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.cpesch.slash/RouteConverter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.cpesch.slash/RouteConverter)
<a href="https://hosted.weblate.org/engage/routeconverter/?utm_source=widget"><img src="https://hosted.weblate.org/widgets/routeconverter/-/svg-badge.svg" alt="Translation status"/></a>

What is RouteConverter?
=======================

RouteConverter is a popular open source tool to display, edit, enrich and convert
routes, tracks and waypoints licensed under the GNU Public License.
See http://www.routeconverter.com/about/ for details about features, supported formats
and languages, screenshots, frequently asked questions and how you can help.

Downloads
=========

Stable releases are available at http://www.routeconverter.com/releases/

Prereleases are frequently offered at http://www.routeconverter.com/prereleases/

If you want to contribute
=========================

Patches and pull requests are always welcome. If you minimize your diff, it's more
likely that your contribution will be applied to the code base. Please stick to the
code standards and formatting that you run across. And don't forget to add tests for
your changes ;-)

CONTRIBUTORS.txt provides a list of the people who helped developing RouteConverter.

How to develop for RouteConverter
=================================

1. Install a recent Java SDK, version 8 or later, from http://www.oracle.com/technetwork/java/javase/downloads/

2. Clone RouteConverter from github:
   
       git clone git://github.com/cpesch/RouteConverter.git

3. Open RouteConverter sources in an Integrated Development Environment (IDE)

   IntelliJ
   * Install IntelliJ IDEA Community Edition from http://www.jetbrains.com/idea/download/
   * Choose "File/Open Project..." and the root pom.xml.

   Eclipse
   * Install Eclipse IDE for Java Developers from http://www.eclipse.org/downloads/
   * Install m2eclipse from http://m2eclipse.sonatype.org/sites/m2e/
   * Choose "File/Import..." and "General/Maven Projects" and the root directory.

   NetBeans
   * Install NetBeans IDE Java SE from http://netbeans.org/downloads/
   * Install Git via "My NetBeans > Install Plugins"
   * Choose "Open Project..." and the root directory.

4. Let JAVA_HOME refer to the Java SDK
   
       set JAVA_HOME=c:\Programm Files\Java\jdk1.8.0_271

   Put JAVA_HOME into your PATH

       set %PATH%=%JAVA_HOME%:%PATH%

5. Build RouteConverter with the Maven wrapper
    
       mvnw clean package

6. Run RouteConverter
    
       java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
       java -jar RouteConverterLinuxOpenSource/target/RouteConverterLinuxOpenSource.jar
       java -jar RouteConverterMacOpenSource/target/RouteConverterMacOpenSource.jar
       java -jar RouteConverterWindowsOpenSource/target/RouteConverterWindowsOpenSource.jar

Have fun!

Christian
