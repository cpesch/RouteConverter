1. Download Java 14 for Windows from 
   https://download.java.net/openjdk/jdk14/ri/openjdk-14+36_windows-x64_bin.zip
   https://adoptopenjdk.net/releases.html
   
2. Analyze module dependencies
   jdk-14\bin\jdeps -s RouteConverterWindowsOpenSource.jar

3. Build custom JRE 
   build-jre.bat

4. Install NSIS from https://nsis.sourceforge.io/Download
   
5. Build installer with NSIS from RouteConverterBundle.nsi

    

