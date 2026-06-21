!include LogicLib.nsh
!include WinMessages.nsh
!include FileFunc.nsh

SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

!define JRE "${jre.version}"
; build-time source of the bundled JRE (relative to this .nsi in target/)
!define JRE_SRC "..\jre-${JRE}"
; runtime extraction target (persisted across launches, namespaced under LocalAppData)
!define JRE_DIR "$LOCALAPPDATA\RouteConverter\jre-${JRE}"
OutFile "RouteConverterWindows.exe"

Icon "RouteConverter.ico"
VIProductVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductName "RouteConverter"
VIAddVersionKey LegalCopyright "Copyright (c) 2008-${current.year} Christian Pesch"
VIAddVersionKey FileDescription "RouteConverter for Windows bundled with JRE"
VIAddVersionKey FileVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductVersion "${project.version} / OpenJRE ${JRE} (x64)"
VIAddVersionKey InternalName "RouteConverter"
VIAddVersionKey OriginalFilename "RouteConverter.exe"

Section
  SetOverwrite off

  SetOutPath "${JRE_DIR}"
  File /r "${JRE_SRC}\*"

  InitPluginsDir
  SetOutPath $PluginsDir
  File "RouteConverterWindows.jar"
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"${JRE_DIR}\bin\java.exe" -server -jar $PluginsDir\RouteConverterWindows.jar $R0'
  RMDir /r $PluginsDir
SectionEnd
