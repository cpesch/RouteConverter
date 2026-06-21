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
!define JRE_DIR "$LOCALAPPDATA\TimeAlbumPro\jre-${JRE}"
OutFile "TimeAlbumProWindows.exe"

Icon "TimeAlbumPro.ico"
VIProductVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductName "TimeAlbumPro"
VIAddVersionKey LegalCopyright "Copyright (c) Since 2007 Co-developed by RouteConverter and Columbus"
VIAddVersionKey FileDescription "TimeAlbumPro for Windows bundled with JRE"
VIAddVersionKey FileVersion ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.0.${maven.build.number}
VIAddVersionKey ProductVersion "${project.version} / OpenJRE ${JRE} (x64)"
VIAddVersionKey InternalName "TimeAlbumPro"
VIAddVersionKey OriginalFilename "TimeAlbumPro.exe"

Section
  SetOverwrite off

  SetOutPath "${JRE_DIR}"
  File /r "${JRE_SRC}\*"

  InitPluginsDir
  SetOutPath $PluginsDir
  File "TimeAlbumProWindows.jar"
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"${JRE_DIR}\bin\java.exe" -server -jar $PluginsDir\TimeAlbumProWindows.jar $R0'
  RMDir /r $PluginsDir
SectionEnd
