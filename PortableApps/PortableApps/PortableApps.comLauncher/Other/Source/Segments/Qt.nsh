${SegmentFile}

${SegmentPostPrimary}
	; The Qt plugin cache and a few other things leave keys inside
	; HKCU\Software\Trolltech\OrganizationDefaults\*\X:\...\dirname which need
	; to be cleared up. They're useless, they've got just values like foo.dll
	; with a version number in them, but they need removing. Due to the
	; directory-recursive key nature of them, we can just scrap the package
	; directory for each and then prune the tree as far up as it's empty.
	StrCpy $R0 1
	${Do}
		ClearErrors
		${ReadLauncherConfig} $0 QtKeysCleanup $R0
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		StrCpy $1 Software\Trolltech\OrganizationDefaults\$0\$AppDirectory
		DeleteRegKey HKCU $1
		${Do}
			${GetParent} $1 $1
			DeleteRegKey /ifempty HKCU $1
		${LoopUntil} $1 == "Software\Trolltech"

		IntOp $R0 $R0 + 1
	${Loop}
	; We don't need to set $UsesRegistry to true, the registry plug-in hasn't
	; been used and unloading it is what UsesRegistry is for.
!macroend
