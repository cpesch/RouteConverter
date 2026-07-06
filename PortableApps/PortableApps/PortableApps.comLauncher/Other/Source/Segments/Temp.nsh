${SegmentFile}

Var UsesContainedTempDirectory
Var TempDirectory
Var TMP ; $TEMP is read-only but may be "wrong", as we see it, after launcher nesting

${Segment.onInit}
	ClearErrors
	ReadEnvStr $TMP PAL:_TEMP
	${If} ${Errors}
		StrCpy $TMP $TEMP
	${Else}
		${SetEnvironmentVariable} TEMP $TMP
		${SetEnvironmentVariable} TMP $TMP
	${EndIf}
!macroend

${SegmentInit}
	ClearErrors
	${ReadLauncherConfig} $UsesContainedTempDirectory Launch CleanTemp
	${IfNot} ${Errors}
	${AndIf} $UsesContainedTempDirectory != true
	${AndIf} $UsesContainedTempDirectory != false
		${InvalidValueError} [Launch]:CleanTemp $UsesContainedTempDirectory
	${EndIf}
!macroend

${SegmentPre}
	${If} $UsesContainedTempDirectory != false
		ClearErrors
		${If} $WaitForProgram == false
			StrCpy $TempDirectory $DataDirectory\Temp
		${Else}
			StrCpy $TempDirectory $TMP\$AppIDTemp
		${EndIf}
		${DebugMsg} "Creating temporary directory $TempDirectory"
		${If} $SecondaryLaunch != true
		${AndIf} ${FileExists} $TempDirectory
			RMDir /r $TempDirectory
		${EndIf}
		CreateDirectory $TempDirectory
	${Else}
		StrCpy $TempDirectory $TMP
	${EndIf}

	${DebugMsg} "Setting %TEMP% and %TMP% to $TempDirectory"
	${SetEnvironmentVariablesPath} TEMP $TempDirectory
	${SetEnvironmentVariable} TMP $TempDirectory
	${SetEnvironmentVariable} PAL:_TEMP $TMP
!macroend

${SegmentPostPrimary}
	${If} $UsesContainedTempDirectory != false
	${AndIf} $TempDirectory != "" ; may occur if status = running
		${DebugMsg} "Removing contained temporary directory $TempDirectory."
		RMDir /r $TempDirectory
	${EndIf}
!macroend
