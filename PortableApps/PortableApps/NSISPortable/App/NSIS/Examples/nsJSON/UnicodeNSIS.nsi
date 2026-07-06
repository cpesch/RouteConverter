!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_UnicodeNSIS.exe
RequestExecutionLevel user
ShowInstDetails show
Unicode true

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

Section

	nsJSON::Set /tree testTree testNode /value testValue

	nsJSON::Get /tree testTree testNode /end
	Pop $R0
	DetailPrint $R0

	nsJSON::Serialize /tree testTree
	Pop $R0
	DetailPrint $R0

SectionEnd