${SegmentFile}

Var LauncherFile
Var Bits

${Segment.onInit}
	; These may be needed with RunAsAdmin so they can't go in Init.

	${GetBaseName} $EXEFILE $BaseName
	StrCpy $LauncherFile $EXEDIR\App\AppInfo\Launcher\$BaseName.ini

	ClearErrors
	ReadINIStr $AppID $EXEDIR\App\AppInfo\appinfo.ini Details AppID
	ReadINIStr $AppNamePortable $EXEDIR\App\AppInfo\appinfo.ini Details Name
	${If} ${Errors}
		;=== Launcher file missing or missing crucial details
		StrCpy $AppNamePortable "PortableApps.com Launcher"
		StrCpy $MissingFileOrPath $EXEDIR\App\AppInfo\appinfo.ini
		MessageBox MB_OK|MB_ICONSTOP `$(LauncherFileNotFound)`
		Quit
	${EndIf}

	${ReadLauncherConfig} $AppName Launch AppName
	${If} $AppName == ""
		; Calculate the application name - non-portable version
		StrCpy $0 $AppNamePortable "" -9
		${If} $0 == " Portable"
			StrCpy $AppName $AppNamePortable -9
		${Else}
			StrCpy $1 $AppNamePortable "" -18
			${If} $0 == ", Portable Edition"
				StrCpy $AppName $AppNamePortable -18
			${Else}
				StrCpy $AppName $AppNamePortable
			${EndIf}
		${EndIf}
	${EndIf}

	; Work out if it's 64-bit or 32-bit
	System::Call kernel32::GetCurrentProcess()i.s
	System::Call kernel32::IsWow64Process(is,*i.r0)
	${If} $0 == 0
		StrCpy $Bits 32
	${Else}
		StrCpy $Bits 64
	${EndIf}

!macroend

${SegmentInit}
	; Copy the launcher INI file to $PLUGINSDIR so that it doesn't go splurk if
	; the disk is pulled out and can clean up.
	StrCpy $LauncherFile $EXEDIR\App\AppInfo\Launcher\$BaseName.ini
	${If} ${FileExists} $LauncherFile
		InitPluginsDir
		CopyFiles /SILENT $LauncherFile $PLUGINSDIR\launcher.ini
		StrCpy $LauncherFile $PLUGINSDIR\launcher.ini
	${Else}
		StrCpy $MissingFileOrPath $LauncherFile
		MessageBox MB_OK|MB_ICONSTOP `$(LauncherFileNotFound)`
		Quit
	${EndIf}

	; If there are command line arguments, we use
	; [Launch]:ProgramExecutableWhenParameters if it exists, falling back to
	; the normal [Launch]ProgramExecutable if it's not set or if there aren't
	; arguments.
	${GetParameters} $0
	StrCpy $ProgramExecutable ""

	${If} $Bits = 64
		${If} $0 != ""
			${ReadLauncherConfig} $ProgramExecutable Launch ProgramExecutableWhenParameters64
		${EndIf}
		${If} $ProgramExecutable == ""
			${ReadLauncherConfig} $ProgramExecutable Launch ProgramExecutable64
		${EndIf}
	${EndIf}

	${If} $0 != ""
	${AndIf} $ProgramExecutable == ""
		${ReadLauncherConfig} $ProgramExecutable Launch ProgramExecutableWhenParameters
	${EndIf}

	${If} $ProgramExecutable == ""
		${ReadLauncherConfig} $ProgramExecutable Launch ProgramExecutable
	${EndIf}

	${If} $ProgramExecutable == ""
		; Launcher file missing or missing crucial details (what am I to launch?)
		MessageBox MB_OK|MB_ICONSTOP `$EXEDIR\App\AppInfo\Launcher\$BaseName.ini is missing [Launch]:ProgramExecutable - what am I to launch?`
		Quit
	${EndIf}

!macroend

${SegmentPreExecPrimary}
	; Save the $PLUGINSDIR so that in case of crash it can still be cleaned up next time
	${WriteRuntimeData} PortableApps.comLauncher PluginsDir $PLUGINSDIR
!macroend

${SegmentUnload}
	; Clear up $PLUGINSDIR, the runtime data which says we're running, and the
	; $PLUGINSDIR from before the hypothetical power failure.
	FileClose $_FEIP_FileHandle
	Delete $PLUGINSDIR\launcher.ini
	${If} $SecondaryLaunch != true
		${ReadRuntimeData} $0 PortableApps.comLauncher PluginsDir
		${If}    $0 != ""
		${AndIf} $0 != $PLUGINSDIR
			RMDir /r $0
		${EndIf}
		Delete $DataDirectory\PortableApps.comLauncherRuntimeData-$BaseName.ini
	${EndIf}
	Delete $PLUGINSDIR\runtimedata.ini
	; Unload the system plug-in (if it's still there?)
	System::Free 0
!macroend
