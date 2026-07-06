${SegmentFile}

; For usage of XML features, see FileWrite.nsh and Language.nsh.

!ifdef XML_ENABLED
	!include MyXML.nsh
!endif

; Used in combination with XML_ENABLED in FileWrite.nsh and Language.nsh to
; warn the user if they try to use XML features without enabling it.
!macro XML_WarnNotActivated Section
	MessageBox MB_OK|MB_ICONSTOP "To use XML features of the Launcher you must set [Activate]:XML=true and then regenerate the launcher. Continuing, but ${Section} will not be used."
!macroend

${SegmentUnload}
!ifdef XML_ENABLED
	${xml::Unload}
!endif
!macroend
