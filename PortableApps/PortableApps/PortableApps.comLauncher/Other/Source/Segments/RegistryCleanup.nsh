${SegmentFile}

${SegmentPostPrimary}
	${If} $UsesRegistry == true
		; RegistryCleanupIfEmpty
		StrCpy $R0 1
		${Do}
			ClearErrors
			${ReadLauncherConfig} $1 RegistryCleanupIfEmpty $R0
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ValidateRegistryKey} $1
			${DebugMsg} "Deleting registry key $1 if it is empty."
			${registry::DeleteKeyEmpty} $1 $R9
			IntOp $R0 $R0 + 1
		${Loop}

		; RegistryCleanupForce
		StrCpy $R0 1
		${Do}
			ClearErrors
			${ReadLauncherConfig} $1 RegistryCleanupForce $R0
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ValidateRegistryKey} $1
			${DebugMsg} "Deleting registry key $1."
			${registry::DeleteKey} $1 $R9
			IntOp $R0 $R0 + 1
		${Loop}
	${EndIf}
!macroend
