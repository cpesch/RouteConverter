!macro _startswith _a _b _t _f
    !insertmacro _LOGICLIB_TEMP
	StrLen $_LOGICLIB_TEMP `${_b}`
	StrCpy $_LOGICLIB_TEMP `${_a}` $_LOGICLIB_TEMP
	StrCmp $_LOGICLIB_TEMP `${_b}` `${_t}` `${_f}`
!macroend
