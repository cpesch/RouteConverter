;Language: Filipino (1124)
;By John T. Haller (Machine Translation)

!insertmacro LANGFILE "Filipino" = "Filipino" "Filipino"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Maligayang pagdating sa $(^NameDA) Setup"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Gagabayan ka ng Setup sa pag-install ng $(^NameDA).$\r$\n$\r$\nInirerekomenda na isara mo ang lahat ng iba pang application bago simulan ang Setup. Gagawin nitong posible na i-update ang mga nauugnay na file ng system nang hindi kinakailangang i-reboot ang iyong computer.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Maligayang pagdating sa $(^NameDA) I-uninstall"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Gagabayan ka ng pag-setup sa pag-uninstall ng $(^NameDA).$\r$\n$\r$\n Bago simulan ang pag-uninstall, tiyaking hindi tumatakbo ang $(^NameDA).$\r$\n$\ r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Kasunduan sa Lisensya"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Pakisuri ang mga tuntunin ng lisensya bago i-install ang $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Kung tatanggapin mo ang mga tuntunin ng kasunduan, i-click ang Tatanggapin para magpatuloy. Dapat mong tanggapin ang kasunduan sa pag-install ng $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Kung tinatanggap mo ang mga tuntunin ng kasunduan, i-click ang check box sa ibaba. Dapat mong tanggapin ang kasunduan sa pag-install ng $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Kung tatanggapin mo ang mga tuntunin ng kasunduan, piliin ang unang opsyon sa ibaba. Dapat mong tanggapin ang kasunduan sa pag-install ng $(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Kasunduan sa Lisensya"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Pakisuri ang mga tuntunin ng lisensya bago i-uninstall ang $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Kung tatanggapin mo ang mga tuntunin ng kasunduan, i-click ang I Agree para magpatuloy. Dapat mong tanggapin ang kasunduan sa pag-uninstall ng $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Kung tinatanggap mo ang mga tuntunin ng kasunduan, i-click ang check box sa ibaba. Dapat mong tanggapin ang kasunduan sa pag-uninstall ng $(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Kung tatanggapin mo ang mga tuntunin ng kasunduan, piliin ang unang opsyon sa ibaba. Dapat mong tanggapin ang kasunduan sa pag-uninstall ng $(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Pindutin ang Page Down upang makita ang natitirang bahagi ng kasunduan."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Pumili ng Mga Bahagi"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Piliin kung aling mga feature ng $(^NameDA) ang gusto mong i-install."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Pumili ng Mga Bahagi"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Piliin kung aling mga feature ng $(^NameDA) ang gusto mong i-uninstall."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Paglalarawan"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Iposisyon ang iyong mouse sa ibabaw ng isang bahagi upang makita ang paglalarawan nito."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Pumili ng isang bahagi upang makita ang paglalarawan nito."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Piliin ang I-install ang Lokasyon"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Piliin ang folder kung saan i-install ang $(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Piliin ang I-uninstall ang Lokasyon"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Piliin ang folder kung saan ia-uninstall ang $(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Pag-install"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Mangyaring maghintay habang naka-install ang $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Kumpleto na ang Pag-install"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Matagumpay na nakumpleto ang pag-setup."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Natigil ang Pag-install"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Hindi matagumpay na nakumpleto ang pag-setup."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Ina-uninstall"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Mangyaring maghintay habang ina-uninstall ang $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Kumpleto na ang Pag-uninstall"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Matagumpay na nakumpleto ang pag-uninstall."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Natigil ang Pag-uninstall"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Hindi matagumpay na nakumpleto ang pag-uninstall."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Kinukumpleto ang $(^NameDA) Setup"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "Ang $(^NameDA) ay na-install sa iyong computer.$\r$\n$\r$\nI-click ang Tapos upang isara ang Setup."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Dapat na i-restart ang iyong computer upang makumpleto ang pag-install ng $(^NameDA). Gusto mo bang mag-reboot ngayon?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Pagkumpleto ng $(^NameDA) I-uninstall"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "Ang $(^NameDA) ay na-uninstall mula sa iyong computer.$\r$\n$\r$\nI-click ang Tapos upang isara ang Setup."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Dapat na i-restart ang iyong computer upang makumpleto ang pag-uninstall ng $(^NameDA). Gusto mo bang mag-reboot ngayon?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "I-reboot ngayon"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Gusto kong manu-manong i-reboot mamaya"
  ${LangFileString} MUI_TEXT_FINISH_RUN "Patakbuhin ang $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "Ipakita ang Readme"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "Tapusin"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Piliin ang Start Menu Folder"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Pumili ng folder ng Start Menu para sa mga shortcut na $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Piliin ang folder ng Start Menu kung saan mo gustong gumawa ng mga shortcut ng program. Maaari ka ring maglagay ng pangalan para gumawa ng bagong folder."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Huwag gumawa ng mga shortcut"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "I-uninstall ang $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Alisin ang $(^NameDA) mula sa iyong computer."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Sigurado ka bang gusto mong ihinto ang $(^Name) Setup?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Sigurado ka bang gusto mong ihinto ang $(^Name) Uninstall?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Pumili ng Mga User"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Piliin kung aling mga user ang gusto mong i-install $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Piliin kung gusto mong i-install ang $(^NameDA) para lang sa iyong sarili o para sa lahat ng user ng computer na ito. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "I-install para sa sinumang gumagamit sa computer na ito"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "I-install para lang sa akin"
!endif
