!include MUI2.nsh

Name `nsJSON plug-in`
OutFile nsJSON_Example.exe
RequestExecutionLevel user
ShowInstDetails show

!define MUI_COMPONENTSPAGE_SMALLDESC
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

Section

  CreateDirectory $EXEDIR\Output

SectionEnd

Section /o `Parse Example1.json` EXAMPLE1

  ; Input: Example1.json; output: Example1.json.
  nsJSON::Set /file $EXEDIR\Input\Example1.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example1.json
  DetailPrint `Generate: $EXEDIR\Output\Example1.json`

SectionEnd

Section /o `Convert Example1_Unicode.json (no BOM)` EXAMPLE1B

  ; Use the /unicode switch if the input file is Unicode.
  nsJSON::Set /file /unicode $EXEDIR\Input\Example1_Unicode.json

  ; Generate an ANSII output file.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example1B_ASCII.json
  DetailPrint `Generate: $EXEDIR\Output\Example1B_ASCII.json`

  ; Generate a Unicode output file.
  nsJSON::Serialize /format /file /unicode $EXEDIR\Output\Example1B_Unicode.json
  DetailPrint `Generate: $EXEDIR\Output\Example1B_Unicode.json`

SectionEnd

Section /o `Convert Example1_Unicode_UTF16BE.json (BOM)` EXAMPLE1C

  ; Use the /unicode switch if the input file is Unicode.
  nsJSON::Set /file /unicode $EXEDIR\Input\Example1_Unicode_UTF16BE.json

  ; Generate an ANSII output file.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example1C_ASCII.json
  DetailPrint `Generate: $EXEDIR\Output\Example1C_ASCII.json`

  ; Generate a Unicode output file.
  nsJSON::Serialize /format /file /unicode $EXEDIR\Output\Example1C_Unicode.json
  DetailPrint `Generate: $EXEDIR\Output\Example1C_Unicode.json`

SectionEnd

Section /o `Convert Example1_Unicode_UTF16LE.json (BOM)` EXAMPLE1D

  ; Use the /unicode switch if the input file is Unicode.
  nsJSON::Set /file /unicode $EXEDIR\Input\Example1_Unicode_UTF16LE.json

  ; Generate an ANSII output file.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example1D_ASCII.json
  DetailPrint `Generate: $EXEDIR\Output\Example1D_ASCII.json`

  ; Generate a Unicode output file.
  nsJSON::Serialize /format /file /unicode $EXEDIR\Output\Example1D_Unicode.json
  DetailPrint `Generate: $EXEDIR\Output\Example1D_Unicode.json`

SectionEnd

Section /o `Convert Example1_Unicode_UTF8.json (sig)` EXAMPLE1E

  ; No /unicode switch is used for UTF8.
  nsJSON::Set /file $EXEDIR\Input\Example1_Unicode_UTF8.json

  ; Generate an ANSII output file.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example1_ASCII.json
  DetailPrint `Generate: $EXEDIR\Output\Example1_ASCII.json`

  ; Generate a Unicode output file.
  nsJSON::Serialize /format /file /unicode $EXEDIR\Output\Example1_Unicode.json
  DetailPrint `Generate: $EXEDIR\Output\Example1_Unicode.json`

SectionEnd

Section /o `Parse Example2.json` EXAMPLE2

  ; Input: Example2.json; output: Example2.json.
  nsJSON::Set /file $EXEDIR\Input\Example2.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example2.json
  DetailPrint `Generate: $EXEDIR\Output\Example2.json`

SectionEnd

Section /o `Parse Example3.json` EXAMPLE3

  ; Input: Example3.json; output: Example3.json.
  nsJSON::Set /file $EXEDIR\Input\Example3.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example3.json
  DetailPrint `Generate: $EXEDIR\Output\Example3.json`

SectionEnd

