!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_ConsoleExec.exe
RequestExecutionLevel user
ShowInstDetails show

!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE ComponentsPage_Leave
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

Section

  CreateDirectory $EXEDIR\Output

SectionEnd

Section /o `Console App` NATIVE

  InitPluginsDir
  StrCpy $R0 $PLUGINSDIR\ConsoleApp.exe
  File /oname=$R0 Bin\ConsoleApp.exe

  nsJSON::Set /tree ConsoleExec /value `{ "Path": "$R0", "WorkingDir": "$PLUGINSDIR", "RawOutput": true }`

  ; Provide arguments as a single string or an array.
  nsJSON::Set /tree ConsoleExec Arguments /value `[]`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg1"`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg2"`

  ; Provide input as a single string or an array.
  nsJSON::Set /tree ConsoleExec Input /value `"hello"`

  ; Export the tree to file for debugging.
  DetailPrint `Generate: $EXEDIR\Output\ConsoleExecInput.json from ConsoleExec`
  nsJSON::Serialize /tree ConsoleExec /format /file $EXEDIR\Output\ConsoleExecInput.json

  ; Execute the console application.
  DetailPrint `Exec: $R0`
  nsJSON::Set /tree ConsoleExecOutput /exec ConsoleExec

  ; Save the output to file.
  DetailPrint `Generate: $EXEDIR\Output\ConsoleExecOutput.json from ConsoleExecOutput`
  nsJSON::Serialize /tree ConsoleExecOutput /format /file $EXEDIR\Output\ConsoleExecOutput.json

SectionEnd

Section /o `Async Console App` NATIVEASYNC

  InitPluginsDir
  StrCpy $R0 $PLUGINSDIR\ConsoleApp.exe
  File /oname=$R0 Bin\ConsoleApp.exe

  nsJSON::Set /tree ConsoleExec /value `{ "Async": true, "Path": "$R0", "WorkingDir": "$PLUGINSDIR", "RawOutput": true }`

  ; Provide arguments as a single string or an array.
  nsJSON::Set /tree ConsoleExec Arguments /value `[]`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg1"`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg2"`

  ; Provide input as a single string or an array.
  nsJSON::Set /tree ConsoleExec Input /value `"hello"`

  ; Export the tree to file for debugging.
  DetailPrint `Generate: $EXEDIR\Output\AsyncConsoleExecInput.json from ConsoleExec`
  nsJSON::Serialize /tree ConsoleExec /format /file $EXEDIR\Output\AsyncConsoleExecInput.json

  ; Execute the console application.
  DetailPrint `Exec: $R0`
  nsJSON::Set /tree AsyncConsoleExecOutput /exec ConsoleExec

  ; Wait until done.
  ${Do}
    Sleep 1000
    nsJSON::Wait ConsoleExec /timeout 0
    Pop $R0
    ${If} $R0 != wait
      ${Break}
    ${EndIf}
    DetailPrint `Waiting...`
  ${Loop}

  DetailPrint `Finished...`

  ; Save the output to file.
  DetailPrint `Generate: $EXEDIR\Output\AsyncConsoleExecOutput.json from AsyncConsoleExecOutput`
  nsJSON::Serialize /tree AsyncConsoleExecOutput /format /file $EXEDIR\Output\AsyncConsoleExecOutput.json

SectionEnd

