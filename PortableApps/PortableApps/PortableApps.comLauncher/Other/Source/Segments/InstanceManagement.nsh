${SegmentFile}

Var SecondaryLaunch

; A simple macro to avoid code duplication
!macro _InstanceManagement_QuitIfRunning
	${If} $SecondaryLaunch != true
	${AndIf} ${ProcessExists} $0
		MessageBox MB_OK|MB_ICONSTOP `$(LauncherAlreadyRunning)`
		Quit
	${EndIf}
!macroend

${SegmentInit}
	; First check if the launcher is already running with a mutex
	System::Call 'kernel32::CreateMutex(i0,i0,t"PortableApps.comLauncher$AppID-$BaseName")?e'
	Pop $0

	${IfNot} $0 = 0 ; It's already running
		; Is a second portable instance disallowed?
		ClearErrors
		${ReadLauncherConfig} $0 Launch SinglePortableAppInstance
		${If} $0 == true
			${DebugMsg} "Launcher already running and [Launch]:SinglePortableAppInstance=true: aborting."
			Quit
		${ElseIf} $0 != false
		${AndIfNot} ${Errors}
			${InvalidValueError} [Launch]:SinglePortableAppInstance $0
		${EndIf}
		; Set it up for a secondary launch.
		${DebugMsg} "Launcher already running: secondary launch."
		StrCpy $SecondaryLaunch true
		StrCpy $WaitForProgram false
		StrCpy $DisableSplashScreen true
	${EndIf}

	; Check that what we're going to execute exists (it'd be a pretty poor
	; party if it didn't)
	${IfNot} ${FileExists} $EXEDIR\App\$ProgramExecutable
	${AndIfNot} $UsingJavaExecutable == true
		;=== Program executable not where expected
		StrCpy $MissingFileOrPath App\$ProgramExecutable
		MessageBox MB_OK|MB_ICONSTOP `$(LauncherFileNotFound)`
		Quit
	${EndIf}

	; Check if the application (portable or not) is already running
	ClearErrors
	${ReadLauncherConfig} $0 Launch SingleAppInstance
	${If} $0 == true
	${OrIf} ${Errors}
		${IfNot} $UsingJavaExecutable == true
			${GetFileName} $ProgramExecutable $0
			!insertmacro _InstanceManagement_QuitIfRunning
		${EndIf}
	${ElseIf} $0 != false
		${InvalidValueError} [Launch]:SingleAppInstance $0
	${EndIf}

	; Check to make sure the value in [Launch]:CloseEXE isn't running
	ClearErrors
	${ReadLauncherConfig} $0 Launch CloseEXE
	${IfNot} ${Errors}
		!insertmacro _InstanceManagement_QuitIfRunning
	${EndIf}

	; Will we need to wait for the program?  This should only EVER be used if
	; there's no cleanup needed.  In the future I plan on automatically
	; calculating this value in another tool which will serve as the Generator.
	;
	; WaitForProgram may have already been set to false by the mutex check
	; above; we don't want to mess that up, so check if it's already set.
	${If} $WaitForProgram == ""
		ClearErrors
		${ReadLauncherConfig} $WaitForProgram Launch WaitForProgram
		${IfNot} ${Errors}
		${AndIf} $WaitForProgram != true
		${AndIf} $WaitForProgram != false
			${InvalidValueError} [Launch]:WaitForProgram $WaitForProgram
		${EndIf}
	${EndIf}
!macroend
