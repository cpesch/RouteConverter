${SegmentFile}

${SegmentInit}
	; We don't permit running from PROGRAMFILES at all; the Installer will take
	; care of most people (same restrictions), but some may shift it back in
	; there... so, we offer the nice and verbose environment variable (but
	; don't tell them about it normally) if they *really* want to void the
	; warranty they don't have under the GPL.

	; Fear not, $PROGRAMFILES64 == $PROGRAMFILES32 on a 32-bit machine
	${If} $EXEDIR startswith $PROGRAMFILES32
		StrCpy $0 $PROGRAMFILES32
	${ElseIf} $EXEDIR startswith $PROGRAMFILES64
		StrCpy $0 $PROGRAMFILES64
	${Else}
		StrCpy $0 ""
	${EndIf}

	${If} $0 != ""
		; We had fun deciding on these.
		ReadEnvStr $1 IPromiseNotToComplainWhenPortableAppsDontWorkRightInProgramFiles
		${If} $1 S== "I understand that this may not work and that I can not ask for help with any of my apps when operating in this fashion."
			${DebugMsg} "You're making me sad by the way you voided your warranty, running in Program Files."
		${Else}
			; This string doesn't let on about the disable switch (by design)
			MessageBox MB_OK|MB_ICONSTOP `$(LauncherProgramFiles)`
			Quit
		${EndIf}
	${EndIf}

	; Check if UNC paths are permitted
	StrCpy $1 nounc
	${IfThen} $EXEDIR startswith "\\" ${|} StrCpy $1 unc ${|}
	ClearErrors
	${ReadLauncherConfig} $0 Launch SupportsUNC
	${If} $0 == no
		${If} $1 == unc
			MessageBox MB_OK|MB_ICONSTOP `$(LauncherNoUNCSupport)`
			Quit
		${EndIf}
	${ElseIf} $0 == warn
	${OrIf} ${Errors}
		${If} $1 == unc
		${AndIf} ${Cmd} `MessageBox MB_YESNO|MB_ICONSTOP $(LauncherUNCWarn) IDNO`
			Quit
		${EndIf}
	${ElseIf} $0 == yes
		Nop ; It's OK, so do nothing
	${Else}
		${InvalidValueError} [Launch]:SupportsUNC $0
	${EndIf}

	; Check if spaces in the path are permitted
	ClearErrors
	${ReadLauncherConfig} $0 Launch NoSpacesInPath
	${If} $0 == true
		${WordFind} $EXEDIR ` ` E+1 $R9
		${IfNot} ${Errors} ; errors = space not found, no errors means space in path
			MessageBox MB_OK|MB_ICONSTOP $(LauncherNoSpaces)
			Quit
		${EndIf}
	${ElseIf} $0 != false
	${AndIfNot} ${Errors}
		${InvalidValueError} [Launch]:NoSpacesInPath $0
	${EndIf}
!macroend
