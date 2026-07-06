; At the time of writing, a number of our applications (Songbird comes to mind)
; can't cope with them being moved to a different directory. Drive letters we
; always cope with, but sometimes we won't be able to cope with moving
; directory (and sometimes we just don't *try*).
;
; Plan A: make it so that the PortableApps.com Launcher (and our
; implementations with the Launcher) will always update all paths. Nice idea,
; but unpractical. It's up to the launcher developer to make it do that.
;
; Plan B: make the launcher detect when it goes to a different path (not just
; drive letter) which is easy, and then warn the user that it may not work
; because they did that, are they sure the want to continue?
;
; The solution implemented here, Plan C, is a hybrid of these, allowing warning
; or stopping the user before they break anything, or allowing it completely.

${SegmentFile}

Var LastDirectory
Var CurrentDirectory

${SegmentInit}
	; Strip the root from the current executable's directory; take UNC path into account
	${GetRoot} $EXEDIR $0
	StrLen $0 $0
	StrCpy $CurrentDirectory $EXEDIR '' $0
	${If} $CurrentDirectory == ''
		StrCpy $CurrentDirectory '\'
	${EndIf}

	ReadINIStr $LastDirectory $EXEDIR\Data\settings\$AppIDSettings.ini $AppIDSettings LastDirectory
	${IfThen} $LastDirectory == "" ${|} StrCpy $LastDirectory $CurrentDirectory ${|}
	${If} $LastDirectory != $CurrentDirectory
		${DebugMsg} "Directory has been moved from $LastDirectory to $CurrentDirectory."
		ClearErrors
		${ReadLauncherConfig} $0 Launch DirectoryMoveOK
		${If} $0 == no
			MessageBox MB_OK|MB_ICONSTOP "$(LauncherDirectoryMoveNotAllowed)"
			Quit
		${ElseIf} $0 == warn
		${OrIf} ${Errors} ; value not set
			; Are you sure you want to continue?
			${If} ${Cmd} ${|} MessageBox MB_YESNO|MB_ICONSTOP "$(LauncherDirectoryMoveWarn)" IDNO ${|}
				Quit
			${EndIf}
		${ElseIf} $0 == yes
			Nop ; It's OK, so do nothing
		${Else}
			${InvalidValueError} [Launch]:DirectoryMoveOK $0
		${EndIf}
	${EndIf}

	${SetEnvironmentVariablesPath} PAL:PackagePartialDir $CurrentDirectory
	${SetEnvironmentVariablesPath} PAL:LastPackagePartialDir $LastDirectory
!macroend

${SegmentPrePrimary}
	; Past the possible abort stage so it's safe to say we've run from this directory.
	WriteINIStr $DataDirectory\settings\$AppIDSettings.ini $AppIDSettings LastDirectory $CurrentDirectory
!macroend
