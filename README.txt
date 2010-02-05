How to develop for RouteConverter
=================================

1. Install a Java 6 SDK from http://java.sun.com/javase/downloads/index.jsp

2. Install Maven 2.10 or 2.2.1 from http://maven.apache.org/download.html

3. Open RouteConverter in an Integrated Development Environment (IDE)

   Install Intellij IDEA 9 Community Edition from http://www.jetbrains.com/idea/download/
   Choose "File/Open Project..." and the root pom.xml.

   Install Eclipse IDE for Java Developers from http://www.eclipse.org/downloads/
   Choose "File/Import..." and "Maven Projects" and the root directory.

4. Build RouteConverter

   Set JAVA_HOME to a Java 6 SDK, put Maven into your PATH and call
   > mvn clean package
   If you're using Maven 2.0.9 which suffers from issue http://jira.codehaus.org/browse/MNG-4032 call
   > mvn clean install

5. Run RouteConverter
   > java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
   > java -jar RouteConverterLinux/target/RouteConverterLinux.jar
   > java -jar RouteConverterMac/target/RouteConverterMac.jar
   > java -jar RouteConverterWindows/target/RouteConverterWindows.jar
