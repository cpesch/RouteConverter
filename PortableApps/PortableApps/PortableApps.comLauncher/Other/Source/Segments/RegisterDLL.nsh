/*${SegmentFile}

${SegmentPrePrimary}
	StrCpy $R0 1
	${Do}
		ClearErrors
		${ReadLauncherConfig} $0 RegisterDLL $R0
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		${ParseLocations} $0
		${DebugMsg} "Registering DLL $0."
		;ExecWait '$SYSDIR\regsvr32.exe /s "$0"' $R9
		;${If} $R9 <> 0 ; 0 = success
		ClearErrors
		RegDLL $0
		${If} ${Errors}
			${WriteRuntimeData} FailedRegisterDLL $0 true
			${DebugMsg} "Failed to register DLL $0."
		${EndIf}
		IntOp $R0 $R0 + 1
	${Loop}
!macroend

${SegmentPostPrimary}
	StrCpy $R0 1
	${Do}
		ClearErrors
		${ReadLauncherConfig} $0 RegisterDLL $R0
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		${ReadRuntimeData} $R9 FailedRegisterDLL $0
		${If} ${Errors} ; didn't fail
			${ParseLocations} $0
			${DebugMsg} "Unregistering DLL $0."
			;ExecWait '$SYSDIR\regsvr32.exe /s /u "$0"'
			UnRegDLL $0
		${EndIf}
		IntOp $R0 $R0 + 1
	${Loop}
!macroend*/