Section /o `.NET Console App` DOTNET

  InitPluginsDir
  StrCpy $R0 $PLUGINSDIR\ConsoleAppDotNet.exe
  File /oname=$R0 Bin\ConsoleAppDotNet.exe

  nsJSON::Set /tree ConsoleExec /value `{ "Path": "$R0", "WorkingDir": "$PLUGINSDIR" }`

  ; Provide arguments as a single string or an array.
  nsJSON::Set /tree ConsoleExec Arguments /value `[]`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg1"`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg2"`

  ; Provide input as a single string or an array.
  nsJSON::Set /tree ConsoleExec Input /value `"hello"`

  ; Export the tree to file for debugging.
  DetailPrint `Generate: $EXEDIR\Output\ConsoleExecDotNetInput.json from ConsoleExec`
  nsJSON::Serialize /tree ConsoleExec /format /file $EXEDIR\Output\ConsoleExecDotNetInput.json

  ; Execute the console application.
  DetailPrint `Exec: $R0`
  nsJSON::Set /tree ConsoleExecOutput /exec ConsoleExec

  ; Save the output to file.
  DetailPrint `Generate: $EXEDIR\Output\ConsoleExecDotNetOutput.json from ConsoleExecOutput`
  nsJSON::Serialize /tree ConsoleExecOutput /format /file $EXEDIR\Output\ConsoleExecDotNetOutput.json

SectionEnd

Section /o `UI Thread Console App` UINATIVE

SectionEnd

Function ComponentsPage_Leave

  ${IfNot} ${SectionIsSelected} ${UINATIVE}
    Return
  ${EndIf}

  GetDlgItem $R0 $HWNDPARENT 1
  EnableWindow $R0 0
  GetDlgItem $R0 $HWNDPARENT 2
  EnableWindow $R0 0
  GetDlgItem $R0 $HWNDPARENT 3
  EnableWindow $R0 0
  FindWindow $R0 `#32770` `` $HWNDPARENT
  GetDlgItem $R0 $R0 1032
  EnableWindow $R0 0

  Banner::Show /set 76 `Executing ConsoleApp.exe` `Please wait...`

  InitPluginsDir
  StrCpy $R0 $PLUGINSDIR\ConsoleApp.exe
  File /oname=$R0 Bin\ConsoleApp.exe

  nsJSON::Set /tree ConsoleExec /value `{ "UIAsync": true, "Path": "$R0", "WorkingDir": "$PLUGINSDIR", "RawOutput": true }`

  ; Provide arguments as a single string or an array.
  nsJSON::Set /tree ConsoleExec Arguments /value `[]`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg1"`
  nsJSON::Set /tree ConsoleExec Arguments /value `"arg2"`

  ; Provide input as a single string or an array.
  nsJSON::Set /tree ConsoleExec Input /value `"hello"`

  ; Export the tree to file for debugging.
  nsJSON::Serialize /tree ConsoleExec /format /file $EXEDIR\Output\UIConsoleExecInput.json

  ; Execute the console application.
  nsJSON::Set /tree UIConsoleExecOutput /exec ConsoleExec

  ; Save the output to file.
  nsJSON::Serialize /tree UIConsoleExecOutput /format /file $EXEDIR\Output\UIConsoleExecOutput.json

  Banner::Destroy

  GetDlgItem $R0 $HWNDPARENT 1
  EnableWindow $R0 1
  GetDlgItem $R0 $HWNDPARENT 2
  EnableWindow $R0 1
  GetDlgItem $R0 $HWNDPARENT 3
  EnableWindow $R0 1
  FindWindow $R0 `#32770` `` $HWNDPARENT
  GetDlgItem $R0 $R0 1032
  EnableWindow $R0 1

FunctionEnd

LangString NATIVEDesc ${LANG_ENGLISH} `Executes native console app with STDIN data and reads JSON from STDOUT`
LangString NATIVEASYNCDesc ${LANG_ENGLISH} `Executes native console app asynchronously with STDIN data and reads JSON from STDOUT`
LangString DOTNETDesc ${LANG_ENGLISH} `Executes .NET console app with STDIN data and reads JSON from STDOUT`
LangString UINATIVEDesc ${LANG_ENGLISH} `Executes native console app asynchronously while processing window messages`

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${NATIVE} $(NATIVEDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${NATIVEASYNC} $(NATIVEASYNCDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${DOTNET} $(DOTNETDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${UINATIVE} $(UINATIVEDesc)
!insertmacro MUI_FUNCTION_DESCRIPTION_END