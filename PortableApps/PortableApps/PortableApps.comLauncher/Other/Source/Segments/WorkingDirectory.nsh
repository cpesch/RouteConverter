${SegmentFile}

${SegmentPreExec}
	ClearErrors
	${ReadLauncherConfig} $0 Launch WorkingDirectory
	${IfNot} ${Errors}
		${ParseLocations} $0
		${DebugMsg} "Setting working directory to $0."
		SetOutPath $0
	${EndIf}
!macroend
