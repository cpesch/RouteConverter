${SegmentFile}

Var LastDrive
Var CurrentDrive

; NOTE: (Last|Current)Drive refer to $EXEDIR, even with Live mode
; TODO: make it (Last|Current)(App|Data)?Drive

${SegmentInit}
	; Load the last drive letter and get the current drive letter.  If
	; LastDrive is not set, we set it to CurrentDrive, then any [FileWrite]
	; with Type=Replace will skip last->current replacement as they'll be the
	; same.
	ReadINIStr $LastDrive $EXEDIR\Data\settings\$AppIDSettings.ini $AppIDSettings LastDrive
	${GetRoot} $EXEDIR $CurrentDrive
	${IfThen} $LastDrive == "" ${|} StrCpy $LastDrive $CurrentDrive ${|}
	${DebugMsg} "Current drive is $CurrentDrive, last drive is $LastDrive"

	StrCpy $0 $CurrentDrive 1
	StrCpy $1 $LastDrive 1
	${SetEnvironmentVariable} PAL:Drive $CurrentDrive
	${SetEnvironmentVariable} PAL:LastDrive $LastDrive
	; TODO: decide what to do about this for UNC paths
	${SetEnvironmentVariable} PAL:DriveLetter $0
	${SetEnvironmentVariable} PAL:LastDriveLetter $1
!macroend

${SegmentPrePrimary}
	; Past the possible abort stage so it's safe to say we've run from this drive.
	WriteINIStr $DataDirectory\settings\$AppIDSettings.ini $AppIDSettings LastDrive $CurrentDrive
!macroend
