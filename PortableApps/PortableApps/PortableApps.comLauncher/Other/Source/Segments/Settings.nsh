${SegmentFile}

${SegmentInit}
	; Check for settings
	${IfNot} ${FileExists} $EXEDIR\Data\settings
		${DebugMsg} "$EXEDIR\Data\settings does not exist. Creating it."
		CreateDirectory $EXEDIR\Data\settings
		${If} ${FileExists} $EXEDIR\App\DefaultData\*.*
			${DebugMsg} "Copying default data from $EXEDIR\App\DefaultData to $EXEDIR\Data."
			CopyFiles /SILENT $EXEDIR\App\DefaultData\*.* $EXEDIR\Data
		${EndIf}
	${EndIf}
!macroend
