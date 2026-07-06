;Language: Sesotho (1132)
;By Joost Verburg

!insertmacro LANGFILE "Sesotho" = "Sesotho" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Rea u amohela Setapong ea $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Setapo e tla u tataisa ha u kenya lenaneo lena la $(^NameDA).$\r$\n$\r$\nRe u khothalletsa hore u koale mananeo a mang kaofela pele u bula Setapo. Sena se tla etsa hore u khone ho nchafatsa lifaele tse hlokahalang mochineng ntle le hore u time k’homphuieutha ea hao u be u e bule hape.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Rea u amohela mothating oa ho ntša lenaneo la $(^NameDA) "
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Setapo e tla u tataisa ha u ntša lenaneo lena la $(^NameDA).$\r$\n$\r$\nPele u qala ho ntša lenaneo lena, re kōpa u tiise hore $(^NameDA) ha ea buloa.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Tumellano ea Laesense"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Re kōpa u hlahlobe lintlha tsa tumellano ea laesense pele u kenya lenaneo la $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Haeba u amohela lintlha tsena tsa tumellano, tobetsa konopo e reng, Kea Lumela e le hore u tsoele pele. U lokela ho amohela tumellano ena e le hore u kenye lenaneo la $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Haeba u amohela lintlha tsena tsa tumellano, tšoaea lebokose le ka tlaase mona. U lokela ho amohela tumellano ena e le hore u kenye lenaneo la $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Haeba u amohela lintlha tsena tsa tumellano, khetha ntlha ea pele ka tlaase mona. U lokela ho amohela tumellano ena e le hore u kenye lenaneo la $(^NameDA). $_CLICK" 
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Tumellano ea Laesense"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Re kōpa u hlahlobe lintlha tsa tumellano ea laesense pele u ntša lenaneo la $(^NameDA) k’homphieutheng ea hao."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Haeba u amohela lintlha tsena tsa tumellano, tobetsa konopo e reng, Kea Lumela e le hore u tsoele pele. U lokela ho amohela tumellano ena e le hore u ntše lenaneo la $(^NameDA) k’homphieutheng ea hao."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Haeba u amohela lintlha tsena tsa tumellano, tšoaea lebokose le ka tlaase mona. U lokela ho amohela tumellano ena e le hore u ntše lenaneo la $(^NameDA) k’homphieutheng ea hao. $_CLICK" 
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Haeba u amohela lintlha tsena tsa tumellano, khetha ntlha ea pele ka tlaase mona. U lokela ho amohela tumellano ena e le hore u ntše lenaneo la $(^NameDA) k’homphieutheng ea hao. $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Penya konopo ea Page Down e le hore u bone tumellano ena kaofela."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Khetha Likarolo"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Khetha hore na ke litšobotsi life tsa $(^NameDA) tseo u batlang ho li kenya k’homphieutheng ea hao."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Khetha Likarolo"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Khetha hore na ke litšobotsi life tsa $(^NameDA) tseo u batlang ho li ntša k’homphieutheng ea hao."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Tlhaloso"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Beha mause holim’a karolo e le hore u bone tlhaloso ea eona."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Beha mause holim’a karolo e le hore u bone tlhaloso ea eona."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Khetha Moo Lenaneo le Kenngoang Teng"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Khetha sephuthelo seo u tla kenya lenaneo la $(^NameDA) ho sona."
!endif

!ifdef MUI_UNDIRECTORYSPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Khetha Moo Lenaneo le Ntšoang Teng"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Khetha sephuthelo seo u tla ntša lenaneo la $(^NameDA) ho sona." 
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "E Kenya Lenaneo"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Re kōpa u eme hanyenyane ha e ntse e kenya lenaneo la $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "E Qetile ho Kenya Lenaneo"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "E qetile ho etsa Setapo."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "E Tlohetse ho Kenya Lenaneo"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Setapo ha ea qetoa."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "E Ntša Lenaneo"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Re kōpa u eme hanyenyane ha e ntse e ntša lenaneo la $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "E Qetile ho Ntša Lenaneo"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "E khonne ho ntša lenaneo hantle."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "E Tlohetse ho Ntša Lenaneo"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Ha ea khona ho ntša lenaneo hantle."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "E Qetela Setapo ea $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) e kentsoe k’homphieutheng ea hao.$\r$\n$\r$\nTobetsa konopo ea Qetile e le hore u koale Setapo."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "U lokela ho bula k’homphieutha ea hao bocha e le hore e qetele ho kenya lenaneo la $(^NameDA). Na u batla ho e bula hape hona joale?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "E Qetela ho Ntša Lenaneo la $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) e ntšitsoe k’homphieutheng ea hao.$\r$\n$\r$\nTobetsa konopo ea Qetile e le hore u koale Setapo."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "U lokela ho bula k’homphieutha ea hao bocha e le hore e qetele ho ntša lenaneo la $(^NameDA). Na u batla ho e bula hape hona joale?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Bula mochine hona joale"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Ke batla ho ipulela mochine hamorao"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Bula $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Mpontše Faele ea Nka Malebela"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Qetile"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Khetha Sephuthelo sa Menyu ea Start"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Khetha sephuthelo sa Menyu ea litaelo tse khaoletsang tsa $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Khetha sephuthelo sa Menyu ea Start seo u batlang ho iketsetsa litaelo tse khaoletsang tsa lenaneo ho sona. U ka boela ua kenya lebitso hore u etse sephuthelo se secha."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "U se ke ua etsa litaelo tse khaoletsang"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Ntša lenaneo la $(^NameDA) k’homphieutheng"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Tlosa lenaneo la $(^NameDA) k’homphieutheng ea hao."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Na u kholisehile hore u batla ho tsoa Setapong ea $(^Name)?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Na u kholisehile hore u batla ho khaotsa ho Ntša Lenaneo la $(^Name) k’homphieutheng?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Khetha Batho ba ka Sebelisang Lenaneo"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Khetha batho bao u ba kenyetsang lenaneo la $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Khetha hore na lenaneo la $(^NameDA) le sebelisoe ke uena feela kapa ke bohle ba sebelisang k’homphieutha ena. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Kenya lenaneo lena hore le sebelisoe ke mang kapa mang ea sebelisang k’homphieutha ena"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Kenya lenaneo lena hore le sebelisoe ke ’na feela"
!endif
