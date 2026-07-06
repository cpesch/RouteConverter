!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_Sort.exe
RequestExecutionLevel user
ShowInstDetails show

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

!macro nJSON_Sort_Test Options
  nsJSON::Sort /options ${Options} /end
  nsJSON::Get /end
  Pop $R0
  DetailPrint $R0
!macroend
!define nJSON_Sort_Test `!insertmacro nJSON_Sort_Test`

Section

  nsJSON::Set /value `{ "D": "X", "b": 3, "a": 22, "c" : 2, "d": "x", "y": { "f": 33, "a": 9, "n": [ 1, 5, -10, 11, "m" ] } }` /end

  DetailPrint `Sorted root node values only`
  ${nJSON_Sort_Test} 0

  DetailPrint `Sorted root node values only numerically`
  ${nJSON_Sort_Test} 2

  DetailPrint `Sorted root node by keys only`
  ${nJSON_Sort_Test} 8

  DetailPrint `Sorted values numerically + recursively`
  ${nJSON_Sort_Test} 18

SectionEnd