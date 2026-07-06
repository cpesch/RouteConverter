!include MUI2.nsh

!define HttpWebRequestURL `http://www.afrowsoft.co.uk/test/HttpWebRequest.php`

Name `nsJSON plug-in`
OutFile nsJSON_HttpWebRequest.exe
RequestExecutionLevel user
ShowInstDetails show

!define MUI_COMPONENTSPAGE_SMALLDESC
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE English

Section

  CreateDirectory $EXEDIR\Output

SectionEnd

Section /o `POST` DATAPOST

  StrCpy $R0 `{ "Name": "Jonathan Doe", "Age": 23, "Formula": "a + b == 13%!", "ViewTimes": [ "10:00", "13:43", "21:19", "03:10" ] }`

  nsJSON::Set /tree HttpWebRequest /value `{ "Url": "${HttpWebRequestURL}", "Verb": "POST", "Agent": "Mozilla/5.0 (Windows NT 10.0; rv:10.0) Gecko/20100101 Firefox/10.0" }`
  DetailPrint `Send: $R0`
  DetailPrint `Send to: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebRequest Data /value $R0

  nsJSON::Set /tree HttpWebRequest Params /value `{ "a": "string", "b": true, "c": 2 }`

  DetailPrint `Generate: $EXEDIR\Output\HttpWebRequest.json from HttpWebRequest`
  nsJSON::Serialize /tree HttpWebRequest /format /file $EXEDIR\Output\HttpWebRequest.json

  DetailPrint `Download: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebResponse /http HttpWebRequest

  DetailPrint `Generate: $EXEDIR\Output\HttpWebResponse.json from HttpWebResponse`
  nsJSON::Serialize /tree HttpWebResponse /format /file $EXEDIR\Output\HttpWebResponse.json

SectionEnd

Section /o `Async POST` DATAPOSTASYNC

  StrCpy $R0 `{ "Name": "Jonathan Doe", "Age": 23, "Formula": "a + b == 13%!", "ViewTimes": [ "10:00", "13:43", "21:19", "03:10" ] }`

  nsJSON::Set /tree HttpWebRequest /value `{ "Url": "${HttpWebRequestURL}", "Verb": "POST", "Async": true }`
  DetailPrint `Send: $R0`
  DetailPrint `Send to: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebRequest Data /value $R0

  nsJSON::Set /tree HttpWebRequest Params /value `{ "a": "string", "b": true, "c": 2 }`

  DetailPrint `Generate: $EXEDIR\Output\HttpWebRequest.json from HttpWebRequest`
  nsJSON::Serialize /tree HttpWebRequest /format /file $EXEDIR\Output\HttpWebRequest.json

  DetailPrint `Download: ${HttpWebRequestURL}`
  nsJSON::Set /tree AsyncHttpWebResponse /http HttpWebRequest

  ; Wait until done.
  ${Do}
    Sleep 1000
    nsJSON::Wait HttpWebRequest /timeout 0
    Pop $R0
    ${If} $R0 != wait
      ${Break}
    ${EndIf}
    DetailPrint `Waiting...`
  ${Loop}

  DetailPrint `Finished...`

  DetailPrint `Generate: $EXEDIR\Output\AsyncHttpWebResponse.json from AsyncHttpWebResponse`
  nsJSON::Serialize /tree AsyncHttpWebResponse /format /file $EXEDIR\Output\AsyncHttpWebResponse.json

SectionEnd

Section /o `Raw POST` RAWDATAPOST

  StrCpy $R0 `Name=Jonathan+Doe&Age=23&Formula=a+%2B+b+%3D%3D+13%25%21&ViewTimes[]=10%3A00&ViewTimes[]=13%3A43&ViewTimes[]=21%3A19&ViewTimes[]=03%3A10`

  nsJSON::Set /tree HttpWebRequest /value `{ "Url": "${HttpWebRequestURL}", "Verb": "POST", "DataType": "Raw" }`
  DetailPrint `Send: $R0`
  DetailPrint `Send to: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebRequest Data /value `"$R0"`

  DetailPrint `Generate: $EXEDIR\Output\HttpWebRequest_Raw.json from HttpWebRequest`
  nsJSON::Serialize /tree HttpWebRequest /format /file $EXEDIR\Output\HttpWebRequest_Raw.json

  DetailPrint `Download: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebResponse /http HttpWebRequest

  DetailPrint `Generate: $EXEDIR\Output\HttpWebResponse_Raw.json from HttpWebResponse`
  nsJSON::Serialize /tree HttpWebResponse /format /file $EXEDIR\Output\HttpWebResponse_Raw.json

SectionEnd

Section /o `JSON POST` JSONPOST

  nsJSON::Set /tree HttpWebRequest /value `{ "Url": "${HttpWebRequestURL}", "Verb": "POST", "DataType": "JSON" }`
  DetailPrint `Send: $EXEDIR\Input\Example1.json`
  DetailPrint `Send to: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebRequest Data /file $EXEDIR\Input\Example1.json

  DetailPrint `Generate: $EXEDIR\Output\HttpWebRequest_JSON.json from HttpWebRequest`
  nsJSON::Serialize /tree HttpWebRequest /format /file $EXEDIR\Output\HttpWebRequest_JSON.json

  DetailPrint `Download: ${HttpWebRequestURL}`
  nsJSON::Set /tree HttpWebResponse /http HttpWebRequest

  DetailPrint `Generate: $EXEDIR\Output\HttpWebResponse_JSON.json from HttpWebResponse`
  nsJSON::Serialize /tree HttpWebResponse /format /file $EXEDIR\Output\HttpWebResponse_JSON.json

SectionEnd

LangString DataPOSTDesc ${LANG_ENGLISH} `Sends POST data and parses the JSON response`
LangString DataPOSTAsyncDesc ${LANG_ENGLISH} `Asynchronously sends POST data and parses the JSON response`
LangString RawDataPOSTDesc ${LANG_ENGLISH} `Sends raw POST data and parses the JSON response`
LangString JSONPOSTDesc ${LANG_ENGLISH} `Sends Example1.json and parses the JSON response`

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${DATAPOST} $(DataPOSTDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${DATAPOSTASYNC} $(DataPOSTAsyncDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${RAWDATAPOST} $(RawDataPOSTDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${JSONPOST} $(JSONPOSTDesc)
!insertmacro MUI_FUNCTION_DESCRIPTION_END