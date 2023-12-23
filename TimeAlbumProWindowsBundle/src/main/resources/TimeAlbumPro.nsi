!include LogicLib.nsh
!include WinMessages.nsh
!include FileFunc.nsh

SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

!define JRE "17.0.9"
!define JRE_PATH "..\jre-${JRE}"
OutFile "TimeAlbumProWindowsBundle.exe"

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

  SetOutPath "$TEMP\${JRE_PATH}"
  File /r "${JRE_PATH}\*"

  InitPluginsDir
  SetOutPath $PluginsDir
  File "TimeAlbumProWindows.jar"
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"$TEMP\${JRE_PATH}\bin\java.exe" -server -jar $PluginsDir\TimeAlbumProWindows.jar $R0'
  RMDir /r $PluginsDir
SectionEnd
