;Language: Igbo (1136)
;By Joost Verburg

!insertmacro LANGFILE "Igbo" = "Igbo" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Anyị na-anabata gị ná Ntinye $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Setup will guide you through the installation of $(^NameDA).$\r$\n$\r$\nIt is recommended that you close all other applications before starting Setup. This will make it possible to update relevant system files without having to reboot your computer.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Welcome to the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Setup will guide you through the uninstallation of $(^NameDA).$\r$\n$\r$\nBefore starting the uninstallation, make sure $(^NameDA) is not running.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Nkwekọrịta Akwụkwọ Ikike"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Biko gụgharịa ihe e dere ná nkwekọrịta akwụkwọ ikike tupu i tinyewe $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Ọ bụrụ na ị nabatara ihe e dere ná nkwekọrịta ahụ, pịa M Kwere iji gaa n'ihu. Ị ga-anabatarịrị nkwekọrịta ahụ iji tinye $(^NameDA)."
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
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Pịa Page Down iji hụ ihe ndị ọzọ dị ná nkwekọrịta a."
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
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Nkọwa"
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
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "O Tinyewela Ya"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Biko chere ka a na-etinyere gị $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "O Tinyechaala Ya"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Ntinye gara nke ọ̣ma."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Installation Aborted"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Ntinye agaghị nke ọma."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "O Wepụwala Ya"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Biko chere ka a na-ewepụrụ gị $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "O Wepụchaala Ya"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "O wepụchara ya nke ọma."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "O Wepụlighị Ya"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "O wepụchaghị ya nke ọma."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Itinyecha $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "E tinyela $(^NameDA) na kọmputa gị.$\r$\n$\r$\nPịa Mechaa iji mechie Ntinye."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Ị ga-amaliteghachirịrị kọmputa gị iji tinyechaa $(^NameDA). Ị̀ chọrọ ịmaliteghachi ya ugbu a?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Completing the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) has been uninstalled from your computer.$\r$\n$\r$\nClick Finish to close Setup."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Your computer must be restarted in order to complete the uninstallation of $(^NameDA). Do you want to reboot now?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Maliteghachi ugbu a"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "M chọrọ iji aka m maliteghachi ya ma e mechaa"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Malite $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Gosi Gụọ"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&O mechaala"  
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
  ${LangFileString} MUI_TEXT_ABORTWARNING "Ì ji n'aka ka ị chọghị ka Ntinye $(^Name) ruo n'isi?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Ì ji n'aka na ị chọrọ ka Mwepụ $(^Name) kwụsị?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Choose Users"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Choose for which users you want to install $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Select whether you want to install $(^NameDA) for yourself only or for all users of this computer. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Install for anyone using this computer"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Install just for me"
!endif
