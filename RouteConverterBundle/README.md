# RouteConverter for Windows bundled with JRE 11

1. Download and extract Java 11 OpenJDK for Windows ZIP to jdk-11.0.14
   https://github.com/ojdkbuild/ojdkbuild/releases/tag/java-11-openjdk-11.0.14.9-2
   
2. Analyze module dependencies and check for errors
   jdk-11.0.14\bin\jdeps -s target\RouteConverterWindowsOpenSource.jar

3. Build custom JRE 
   build-jre.bat

4. Install NSIS from https://nsis.sourceforge.io/Download
   
5. Build installer with NSIS from RouteConverterBundle.nsi
