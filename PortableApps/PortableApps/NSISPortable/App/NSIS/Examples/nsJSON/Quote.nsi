!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_Quote.exe
RequestExecutionLevel user
ShowInstDetails show

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

!macro nJSON_Quote_Test Unicode Always Input
  !if `${Always}` == true
    nsJSON::Quote /always `${Input}`
  !else if `${Unicode}` == true
    nsJSON::Quote /unicode `${Input}`
  !else
    nsJSON::Quote `${Input}`
  !endif
  Pop $R0
  !if `${Always}` == true
    DetailPrint `${Input} -> $R0 (/always)`
  !else if `${Unicode}` == true
    DetailPrint `${Input} -> $R0 (/unicode)`
  !else
    DetailPrint `${Input} -> $R0`
  !endif
!macroend
!define nJSON_Quote_Test `!insertmacro nJSON_Quote_Test false false`
!define nJSON_Quote_Test_Unicode `!insertmacro nJSON_Quote_Test true false`
!define nJSON_Quote_Test_Always `!insertmacro nJSON_Quote_Test false true`

Section

  ${nJSON_Quote_Test} `"`
  ${nJSON_Quote_Test} `\`
  ${nJSON_Quote_Test} `£`
  ${nJSON_Quote_Test} `¡`
  ${nJSON_Quote_Test} `"¡"`
  ${nJSON_Quote_Test} `"some"text"`
  ${nJSON_Quote_Test_Always} `"some"text"`
  ${nJSON_Quote_Test_Unicode} `£`
  ${nJSON_Quote_Test_Unicode} `¡`
  ${nJSON_Quote_Test_Unicode} `"¡"`

SectionEnd