# RouteConverter for Windows bundled with JRE 17

1. Download and extract Java 17 OpenJDK for Windows ZIP to jdk-17.0.8
   https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.8.1_1.zip

2. Download and extract Java 17 OpenJRE for Windows ZIP to jre-17.0.8
   https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8.1%2B1/OpenJDK17U-jre_x64_windows_hotspot_17.0.8.1_1.zip

3. Analyze module dependencies and check for errors

       jdk-17.0.8\bin\jdeps -s target\RouteConverterWindowsOpenSource.jar

4. Build custom JRE

       build-jre.bat

5. Install NSIS from https://nsis.sourceforge.io/Download
   
6. Build installer with NSIS 

       build-app.bat
