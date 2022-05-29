!include LogicLib.nsh
!include WinMessages.nsh
!include FileFunc.nsh

SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

!define JRE "jre-11.0.14"
OutFile "target\RouteConverterBundle.exe"

Icon "RouteConverter.ico"
VIProductVersion 2.31.2.00000
VIAddVersionKey ProductName "RouteConverter"
VIAddVersionKey LegalCopyright "Copyright (c) 2008-2022 Christian Pesch"
VIAddVersionKey FileDescription "RouteConverter for Windows Offline Vector Map Ed."
VIAddVersionKey FileVersion 2.31.2.00000
VIAddVersionKey ProductVersion "2.31 / OpenJRE 11.0.14 (x64)"
VIAddVersionKey InternalName "RouteConverter"
VIAddVersionKey OriginalFilename "RouteConverter.exe"

Section
  SetOverwrite off

  SetOutPath "$TEMP\${JRE}"
  File /r "${JRE}\*"

  InitPluginsDir
  SetOutPath $PluginsDir
  File "target\RouteConverterWindowsOpenSource.jar"
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"$TEMP\${JRE}\bin\java.exe" -server -jar $PluginsDir\RouteConverterWindowsOpenSource.jar $R0'
  RMDir /r $PluginsDir
SectionEnd
