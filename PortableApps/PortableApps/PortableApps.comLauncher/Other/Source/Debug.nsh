; Macro: check if in debug mode for the current section {{{1
!macro !getdebug
	!ifdef DEBUG
		!undef DEBUG
	!endif
	!ifdef DEBUG_ALL
		!define DEBUG
	!else
		!ifdef Segment
			!ifdef DEBUG_SEGMENT_${Segment}
				!define DEBUG
			!endif
		!else ifdef DEBUG_GLOBAL
			!define DEBUG
		!endif
	!endif
!macroend
!define !getdebug "!insertmacro !getdebug"

; Macro: print a debug message {{{1
!macro DebugMsg _MSG
	${!getdebug}
	!ifdef DEBUG

		; Logging to file {{{2
		!ifndef DEBUG_OUTPUT
			!define _DebugMsg_OK
		!else if ${DEBUG_OUTPUT} == file
			!define _DebugMsg_OK
		!endif
		!ifdef _DebugMsg_OK
			!ifdef Segment
				!define _DebugMsg_Seg "${Segment}/${__FUNCTION__}"
			!else
				!define _DebugMsg_Seg "Global"
			!endif
			!ifndef _DebugMsg_FileOpened
				Var /GLOBAL _DebugMsg_File
				FileOpen $_DebugMsg_File $EXEDIR\Data\debug.log w
				FileWrite $_DebugMsg_File "PortableApps.com Launcher ${Version} debug messages for ${NamePortable} (${AppID})$\r$\n"
				; TODO: hg revision number from .hg/branch, branchheads.cache
				; My ${!ifexist} doesn't work in Wine, not sure if I can fix it
				!define _DebugMsg_FileOpened
			!else
				FileOpen  $_DebugMsg_File $EXEDIR\Data\debug.log a
				FileSeek  $_DebugMsg_File 0 END
			!endif
			FileWrite $_DebugMsg_File "${_DebugMsg_Seg} (line ${__LINE__}): ${_MSG}$\r$\n$\r$\n"
			FileClose $_DebugMsg_File
			!undef _DebugMsg_Seg
			!undef _DebugMsg_OK
		!endif ;}}}

		; Logging to display: message box {{{2
		!ifndef DEBUG_OUTPUT
			!define _DebugMsg_OK
		!else if ${DEBUG_OUTPUT} == messagebox
			!define _DebugMsg_OK
		!endif
		!ifdef _DebugMsg_OK
			!ifdef Segment
				!define _DebugMsg_Seg "$\r$\n$\r$\nSegment: ${Segment}$\r$\nHook: ${__FUNCTION__}"
			!else
				!define _DebugMsg_Seg ""
			!endif
			MessageBox MB_OKCANCEL|MB_ICONINFORMATION "Debug message at line ${__LINE__}${_DebugMsg_Seg}$\r$\n____________________$\r$\n$\r$\n${_MSG}" IDOK +2
				Abort
			!undef _DebugMsg_Seg
			!undef _DebugMsg_OK
		!endif ;}}}
	!endif
!macroend
!define DebugMsg "!insertmacro DebugMsg" ; }}}

; If you want to debug this, create Debug.nsh in the package's
; App\AppInfo\Launcher directory. It should then have lines like these:
; · Debug everything
;     !define DEBUG_ALL
;   · This leaves out the "about to execute segment" and "finished executing
;     segment" messages unless you put this line in:
;       !define DEBUG_SEGWRAP
; · Debug just certain portions
;   · Debug outside any segments
;       !define DEBUG_GLOBAL
;   · Debug a given segment or segments
;       !define DEBUG_SEGMENT_[SegmentName]
!include /NONFATAL "${PACKAGE}\App\AppInfo\Launcher\Debug.nsh"
