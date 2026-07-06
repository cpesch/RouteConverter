/* The PortableApps.com Launcher now does everything in it in "segments".
 * This means code can be grouped together by what it does rather than by when it runs.
 *
 * To facilitate this, there are a number of "hooks" you can latch onto.
 * The Primary and Secondary variants are for primary and secondary instances of the application.
 *   .onInit                     - things which must go in .onInit
 *   Init                        - load data into variables, do startup stuff
 *   Pre(Primary|Secondary)?     - Set up portability
 *   PreExec(Primary|Secondary)? - Just before it gets executed
 *   Post(Primary|Secondary)?    - Move settings and whatnot back
 *   Unload                      - Unload plug-ins, clean up the launcher itself
 *
 * When a new segment is created, be sure to add it below in RunSegmentAction(Reverse).
 */

; Create segment file definitions {{{1
!macro _CreateSegmentDef _TYPE
	!ifdef Segment${_TYPE}
		!undef Segment${_TYPE}
	!endif
	!define Segment${_TYPE} "!macro ${__FILE__}_${_TYPE}"
!macroend
!define _CreateSegmentDef "!insertmacro _CreateSegmentDef"

!macro SegmentFile
	${_CreateSegmentDef} .onInit
	${_CreateSegmentDef} Init
	${_CreateSegmentDef} Pre
	${_CreateSegmentDef} PrePrimary
	${_CreateSegmentDef} PreSecondary
	${_CreateSegmentDef} PreExec
	${_CreateSegmentDef} PreExecPrimary
	${_CreateSegmentDef} PreExecSecondary
	${_CreateSegmentDef} Post
	${_CreateSegmentDef} PostPrimary
	${_CreateSegmentDef} PostSecondary
	${_CreateSegmentDef} Unload
!macroend
!define SegmentFile "!insertmacro SegmentFile"

; Run an action {{{1
!macro RunSegment Segment
	!ifdef _DisableHook_${Segment}_${__FUNCTION__}
		!echo "Segment ${Segment}, hook ${__FUNCTION__} has been disabled by Custom.nsh."
	!else ifmacrondef ${Segment}.nsh_${__FUNCTION__}
		!if ${Segment} != Custom
			!warning "Segment ${Segment}, hook ${__FUNCTION__} was called but does not exist!"
		!endif
	!else
		${!getdebug}
		!ifdef DEBUG && DEBUG_SEGWRAP
			${DebugMsg} "About to execute segment"
		!endif
		!insertmacro ${Segment}.nsh_${__FUNCTION__}
		!ifdef DEBUG && DEBUG_SEGWRAP
			${DebugMsg} "Finished executing segment"
		!endif
	!endif
!macroend
!define RunSegment "!insertmacro RunSegment"

/* Run an action (not being used) {{{1
 * action = (.on)?Init|Unload|(Pre(Exec)?|Post)(Primary|Secondary)?
 * ${RunSegmentAction}        action
 * ${RunSegmentActionReverse} action <-- use this for Post as it does them in the reverse order (so that it's nested)
 * /
******************************************************************
* Not using these macros at the moment.                          *
* Too much like hard work maintaining a list like that just now. *
******************************************************************

!macro _RunSingleSegmentAction _SEGMENT
	!ifmacrodef ${_SEGMENT}_${_ACTION}
		!insertmacro ${_SEGMENT}_${_ACTION}
	!endif
!macroend
!define _RunSingleSegmentAction "!insertmacro _RunSingleSegment"

!macro RunSegmentAction _ACTION
	${_RunSingleSegmentAction} Mutex
	${_RunSingleSegmentAction} SplashScreen
	${_RunSingleSegmentAction} WorkingDirectory
	${_RunSingleSegmentAction} RefreshShellIcons
!macroend
!define RunSegmentAction "!insertmacro RunSegment"

!macro RunSegmentActionReverse _ACTION
	${_RunSingleSegmentAction} RefreshShellIcons
	${_RunSingleSegmentAction} WorkingDirectory
	${_RunSingleSegmentAction} SplashScreen
	${_RunSingleSegmentAction} Mutex
!macroend
!define RunSegmentActionReverse "!insertmacro RunSegment"
/* End this bit */
; Include the segments {{{1
!include Segments\*.nsh

; Customisation file {{{1
!macro DisableHook Segment Hook
	!define _DisableHook_${Segment}_${Hook}
!macroend
!define DisableHook "!insertmacro DisableHook"

!macro DisableSegment Segment
	${DisableHook} ${Segment} .onInit
	${DisableHook} ${Segment} Init
	${DisableHook} ${Segment} Pre
	${DisableHook} ${Segment} PrePrimary
	${DisableHook} ${Segment} PreSecondary
	${DisableHook} ${Segment} PreExec
	${DisableHook} ${Segment} PreExecPrimary
	${DisableHook} ${Segment} PreExecSecondary
	${DisableHook} ${Segment} Post
	${DisableHook} ${Segment} PostPrimary
	${DisableHook} ${Segment} PostSecondary
	${DisableHook} ${Segment} Unload
!macroend
!define DisableSegment "!insertmacro DisableSegment"

!define OverrideExecute "!macro OverrideExecuteFunction"

!include /nonfatal "${PACKAGE}\App\AppInfo\Launcher\Custom.nsh"