Section /o `Parse Example4.json` EXAMPLE4

  ; Input: Example4.json; output: Example4.json.
  nsJSON::Set /file $EXEDIR\Input\Example4.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example4.json
  DetailPrint `Generate: $EXEDIR\Output\Example4.json`

SectionEnd

Section /o `Parse Example5.json` EXAMPLE5

  ; Input: Example5.json; output: Example5.json.
  nsJSON::Set /file $EXEDIR\Input\Example5.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example5.json
  DetailPrint `Generate: $EXEDIR\Output\Example5.json`

SectionEnd

Section /o `Parse Example5.json (via $$R0)` EXAMPLE5B

  ; Input: Example5.json; output: Example5.json.
  nsJSON::Set /file $EXEDIR\Input\Example5.json
  nsJSON::Serialize /format
  Pop $R0
  FileOpen $R1 $EXEDIR\Output\Example5B.json w
  FileWrite $R1 $R0
  FileClose $R1
  DetailPrint `Generate: $EXEDIR\Output\Example5B.json`

SectionEnd

Section /o `Generate Example6.json` EXAMPLE6

  nsJSON::Set /value `{}`
  nsJSON::Set `html` `head` `title` /value `"Example6"`

  ; Build an array using individual calls.
  nsJSON::Set `html` `body` `h1` /value `[]`
  nsJSON::Set `html` `body` `h1` /value `"Hello,\tmy name is"`
  nsJSON::Set `html` `body` `h1` `i` /value `"Stuart."`
  nsJSON::Set `html` `body` `h1` /value `"Howdy"`
  nsJSON::Set `html` `body` `h1` /value `"!"`
  nsJSON::Set `html` `body` `h1` /value `[ true, "hello", false ]`

  ; Build an array using a JSON string.
  nsJSON::Set `html` `body` `h2` /value `[ "I like", { "u": { "i" : "programming" } }, "very much!" ]`

  ; Quotes in node keys are allowed; they are escaped automatically.
  nsJSON::Set `html` `body` `a href="http://www.afrowsoft.co.uk"` /value `"My website!"`

  ; Open the file below in Notepad.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example6.json
  DetailPrint `Generate: $EXEDIR\Output\Example6.json`

SectionEnd

Section /o `Reading from Example1.json` EXAMPLE7

  nsJSON::Set /file $EXEDIR\Input\Example1.json

  ; Read quoted string value.
  ClearErrors
  nsJSON::Get `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossTerm` /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossTerm = $R0`
  ${EndIf}

  ; Read quoted string value with escaping.
  ClearErrors
  nsJSON::Set `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `para2` /value `"A meta-markup language, used to create markup languages\r\nsuch as DocBook."`
  nsJSON::Get `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `para2` /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->para2 = $R0`
  ${EndIf}

  ; Read quoted string value without expanding escape sequences.
  ClearErrors
  nsJSON::Set `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `para3` /value `"A meta-markup language, used to create markup languages\r\nsuch as DocBook."`
  nsJSON::Get /noexpand `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `para3` /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->para3 = $R0`
  ${EndIf}

  ; Read the value of an array (returns a comma delimited list).
  ClearErrors
  nsJSON::Get `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `GlossSeeAlso` /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->GlossSeeAlso = $R0`
  ${EndIf}

  ; Try reading a node that does not exist.
  ClearErrors
  nsJSON::Get `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `GlossSeeAlso2` /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->GlossSeeAlso2 = $R0`
  ${Else}
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->GlossSeeAlso2 = Does not exist!`
  ${EndIf}

  ; Try reading an array element that does not exist.
  ClearErrors
  nsJSON::Get `glossary` `GlossDiv` `GlossList` `GlossEntry` `GlossDef` `GlossSeeAlso` 99 /end
  ${IfNot} ${Errors}
    Pop $R0
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->GlossSeeAlso->#99 = $R0`
  ${Else}
    DetailPrint `glossary->GlossDiv->GlossList->GlossEntry->GlossDef->GlossSeeAlso->#99 = Does not exist!`
  ${EndIf}

