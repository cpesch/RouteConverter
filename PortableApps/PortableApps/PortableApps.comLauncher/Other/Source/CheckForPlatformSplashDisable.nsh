; CheckForPlatformSplashDisable 1.2 (2020-03-20)
;
; Checks if the platform wants the splash screen disabled
; Copyright 2008-2020 John T. Haller of PortableApps.com
; Released under the GPL
;
; Usage: ${CheckForPlatformSplashDisable} _v
;
; Example: ${CheckForPlatformSplashDisable} $DisableSplashScreen
;    If the platform wants it disabled, $DisableSplashScreen will be true.
;    Otherwise it will be whatever its previous value was
;
; Requires: ProcFunc.nsh 2.2 or higher from PortableApps.com

!ifndef CheckForPlatformSplashDisable
	!include LogicLib.nsh
	!include ProcFunc.nsh

	!macro CheckForPlatformSplashDisable _v
		${If} ${_v} != true
			;Get parameter and prep stack
			Push $0
			Push $1
			Push $R0
			
			StrCpy $0 ${_v}

			;Read the environment variable
			ReadEnvStr $1 PortableApps.comDisableSplash
			${If} $1 == true
				${GetParent} $EXEDIR $1
				${If} ${FileExists} "$1\PortableApps.com\PortableAppsPlatform.exe"
					MoreInfo::GetProductName `$1\PortableApps.com\PortableAppsPlatform.exe`
					Pop $R0
					${If} $R0 == "PortableApps.com Platform"
						MoreInfo::GetCompanyName `$1\PortableApps.com\PortableAppsPlatform.exe`
						Pop $R0
						${If} $R0 == "PortableApps.com"
							${If} ${ProcessExists} "PortableAppsPlatform.exe"
								StrCpy $0 true
							${EndIf}
						${EndIf}
					${EndIf}
				${EndIf}
			${EndIf}
			
			;Restore the stack and store the variable
			Pop $R0
			Pop $1
			Exch $0
			Pop ${_v}
		${EndIf}
	!macroend
	!define CheckForPlatformSplashDisable '!insertmacro CheckForPlatformSplashDisable'
!endif