${SegmentFile}

; Explanation of the in-package stuff: In Post, if the target was in the
; package, and the Rename back fails (path is open), instead of killing it
; badly, we copy it (so it won't be destroyed if the app is upgraded between
; runs, or missed if backed up) and on next launch delete the Data version (as
; it's a duplicate).

!macro _DirectoriesMove_Start
	${IfThen} $0 != - ${|} StrCpy $0 $DataDirectory\$0 ${|}
	${ParseLocations} $1
!macroend

${SegmentPrePrimary}
	${ForEachINIPair} DirectoriesMove $0 $1
		!insertmacro _DirectoriesMove_Start

		${If} $0 == $DataDirectory\settings
			MessageBox MB_ICONSTOP "DON'T YOU DARE DO THAT! (You can't [DirectoriesMove] settings)"
		${EndIf}

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
			${ForEachDirectory} $4 $3 $1
				${DebugMsg} "Backing up $4 to $4.BackupBy$AppID"
				Rename $4 $4.BackupBy$AppID
			${NextDirectory}
		${ElseIf} $0 != -
			${ForEachDirectory} $4 $3 $1
				${Break} ; Done this way as we can't nest ForEachPaths
			${NextDirectory}
			${IfNot} ${Errors}
				; Target directories existed, so the Rename back failed; clear out the stuff in Data.
				${DebugMsg} "Matching directories for $1 discovered, deleting $0"
				${ForEachDirectory} $4 $3 $0
					RMDir /r $4
				${NextDirectory}
				StrCpy $7 in-package-done
			${EndIf}
		${EndIf}

		; If the key is -, don't move/copy to the target directory.
		; If portable data exists move/copy it to the target directory.
		${If} $0 == -
			${IfNot} ${WildCardFlag} ; can not create folders with wild-cards (obviously)
				CreateDirectory $1
				${DebugMsg} "DirectoriesMove key -, so only creating the directory $1 (no file copy)."
			${EndIf}
		; If in-package-done, the target directory existed and so we know that the copy back failed;
		${ElseIf} $7 != in-package-done
			; See if the parent local directory exists. If not, create it and
			; note down to delete it at the end if it's empty.
			${GetParent} $1 $4
			${IfNot} ${FileExists} $4
				CreateDirectory $4
				${WriteRuntimeData} DirectoriesMove RemoveIfEmpty:$4 true
			${EndIf}

			${ForEachDirectory} $3 $2 $0
				${IfNotThen} ${WildCardFlag} ${|} ${GetFileName} $1 $2 ${|} ; do not inherit the filename
				${GetRoot} $0 $5 ; compare
				${GetRoot} $1 $6 ; drive
				${If} $5 == $6   ; letters
					${DebugMsg} "Renaming directory $3 to $4\$2"
					Rename $3 $4\$2 ; same volume, rename OK
				${Else}
					${DebugMsg} "Copying $3\*.* to $4\$2\*.*"
					CreateDirectory $4\$2
					CopyFiles /SILENT $3\*.* $4\$2
				${EndIf}
			${NextDirectory}
			${If} ${Errors}
				${IfNotThen} ${WildCardFlag} ${|} CreateDirectory $1 ${|}
				; Nothing to copy, so just create the directory, ready for use.
${!getdebug}
!ifdef DEBUG
				StrLen $2 "$DataDirectory\"
				StrCpy $0 $0 "" $2
				${DebugMsg} "$DataDirectory\$0\*.* does not exist, so not copying it to $1.$\r$\n(Note for developers: if you want default data, remember to put files in App\DefaultData\$0)"
!endif
			${EndIf}
		${EndIf}
	${NextINIPair}
!macroend

${SegmentPostPrimary}
	${ForEachINIPair} DirectoriesMove $0 $1
		!insertmacro _DirectoriesMove_Start

		; Is the target inside the package?
		StrLen $R0 $EXEDIR
		StrCpy $R0 $1 $R0
		${If} $R0 == $EXEDIR
			StrCpy $7 in-package
		${EndIf}

		; If the key is "-", don't copy it back
		; Also if not in Live mode, copy the data back to the Data directory.
		${GetParent} $0 $3
		${ForEachDirectory} $4 $2 $1
			${IfNotThen} ${WildCardFlag} ${|} ${GetFileName} $0 $2 ${|} ; do not inherit the filename
			${If} $0 == -
				${DebugMsg} "DirectoriesMove key -, so not keeping data from $1."
			${ElseIf} $RunLocally != true
				${GetRoot} $0 $5 ; compare
				${GetRoot} $1 $6 ; drive
				${If} $5 == $6   ; letters
					${DebugMsg} "Renaming directory $4 to $3\$2"
					ClearErrors
					Rename $4 $3\$2 ; same volume, rename OK
					${If} ${Errors}
						${DebugMsg} "Rename failed, directory handle presumably open. Trying to recover with copy."
						RMDir /R $3\$2
						CreateDirectory $3\$2
						CopyFiles /SILENT $4\*.* $3\$2
					${EndIf}
				${Else}
					${DebugMsg} "Copying $4\*.* to $3\$2\*.*"
					RMDir /R $3\$2
					CreateDirectory $3\$2
					CopyFiles /SILENT $4\*.* $3\$2
				${EndIf}
			${EndIf}
			; And then remove it from the runtime location
${!getdebug}
!ifdef DEBUG
			${If} $7 != in-package
			${AndIf} ${FileExists} $4
				ClearErrors
				${DebugMsg} "Removing portable settings directory from run location ($4)."
			${EndIf}
!endif
			RMDir /R $4
!ifdef DEBUG
			${If} $7 != in-package
			${AndIf} ${FileExists} $4
			${AndIf} ${Errors}
				${DebugMsg} "Failed to remove directory. Internal state is probably messed up."
				; In the future we may alert the user and request that they
				; close it themselves but I've added enough strings for 2.1
				; (bad excuse, I know).
			${EndIf}
!endif
		${NextDirectory}

		; If the parent directory we put the directory in locally didn't exist
		; before, delete it if it's empty.
		${GetParent} $1 $4
		${ReadRuntimeData} $2 DirectoriesMove RemoveIfEmpty:$4
		${If} $2 == true
			RMDir $4
		${EndIf}

		; And move that backup of any local data from earlier if it exists.
		${ForEachDirectory} $3 $2 $1.BackupBy$AppID
			${GetBaseName} $2 $2
			${DebugMsg} "Moving local settings from $3 to $4\$2"
			Rename $3 $4\$2
		${NextDirectory}
	${NextINIPair}
!macroend
