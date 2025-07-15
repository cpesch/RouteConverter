# RouteConverter for Windows bundled with JRE 17

1. Download and extract Java 17 OpenJDK for Windows ZIP to jdk-17.0.15
   https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.15%2B6/OpenJDK17U-jdk_x64_windows_hotspot_17.0.15_6.zip
 
2. Analyze module dependencies and check for errors

       jdk-17.0.15\bin\jdeps -s target\RouteConverterWindowsOpenSource.jar

3. Build custom JRE

       build-jre.bat

4. Install NSIS from https://nsis.sourceforge.io/Download
   
5. Build installer with NSIS 

       build-app.bat
