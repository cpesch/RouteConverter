${SegmentFile}

${SegmentPrePrimary}
	StrCpy $R0 0
	${Do}
		; This time we ++ at the start so we can use Continue
		IntOp $R0 $R0 + 1
		ClearErrors
		${ReadLauncherConfig} $0 FileWrite$R0 Type
		${ReadLauncherConfig} $7 FileWrite$R0 File
		${IfThen} ${Errors} ${|} ${ExitDo} ${|}
		${ParseLocations} $7

		; Read the remaining items from the config
		${If} $0 == ConfigWrite
			${ReadLauncherConfig} $2 FileWrite$R0 Entry
			${ReadLauncherConfig} $3 FileWrite$R0 Value
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $3
			ClearErrors
			${ReadLauncherConfig} $4 FileWrite$R0 CaseSensitive
			${If} $4 != true
			${AndIf} $4 != false
			${AndIfNot} ${Errors}
				${InvalidValueError} [FileWrite$R0]:CaseSensitive $4
				${Continue}
			${EndIf}
		${ElseIf} $0 == INI
			${ReadLauncherConfig} $2 FileWrite$R0 Section
			${ReadLauncherConfig} $3 FileWrite$R0 Key
			${ReadLauncherConfig} $4 FileWrite$R0 Value
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $4
!ifdef XML_ENABLED
		${ElseIf} $0 == "XML attribute"
			${ReadLauncherConfig} $2 FileWrite$R0 XPath
			${ReadLauncherConfig} $3 FileWrite$R0 Attribute
			${ReadLauncherConfig} $4 FileWrite$R0 Value
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $4
		${ElseIf} $0 == "XML text"
			${ReadLauncherConfig} $2 FileWrite$R0 XPath
			${ReadLauncherConfig} $3 FileWrite$R0 Value
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $3
!else
		${ElseIf} $0 == "XML attribute"
		${OrIf} $0 == "XML text"
			!insertmacro XML_WarnNotActivated [FileWrite$R0]
			${Continue}
!endif
		${ElseIf} $0 == Replace
			${ReadLauncherConfig} $2 FileWrite$R0 Find
			${ReadLauncherConfig} $3 FileWrite$R0 Replace
			${IfThen} ${Errors} ${|} ${ExitDo} ${|}
			${ParseLocations} $2
			${ParseLocations} $3

			ClearErrors
			${ReadLauncherConfig} $4 FileWrite$R0 CaseSensitive

			StrCpy $5 skip ; $5 = "Do we need to replace?"
			${If} $4 == true   ; case sensitive
				${If} $2 S!= $3 ; find != replace?
					StrCpy $5 replace
				${EndIf}
			${Else} ; case insensitive
				${If} $4 != false     ; "false" is valid
				${AndIfNot} ${Errors} ; not set is valid
					${InvalidValueError} [FileWrite$R0]:CaseSensitive $4
					; default to case insensitive and continue on
				${EndIf}
				${If} $2 != $3 ; find != replace?
					StrCpy $5 replace
				${EndIf}
			${EndIf}
			${If} $5 == skip
				${Continue}
			${EndIf}
			; With Replace we actually leave Encoding calculation till later.
			; Generally this will be more efficient as it's probably auto.
		${Else}
			${InvalidValueError} [FileWrite$R0]:Type $0
			${Continue}
		${EndIf}

		; Now actually do it, for each file match.
		; We have all the info and everything is valid.
		${ForEachFile} $1 $R4 $7
			${If} $0 == ConfigWrite
				${If} $4 == true
					${DebugMsg} "Writing configuration to a file with ConfigWriteS.$\r$\nFile: $1$\r$\nEntry: `$2`$\r$\nValue: `$3`"
					${ConfigWriteS} $1 $2 $3 $R9
				${Else} ; false or empty
					${DebugMsg} "Writing configuration to a file with ConfigWrite.$\r$\nFile: $1$\r$\nEntry: `$2`$\r$\nValue: `$3`"
					${ConfigWrite} $1 $2 $3 $R9
				${EndIf}
			${ElseIf} $0 == INI
				${DebugMsg} "Writing INI configuration to a file.$\r$\nFile: $1$\r$\nSection: `$2`$\r$\nKey: `$3`$\r$\nValue: `$4`"
				WriteINIStr $1 $2 $3 $4
!ifdef XML_ENABLED
			${ElseIf} $0 == "XML attribute"
				${DebugMsg} "Writing configuration to a file with XMLWriteAttrib.$\r$\nFile: $1$\r$\nXPath: `$2`$\r$\nAttrib: `$3`$\r$\nValue: `$4`"
				${XMLWriteAttrib} $1 $2 $3 $4
;				${IfThen} ${Errors} ${|} ${DebugMsg} "XMLWriteAttrib XPath error" ${|}
			${ElseIf} $0 == "XML text"
				${ParseLocations} $3
				${DebugMsg} "Writing configuration to a file with XMLWriteText.$\r$\nFile: $1$\r$\nXPath: `$2`$\r$\n$\r$\nValue: `$3`"
				${XMLWriteText} $1 $2 $3
;				${IfThen} ${Errors} ${|} ${DebugMsg} "XMLWriteText XPath error" ${|}
!endif
			${ElseIf} $0 == Replace
				ClearErrors
				${ReadLauncherConfig} $5 FileWrite$R0 Encoding
				${If} ${Errors}
					FileOpen $9 $1 r

					; Using FileReadWord would end up with 0xFEFF as it
					; flips everything back to front like a good little
					; endian parser. (Lilliput and Blefuscu really did
					; cause a lot of trouble!)

					FileReadByte $9 $5
					FileReadByte $9 $6
					IntOp $5 $5 << 8
					IntOp $5 $5 + $6

					${IfThen} $5 = 0xFFFE ${|} StrCpy $5 UTF-16LE ${|}
					FileClose $9
				${ElseIf} $5 != UTF-16LE
				${AndIf} $5 != ANSI
					${InvalidValueError} [FileWrite$R0]:Encoding $5
				${EndIf}
${!getdebug}
!ifdef DEBUG
				${IfThen} $5 == UTF-16LE ${|} StrCpy $R8 "a UTF-16LE" ${|}
				${IfThen} $5 != UTF-16LE ${|} StrCpy $R8 "an ANSI" ${|}
				StrCpy $R9 ``
				${IfThen} $4 != true ${|} StrCpy $R9 in ${|}
				${DebugMsg} "Finding and replacing in $R8 file (case $R9sensitive).$\r$\nFile: $1$\r$\nFind: `$2`$\r$\nReplace: `$3`"
!endif
				${If} $5 == UTF-16LE
					${If} $4 == true
						${ReplaceInFileUTF16LECS} $1 $2 $3
					${Else}
						${ReplaceInFileUTF16LE} $1 $2 $3
					${EndIf}
				${Else}
					${If} $4 == true
						${ReplaceInFileCS} $1 $2 $3
					${Else}
						${ReplaceInFile} $1 $2 $3
					${EndIf}
				${EndIf}
			${EndIf}
		${NextFile}
		;${If} ${Errors}
		;${AndIf} $0 == Replace
			;${DebugMsg} File didn't exist
		;${EndIf}
	${Loop}
!macroend
