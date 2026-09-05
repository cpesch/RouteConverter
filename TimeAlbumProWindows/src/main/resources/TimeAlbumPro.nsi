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
; app jar generation, namespaced by build so a running instance's directory can
; never be pulled out from under it; see issue #372.
!define VERSION "${project.version}-${maven.build.number}"
!define PRODUCT_DIR "$LOCALAPPDATA\TimeAlbumPro"
!define APP_DIR "${PRODUCT_DIR}\app-${VERSION}"
!define STAMP_CONTENT "${VERSION}|${JRE}"
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

  ; fast path: a stamp matching this build+JRE means the app jar for this
  ; generation is already extracted at ${APP_DIR} -- skip both extractions.
  ClearErrors
  FileOpen $1 "${APP_DIR}\.stamp" r
  IfErrors slowpath
  FileRead $1 $2
  FileClose $1
  StrCmp $2 "${STAMP_CONTENT}" fastpath slowpath

  slowpath:
    ; best-effort prune of superseded generations; never disturbs a directory
    ; still held open by a running instance (RMDir /r simply fails on it).
    ClearErrors
    FindFirst $3 $4 "${PRODUCT_DIR}\*.*"
    IfErrors pruneDone
    pruneLoop:
      StrCmp $4 "" pruneDone
      StrCmp $4 "." pruneNext
      StrCmp $4 ".." pruneNext
      StrCmp $4 "app-${VERSION}" pruneNext
      StrCmp $4 "jre-${JRE}" pruneNext
      RMDir /r "${PRODUCT_DIR}\$4"
      pruneNext:
      FindNext $3 $4
      Goto pruneLoop
    pruneDone:
    FindClose $3

    SetOutPath "${JRE_DIR}"
    File /r "${JRE_SRC}\*"

    SetOutPath "${APP_DIR}"
    File "TimeAlbumProWindows.jar"

    ; commit marker written last: an interrupted extraction leaves no valid
    ; stamp, so the next launch redoes it instead of running a truncated jar.
    FileOpen $1 "${APP_DIR}\.stamp" w
    FileWrite $1 "${STAMP_CONTENT}"
    FileClose $1

  fastpath:
  SetOutPath $TEMP
  ${GetParameters} $R0
  nsExec::Exec '"${JRE_DIR}\bin\java.exe" -server -Drouteconverter.bundledJre=true -jar "${APP_DIR}\TimeAlbumProWindows.jar" $R0'
SectionEnd
