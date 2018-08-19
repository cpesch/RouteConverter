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

1. Install a recent Java 8 SDK from http://www.oracle.com/technetwork/java/javase/downloads/

2. Install a recent Maven 3.5 from http://maven.apache.org/download.html

3. Clone RouteConverter from github:
   > git clone git://github.com/cpesch/RouteConverter.git

4. Open RouteConverter sources in an Integrated Development Environment (IDE)

   Install Intellij IDEA Community Edition from http://www.jetbrains.com/idea/download/
   Choose "File/Open Project..." and the root pom.xml.

   Install Eclipse IDE for Java Developers from http://www.eclipse.org/downloads/
   Install m2eclipse from http://m2eclipse.sonatype.org/sites/m2e/
   Choose "File/Import..." and "General/Maven Projects" and the root directory.

   Install NetBeans IDE Java SE from http://netbeans.org/downloads/
   Install Git via "My NetBeans > Install Plugins"
   Choose "Open Project..." and the root directory.

5. Build RouteConverter

   Let JAVA_HOME refer to the Java 8 SDK
   > set JAVA_HOME=c:\Programm Files\Java\jdk1.8.0_151
   Let M2_HOME refer to your Maven 3 Installation
   > set M2_HOME=c:\Program Files\apache-maven-3.5.2
   Put JAVA_HOME and M2_HOME into your PATH
   > set %PATH%=%JAVA_HOME%:%M2_HOME%:%PATH%
   Call
   > mvn clean package

6. Run RouteConverter
   > java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
   > java -jar RouteConverterLinuxGoogle/target/RouteConverterLinuxGoogle.jar
   > java -jar RouteConverterLinuxOpenSource/target/RouteConverterLinuxOpenSource.jar
   > java -jar RouteConverterMacGoogle/target/RouteConverterMacGoogle.jar
   > java -jar RouteConverterWindowsOpenSource/target/RouteConverterWindowsOpenSource.jar
   > java -jar RouteConverterWindowsGoogle/target/RouteConverterWindowsGoogle.jar
   > java -jar RouteConverterWindowsOpenSource/target/RouteConverterWindowsOpenSource.jar

Have fun!
Christian