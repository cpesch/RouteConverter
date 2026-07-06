!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_Syntax.exe
RequestExecutionLevel user
ShowInstDetails show

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

!macro DoTest JSON Description
  StrCpy $R0 ``
  ClearErrors
  nsJSON::Set /value `${JSON}`
  nsJSON::Serialize
  Pop $R0
  DetailPrint `${Description}:`
  DetailPrint `${JSON} -> $R0`
  IfErrors 0 +2
    DetailPrint `Error flag is set!`
  DetailPrint ``
!macroend

Section

  !insertmacro DoTest `{ "Input": [ { "test1": false, } ] }` `Trailing comma`

  !insertmacro DoTest `{ "Input": [ { "test1": false } .? ] }` `Junk characters`

  !insertmacro DoTest `{ "Input": [ { "test1": false } }` `Missing square bracket`

  !insertmacro DoTest `{ "Input": [ { "test1": false ] }` `Missing curly brace`

SectionEnd