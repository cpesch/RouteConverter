;Language: Yoruba (1130)
;By Joost Verburg

!insertmacro LANGFILE "Yoruba" = "Yorùbá" "Yoruba"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Èyí ni Ètò Ìṣiṣẹ́ $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Setup will guide you through the installation of $(^NameDA).$\r$\n$\r$\nIt is recommended that you close all other applications before starting Setup. This will make it possible to update relevant system files without having to reboot your computer.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Welcome to the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Setup will guide you through the uninstallation of $(^NameDA).$\r$\n$\r$\nBefore starting the uninstallation, make sure $(^NameDA) is not running.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Àdéhùn Ìfúnniláṣẹ"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Jọ̀wọ́ ka àdéhùn ìfúnniláṣẹ kó o tó fi $(^NameDA) sí i."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "tó o bá fara mọ́ ohun tá a sọ nínú àdéhùn yìí, tẹ Mo Gbà kó lè máa bá a lọ. O gbọ́dọ̀ fara mọ́ àdéhùn yìí kó o tó fi $(^NameDA) sí i."
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
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Tẹ bọ́tìnì Page Down láti lè rí ìyókù àdéhùn yìí."
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
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Àlàyé nípa rẹ̀"
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
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Ó ti ń fi sí i"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Jọ̀wọ́ ní sùúrù bó ṣe ń fi $(^NameDA) sí i."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Ó ti fi sí i tán"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Ó ti parí lórí ètò ìṣiṣẹ́."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Installation Aborted"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Kò lè parí ètò ìṣiṣẹ́."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Ó ti ń yọ ọ́"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Jọ̀wọ́ ní sùúrù bó ṣe ń yọ $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Ó Ti Yọ Ọ́ Tán"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Ó ti parí yíyọ ọ́."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Ó Ti Ṣíwọ́ Yíyọ Ọ́"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Kò parí yíyọ ọ́."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Ó ń parí Ètò Ìṣiṣẹ́ $(^NameDA) lọ"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "Ó ti fi $(^NameDA) sórí kọ̀ǹpútà rẹ.$\r$\n$\r$\nTẹ Ìparí kó lè pa Ètò Ìṣiṣẹ́."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "O ní láti tún kọ̀ǹpútà rẹ̣ tàn kó tó lè parí iṣẹ́ fífi $(^NameDA) sí i. Ṣe wàá fẹ́ tún kọ̀ǹpútà rẹ tàn báyìí?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Completing the $(^NameDA) Uninstall"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) has been uninstalled from your computer.$\r$\n$\r$\nClick Finish to close Setup."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Your computer must be restarted in order to complete the uninstallation of $(^NameDA). Do you want to reboot now?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Tún un tàn wàyí"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Mo fẹ́ fúnra mi tún un tàn tó bá yá"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Mú kí $(^NameDA) bẹ̀rẹ̀ iṣẹ́"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Gbé Kàmí jáde"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Ìparí"  
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
  ${LangFileString} MUI_TEXT_ABORTWARNING "Ṣé ó dá ọ lójú pé o fẹ́ pa Ètò Ìṣiṣẹ́ $(^Name) tì?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Ṣé ó dá ọ lójú pé o fẹ́ pa Yíyọ $(^Name) Kúrò tì?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Choose Users"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Choose for which users you want to install $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Select whether you want to install $(^NameDA) for yourself only or for all users of this computer. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Install for anyone using this computer"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Install just for me"
!endif