SectionEnd

Section /o `Arrays test` EXAMPLE8

  nsJSON::Set /value `{}`

  ; You can add an array this way.
  nsJSON::Set `array1` /value `[ "value 1", "value 2", { "value 3": "node value" }, "value 4", 5 ]`

  ; Inspect array1.
  nsJSON::Get `array1` /end
  Pop $R0
  DetailPrint `array1 = [$R0]`

  ; Or you can build it this way.
  nsJSON::Set `array2` /value `[]`
  nsJSON::Set `array2` /value `"value 1"`
  nsJSON::Set `array2` /value `"value 2"`
  nsJSON::Set `array2` `value 3` /value `"node value"`
  nsJSON::Set `array2` /value `"value 4"`
  nsJSON::Set `array2` /value `5`

  ; You cannot add the same value again.
  nsJSON::Set `array2` /value `5`

  ; More.
  nsJSON::Set `array2` /value `blah1`
  nsJSON::Set `array2` /value `blah2`

  ; Inspect array2.
  nsJSON::Get `array2` /end
  Pop $R0
  DetailPrint `array2 = [$R0]`

  ; Does an array element exist at the given index?
  nsJSON::Get /exists `array2` /index 0 /end
  Pop $R0
  DetailPrint `array2[0] exists? = $R0`
  ${If} $R0 == yes
    nsJSON::Get `array2` /index 0 /end
    Pop $R0
    DetailPrint `array2[0] = $R0`
  ${EndIf}

  ; Does an array element exist at the given index?
  nsJSON::Get /exists `array2` /index -2 /end
  Pop $R0
  DetailPrint `array2[-2] exists? = $R0`
  ${If} $R0 == yes
    nsJSON::Get `array2` /index -2 /end
    Pop $R0
    DetailPrint `array2[-2] = $R0`
  ${EndIf}

  ; Does an array element exist that matches the value?
  nsJSON::Get /exists `array2` `5` /end
  Pop $R0
  DetailPrint `array2->5 exists? = $R0`

  ; Does an array element exist at the given index?
  nsJSON::Get /exists `array2` /index 6 /end
  Pop $R0
  DetailPrint `array2[6] exists? = $R0`

  ; Open Example8_1.json to see what it now looks like.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example8_1.json
  DetailPrint `Generate: $EXEDIR\Output\Example8_1.json`

  ; Now delete the element at the given index.
  nsJSON::Delete `array1` /index 2 `value 3` /end
  DetailPrint `Delete: array1[2]->value 3`
  nsJSON::Delete `array2` /index 5 /end
  DetailPrint `Delete: array2[5]`

  ; Now delete the elements with the given values.
  nsJSON::Delete `array1` `value 1` /end
  DetailPrint `Delete: array1->value 1`
  nsJSON::Delete `array2` `value 2` /end
  DetailPrint `Delete: array2->value 2`

  ; Inspect array1.
  nsJSON::Get `array1` /end
  Pop $R0
  DetailPrint `array1 = [$R0]`

  ; Inspect array2.
  nsJSON::Get `array2` /end
  Pop $R0
  DetailPrint `array2 = [$R0]`

  ; Open Example8_2.json to see what it now looks like.
  nsJSON::Serialize /format /file $EXEDIR\Output\Example8_2.json
  DetailPrint `Generate: $EXEDIR\Output\Example8_2.json`

SectionEnd

