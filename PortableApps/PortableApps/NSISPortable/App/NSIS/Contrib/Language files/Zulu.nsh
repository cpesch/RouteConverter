;Language: Zulu (1077)
;By Joost Verburg

!insertmacro LANGFILE "Zulu" = "Zulu" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Uyemukelwa kuyi-Setup ye-$(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "I-Setup izokusiza ukuba ukwazi ukufaka i-$(^NameDA).$\r$\n$\r$\nKutuswa ukuba uvale zonke ezinye izinhlelo ngaphambi kokuba uqale i-Setup. Lokhu kuzokwazi ukulungisa amafayela afanele omshini ngaphandle kokuba uwuvale.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Uyemukelwa ohlelweni lokukhipha i-$(^NameDA)"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "I-Setup izokusiza ukuba ukwazi ukukhipha i-$(^NameDA).$\r$\n$\r$\nNgaphambi kokuba uqale ukuyikhipha, qiniseka ukuthi i-$(^NameDA) ayivuliwe.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "ISivumelwano Selayisense"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Siza ubukeze imininingwane yelayisense ngaphambi kokuba ufake i-$(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Uma uyemukela imininingwane yesivumelwano, chofoza inkinobho ethi Ngiyavuma ukuze uqhubeke. Kumelwe wamukele isivumelwano ukuze ukwazi ukufaka i-$(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Uma uyemukela imininingwane yesivumelwano, chofoza ibhokisi elinoqhwishi ngezansi. Kumelwe wamukele isivumelwano ukuze ukwazi ukufaka i-$(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Uma uyemukela imininingwane yesivumelwano, khetha okokuqala ngezansi. Kumelwe wamukele isivumelwano ukuze ukwazi ukufaka i-$(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "ISivumelwano Selayisense"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Siza ubukeze imininingwane yelayisense ngaphambi kokuba ukhiphe i-$(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Uma uyemukela imininingwane yesivumelwano, chofoza inkinobho ethi Ngiyavuma ukuze uqhubeke. Kumelwe wamukele isivumelwano ukuze ukwazi ukukhipha i-$(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Uma uyemukela imininingwane yesivumelwano, chofoza ibhokisi elinoqhwishi ngezansi. Kumelwe wamukele isivumelwano ukuze ukwazi ukukhipha i-$(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Uma uyemukela imininingwane yesivumelwano, khetha okokuqala ngezansi. Kumelwe wamukele isivumelwano ukuze ukwazi ukukhipha i-$(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Cindezela u-Page Down ukuze ubone ingxenye esele yesivumelwano."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Khetha Izingxenye Zohlelo Ozifunayo"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Khetha izingxenye ze-$(^NameDA) ofuna ukuzifaka."
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Incazelo"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Khetha Izingxenye Zohlelo Ozifunayo"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Khetha izingxenye ze-$(^NameDA) ofuna ukuzikhipha."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Beka i-mouse phezu kwengxenye yohlelo ukuze ubone incazelo."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Beka i-mouse phezu kwengxenye yohlelo ukuze ubone incazelo."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Khetha Indawo Ofuna Ukufaka Kuyo"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Khetha indawo ofuna ukufaka kuyo i-$(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYSPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Khetha Indawo Ofuna Ukukhipha Kuyo"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Khetha indawo ofuna ukukhipha kuyo i-$(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Uyayifaka"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Linda njengoba umshini usafaka i-$(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Isiqedile Ukuyifaka"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "I-Setup isebenzengokuphumelelayo."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Ukufaka Lolu Hlelo Kumisiwe"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "I-Setup ayiqedangangokuphumelelayo."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Iyayikhipha"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Linda njengoba umshini usakhipha i-$(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Isiqedile Ukuyikhipha"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Isiqedile ukuyikhipha ngokuphumelelayo."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Ukukhipha Lolu Hlelo Kumisiwe"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Ukukhipha lolu hlelo akuphumelelanga."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Iqedela ukufaka i-$(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "I-$(^NameDA) ifakiwe emshinini wakho.$\r$\n$\r$\nChofoza inkinobho ethi Iqedile ukuze uvale i-Setup."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Kumelwe uvale umshini wakho uphinde uwuvule ukuze uqedele ukufaka i-$(^NameDA). Ingabe ufuna ukuwuvala manje?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Iqedela ukukhipha i-$(^NameDA)"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "I-$(^NameDA) isikhishiwe emshinini wakho.$\r$\n$\r$\nChofoza inkinobho ethi Iqedile ukuze uvale i-Setup."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Kumelwe uvale umshini wakho uphinde uwuvule ukuze uqedele ukukhipha i-$(^NameDA). Ingabe ufuna ukuwuvala manje?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Vala umshini uphinde uwuvule manje"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Ngifuna ukuwuvala ngiphinde ngiwuvule kamuva"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Vula i-$(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Vula iFayela Lokwaziswa"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Iqedile"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Khetha indawo ye-Start Menu"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Khetha indawo ye-Start Menu yokuvula izinhlelo ze-$(^NameDA) ngokushesha."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Khetha indawo ye-Start Menu othanda ukufaka kuyo izindlela ezisheshayo zokuvula lolu hlelo. Ungabhala negama ukuze uvule indawo entsha."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Ungazenzi izindlela ezisheshayo zokuvula izinhlelo"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Khipha i-$(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Khipha i-$(^NameDA) emshinini wakho."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Ingabe uyaqiniseka ukuthi ufuna ukuvala i-Setup ye-$(^Name)?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Ingabe uyaqiniseka ukuthi ufuna ukuyeka ukukhipha i-$(^Name)?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Khetha Abasebenzisa Uhlelo"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Khetha ukuthi obani abasebenzisa lo mshini ofuna ukubafakela i-$(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Khetha ukuthi ufuna ukuzifakela i-$(^NameDA) wena wedwa noma ufuna ukuyifakela bonke abasebenzisa lo mshini. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Ifakele noma ubani osebenzisa lo mshini"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Ifakele mina ngedwa"
!endif
