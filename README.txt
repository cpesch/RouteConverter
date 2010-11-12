How to develop for RouteConverter
=================================

1. Install a recent Java 6 SDK from http://java.sun.com/javase/downloads/index.jsp

2. Install Maven 3 from http://maven.apache.org/download.html

3. Checkout RouteConverter from Subversion:
   > svn checkout http://www.routeconverter.de/svn/RouteConverter/trunk/ RouteConverter

4. Open RouteConverter sources in an Integrated Development Environment (IDE)

   Install Intellij IDEA 9 Community Edition from http://www.jetbrains.com/idea/download/
   Choose "File/Open Project..." and the root pom.xml.

   Install Eclipse IDE for Java Developers from http://www.eclipse.org/downloads/
   Install m2eclipse from http://m2eclipse.sonatype.org/sites/m2e
   Choose "File/Import..." and "General/Maven Projects" and the root directory.

5. Build RouteConverter

   Let JAVA_HOME refer to the Java 6 SDK
   > set JAVA_HOME=c:\Programme\Java\jdk1.6.0_22
   Put Maven into your PATH and call
   > mvn clean package

6. Run RouteConverter
   > java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
   > java -jar RouteConverterLinux/target/RouteConverterLinux.jar
   > java -jar RouteConverterLinux64/target/RouteConverterLinux64.jar
   > java -jar RouteConverterMac/target/RouteConverterMac.jar
   > java -jar RouteConverterMac64/target/RouteConverterMac64.jar
   > java -jar RouteConverterWindows/target/RouteConverterWindows.jar
   > java -jar RouteConverterWindows64/target/RouteConverterWindows64.jar
