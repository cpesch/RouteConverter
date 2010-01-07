How to develop for RouteConverter
=================================

1. Open RouteConverter in an Integrated Development Environments (IDE)

   Intellij IDEA
   -------------
   See http://www.routeconverter.de/forum/thread-306.html how to get
   a license. Choose "Open Project..." with the root pom.xml.

   Eclipse
   -------
   Choose "Import..." and "Maven Projects" and the root directory.

2. Build RouteConverter

   Set JAVA_HOME to a Java 6 SDK and call
   > mvn clean package
   If you're using Maven 2.0.9 which suffers from issue http://jira.codehaus.org/browse/MNG-4032 call
   > mvn clean install

3. Run RouteConverter
   > java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
   > java -jar RouteConverterLinux/target/RouteConverterLinux.jar
   > java -jar RouteConverterMac/target/RouteConverterMac.jar
   > java -jar RouteConverterWindows/target/RouteConverterWindows.jar
