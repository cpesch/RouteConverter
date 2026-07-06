${SegmentFile}

Var RefreshShellIcons

${SegmentInit}
	ClearErrors
	${ReadLauncherConfig} $RefreshShellIcons Launch RefreshShellIcons
	${IfNot} ${Errors}
	${AndIf} $RefreshShellIcons != before
	${AndIf} $RefreshShellIcons != after
	${AndIf} $RefreshShellIcons != both
		${InvalidValueError} [Launch]:RefreshShellIcons $RefreshShellIcons
	${EndIf}
!macroend

${SegmentPreExec}
	${If} $RefreshShellIcons == before
	${OrIf} $RefreshShellIcons == both
		${RefreshShellIcons}
	${EndIf}
!macroend

${SegmentPost}
	${If} $RefreshShellIcons == after
	${OrIf} $RefreshShellIcons == both
		${RefreshShellIcons}
	${EndIf}
!macroend
