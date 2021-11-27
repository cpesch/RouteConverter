# RouteConverter for Windows bundled with JRE 11

1. Download and extract Java 11 OpenJDK for Windows ZIP from
   https://github.com/ojdkbuild/ojdkbuild/releases/tag/java-11-openjdk-11.0.13.8-1
   
2. Analyze module dependencies
   jdk-11.0.13.8-1\bin\jdeps -s target\RouteConverterWindowsOpenSource.jar

3. Build custom JRE 
   build-jre.bat

4. Install NSIS from https://nsis.sourceforge.io/Download
   
5. Build installer with NSIS from RouteConverterBundle.nsi