Section /o `Node iteration test` EXAMPLE9

  nsJSON::Set /file $EXEDIR\Input\Example4.json

  ; Get the node count.
  nsJSON::Get /count `web-app` `servlet` /index 0 `init-param` /end
  Pop $R0

  DetailPrint `Node web-app->servlet[0]->init-param contains $R0 children:`
  ${For} $R1 0 $R0

    nsJSON::Get /key `web-app` `servlet` /index 0 `init-param` /index $R1 /end
    Pop $R2
    nsJSON::Get `web-app` `servlet` /index 0 `init-param` /index $R1 /end
    Pop $R3
    nsJSON::Get /type `web-app` `servlet` /index 0 `init-param` /index $R1 /end
    Pop $R4
    DetailPrint `$R2 = $R3 (type: $R4)`

  ${Next}

SectionEnd

Section /o `Load Example5.json into Example4.json` EXAMPLE10

  ; Input: Example5.json; output: Example5.json.
  nsJSON::Set /file $EXEDIR\Input\Example5.json
  nsJSON::Set `menu` `example4` /file $EXEDIR\Input\Example4.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Example10.json
  DetailPrint `Generate: $EXEDIR\Output\Example10.json`

SectionEnd

Section /o `Copies Preferences.json into PreferencesNew.json` EXAMPLE11

  ; Input: Preferences.json; output: PreferencesNew.json.
  nsJSON::Set /file $EXEDIR\Input\Preferences.json
  nsJSON::Serialize /format /file $EXEDIR\Output\PreferencesNew.json
  DetailPrint `Generate: $EXEDIR\Output\PreferencesNew.json`

SectionEnd

Section /o `Copies Preferences2.json into Preferences2New.json` EXAMPLE12

  ; Input: Preferences2.json; output: Preferences2New.json.
  nsJSON::Set /file $EXEDIR\Input\Preferences2.json
  nsJSON::Serialize /format /file $EXEDIR\Output\Preferences2New.json
  DetailPrint `Generate: $EXEDIR\Output\Preferences2New.json`

SectionEnd

Section /o `Buffer overflow` EXAMPLE13

  ; Input: Example2.json.
  nsJSON::Set /file $EXEDIR\Input\Example2.json
  nsJSON::Serialize /format
  Pop $R0
  StrLen $R1 $R0
  DetailPrint `Output length: $R1`
  MessageBox MB_OK $R0

SectionEnd

Section /o `Get tests` EXAMPLE14

  nsJSON::Set /value `{ "a": "a", "b": 1, "c": {} }`
  nsJSON::Serialize /format
  Pop $R0
  DetailPrint `Test: $R0`

  nsJSON::Get /key /index 0 /end
  Pop $R0
  DetailPrint `Key at index 0: $R0`

  nsJSON::Get /keys /end
  Pop $R0
  DetailPrint `Total keys: $R0`
  StrCpy $R1 0
  ${DoWhile} $R0 > 0
    Pop $R2
    DetailPrint `Key at index $R1: $R2`
    IntOp $R0 $R0 - 1
    IntOp $R1 $R1 + 1
  ${Loop}

  nsJSON::Get /type /index 0 /end
  Pop $R0
  DetailPrint `Type at index 0: $R0`

  nsJSON::Get /exists /index 0 /end
  Pop $R0
  DetailPrint `Index 0 exists?: $R0`

  nsJSON::Get /count /end
  Pop $R0
  DetailPrint `Count: $R0`

  nsJSON::Get /isempty /end
  Pop $R0
  DetailPrint `Is empty?: $R0`

  nsJSON::Get /isempty /index 2 /end
  Pop $R0
  DetailPrint `Is empty at index 2?: $R0`

  ClearErrors
  nsJSON::Get /isempty /index 99 /end
  IfErrors 0 +2
    DetailPrint `Is empty at index 99?: Error flag is set`

SectionEnd

Section /o `Delete tests` EXAMPLE15

  nsJSON::Set /value `{ "a": "a", "b": 1, "c": {} }`
  nsJSON::Serialize /format
  Pop $R0
  DetailPrint `Test: $R0`

  nsJSON::Delete a /end
  Pop $R0
  DetailPrint `Delete key "a": $R0`

  nsJSON::Serialize /format
  Pop $R0
  DetailPrint `Now: $R0`

  ClearErrors
  nsJSON::Delete /index 99 /end
  IfErrors 0 +2
    DetailPrint `Delete at index 99: Error flag is set`

