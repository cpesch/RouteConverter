!ifndef CALLANSIPLUGIN_INCLUDED
!define CALLANSIPLUGIN_INCLUDED
	!ifdef NSIS_UNICODE

		!ifndef CP_UTF8
			!define CP_UTF8 65001
		!endif
		!ifndef CP_ACP
			!define CP_ACP 0
		!endif

		!define PushAsANSI '!insertmacro PushAs_ ${CP_ACP}'
		!define PushAsUTF8 '!insertmacro PushAs_ ${CP_UTF8}'
		!macro PushAs_ ENCODING VAR
			Push `${VAR}`
			System::Call kernel32::WideCharToMultiByte(i${ENCODING},,ts,i-1,t.s,i${NSIS_MAX_STRLEN},,)
		!macroend

		!define PopAsANSI '!insertmacro PopAs_ ${CP_ACP}'
		!define PopAsUTF8 '!insertmacro PopAs_ ${CP_UTF8}'
		!macro PopAs_ ENCODING VAR
			System::Call kernel32::MultiByteToWideChar(i${ENCODING},,ts,i-1,t.s,i${NSIS_MAX_STRLEN})
			Pop ${VAR}
		!macroend

		!define VarToANSI '!insertmacro VarTo_ ${CP_ACP}'
		!define VarToUTF8 '!insertmacro VarTo_ ${CP_UTF8}'
		!macro VarTo_ ENCODING VAR
			Push `${VAR}`
			System::Call kernel32::WideCharToMultiByte(i${ENCODING},,ts,i-1,t.s,i${NSIS_MAX_STRLEN},,)
			Pop ${VAR}
		!macroend

		!define VarFromANSI '!insertmacro VarFrom_ ${CP_ACP}'
		!define VarFromUTF8 '!insertmacro VarFrom_ ${CP_UTF8}'
		!macro VarFrom_ ENCODING VAR
			Push `${VAR}`
			System::Call kernel32::MultiByteToWideChar(i${ENCODING},,ts,i-1,t.s,i${NSIS_MAX_STRLEN})
			Pop ${VAR}
		!macroend

	!else ; NSIS_UNICODE
	
		!define PushAsANSI Push
		!define PushAsUTF8 Push
		!define PopAsANSI Pop
		!define PopAsUTF8 Pop
		!define VarToANSI '!insertmacro VarTo_ 0'
		!define VarToUTF8 '!insertmacro VarTo_ 0'
		!macro VarTo_ ENCODING VAR
		!macroend
		!define VarFromANSI '!insertmacro VarFrom_ 0'
		!define VarFromUTF8 '!insertmacro VarFrom_ 0'
		!macro VarFrom_ ENCODING VAR
		!macroend
	
	!endif ; NSIS_UNICODE
!endif ; CALLANSIPLUGIN_INCLUDED
