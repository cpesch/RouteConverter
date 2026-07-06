${SegmentFile}

; See DirectoriesMove.nsh for explanation of in-package stuff

!macro _FilesMove_Start
	; By the end:
	; $0 = full path to source
	; $1 = full path to target
	; $2 = file name (only used as a temporary variable)
	; $4 = target directory
	${ParseLocations} $1
	${GetFileName} $0 $2
	StrCpy $0 $DataDirectory\$0
	StrCpy $4 $1
	StrCpy $1 $1\$2
!macroend

${SegmentPrePrimary}
	${ForEachINIPair} FilesMove $0 $1
		!insertmacro _FilesMove_Start

		; Is the target inside the package?
		StrLen $R0 $EXEDIR
		StrCpy $R0 $1 $R0
		${If} $R0 == $EXEDIR
			StrCpy $7 in-package
		${Else}
			StrCpy $7 not-in-package
		${EndIf}

		${If} $7 != in-package
			; Backup data from a local installation
			${ForEachFile} $4 $2 $1
				${IfNot} ${FileExists} $4.BackupBy$AppID
					${DebugMsg} "Backing up $4 to $4.BackupBy$AppID"
					Rename $4 $4.BackupBy$AppID
				${EndIf}
			${NextFile}
		${Else}
			${ForEachFile} $4 $2 $1
				${Break} ; Done this way as we can't nest ForEachPaths
			${NextFile}
			${IfNot} ${Errors}
				; Target directories existed, so the Rename back failed; clear out the stuff in Data.
				${DebugMsg} "Matching files for $1 discovered, deleting $0"
				${ForEachFile} $4 $2 $0
					Delete $4
				${NextFile}
				StrCpy $7 in-package-done
			${EndIf}
		${EndIf}

		; See if the parent local directory exists. If not, create it and
		; note down to delete it at the end if it's empty.
		${GetParent} $1 $4
		${IfNot} ${FileExists} $4
			CreateDirectory $4
			${WriteRuntimeData} FilesMove RemoveIfEmpty:$4 true
		${EndIf}

		${If} $7 != in-package-done
			; If portable data exists move/copy it to the target directory.  If the
			; target directory doesn't exist, note down for the end to remove it
			; again if it's empty.
			${ForEachFile} $3 $2 $0
				${DebugMsg} "Copying $3 to $4\$2"
				${GetRoot} $0 $5 ; compare
				${GetRoot} $1 $6 ; drive
				${If} $5 == $6   ; letters
	;				${DebugMsg} "Renaming file from $3 to $4\$2"
					Rename $3 $4\$2 ; same volume, rename OK
				${Else}
	;				${DebugMsg} "Copying file from $3 to $4\$2"
					CopyFiles /SILENT $3 $4\$2
				${EndIf}
			${NextFile}
		${EndIf}
	${NextINIPair}
!macroend

${SegmentPostPrimary}
	${ForEachINIPair} FilesMove $0 $1
		!insertmacro _FilesMove_Start

		; Is the target inside the package?
		StrLen $R0 $EXEDIR
		StrCpy $R0 $1 $R0
		${If} $R0 == $EXEDIR
			StrCpy $7 in-package
		${EndIf}

		; If not in Live mode, copy the data back to the Data directory.
		${GetParent} $0 $3
		${ForEachFile} $4 $2 $1
			${If} $RunLocally != true
				${GetRoot} $0 $5 ; compare
				${GetRoot} $1 $6 ; drive
				${If} $5 == $6   ; letters
					${DebugMsg} "Renaming file from $4 to $3\$2"
					ClearErrors
					Rename $4 $3\$2 ; same volume, rename OK
					${If} ${Errors}
						${DebugMsg} "Rename failed, file handle presumably open. Trying to recover with copy."
						Delete $3\$2
						CopyFiles /SILENT $4 $3\$2
					${EndIf}
				${Else}
					${DebugMsg} "Copying file from $4 to $3\$2"
					Delete $3\$2
					CopyFiles /SILENT $4 $3\$2
				${EndIf}
			${EndIf}
			; And then remove it from the runtime location
${!getdebug}
!ifdef DEBUG
			${If} $7 != in-package
			${AndIf} ${FileExists} $4
				ClearErrors
				${DebugMsg} "Removing portable settings file $4 from run location."
			${EndIf}
!endif
			Delete $4
!ifdef DEBUG
			${If} $7 != in-package
			${AndIf} ${FileExists} $4
			${AndIf} ${Errors}
				${DebugMsg} "Failed to remove file. Internal state is probably messed up."
				; In the future we may alert the user and request that they
				; close it themselves but I've added enough strings for 2.1
				; (bad excuse, I know).
			${EndIf}
!endif
		${NextFile}

		; If the local directory we put it in didn't exist before, delete it if
		; it's empty.
		${GetParent} $1 $4
		${ReadRuntimeData} $2 FilesMove RemoveIfEmpty:$4
		${If} $2 == true
			RMDir $4
		${EndIf}

		; And move that backup of any local data from earlier if it exists.
		${ForEachFile} $3 $2 $1.BackupBy$AppID
			${GetBaseName} $2 $2
			${DebugMsg} "Moving local settings file from $3 to $4\$2"
			Rename $3 $4\$2
		${NextFile}
	${NextINIPair}
!macroend
