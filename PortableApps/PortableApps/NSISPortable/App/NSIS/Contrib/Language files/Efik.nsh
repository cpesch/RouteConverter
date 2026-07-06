;Language: Efik (1538)
;By Joost Verburg

!insertmacro LANGFILE "Efik" = "Efịk" "Efik"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Emi edi $(^NameDA) Nte Ẹtịmde"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Setup will guide you through the installation of $(^NameDA).$\r$\n$\r$\nIt is recommended that you close all other applications before starting Setup. This will make it possible to update relevant system files without having to reboot your computer.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Welcome to the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Setup will guide you through the uninstallation of $(^NameDA).$\r$\n$\r$\nBefore starting the uninstallation, make sure $(^NameDA) is not running.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Ediomi Unyịme"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Mbọk kot ediomi unỵme mbemiso esịnde $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Edieke enyịmede kpukpru se ẹtịn̄de ke ediomi emi, fịk Ami Mmenyịme man aka iso. Ana enyịme ediomi emi mîdịghe idisịnke $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "If you accept the terms of the agreement, click the check box below. You must accept the agreement to install $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "If you accept the terms of the agreement, select the first option below. You must accept the agreement to install $(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "License Agreement"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Please review the license terms before uninstalling $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "If you accept the terms of the agreement, click I Agree to continue. You must accept the agreement to uninstall $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "If you accept the terms of the agreement, click the check box below. You must accept the agreement to uninstall $(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "If you accept the terms of the agreement, select the first option below. You must accept the agreement to uninstall $(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Fịk Ka Isọn̄ man okụt ediomi emi osụhọde."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Choose Components"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Choose which features of $(^NameDA) you want to install."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Choose Components"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Choose which features of $(^NameDA) you want to uninstall."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Editịn̄ nte etiede"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Position your mouse over a component to see its description."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Position your mouse over a component to see its description."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Choose Install Location"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Choose the folder in which to install $(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Choose Uninstall Location"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Choose the folder from which to uninstall $(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Ke esịn"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Mbọk bet ke ini esịnde $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Esịn Okụre"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Nte Ẹtịmde okụre uforo uforo."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Installation Aborted"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Nte Ẹtịmde ikụreke uforo uforo."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Ke osio"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Mbọk bet ke ini osiode $(^NameDA) efep."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Osio Efep Okụre"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Osio Efep Okụre Uforo Uforo."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Etre Ndisio Mfep"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Isioho Ifep Uforo Uforo."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Nte Ẹtịmde $(^NameDA) Ọmọn̄ Okụre"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "Esịn $(^NameDA) ke kọmputa fo.$\r$\n$\r$\nFịk Okụre man emen Nte Ẹtịmde efep."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Ana afiak ọtọn̄ọ kọmputa fo man okụre edisịn $(^NameDA). Ndi omoyom ndifiak ntọn̄o kọmputa?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Completing the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) has been uninstalled from your computer.$\r$\n$\r$\nClick Finish to close Setup."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Your computer must be restarted in order to complete the uninstallation of $(^NameDA). Do you want to reboot now?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Fiak tọn̄ọ idahaemi"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Nyom ndifiak ntọn̄ọ ke idemmi ama ekem"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Kụbọde $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Wụt Kot-emi"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Okụre"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Choose Start Menu Folder"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Choose a Start Menu folder for the $(^NameDA) shortcuts."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Select the Start Menu folder in which you would like to create the program's shortcuts. You can also enter a name to create a new folder."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Do not create shortcuts"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Uninstall $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Remove $(^NameDA) from your computer."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Ndi emenen̄ede oyom ndiwọrọ ke Nte Ẹtịmde $(^Name)?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Ndi emenen̄ede oyom ndiwọrọ ke Edisio $(^Name) Mfep?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Choose Users"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Choose for which users you want to install $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Select whether you want to install $(^NameDA) for yourself only or for all users of this computer. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Install for anyone using this computer"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Install just for me"
!endif
