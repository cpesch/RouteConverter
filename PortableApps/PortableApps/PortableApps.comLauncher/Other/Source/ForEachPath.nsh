/***************************************************
*** Notes ***

Valid wildcards:
  ? matches a single character
  * matches any number of characters

Only one level in the given path is allowed to contain wildcards.
Example: X:\Dir*\File???.??? is not allowed, but X:\Dir\File???.??? is allowed

${ForEachFile/Directory} sets the error flag if no matching file/directory
could be found.
***************************************************/
Var _FEP_FindHandle
Var _FEP_FoundName
Var _FEP_Extension
Var _FEP_WildCardPath
Var _FEP_WildCardChild
Var _FEP_WildCardParent

!macro ForEachPath TYPE FOUND_PATH FILE_NAME SEARCH_PATH
	; Split into two !ifs rather than && due to a horrible NSIS bug where it
	; reckons !if only likes 1-4 parameters when it's inside a macro...
	!if ${TYPE} != FILES
	!if ${TYPE} != DIRECTORIES
		!error "Please use ForEachFile or ForEachDirectory rather than using ForEachPath directly."
	!endif
	!endif
	!ifdef _ForEachPath_Open
		!error "There is already a ForEachPath clause open!"
	!endif
	!define _ForEachPath_Open
	${SplitAfterWildCard} $_FEP_WildCardPath $_FEP_WildCardChild "${SEARCH_PATH}"
	${GetParent} $_FEP_WildCardPath $_FEP_WildCardParent
	${GetFileExt} "${SEARCH_PATH}" $_FEP_Extension
	StrCpy ${FILE_NAME} ''
	${Do}
		ClearErrors
		${IfNot} ${WildCardFlag}
			StrCpy ${FOUND_PATH} "${SEARCH_PATH}"
			${IfNotThen} ${FileExists} "${SEARCH_PATH}" ${|} SetErrors ${|}
		${Else}
			${If} $_FEP_FindHandle = 0
				FindFirst $_FEP_FindHandle $_FEP_FoundName $_FEP_WildCardPath
			${Else}
				FindNext $_FEP_FindHandle $_FEP_FoundName
			${EndIf}
			StrCpy ${FOUND_PATH} $_FEP_WildCardParent\$_FEP_FoundName$_FEP_WildCardChild
		${EndIf}
		${If} ${Errors}
			${IfThen} ${FILE_NAME} == '' ${|} SetErrors ${|}
			${ExitDo}
		${EndIf}
!if ${TYPE} == FILES
		${IfNot} ${FileExists} ${FOUND_PATH}\*.*
		${AndIf} ${FileExists} ${FOUND_PATH}
!else if ${TYPE} == DIRECTORIES
		${If} ${FileExists} ${FOUND_PATH}\*.*
		${AndIf} $_FEP_FoundName != .
		${AndIf} $_FEP_FoundName != ..
!endif
			Push $0
			${GetFileExt} ${FOUND_PATH} $0
			${If} $_FEP_Extension == ''
			${OrIf} $_FEP_Extension *= $0
			${AndIf} $0 != BackupBy$AppID
			${OrIf} $_FEP_Extension == $0 ; .BackupBy$AppID ?
				Pop $0
				${GetFileName} ${FOUND_PATH} ${FILE_NAME}
!macroend

!macro NextPath
	!ifndef _ForEachPath_Open
		!error "There isn't a ForEachPath clause open!"
	!endif
	!undef _ForEachPath_Open
			${Else}
				Pop $0
			${EndIf}
		${EndIf}
	${LoopWhile} ${WildCardFlag}
	${If} $_FEP_FindHandle <> 0
		FindClose $_FEP_FindHandle
		StrCpy $_FEP_FindHandle ''
	${EndIf}
!macroend

!define ForEachFile '!insertmacro ForEachPath FILES'
!define NextFile '!insertmacro NextPath'

!define ForEachDirectory '!insertmacro ForEachPath DIRECTORIES'
!define NextDirectory '!insertmacro NextPath'

!define WildCardFlag '$_FEP_FindHandle != 0'
!define UnSetWildCardFlag 'StrCpy $_FEP_FindHandle 0'
!define SetWildCardFlag 'StrCpy $_FEP_FindHandle ""'

!macro _*= _a _b _t _f
	!verbose push
	!insertmacro _LOGICLIB_TEMP
	Push `${_a}`
	Push `${_b}`
	${CallArtificialFunction} LLWildCardStrCmp_
	Pop $_LOGICLIB_TEMP
	!insertmacro _= $_LOGICLIB_TEMP 0 `${_f}` `${_t}`
	!verbose pop
!macroend
!macro LLWildCardStrCmp_
	Exch $0
	Exch
	Exch $1
	Push $2
	Push $3
	Push $4
	Push $5
		StrCpy $2 -1
		StrCpy $5 -1
	_LLWildCardStrCmp_Loop:
		IntOp $2 $2 + 1
		StrCpy $3 $1 1 $2
	_LLWildCardStrCmp_Asterisk:
		IntOp $5 $5 + 1
		StrCpy $4 $0 1 $5
		StrCmpS $4 '' _LLWildCardStrCmp_Found
		StrCmpS $3 ? _LLWildCardStrCmp_Loop
		StrCmpS $3 * _LLWildCardStrCmp_Asterisk
		StrCmp $3 $4 _LLWildCardStrCmp_Loop _LLWildCardStrCmp_NotFound
	_LLWildCardStrCmp_Found:
		StrCmpS $3 * +2
		StrCmpS $3 '' 0 _LLWildCardStrCmp_NotFound
		StrCpy $0 1
		Goto _LLWildCardStrCmp_End
	_LLWildCardStrCmp_NotFound:
		StrCpy $0 0
	_LLWildCardStrCmp_End:
	Pop $5
	Pop $4
	Pop $3
	Pop $2
	Pop $1
	Exch $0
!macroend

!macro SplitAfterWildCard PARENT_PART CHILD_PART PATH
	!verbose push
	Push `${PATH}`
	${CallArtificialFunction} SplitAfterWildCard_
	Pop ${CHILD_PART}
	Pop ${PARENT_PART}
	!verbose pop
!macroend
!define SplitAfterWildCard `!insertmacro SplitAfterWildCard`
!macro SplitAfterWildCard_
	Exch $0
	Push $1
	Push $2
	Push $3
		StrCpy $1 -1
		StrLen $3 $0
	_SAWC_Loop:
		IntOp $1 $1 + 1
		StrCpy $2 $0 1 $1
		StrCmpS $2 ? _SAWC_Found_Loop
		StrCmpS $2 * _SAWC_Found_Loop
		IntCmp $3 $1 _SAWC_NotFound '' _SAWC_Loop

	_SAWC_Found_Loop:
		IntOp $1 $1 + 1
		StrCpy $2 $0 1 $1
		StrCmpS $2 '\' _SAWC_Found_End
		IntCmp $3 $1 0 '' _SAWC_Found_Loop

	_SAWC_Found_End:
		StrCpy $1 $0 $1
		StrLen $3 $1
		StrCpy $0 $0 '' $3
		${SetWildCardFlag}
		Goto _SAWC_End
	_SAWC_NotFound:
		StrCpy $1 $0
		StrCpy $0 ''
		${UnSetWildCardFlag}
	_SAWC_End:
	Pop $3
	Pop $2
	Exch $1
	Exch
	Exch $0
!macroend
