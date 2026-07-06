/*
_____________________________________________________________________________

                      NullByte.nhs v0.3
_____________________________________________________________________________

 2010 Mark Sikkema aka gringoloco
 2010 Chris Morgan (ForEachValueInNullSeparatedString)
 License: zlib/libpng
_____________________________________________________________________________
;=== instructions

${CreateBuffer} [HANDLE_OUT] [length (in chars)]	;creates a handle of the given length (in TCHARS, WCHARS for NSISu) to a buffer
${FreeBuffer} [HANDLE_IN]				;frees the buffer
${WriteNullString} [HANDLE_IN] "[STRING_IN]"		;writes the string to the buffer and replaces all delimiters with a null byte
${ReadNullString} [HANDLE_IN] "[STRING_OUT]"		;reads the string from the buffer and replaces all null bytes with the delimiters

There is also a LogicLib-style way of reading null-separated strings:
  ${ForEachValueInNullSeparatedString} [HANDLE] [STRING_OUT]
  ${NextValueInNullSeparatedString}

BTW: You are able to re-write to the same buffer again and again, without the need to re-create it
_____________________________________________________________________________
;=== example script

OutFile "TestNull.exe"

;!define \0 "?" ;optional, if a different delimiter is needed
!include NullByte.nsh

VAR HANDLE
VAR STRING

Section "Main"
StrCpy $STRING "test1${\0}test2${\0}test3"

${CreateBuffer} $HANDLE ${NSIS_MAX_STRLEN}

${WriteNullString} $HANDLE "$STRING"

System::Call /NOUNLOAD "*$HANDLE(&t${NSIS_MAX_STRLEN} .r9)" ; read string, as it will show in NSIS regularly
Messagebox MB_OK "The string in NSIS shows like this now:$\n$9$\n$\n press ok to show whole string"

StrLen $2 "$STRING"
IntOp $2 $2 + 2
IntOp $2 $2 * ${NSIS_CHAR_SIZE}
System::Call "Advapi32::RegCreateKey(i, t, *i) i(0x80000001, 'Software\Test', .r0) .r3"
System::Call "Advapi32::RegSetValueEx(i, t, i, i, i, i) i(r0, '', 0, 7, $HANDLE, r2) .r3"
System::Call "Advapi32::RegCloseKey(i) i(r0)"
${ReadNullString} $HANDLE $9

Messagebox MB_OK "Reversed the string:$\n$9$\nand wrote the buffer to the registry, please check...$\nHKCU\Software\Test$\nPress OK to delete the key."
DeleteRegKey HKCU "Software\Test"

${FreeBuffer} $HANDLE
SectionEnd

*/
;_____________________________________________________________________________
;=== defines & includes

!ifndef CreateBuffer
!ifndef LOGICLIB
	!include LogicLib.nsh
!endif
!ifndef \0
	!define \0 "/" ;delimiter
!endif
!ifndef NSIS_CHAR_SIZE
	!define NSIS_CHAR_SIZE 1
!endif
!define CreateBuffer '!insertmacro CreateBuffer'
!define FreeBuffer '!insertmacro FreeBuffer'
!define WriteNullString '!insertmacro WriteNullString'
!define ReadNullString '!insertmacro ReadNullString'
;_____________________________________________________________________________
;=== macros

!macro CreateBuffer HANDLE LENGTH
System::Call /NOUNLOAD "*(&t${LENGTH}) i.s"
Pop ${HANDLE}
!macroend

!macro FreeBuffer HANDLE
System::Free /NOUNLOAD ${HANDLE}
!macroend

!macro WriteNullString HANDLE STRING
Push ${HANDLE}
Push `${STRING}`
Exch $0 ;STRING
Exch
Exch $1 ;HANDLE
Push $2
Push $3
StrLen $3 "$0"
IntOp $2 $3 * ${NSIS_CHAR_SIZE}					;to not overflow the memory
IntOp $2 $2 + ${NSIS_CHAR_SIZE}					;account for the null
System::Call "*$1(&t$2 r0)"					;write the string
${While} $3 >= 0						;loop through all chars out of the string
	StrCpy $2 $0 1 -$3					;search for delimiter
	${IfThen} $2 == ${\0} ${|} \
		System::Call /NOUNLOAD "*$1(&t1 ``)" ${|}	;if char==delimiter write null
	IntOp $1 $1 + ${NSIS_CHAR_SIZE}				;next location
	IntOp $3 $3 - 1						;next char
${EndWhile}
System::Call /NOUNLOAD "*$1(&t1 ``)"				;write the last null for termination
Pop $3
Pop $2
Pop $1
Pop $0
!macroend


!macro ReadNullString HANDLE STRING
Push $0
Push $1
Push $2
StrCpy $1 ${HANDLE}
StrCpy $0 ""
${Do}
	System::Call /NOUNLOAD "*$1(&t${NSIS_MAX_STRLEN} .r2)"	;read first/next string
	${IfThen} $2 == "" ${|} ${Break} ${|}			;found double null, no more strings
	StrCpy $0 "$0$2${\0}"					;combine the strings and delimiters
	StrLen $2 $2
	IntOp $2 $2 * ${NSIS_CHAR_SIZE}				;unicode nsis compatible
	IntOp $1 $1 + $2					;go to end of (separated) string
	IntOp $1 $1 + ${NSIS_CHAR_SIZE}				;skip the null byte
${Loop}
StrCpy  $0 $0 -1						;remove last delimiter
Pop $2
Pop $1
Exch $0
Pop ${STRING}
!macroend
;_____________________________________________________________________________

Var _NB_Index
Var _NB_StrLen

!define ForEachValueInNullSeparatedString '!insertmacro ForEachValueInNullSeparatedString'
!define NextValueInNullSeparatedString '!insertmacro NextValueInNullSeparatedString'

!macro ForEachValueInNullSeparatedString HANDLE STRING
	StrCpy $_NB_Index ${HANDLE}
	${Do}
		System::Call /NOUNLOAD "*$_NB_Index(&t${NSIS_MAX_STRLEN} .s)"	;read first/next string
		Pop ${STRING}
		${IfThen} ${STRING} == "" ${|} ${Break} ${|}			;found double null, no more strings
		StrLen $_NB_StrLen ${STRING}
!macroend

!macro NextValueInNullSeparatedString
		IntOp $_NB_StrLen $_NB_StrLen * ${NSIS_CHAR_SIZE}	;unicode nsis compatible
		IntOp $_NB_Index $_NB_Index + $_NB_StrLen		;go to end of (separated) string
		IntOp $_NB_Index $_NB_Index + ${NSIS_CHAR_SIZE}		;skip the null byte
	${Loop}
!macroend
!endif
