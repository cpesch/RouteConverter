!include TrimWhite.nsh

Var _FEIP_FileHandle
Var _FEIP_Line
Var _FEIP_LineLength
Var _FEIP_CharNum
Var _FEIP_Char

!macro ForEachINIPair SECTION KEY VALUE
	!ifdef _ForEachINIPair_Open
		!error "There is already a ForEachINIPair clause open!"
	!endif
	!define _ForEachINIPair_Open
	${If} $_FEIP_FileHandle == ""
		FileOpen $_FEIP_FileHandle $LauncherFile r
	${Else}
		FileSeek $_FEIP_FileHandle 0
	${EndIf}
	${Do}
		ClearErrors
		FileRead $_FEIP_FileHandle $_FEIP_Line
		${TrimNewLines} $_FEIP_Line $_FEIP_Line
		${If} ${Errors} ; end of file
		${OrIf} $_FEIP_Line == "[${SECTION}]" ; right section
			${ExitDo}
		${EndIf}
	${Loop}

	${IfNot} ${Errors} ; right section
		${Do}
			ClearErrors
			FileRead $_FEIP_FileHandle $_FEIP_Line

			StrCpy $_FEIP_LineLength $_FEIP_Line 1
			${If} ${Errors} ; end of file
			${OrIf} $_FEIP_LineLength == '[' ; new section
				${ExitDo} ; finished
			${EndIf}

			${If} $_FEIP_LineLength == ';' ; a comment line
				${Continue}
			${EndIf}

			StrLen $_FEIP_LineLength $_FEIP_Line
			StrCpy $_FEIP_CharNum '0'
			${Do}
				StrCpy $_FEIP_Char $_FEIP_Line 1 $_FEIP_CharNum
				${IfThen} $_FEIP_Char == '=' ${|} ${ExitDo} ${|}
				IntOp $_FEIP_CharNum $_FEIP_CharNum + 1
			${LoopUntil} $_FEIP_CharNum > $_FEIP_LineLength

			${TrimNewLines} $_FEIP_Line $_FEIP_Line

			${If} $_FEIP_Char == '='
				StrCpy ${KEY} $_FEIP_Line $_FEIP_CharNum
				IntOp $_FEIP_CharNum $_FEIP_CharNum + 1
				StrCpy ${VALUE} $_FEIP_Line "" $_FEIP_CharNum

				; Get rid of any leading or trailing whitespace
				${TrimWhite} ${KEY}
				${TrimWhite} ${VALUE}

				; Get rid of quotes on a quoted string
				; (This leaves whitespace inside intact.)
				StrCpy $_FEIP_CharNum ${VALUE} 1
				StrCpy $_FEIP_Char ${VALUE} "" -1
				${If} $_FEIP_CharNum == $_FEIP_Char
					${If} $_FEIP_Char == "'"
					${OrIf} $_FEIP_Char == '"'
						StrCpy ${VALUE} ${VALUE} -1 1
					${EndIf}
				${EndIf}
!macroend

!macro NextINIPair
	!ifndef _ForEachINIPair_Open
		!error "There isn't a ForEachINIPair clause open!"
	!endif
	!undef _ForEachINIPair_Open
			${EndIf}
		${Loop}
	${EndIf}
	;FileClose $_FEIP_FileHandle
!macroend

!define ForEachINIPair '!insertmacro ForEachINIPair'
!define NextINIPair '!insertmacro NextINIPair'
