;Language: Twi (1539)

!insertmacro LANGFILE "Twi" = "Twi" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Wode $(^NameDA) Rebɛgu W’afiri So"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "“Setup” bɛkyerɛ wo sɛnea wode $(^NameDA) dwumadi no begu w’afiri no so.$\r$\n$\r$\nƐbɛyɛ papa sɛ wudum w’afiri so dwumadi a aka nyinaa ansa na woafi “Setup” no ase. Eyi bɛboa ama dwumadi no mu nneɛma atitiriw nyinaa ahyehyɛ yiye wɔ w’afiri no so, sɛnea ɛbɛyɛ a ɛho renhia sɛ wudum w’afiri no san sɔ.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Worebeyi $(^NameDA) Afi W’afiri no So"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "“Setup” bɛkyerɛ wo sɛnea wubeyi $(^NameDA) afi w’afiri so.$\r$\n$\r$\nAnsa na wubefi ase ayi dwumadi no, hwɛ hu sɛ woadum $(^NameDA) biara ansa.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Tumi Krataa Nhyehyɛe"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Yɛsrɛ wo, kenkan tumi krataa nhehyɛe no ansa na wode $(^NameDA) agu w’afiri no so."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Sɛ wugye nhyehyɛe no tom a, klik Migye Tom so ma ɛntoa so. Ɛsɛ sɛ wugye nhyehyɛe no tom ansa na woatumi de $(^NameDA) agu w’afiri no so."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Sɛ wugye nhyehyɛe no tom a, klik adaka a ɛwɔ ase ha no so. Ehia sɛ wugye nhyehyɛe no tom ansa na woatumi de $(^NameDA) agu w’afiri no so. $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Sɛ wugye nhyehyɛe no tom a, sɛlɛt nea edi kan wɔ ase hɔ no. Ɛsɛ sɛ wugye nhyehyɛe no tom ansa na woatumi de $(^NameDA) agu w’afiri no so. $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Tumi Krataa Nhyehyɛe"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Yɛsrɛ wo, kenkan tumi krataa ho nhehyɛe no ansa na woayi $(^NameDA) afi w’afiri no so."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Sɛ wugye nhyehyɛe no tom a, klik Migye Tom so ma ɛntoa so. Ɛsɛ sɛ wugye nhyehyɛe no tom ansa na woatumi de $(^NameDA) agu w’afiri no so."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Sɛ wugye nhyehyɛe no tom a, klik adaka a ɛwɔ ase ha no so. Ɛsɛ sɛ wugye nhyehyɛe no tom ansa na woatumi ayi $(^NameDA) afi w’afiri no so. $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Sɛ wugye nhyehyɛe no tom a, sɛlɛt nea edi kan wɔ ase hɔ no. Ɛsɛ sɛ wugye nhyehyɛe no tom ansa na woatumi ayi $(^NameDA) afi w’afiri no so. $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Mia “Page Down” so wɔ “keyboard” no so na kenkan nhyehyɛe no mu nsɛm a aka no."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Paw Nea Wopɛ"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Paw $(^NameDA) mu nneɛma a wopɛ sɛ wode gu so."
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Description"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Paw Nea Wopɛ"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Paw $(^NameDA) mu nneɛma a wopɛ sɛ wode gu so."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Fa wo “mouse” no to biribi so fa hwɛ ɛho nkyerɛkyerɛmu."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Fa wo “mouse” no to biribi so fa hwɛ ɛho nkyerɛkyerɛmu."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Paw Baabi a Ɛmfa Ngu"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Paw Folda a Ɛmfa $(^NameDA) Ngu Mu."
!endif

!ifdef MUI_UNDIRECTORYSPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Paw Baabi a Enyi Mfi"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Paw Folda a Enyi $(^NameDA) no Mfi Mu."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Ɛrekogu w’afiri no so"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Yɛsrɛ sɛ nya bere kakra ma $(^NameDA) nkogu w’afiri no so."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Awie"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Woatumi de dwumadi no agu so awie."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Anyɛ Yiye"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Woantumi amfa dwumadi no angu w’afiri no so anwie."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Ɛreyi afi so"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Yɛsrɛ sɛ nya bere kakra ma $(^NameDA) mfi w’afiri no so."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Woayi Afi so Awie"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Woayi afi so korakora."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Woantumi Anyi Amfi So"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Woantumi anyi dwumadi no amfi so."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Wode $(^NameDA) Agu So Awie"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) akɔgu w’afiri no so.$\r$\n$\r$\nKlik “Wie” so na ɛmmra awiei."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Ɛsɛ sɛ wudum w’afiri no san sɔ bio na ama $(^NameDA) dwumadi no ahyehyɛ yiye. Wopɛ sɛ wudum no na wosan sɔ seesei ara?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Woreyi $(^NameDA) no Afi So"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) afi w’afiri no so.$\r$\n$\r$\nKlik “Wie” so na ɛmmra awiei."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Ɛsɛ wudum w’afiri no san sɔ bio na ama $(^NameDA) afi w’afiri no so korakora. Wopɛ sɛ wudum no na wosan sɔ seesei ara?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Dum no na san sɔ seesei ara"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Akyiri yi medum no asan asɔ"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Run $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "Bue Readme"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "Wie"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Choose Start Menu Folder"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Choose a Start Menu folder for the $(^NameDA) shortcuts."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Select the Start Menu folder in which you would like to create the program's shortcuts. You can also enter a name to create a new folder."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Do not create shortcuts"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Yi $(^NameDA) fi so"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Yi $(^NameDA) fi w’afiri no so."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Wompɛ sɛ wotoa so de $(^Name) gu w’afiri no so?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Wompɛ sɛ wuyi $(^Name) fi so?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Choose Users"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Choose for which users you want to install $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Select whether you want to install $(^NameDA) for yourself only or for all users of this computer. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Install for anyone using this computer"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Install just for me"
!endif
