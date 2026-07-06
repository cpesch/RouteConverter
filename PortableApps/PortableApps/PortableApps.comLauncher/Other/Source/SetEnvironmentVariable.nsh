!macro SetEnvironmentVariable _VAR _VAL
; To be able to cope with strings approaching NSIS_MAX_STRLEN, we use r0-R9
; or failing that Push so that there's no possibility of string overflow.
; While this looks long, almost all of it is compiler instructions so it's
; short to actually run (up to two Pushes and a System::Call).
	!verbose push
	!verbose 3
	!if "${_VAL}" == $0
		!define VAL r0
	!else if "${_VAL}" == $1
		!define VAL r1
	!else if "${_VAL}" == $2
		!define VAL r2
	!else if "${_VAL}" == $3
		!define VAL r3
	!else if "${_VAL}" == $4
		!define VAL r4
	!else if "${_VAL}" == $5
		!define VAL r5
	!else if "${_VAL}" == $6
		!define VAL r6
	!else if "${_VAL}" == $7
		!define VAL r7
	!else if "${_VAL}" == $8
		!define VAL r8
	!else if "${_VAL}" == $9
		!define VAL r9
	!else if "${_VAL}" == $R0
		!define VAL R0
	!else if "${_VAL}" == $R1
		!define VAL R1
	!else if "${_VAL}" == $R2
		!define VAL R2
	!else if "${_VAL}" == $R3
		!define VAL R3
	!else if "${_VAL}" == $R4
		!define VAL R4
	!else if "${_VAL}" == $R5
		!define VAL R5
	!else if "${_VAL}" == $R6
		!define VAL R6
	!else if "${_VAL}" == $R7
		!define VAL R7
	!else if "${_VAL}" == $R8
		!define VAL R8
	!else if "${_VAL}" == $R9
		!define VAL R9
	!else
		Push "${_VAL}"
		!define VAL s
	!endif
	!if "${_VAR}" == $0
		!define VAR r0
	!else if "${_VAR}" == $1
		!define VAR r1
	!else if "${_VAR}" == $2
		!define VAR r2
	!else if "${_VAR}" == $3
		!define VAR r3
	!else if "${_VAR}" == $4
		!define VAR r4
	!else if "${_VAR}" == $5
		!define VAR r5
	!else if "${_VAR}" == $6
		!define VAR r6
	!else if "${_VAR}" == $7
		!define VAR r7
	!else if "${_VAR}" == $8
		!define VAR r8
	!else if "${_VAR}" == $9
		!define VAR r9
	!else if "${_VAR}" == $R0
		!define VAR R0
	!else if "${_VAR}" == $R1
		!define VAR R1
	!else if "${_VAR}" == $R2
		!define VAR R2
	!else if "${_VAR}" == $R3
		!define VAR R3
	!else if "${_VAR}" == $R4
		!define VAR R4
	!else if "${_VAR}" == $R5
		!define VAR R5
	!else if "${_VAR}" == $R6
		!define VAR R6
	!else if "${_VAR}" == $R7
		!define VAR R7
	!else if "${_VAR}" == $R8
		!define VAR R8
	!else if "${_VAR}" == $R9
		!define VAR R9
	!else
		Push "${_VAR}"
		!define VAR s
	!endif
	!verbose pop
	${DebugMsg} "Setting environment variable ${_VAR} to ${_VAL} (internal representations: ${VAR}, ${VAL})"
	System::Call Kernel32::SetEnvironmentVariable(t${VAR},t${VAL})
	!verbose push
	!verbose 3
	!undef VAR
	!undef VAL
	!verbose pop
!macroend
!define SetEnvironmentVariable "!insertmacro SetEnvironmentVariable"

!macro SetEnvironmentVariableDefault NAME VALUE
	Push $R9
	ReadEnvStr $R9 "${NAME}"
	${If} $R9 == ""
		Pop $R9
		${SetEnvironmentVariable} "${NAME}" "${VALUE}"
	${Else}
		Pop $R9
	${EndIf}
!macroend
!define SetEnvironmentVariableDefault "!insertmacro SetEnvironmentVariableDefault"
