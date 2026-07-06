${SegmentFile}

Var DisableSplashScreen

${SegmentInit}
	${If} $DisableSplashScreen != true
		${ReadUserConfig} $DisableSplashScreen DisableSplashScreen
		; Hmm... I think we'll skip validating this.

		ClearErrors
		${ReadLauncherConfig} $0 Launch SplashTime
		${IfNot} ${Errors}
		${AndIf} $0 = 0 ; = is IntCmp, which which a string is 0
		${AndIf} $0 != 0 ; ... but "0" is valid.  Silly, but valid.
			${InvalidValueError} [Launch]:SplashTime $0 ; string, not int
		${EndIf}

		${IfNotThen} ${FileExists} $EXEDIR\App\AppInfo\Launcher\splash.jpg ${|} StrCpy $DisableSplashScreen true ${|}

		${CheckForPlatformSplashDisable} $DisableSplashScreen

		${If} $DisableSplashScreen != true
			${IfThen} $0 = 0 ${|} StrCpy $0 1200 ${|}
			newadvsplash::show /NOUNLOAD $0 0 0 -1 /L $EXEDIR\App\AppInfo\Launcher\splash.jpg
		${EndIf}
	${EndIf}
!macroend

${SegmentPreExecPrimary}
	ClearErrors
	${ReadLauncherConfig} $DisableSplashScreen Launch LaunchAppAfterSplash
	${If} $DisableSplashScreen == true
		newadvsplash::stop /WAIT
	${ElseIfNot} ${Errors}
	${AndIf} $DisableSplashScreen != false
		${InvalidValueError} [Launch]:LaunchAppAfterSplash $DisableSplashScreen
	${EndIf}
!macroend

${SegmentUnload}
	${If} $DisableSplashScreen != true
		newadvsplash::stop /WAIT
	${EndIf}
!macroend
