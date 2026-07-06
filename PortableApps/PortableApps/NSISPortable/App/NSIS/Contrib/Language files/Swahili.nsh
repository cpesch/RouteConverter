;Language: Swahili (1089)
;By Joost Verburg

!insertmacro LANGFILE "Swahili" = "Swahili" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Karibu kwenye Usanidi wa $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Usanidi utakuongoza katika mchakato wote wa kusakinisha $(^NameDA).$\r$\n$\r$\nInapendekezwa ufunge programu nyingine zote kabla ya kuanza Usanidi. Kufanya hivyo kutawezesha usasishaji wa faili za mfumo zinazohusika pasipo uhitaji wa kuzima na kuwasha upya kompyuta yako.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Karibu kwenye Usakinushaji wa $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Usanidi utakuongoza katika mchakato wote wa kusakinusha $(^NameDA).$\r$\n$\r$\nKabla ya kuanza usakinushaji, hakikisha kwamba $(^NameDA) imefungwa.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Leseni ya Makubaliano"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Tafadhali soma matakwa ya leseni kabla ya kusakinisha (kuweka kwenye kompyuta) $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Ikiwa unakubali matakwa ya makubaliano, bofya Nakubali ili kuendelea. Lazima ukubali matakwa hayo kabla ya kusakinisha (kuweka kwenye kompyuta) $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Ikiwa unakubali matakwa ya makubaliano, bofya kisanduku cha alama kilicho chini. Lazima ukubali matakwa hayo kabla ya kusakinisha (kuweka kwenye kompyuta) $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Ikiwa unakubali matakwa ya makubalino, teua chaguo la kwanza lililo hapa chini. Lazima ukubali matakwa hayo kabla ya kusakinisha (kuweka kwenye kompyuta) $(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Leseni ya Makubaliano"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Tafadhali soma matakwa ya leseni kabla ya kusakinusha $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Ikiwa unakubali matakwa ya makubaliano, bofya Nakubali ili kuendelea. Lazima ukubali matakwa hayo kabla ya kusakinusha $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Ikiwa unakubali matakwa ya makubaliano, bofya kisanduku cha alama kilicho chini. Lazima ukubali matakwa hayo kabla ya kusakinusha $(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Ikiwa unakubali matakwa ya makubalino, teua chaguo la kwanza lililo hapa chini. Lazima ukubali matakwa hayo kabla ya kusakinusha $(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Bofya Page Down uone sehemu inayosalia ya makubaliano."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Teua Vijenzi"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Teua vipengele vya $(^NameDA) unavyotaka kusakinisha."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Teua Vijenzi"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Teua vipengele vya $(^NameDA) unavyotaka kusakinusha."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Ufafanuzi"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Umbiza kipanya juu ya kijenzi fulani upate kuona ufafanuzi wake."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Umbiza kipanya juu ya kijenzi fulani upate kuona ufafanuzi wake."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Teua mahali pa kusakinishia"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Teua folda ya kusakinishia $(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Teua mahali pa kusakinushia"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Teua folda ya kusakinushia $(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Inasakinisha"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Tafadhali subiri, $(^NameDA) inasakinishwa."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Usakinishaji Umekamilika"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Usanidi umekamilika ifaavyo."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Usakinishaji Umetunguliwa"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Usanidi haukukamilika ifaavyo."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Inasakinusha"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Tafadhali subiri, $(^NameDA) inasakinushwa."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Usakinushaji Umekamilika"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Usakinushaji umekamilika ifaavyo."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Usakinushaji Umetunguliwa "
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Usakinushaji haukukamilika ifaavyo."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Inamalizia Usanidi wa $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) imesakinishwa katika kompyuta yako.$\r$\n$\r$\nBofya Maliza ili kufunga Usanidi."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Lazima kompyuta yako iwashwe upya ili kukamilisha usakinishaji wa $(^NameDA). Ungependa iwashwe upya sasa?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Inamalizia Usakinushaji wa $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) has been uninstalled from your computer.$\r$\n$\r$\nBofya Maliza ili kufunga Usanidi."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Lazima kompyuta yako iwashwe upya ili kukamilisha usakinushaji wa $(^NameDA). Ungependa iwashwe upya sasa?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Washa sasa"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Nitaiwasha baadaye"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Anzisha $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Onyesha faili yenye maagizo"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Maliza"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Teua folda ya Menyu ya Kuanzisha"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Teua folda ya Menyu ya mikato ya Kuanzisha $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Teua folda ya Menyu ya Kuanzisha ambamo ungependa kuunda mikato ya programu. Unaweza pia kuingiza jina ili kuunda folda mpya."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Mikato isiundwe"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Sakinusha $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Ondoa $(^NameDA) katika kompyuta."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Una hakika kwamba unataka kukatiza Usanidi wa $(^Name)?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Una hakika kwamba unataka kukatiza Usakinushaji wa $(^Name)?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Teua Watumiaji"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Teua watumiaji unaotaka kuwasikinishia $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Teua ikiwa unataka kusakinisha $(^NameDA) kwa ajili yako peke yako au kwa ajili ya wote wanaotumia kompyuta hii. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Sakinisha kwa ajili ya wote wanaotumia kompyuta hii"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Sakinisha kwa ajili yangu"
!endif
