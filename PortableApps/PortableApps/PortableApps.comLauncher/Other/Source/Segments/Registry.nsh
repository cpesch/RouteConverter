${SegmentFile}

!include Registry.nsh

Var UsesRegistry

${SegmentInit}
	ClearErrors
	${ReadLauncherConfig} $UsesRegistry Activate Registry
	${If} $UsesRegistry == true
		${DebugMsg} "Registry sections enabled."
	${ElseIf} $UsesRegistry != false
	${AndIfNot} ${Errors}
		${InvalidValueError} [Activate]:Registry $UsesRegistry
	${EndIf}
!macroend

${SegmentUnload}
	${IfThen} $UsesRegistry == true ${|} ${registry::Unload} ${|}
!macroend

!define ValidateRegistryKey `!insertmacro ValidateRegistryKeyCall`
!macro ValidateRegistryKeyCall KEY
	Push `${KEY}`
	${CallArtificialFunction} ValidateRegistryKey_
	Pop `${KEY}`
!macroend
!macro ValidateRegistryKey_
	; HKEY_CLASSES_ROOT  --> HKCU\Software\Classes
	; HKEY_CURRENT_USER  --> HKCU
	; HKEY_LOCAL_MACHINE --> HKLM
	; HKCR               --> HKCU\Software\Classes
	Exch $0
	Push $1
	StrCpy $1 $0 17
	${If} $1 == HKEY_CLASSES_ROOT
		StrCpy $0 $0 "" 17
		StrCpy $0 HKCU\Software\Classes$0
	${ElseIf} $1 == HKEY_CURRENT_USER
		StrCpy $0 $0 "" 17
		StrCpy $0 HKCU$0
	${Else}
		StrCpy $1 $0 18
		${If} $1 == HKEY_LOCAL_MACHINE
			StrCpy $0 $0 "" 18
			StrCpy $0 HKLM$0
		${Else}
			StrCpy $1 $0 4
			${If} $1 == HKCR
				StrCpy $0 $0 "" 4
				StrCpy $0 HKCU\Software\Classes$0
			${ElseIf} $1 != HKCU
			${AndIf} $1 != HKLM
				MessageBox MB_OK|MB_ICONSTOP `Note to portable application developer: registry hive in key "$0" is bad, should start with HKCR, HKCU or HKLM. Please fix this. (The launcher will continue running.)`
			${EndIf}
		${EndIf}
	${EndIf}
	Pop $1
	Exch $0
!macroend

!macro _LL_RegistryKeyExists _a _b _t _f
	!insertmacro _LOGICLIB_TEMP
	${registry::KeyExists} `${_b}` $_LOGICLIB_TEMP
	!insertmacro _= $_LOGICLIB_TEMP 0 `${_t}` `${_f}`
!macroend
!define RegistryKeyExists `"" LL_RegistryKeyExists`
