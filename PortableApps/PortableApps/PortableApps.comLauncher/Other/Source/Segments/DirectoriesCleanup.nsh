${SegmentFile}

${SegmentPostPrimary}
	;=== DirectoriesCleanupIfEmpty
		StrCpy $R0 1
		${Do}
			ClearErrors
			${ReadLauncherConfig} $1 DirectoriesCleanupIfEmpty $R0
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $1
			${ForEachDirectory} $2 $3 $1
				${DebugMsg} "Cleaning up $2 if it is empty."
				RMDir $2
			${NextDirectory}
			IntOp $R0 $R0 + 1
		${Loop}

	;=== DirectoriesCleanupForce
		StrCpy $R0 1
		${Do}
			ClearErrors
			${ReadLauncherConfig} $1 DirectoriesCleanupForce $R0
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $1
			${ForEachDirectory} $2 $3 $1
				${DebugMsg} "Removing directory $2."
				RMDir /r $2
			${NextDirectory}
			IntOp $R0 $R0 + 1
		${Loop}
!macroend
