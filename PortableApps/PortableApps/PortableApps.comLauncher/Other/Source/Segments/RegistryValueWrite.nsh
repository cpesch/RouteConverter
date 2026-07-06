${SegmentFile}

${SegmentPrePrimary}
	${If} $UsesRegistry == true
		${ForEachINIPair} RegistryValueWrite $0 $1
			; $0 = path\value
			; $1 = type:string or string, then string
			; $2 = path
			; $3 = value
			; $4 = type (default REG_SZ)
			${ValidateRegistryKey} $0
			StrCpy $2 $0 "" -1
			${If} $2 == "\"
				StrCpy $2 $0 -1
				StrCpy $3 "" ; default value
			${Else}
				${GetParent} $0 $2 ; key
				${GetFileName} $0 $3 ; item
			${EndIf}

			StrLen $4 $1
			StrCpy $5 0
			${Do}
				StrCpy $6 $1 1 $5
				${IfThen} $6 == : ${|} ${ExitDo} ${|}
				IntOp $5 $5 + 1
			${LoopUntil} $5 > $4

			${If} $6 == :
				StrCpy $4 $1 $5 ; type (e.g. REG_DWORD)
				IntOp $5 $5 + 1
				StrCpy $1 $1 "" $5 ; value
			${Else}
				StrCpy $4 REG_SZ
			${EndIf}

			${ParseLocations} $1

			; TODO: I'm not quite certain yet if this is not working, and if it
			; isn't working, whether it's due to not working on a value which
			; already exists, or because it's busy doing something and needs a
			; short Sleep before continuing.

			; If we need to delete it first: 
			;${registry::DeleteValue} $2 $3 $R9 ; path, value, return
			;${DebugMsg} "Deleted value $3 from path $2 (return code $R9)"

			; If we need to sleep:
			;Sleep 50

			${DebugMsg} "Writing '$1' (type '$4') to key '$2', value '$3' (Short form: $2\$3=$4:$1)"
			${registry::Write} $2 $3 $1 $4 $R9 ; path, value, string, type, return
		${NextINIPair}
	${EndIf}
!macroend
