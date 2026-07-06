${SegmentFile}

; Currently services are disabled as
;   (a) they're not used yet (possibly unstable) and
;   (b) the plug-in is fairly large (at time of reporting, 122591B vs. 96901B, 25KB larger)
; TODO: switch back to NSIS code... got to sort out the null byte issue with dependencies.

;!define SERVICES_ENABLED

!ifndef SERVICES_ENABLED
	!echo "The Services segment is currently disabled."
!else ifdef NSIS_UNICODE
	!warning "The Services segment is disabled for your build as the SimpleSC plug-in is not Unicode-compatible."
	!undef SERVICES_ENABLED
!endif


${SegmentPrePrimary}
!ifdef SERVICES_ENABLED
	StrCpy $R0 1
	${Do}
		ClearErrors
		${ReadLauncherConfig} $1 Service$R0 Name
		${ReadLauncherConfig} $4 Service$R0 Path
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		${ParseLocations} $4
		SimpleSC::ExistsService $1
		Pop $2
		${If} $2 == 0 ; Service already exists
			${ReadLauncherConfig} $2 Service$R0 IfExists
			${If} $2 == replace
				MessageBox MB_ICONEXCLAMATION "TODO: The backing up and replacement of services is not yet implemented. The local service will remain."
				/*
				SimpleSC::GetServiceDisplayName $1
				Pop $9
				Pop $2
				${DebugMsg} "Local service $1's display name is $2 (error code $9)"
				SimpleSC::GetServiceBinaryPath $1
				Pop $9
				Pop $3
				${DebugMsg} "Local service $1's binary path is $3 (error code $9)"
				; TODO: this is going to be very messy. I'm not going to do it till later.
				;${NewServiceLib.BackupService} $1 $DataDirectory\PortableApps.comLauncherWorkingData.ini Service$1
				SimpleSC::RemoveService $1
				Pop $9
				${DebugMsg} "Removed local service $1 (error code $9)
				*/
			${EndIf}
			${WriteRuntimeData} Service$R0 ExistedBefore true
			StrCpy $R9 no-create
		${EndIf}
		${If} $R9 == no-create
			${DebugMsg} "Not creating service $1 (local service already exists)"
		${Else}
			${ReadLauncherConfigWithDefault} $2 Service$R0 Display $1
			${ReadLauncherConfig} $3 Service$R0 Type
			${If} $3 == driver-kernel
				StrCpy $3 1
			${ElseIf} $3 == driver-file-system
				StrCpy $3 2
			${Else}
				StrCpy $3 16
			${EndIf}
			${ReadLauncherConfig} $5 Service$R0 Dependencies
			${ReadLauncherConfig} $6 Service$R0 User
			${If} $4 == LocalService
			${OrIf} $4 == NetworkService
				StrCpy $4 "NT AUTHORITY\$4"
			${EndIf}
			SimpleSC::InstallService $1 $2 $3 3 $4 $5 $6 "" ; the 3 is for manual start, "" is an empty password
			Pop $9
			${DebugMsg} "Installed service $1 (error code $9)"
			ClearErrors
			${ReadLauncherConfig} $7 Service$R0 Description
			${If} ${Errors}
				SimpleSC::SetServiceDescription $1 $7
				Pop $9
				${DebugMsg} "Set service $1's description to $7"
			${EndIf}
			IntOp $R0 $R0 + 1
		${EndIf}
	${Loop}
!endif
!macroend

${SegmentPostPrimary}
!ifdef SERVICES_ENABLED
	StrCpy $R0 1
	${Do}
		ClearErrors
		${ReadLauncherConfig} $1 Service$R0 Name
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		; TODO: save state in the runtime data to prevent doing anything silly.
		; Possibly also check the service path to make sure it's the right one we delete.
		SimpleSC::GetServiceStatus $1
		Pop $9 ; error code
		Pop $2 ; return value
		${DebugMsg} "Service $1's status: $2 (error code $9)"
		${If} $2 != 1 ; 1 = stopped
			SimpleSC::StopService $1
			Pop $9
			${DebugMsg} "Stopped service $1 (error code $9)"
		${EndIf}
		SimpleSC::RemoveService $1
		Pop $9
		${DebugMsg} "Removed service $1 (error code $9)"
	${Loop}
!endif
!macroend
