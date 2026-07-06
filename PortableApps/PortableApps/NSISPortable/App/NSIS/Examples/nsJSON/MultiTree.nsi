!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_MultiTree.exe
RequestExecutionLevel user
ShowInstDetails show

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

Section

  CreateDirectory $EXEDIR\Output

  DetailPrint `Read: $EXEDIR\Input\Example1.json into Example1`
  nsJSON::Set /tree Example1 /file $EXEDIR\Input\Example1.json
  DetailPrint `Read: $EXEDIR\Input\Example2.json into Example2`
  nsJSON::Set /tree Example2 /file $EXEDIR\Input\Example2.json

  DetailPrint `Generate: $EXEDIR\Output\Example2.json from Example2`
  nsJSON::Serialize /tree Example2 /format /file $EXEDIR\Output\Example2.json
  DetailPrint `Generate: $EXEDIR\Output\Example1.json from Example1`
  nsJSON::Serialize /tree Example1 /format /file $EXEDIR\Output\Example1.json

SectionEnd