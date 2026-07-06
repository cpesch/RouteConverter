!ifndef TrimWhite
!define TrimWhite "!insertmacro TrimWhiteCall"
!macro TrimWhiteCall STRING
	Push ${STRING}
	${CallArtificialFunction} TrimWhite_
	Pop ${STRING}
!macroend
!macro TrimWhite_
	; $R1 is the string, $R2 is for working
	Exch $R1
	Push $R2
 
	; Trim from the left hand side of the string
	StrCpy $R2 $R1 1      ; look at the first character.
	StrCmp $R2 " " +2     ; is it a space?
	StrCmp $R2 "$\t" 0 +3 ; or a tab?
	StrCpy $R1 $R1 "" 1   ; if so, remove it
	Goto -4               ; and go through again.

	; Trim from the right hand side of the string
	StrCpy $R2 $R1 1 -1   ; look at the last character.
	StrCmp $R2 " " +2     ; is it a space?
	StrCmp $R2 "$\t" 0 +3 ; or a tab?
	StrCpy $R1 $R1 -1     ; if so, remove it
	Goto -4               ; and go through again.
 
	; Put everything back as we need it
	Pop $R2
	Exch $R1
!macroend
!endif
