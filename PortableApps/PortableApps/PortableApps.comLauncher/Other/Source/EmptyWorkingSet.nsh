; Moves unused physical memory to virtual memory
!macro EmptyWorkingSet
	System::Call "kernel32::GetCurrentProcess()i.s"
	System::Call "psapi::EmptyWorkingSet(is)"
!macroend
!define EmptyWorkingSet "!insertmacro EmptyWorkingSet"
