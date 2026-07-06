; TODO: ANSI codepage support
!ifndef MYXML_INCLUDED
	!define MYXML_INCLUDED
	!include XML.nsh
	!include Util.nsh
	!ifndef CP_UTF8
		!define CP_UTF8 65001
	!endif

	!macro MYXML_VARIABLES
		!ifndef MYXML_VARIABLES
			!define MYXML_VARIABLES
			Var /GLOBAL MYXML_BOM
			Var /GLOBAL MYXML_TEMPFILENAME
			InitPluginsDir
		!endif
		${If} $MYXML_TEMPFILENAME == ''
			GetTempFileName $MYXML_TEMPFILENAME $PLUGINSDIR
		${EndIf}
	!macroend

	!verbose push
	!verbose 3
	!ifndef _MYXML_VERBOSE
		!define _MYXML_VERBOSE 3
	!endif
	!verbose ${_MYXML_VERBOSE}
	!define MYXML_VERBOSE `!insertmacro MYXML_VERBOSE`
	!verbose pop

	!macro MYXML_VERBOSE _VERBOSE
		!verbose push
		!verbose 3
		!undef _MYXML_VERBOSE
		!define _MYXML_VERBOSE ${_VERBOSE}
		!verbose pop
	!macroend


	!macro XMLReadAttribCall  _FILE _XPATH _ATTRIB _VAR
		!verbose push
		!verbose ${_MYXML_VERBOSE}
		Push `${_FILE}`
		Push `${_XPATH}`
		Push `${_ATTRIB}`
		${CallArtificialFunction} XMLReadAttrib_
		Pop ${_VAR}
		!verbose pop
	!macroend

	!macro XMLWriteAttribCall _FILE _XPATH _ATTRIB _VAR
		!verbose push
		!verbose ${_MYXML_VERBOSE}
		Push `${_FILE}`
		Push `${_XPATH}`
		Push `${_ATTRIB}`
		Push `${_VAR}`
		${CallArtificialFunction} XMLWriteAttrib_
		!verbose pop
	!macroend

	!macro XMLReadTextCall  _FILE _XPATH _VAR
		!verbose push
		!verbose ${_MYXML_VERBOSE}
		Push `${_FILE}`
		Push `${_XPATH}`
		${CallArtificialFunction} XMLReadText_
		Pop ${_VAR}
		!verbose pop
	!macroend

	!macro XMLWriteTextCall _FILE _XPATH _VAR
		!verbose push
		!verbose ${_MYXML_VERBOSE}
		Push `${_FILE}`
		Push `${_XPATH}`
		Push `${_VAR}`
		${CallArtificialFunction} XMLWriteText_
		!verbose pop
	!macroend


	!define XMLReadAttrib '!insertmacro XMLReadAttribCall'
	!macro XMLReadAttrib_
		!insertmacro MYXML_VARIABLES
		Exch $2 ;_ATTRIB
		Exch
		Exch $1 ;_XPATH
		Exch
		Exch 2
		Exch $0 ;_FILE
		Exch 2
		Push $3
		Push $0
		${CallArtificialFunction2} MyXMLUTF16ToUTF8_
		Pop $0
		${xml::LoadFile} $0 $3
		IntCmp $3 -1 XMLReadAttrib_error
		${xml::RootElement} $4 $3
		IntCmp $3 -1 XMLReadAttrib_error
		${xml::XPathNode} $1 $3
		IntCmp $3 -1 XMLReadAttrib_error
		${xml::GetAttribute} $2 $0 $3
		IntCmp $3 -1 XMLReadAttrib_error
		Goto XMLReadAttrib_End
		XMLReadAttrib_error:
		SetErrors
		StrCpy $0 ''
		XMLReadAttrib_End:
		Pop $3
		Pop $2
		Pop $1
		Exch $0
	!macroend

	!define XMLWriteAttrib '!insertmacro XMLWriteAttribCall'
	!macro XMLWriteAttrib_
		!insertmacro MYXML_VARIABLES
		Exch $3 ;_VAR
		Exch
		Exch $2 ;_ATTRIB
		Exch
		Exch 2
		Exch $1 ;_XPATH
		Exch 2
		Exch 3
		Exch $0 ;_FILE
		Exch 3
		Push $4
		Push $5
		StrCpy $5 $0
		Push $0
		${CallArtificialFunction2} MyXMLUTF16ToUTF8_
		Pop $0
		${xml::LoadFile} $0 $4
		IntCmp $4 -1 XMLWriteAttrib_error
		${xml::RootElement} $4 $4
		IntCmp $4 -1 XMLWriteAttrib_error
		${xml::XPathNode} $1 $4
		IntCmp $4 -1 XMLWriteAttrib_error
		${xml::SetAttribute} $2 $3 $4
		IntCmp $4 -1 XMLWriteAttrib_error
		${xml::SaveFile} $0 $4
		IntCmp $4 -1 XMLWriteAttrib_error
		Push $5
		${CallArtificialFunction2} MyXMLUTF8ToUTF16_
		Goto XMLWriteAttrib_End
		XMLWriteAttrib_error:
		SetErrors
		XMLWriteAttrib_End:
		Pop $5
		Pop $4
		Pop $3
		Pop $2
		Pop $1
		Pop $0
	!macroend

	!define XMLReadText '!insertmacro XMLReadTextCall'
	!macro XMLReadText_
		!insertmacro MYXML_VARIABLES
		Exch $1 ;_XPATH
		Exch
		Exch $0 ;_FILE
		Exch
		Push $2
		Push $0
		${CallArtificialFunction2} MyXMLUTF16ToUTF8_
		Pop $0
		${xml::LoadFile} $0 $2
		IntCmp $2 -1 XMLReadText_error
		${xml::RootElement} $2 $2
		IntCmp $2 -1 XMLReadText_error
		${xml::XPathNode} $1 $2
		IntCmp $2 -1 XMLReadText_error
		${xml::GetText} $0 $2
		IntCmp $2 -1 XMLReadText_error
		Goto XMLReadText_End
		XMLReadText_error:
		SetErrors
		StrCpy $0 ''
		XMLReadText_End:
		Pop $2
		Pop $1
		Exch $0
	!macroend

	!define XMLWriteText '!insertmacro XMLWriteTextCall'
	!macro XMLWriteText_
		!insertmacro MYXML_VARIABLES
		Exch $2 ;_VAR
		Exch
		Exch $1 ;_XPATH
		Exch
		Exch 2
		Exch $0 ;_FILE
		Exch 2
		Push $3
		Push $4
		StrCpy $4 $0
		Push $0
		${CallArtificialFunction2} MyXMLUTF16ToUTF8_
		Pop $0
		${xml::LoadFile} $0 $3
		IntCmp $3 -1 XMLWriteText_error
		${xml::RootElement} $3 $3
		IntCmp $3 -1 XMLWriteText_error
		${xml::XPathNode} $1 $3
		IntCmp $3 -1 XMLWriteText_error
		${xml::SetText} $2 $3
		IntCmp $3 -1 XMLWriteText_error
		${xml::SaveFile} $0 $3
		IntCmp $3 -1 XMLWriteText_error
		Push $4
		${CallArtificialFunction2} MyXMLUTF8ToUTF16_
		Goto XMLWriteText_End
		XMLWriteText_error:
		SetErrors
		XMLWriteText_End:
		Pop $4
		Pop $3
		Pop $2
		Pop $1
		Pop $0
	!macroend

	!macro MyXMLUTF16ToUTF8_
		Exch $0
		Push $1
		Push $2
		Push $3
		Push $4
		Push $5
		FileOpen $1 $0 r
		FileReadWord $1 $5
		FileSeek $1 0 END $2
		FileSeek $1 2 SET
		IntCmp $5 0xFEFF 0 MyXMLUTF16ToUTF8_close
		StrCpy $MYXML_BOM FFFE
		System::Alloc $2
		Pop $3
		IntOp $2 $2 - 2
		System::Call 'kernel32::ReadFile(i r1, i r3, i r2, t.,)'
		FileClose $1
		System::Call 'kernel32::WideCharToMultiByte(i ${CP_UTF8}, i 0, i r3, i -1, i 0, i 0, n, n) i .r5'
		System::Alloc $5
		Pop $4
		System::Call 'kernel32::WideCharToMultiByte(i ${CP_UTF8}, i 0, i r3, i -1, i r4, i r5, n, n)'
		FileOpen $1 $MYXML_TEMPFILENAME w
		StrCpy $0 $MYXML_TEMPFILENAME
		FileWriteWord $1 0xBBEF
		FileWriteByte $1 0xBF
		IntOp $5 $5 - 1
		System::Call 'kernel32::WriteFile(i r1, i r4, i r5, t.,)'
		System::Free $3
		System::Free $4
		MyXMLUTF16ToUTF8_close:
		FileClose $1
		Pop $5
		Pop $4
		Pop $3
		Pop $2
		Pop $1
		Exch $0
	!macroend

	!macro MyXMLUTF8ToUTF16_
		Exch $0
		Push $1
		Push $2
		Push $3
		Push $4
		Push $5
		StrCmpS $MYXML_BOM FFFE 0 MyXMLUTF8ToUTF16_end
		StrCpy $MYXML_BOM 0
		FileOpen $1 $MYXML_TEMPFILENAME r
		FileSeek $1 0 END $2
		IntOp $2 $2 - 3
		FileSeek $1 3 SET
		System::Alloc $2
		Pop $3
		System::Call 'kernel32::ReadFile(i r1, i r3, i r2, t.,)'
		FileClose $1
		System::Call 'kernel32::MultiByteToWideChar(i ${CP_UTF8}, i 0, i r3, i r2, i 0, i 0) i .r5'
		IntOp $5 $5 * 2
		System::Alloc $5
		Pop $4
		System::Call 'kernel32::MultiByteToWideChar(i ${CP_UTF8}, i 0, i r3, i r2, i r4, i r5)'
		FileOpen $1 $0 w
		FileWriteWord $1 0xFEFF
		System::Call 'kernel32::WriteFile(i r1, i r4, i r5, t.,)'
		FileClose $1
		System::Free $3
		System::Free $4
		MyXMLUTF8ToUTF16_end:
		Pop $5
		Pop $4
		Pop $3
		Pop $2
		Pop $1
		Pop $0
	!macroend
!endif ;MYXML_INCLUDED
