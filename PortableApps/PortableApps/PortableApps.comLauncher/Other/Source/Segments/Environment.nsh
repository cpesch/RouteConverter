${SegmentFile}

${SegmentPre}
	${ForEachINIPair} Environment $0 $1
		; Very simple, just parse the environment in the value and set it.
		${ParseLocations} $1
		${DebugMsg} "Setting environment variable $0 to $1"
		System::Call Kernel32::SetEnvironmentVariable(tr0,tr1)
	${NextINIPair}
!macroend
