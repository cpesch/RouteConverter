!include LogicLib.nsh
!include WinMessages.nsh
!include FileFunc.nsh

SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

!define JRE "17.0.9"
!define JRE_PATH "..\jre-${JRE}"
OutFile "RouteConverterWindowsBundle.exe"

Icon "RouteConverter.ico"
VIProductVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductName "RouteConverter"
VIAddVersionKey LegalCopyright "Copyright (c) 2008-2023 Christian Pesch"
VIAddVersionKey FileDescription "RouteConverter for Windows bundled with JRE"
VIAddVersionKey FileVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductVersion "${project.version} / OpenJRE ${JRE} (x64)"
VIAddVersionKey InternalName "RouteConverter"
VIAddVersionKey OriginalFilename "RouteConverter.exe"

Section
  SetOverwrite off

  SetOutPath "$TEMP\${JRE_PATH}"
  File /r "${JRE_PATH}\*"

  InitPluginsDir
  SetOutPath $PluginsDir
  File "RouteConverterWindowsOpenSource.jar"
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"$TEMP\${JRE_PATH}\bin\java.exe" -server -jar $PluginsDir\RouteConverterWindowsOpenSource.jar $R0'
  RMDir /r $PluginsDir
SectionEnd
