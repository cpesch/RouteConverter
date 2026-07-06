${SegmentFile}

${SegmentPrePrimary}
	${If} $UsesRegistry == true
		${ForEachINIPair} RegistryKeys $0 $1
			;=== Backup the registry
			${ValidateRegistryKey} $1
			${IfNot} ${RegistryKeyExists} HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1
			${AndIf} ${RegistryKeyExists} $1
				${DebugMsg} "Backing up registry key $1 to HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1"
				${registry::MoveKey} $1 HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1 $R9
				${If} $R9 == -1 ; failure (probably HKLM without admin)
					${WriteRuntimeData} FailedRegistryKeys $0 true
				${EndIf}
			${EndIf}

			${If} $0 == -
				${DebugMsg} "File name -, not data to import."
			${ElseIf} ${FileExists} $DataDirectory\settings\$0.reg
				${DebugMsg} "Loading $DataDirectory\settings\$0.reg into the registry."
				${registry::RestoreKey} $DataDirectory\settings\$0.reg $R9
				${If} $R9 != 0 ; -1 = failure (probably HKLM without admin), 0 = success
					${WriteRuntimeData} FailedRegistryKeys $0 true
					${!getdebug}
					!ifdef DEBUG
						StrCpy $R9 $1 4
						${If} $R9 == HKLM
						${AndIf} $RunAsAdmin != force
							StrCpy $R9 " Note for developers: to always be able to write to HKLM, you will need to set [Launch]:RunAsAdmin to force."
						${Else}
							StrCpy $R9 ""
						${EndIf}
					!endif
					${DebugMsg} "Failed to load $DataDirectory\settings\$0.reg into the registry.$R9"
				${EndIf}
			${Else}
				${DebugMsg} "File $DataDirectory\settings\$0.reg doesn't exist, not loaded into the registry."
			${EndIf}
		${NextINIPair}
	${EndIf}
!macroend

${SegmentPostPrimary}
	${If} $UsesRegistry == true
		${ForEachINIPair} RegistryKeys $0 $1
			${ValidateRegistryKey} $1
			${If} $0 == -
				${DebugMsg} "Registry key $1 will not be saved."
			${Else}
				ClearErrors
				${ReadRuntimeData} $R9 FailedRegistryKeys $0
				${If} ${Errors} ; didn't fail
				${AndIf} $RunLocally != true
					${DebugMsg} "Saving registry key $1 to $DataDirectory\settings\$0.reg."
					${registry::SaveKey} $1 $DataDirectory\settings\$0.reg "" $R9
				${EndIf}
			${EndIf}

			${DebugMsg} "Deleting registry key $1."
			${registry::DeleteKey} $1 $R9
			${If} ${RegistryKeyExists} HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1
				${DebugMsg} "Moving registry key HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1 to $1."
				${registry::MoveKey} HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1 $1 $R9
				${Do}
					${GetParent} $1 $1
					${registry::DeleteKeyEmpty} HKEY_CURRENT_USER\Software\PortableApps.com\Keys\$1 $R9
				${LoopUntil} $1 == ""
			${EndIf}
		${NextINIPair}
		${registry::DeleteKeyEmpty} HKEY_CURRENT_USER\Software\PortableApps.com $R9
	${EndIf}
!macroend
