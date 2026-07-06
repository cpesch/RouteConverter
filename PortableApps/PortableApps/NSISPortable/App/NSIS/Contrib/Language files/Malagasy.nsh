;Language: Malagasy (1536)
;By Joost Verburg

!insertmacro LANGFILE "Malagasy" = "Malagasy" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Tonga Soa eto Amin'ny Fampidirana ny $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Hatoro anao izay tokony hataonao mandritra ny fampidirana ny $(^NameDA).$\r$\n$\r$\nTsara raha akatona ny programa hafa rehetra, alohan'ny hanombohana ny Fampidirana. Tsy voatery hamerina hamelona ny ordinatera ianao amin'izay, rehefa hatao ny fanovana ilaina ao amin'ny ordinateranao.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Tonga Soa eto Amin'ny Fanesorana ny $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Hatoro anao izay tokony hataonao mandritra ny fanesorana ny $(^NameDA).$\r$\n$\r$\nAtaovy azo antoka fa tsy misokatra ny $(^NameDA), alohan'ny hanombohana ny Fanesorana.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Fifanekena"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Jereo aloha izay voalaza ao amin'ny fifanekena, alohan'ny hampidirana ny $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Raha ekenao izay voalaza ao amin'ny fifanekena ary te hanohy ianao, dia tsindrio ny Ekeko. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hampiditra ny $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Raha ekenao izay voalaza ao amin'ny fifanekena, dia mariho eo amin'ny efajoro kely eto ambany. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hampiditra ny $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Raha ekenao izay voalaza ao amin'ny fifanekena, dia mariho eo amin'ilay safidy voalohany eto ambany. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hampiditra ny $(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Fifanekena"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Jereo aloha izay voalaza ao amin'ny fifanekena, alohan'ny hanesorana ny $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Raha ekenao izay voalaza ao amin'ny fifanekena ary te hanohy ianao, dia tsindrio ny Ekeko. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hanaisotra ny $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Raha ekenao izay voalaza ao amin'ny fifanekena, dia mariho eo amin'ny efajoro kely eto ambany. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hanaisotra ny $(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Raha ekenao izay voalaza ao amin'ny fifanekena, dia mariho eo amin'ilay safidy voalohany eto ambany. Tsy maintsy manaiky an'io fifanekena io ianao vao ho afaka hanaisotra ny $(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ;${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Tsindrio ny bokotra PgSuiv/Page Down eo amin'ny klavie, raha te hahita ny ambin'ny fifanekena ianao."
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Tsindrio ny bokotra PgSuiv/Page Down eo amin'ny klavie, mba hahitana ny tohiny."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Fidio Izay Tianao"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Fidio izay tianao hampidirina."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Fidio Izay Tianao"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Fidio izay tianao hesorina."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Mombamomba Azy"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Tondroy amin'ny totozy izay tianao, mba hahitana ny mombamomba azy."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Tondroy amin'ny totozy izay tianao, mba hahitana ny mombamomba azy."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Hifidy ny Toerana Hasiana Azy"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Fidio ny toerana fampirimana tianao hampidirana ny $(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Hifidy ny Toerana Hanesorana Azy"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Fidio ny toerana fampirimana tianao hanesorana ny $(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Fampidirana"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Mahandrasa kely, mandra-pampiditra ny $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Vita ny Fampidirana"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Vita soa aman-tsara ny fampidirana."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Notapahina ny Fampidirana"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Tsy vita hatramin'ny farany ny fampidirana."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Fanesorana"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Mahandrasa kely, mandra-panaisotra ny $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Vita ny Fanesorana"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Vita soa aman-tsara ny fanesorana."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Notapahina ny Fanesorana"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Tsy vita hatramin'ny farany ny fanesorana."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Eo Am-pamitana ny Fampidirana ny $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "Tafiditra ato amin'ny ordinateranao ny $(^NameDA).$\r$\n$\r$\nTsindrio ny Vita mba hanakatonana ny Fampidirana."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Tsy maintsy velomina indray ny ordinateranao vao ho vita tanteraka ny fampidirana ny $(^NameDA). Tianao hatao izao ve izany?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Eo Am-pamitana ny Fanesorana ny $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "Nesorina tato amin'ny ordinateranao ny $(^NameDA).$\r$\n$\r$\nTsindrio ny Vita mba hanakatonana ny Fanesorana."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Tsy maintsy velomina indray ny ordinateranao vao ho vita tanteraka ny fanesorana ny $(^NameDA). Tianao hatao izao ve izany?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Hamelona izao"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Tiako haverina velomina aoriana kely"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Handefa ny $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "H&ampiseho ny VakioAho"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Vita"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Hifidy Toerana Fampirimana ao Amin'ny Menu Démarrer/Start Menu"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Mifidiana toerana fampirimana ao amin'ny Menu Démarrer/Start Menu mba hametrahana ny hitsin-dalan'ny $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Mifidiana na mamoròna toerana fampirimana ny hitsin-dalan'ny $(^NameDA) ao amin'ny Menu Démarrer/Start Menu."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Tsy hamorona hitsin-dalana"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Hanaisotra ny $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Hanala ny $(^NameDA) avy ao amin'ny ordinateranao."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Tena te hiala ato amin'ny Fampidirana ny $(^Name) ve ianao?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Tena te hiala ato amin'ny Fanesorana ny $(^Name) ve ianao?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Hifidy An'izay Hampiasa Azy"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Fidio ny kaontin'izay olona miray ordinatera aminao, ka tianao ho afaka hampiasa ny $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Lazao raha ianao irery no tianao ho afaka hampiasa ny $(^NameDA), na raha tianao ho afaka hampiasa azy io izay rehetra miray ordinatera aminao. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Ho an'izay rehetra miray ordinatera amiko"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Ho ahy irery"
!endif
