${SegmentFile}

Var RunLocally

${SegmentInit}
	${ReadUserConfig} $RunLocally RunLocally
!macroend

${SegmentPre}
	${If} $RunLocally == true
		${DebugMsg} "Live mode enabled"
		ClearErrors
		${ReadLauncherConfig} $0 LiveMode CopyApp
		${If} $0 == true
		${OrIf} ${Errors}
			${If} $SecondaryLaunch != true
				${DebugMsg} "Live mode: copying $EXEDIR\App to $TMP\$AppIDLive\App"
				CreateDirectory $TMP\$AppIDLive
				CopyFiles /SILENT $EXEDIR\App $TMP\$AppIDLive
			${EndIf}
			StrCpy $AppDirectory $TMP\$AppIDLive\App
		${ElseIf} $0 != false
			${InvalidValueError} [LiveMode]:CopyApp $0
		${EndIf}
		;For the time being at least, I've disabled the option of not copying Data, as it makes file moving etc. from %DataDirectory% break
		;ClearErrors
		;${ReadLauncherConfig} $0 LiveMode CopyData
		;${If} $0 == true
		;${OrIf} ${Errors}
			${If} $SecondaryLaunch != true
				${DebugMsg} "Live mode: copying $EXEDIR\Data to $TMP\$AppIDLive\Data"
				CreateDirectory $TMP\$AppIDLive
				CopyFiles /SILENT $EXEDIR\Data $TMP\$AppIDLive
			${EndIf}
			StrCpy $DataDirectory $TMP\$AppIDLive\Data
		;${ElseIf} $0 != false
		;	${InvalidValueError} [LiveMode]:CopyData $0
		;${EndIf}
		${If} ${FileExists} $TMP\$AppIDLive
			${SetFileAttributesDirectoryNormal} $TMP\$AppIDLive
		${EndIf}

		${SetEnvironmentVariablesPath} PAL:AppDir $AppDirectory
		${SetEnvironmentVariablesPath} PAL:DataDir $DataDirectory

		; Wait for the program to finish if we are a primary instance
		${If} $SecondaryLaunch != true
			StrCpy $WaitForProgram true
		${EndIf}
	${EndIf}

	CreateDirectory $DataDirectory
!macroend

${SegmentPostPrimary}
	${If} $RunLocally == true
		${DebugMsg} "Removing Live mode directory $TMP\$AppIDLive."
		RMDir /r $TMP\$AppIDLive
	${EndIf}
!macroend