SectionEnd

Section /o `Empty keys` EXAMPLE16

  nsJSON::Set /value `{ "": "a", 1, "c": {} }`
  nsJSON::Serialize /format
  Pop $R0
  DetailPrint `Test: $R0`

  nsJSON::Set a "" /value `"abc"`
  nsJSON::Serialize /format
  Pop $R0
  DetailPrint `Test: $R0`

SectionEnd

LangString Example1Desc ${LANG_ENGLISH} `Parses Example1.json and then generates Output\Example1.json`
LangString Example1BDesc ${LANG_ENGLISH} `Parses Example1_Unicode.json (no BOM) and then generates Unicode and ASCII copies`
LangString Example1CDesc ${LANG_ENGLISH} `Parses Example1_Unicode_UTF16BE.json (BOM) and then generates Unicode and ASCII copies`
LangString Example1DDesc ${LANG_ENGLISH} `Parses Example1_Unicode_UTF16LE.json (BOM) and then generates Unicode and ASCII copies`
LangString Example1EDesc ${LANG_ENGLISH} `Parses Example1_Unicode_UTF8.json (sig) and then generates Unicode and ASCII copies`
LangString Example2Desc ${LANG_ENGLISH} `Parses Example2.json and then generates Output\Example2.json`
LangString Example3Desc ${LANG_ENGLISH} `Parses Example3.json and then generates Output\Example3.json`
LangString Example4Desc ${LANG_ENGLISH} `Parses Example4.json and then generates Output\Example4.json`
LangString Example5Desc ${LANG_ENGLISH} `Parses Example5.json and then generates Output\Example5.json`
LangString Example5BDesc ${LANG_ENGLISH} `Parses Example5.json and then generates Output\Example5B.json using $$R0`
LangString Example6Desc ${LANG_ENGLISH} `Generates Output\Example6.json using Parse, Set and Serialize`
LangString Example7Desc ${LANG_ENGLISH} `Parses Example1.json and then reads values from the tree using Get`
LangString Example8Desc ${LANG_ENGLISH} `Tests JSON array manipulation while generating Output\Example8.json`
LangString Example9Desc ${LANG_ENGLISH} `Iterates through some nodes in Example4.json by index`
LangString Example10Desc ${LANG_ENGLISH} `Parses Example5.json into Example4.json and then generates Output\Example10.json`
LangString Example11Desc ${LANG_ENGLISH} `Parses Preferences.json and then generates Output\PreferencesNew.json`
LangString Example12Desc ${LANG_ENGLISH} `Parses Preferences2.json and then generates Output\Preferences2New.json`
LangString Example13Desc ${LANG_ENGLISH} `Parses Example2.json (size>NSIS_MAX_STRLEN) and outputs the result.`
LangString Example14Desc ${LANG_ENGLISH} `Simple Get function tests.`
LangString Example15Desc ${LANG_ENGLISH} `Simple Delete function tests.`
LangString Example16Desc ${LANG_ENGLISH} `Empty/missing value keys tests.`

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE1} $(Example1Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE1B} $(Example1BDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE1C} $(Example1CDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE1D} $(Example1DDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE1E} $(Example1EDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE2} $(Example2Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE3} $(Example3Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE4} $(Example4Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE5} $(Example5Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE5B} $(Example5BDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE6} $(Example6Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE7} $(Example7Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE8} $(Example8Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE9} $(Example9Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE10} $(Example10Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE11} $(Example11Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE12} $(Example12Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE13} $(Example13Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE14} $(Example14Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE15} $(Example15Desc)
  !insertmacro MUI_DESCRIPTION_TEXT ${EXAMPLE16} $(Example16Desc)
!insertmacro MUI_FUNCTION_DESCRIPTION_END