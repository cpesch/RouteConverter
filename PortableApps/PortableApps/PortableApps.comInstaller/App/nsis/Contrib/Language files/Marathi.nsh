;Language: Marathi (1102)
;By John T. Haller (machine generated)

!insertmacro LANGFILE "Marathi" "Marathi" "मराठी" "Marathi" ; See \Include\LangFile.nsh for a description of these parameters

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "$(^NameDA) सेटअपमध्ये आपले स्वागत आहे"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "$(^NameDA) च्या स्थापनेसाठी सेटअप तुम्हाला मार्गदर्शन करेल.$\r$\n$\r$\nसेटअप सुरू करण्यापूर्वी तुम्ही इतर सर्व अॅप्लिकेशन्स बंद करा अशी शिफारस केली जाते. यामुळे तुमचा संगणक रीबूट न करता संबंधित सिस्टम फाइल्स अपडेट करणे शक्य होईल.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "$(^NameDA) अनइंस्टॉल मध्ये आपले स्वागत आहे"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "$(^NameDA) च्या विस्थापनासाठी सेटअप तुम्हाला मार्गदर्शन करेल.$\r$\n$\r$\nविस्थापन सुरू करण्यापूर्वी, $(^NameDA) चालत नसल्याचे सुनिश्चित करा.$\r$\n$\r $\n$_क्लिक करा"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "परवाना करार"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "कृपया $(^NameDA) स्थापित करण्यापूर्वी परवाना अटींचे पुनरावलोकन करा."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "तुम्ही कराराच्या अटी स्वीकारल्यास, सुरू ठेवण्यासाठी मी सहमत आहे क्लिक करा. तुम्ही $(^NameDA) स्थापित करण्यासाठी करार स्वीकारला पाहिजे."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "तुम्ही कराराच्या अटी मान्य करत असल्यास, खालील चेक बॉक्सवर क्लिक करा. तुम्ही $(^NameDA) स्थापित करण्यासाठी करार स्वीकारला पाहिजे. $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "तुम्ही कराराच्या अटी मान्य केल्यास, खालील पहिला पर्याय निवडा. तुम्ही $(^NameDA) स्थापित करण्यासाठी करार स्वीकारला पाहिजे. $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "परवाना करार"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "कृपया $(^NameDA) विस्थापित करण्यापूर्वी परवाना अटींचे पुनरावलोकन करा."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "तुम्ही कराराच्या अटी स्वीकारल्यास, सुरू ठेवण्यासाठी मी सहमत आहे क्लिक करा. $(^NameDA) विस्थापित करण्यासाठी तुम्ही करार स्वीकारला पाहिजे."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "तुम्ही कराराच्या अटी मान्य करत असल्यास, खालील चेक बॉक्सवर क्लिक करा. $(^NameDA) विस्थापित करण्यासाठी तुम्ही करार स्वीकारला पाहिजे. $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "तुम्ही कराराच्या अटी मान्य केल्यास, खालील पहिला पर्याय निवडा. $(^NameDA) विस्थापित करण्यासाठी तुम्ही करार स्वीकारला पाहिजे. $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "उर्वरित करार पाहण्यासाठी पृष्ठ खाली दाबा."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "घटक निवडा"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "तुम्हाला $(^NameDA) ची कोणती वैशिष्ट्ये स्थापित करायची आहेत ते निवडा."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "घटक निवडा"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "तुम्हाला $(^NameDA) ची कोणती वैशिष्ट्ये विस्थापित करायची आहेत ते निवडा."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "वर्णन"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "एखाद्या घटकाचे वर्णन पाहण्यासाठी तुमचा माउस त्याच्यावर ठेवा."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "त्याचे वर्णन पाहण्यासाठी घटक निवडा."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "स्थापित स्थान निवडा"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "$(^NameDA) ज्या फोल्डरमध्ये स्थापित करायचे ते निवडा."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "विस्थापित स्थान निवडा"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "ज्या फोल्डरमधून $(^NameDA) विस्थापित करायचे ते निवडा."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "स्थापित करत आहे"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "कृपया $(^NameDA) स्थापित होत असताना प्रतीक्षा करा."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "स्थापना पूर्ण"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "सेटअप यशस्वीरित्या पूर्ण झाला."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "स्थापना रद्द केली"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "सेटअप यशस्वीरित्या पूर्ण झाले नाही."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "विस्थापित करत आहे"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "कृपया $(^NameDA) अनइंस्टॉल होत असताना प्रतीक्षा करा."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "विस्थापित पूर्ण"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "विस्थापित यशस्वीरित्या पूर्ण झाले."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "विस्थापित करणे रद्द केले"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "विस्थापित यशस्वीरित्या पूर्ण झाले नाही."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) सेटअप पूर्ण करत आहे"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) तुमच्या संगणकावर स्थापित केले गेले आहे.$\r$\n$\r$\nसेटअप बंद करण्यासाठी Finish वर क्लिक करा."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) ची स्थापना पूर्ण करण्यासाठी तुमचा संगणक रीस्टार्ट करणे आवश्यक आहे. तुम्हाला आता रीबूट करायचे आहे का?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) अनइंस्टॉल पूर्ण करत आहे"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) तुमच्या संगणकावरून विस्थापित केले गेले आहे.$\r$\n$\r$\nसेटअप बंद करण्यासाठी समाप्त क्लिक करा."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) चे विस्थापन पूर्ण करण्यासाठी तुमचा संगणक रीस्टार्ट करणे आवश्यक आहे. तुम्हाला आता रीबूट करायचे आहे का?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "आता रीबूट करा"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "मला नंतर व्यक्तिचलितपणे रीबूट करायचे आहे"
  ${LangFileString} MUI_TEXT_FINISH_RUN "सुरू करा $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "रीडमी दाखवा"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "समाप्त करा"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "स्टार्ट मेनू फोल्डर निवडा"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "$(^NameDA) शॉर्टकटसाठी स्टार्ट मेनू फोल्डर निवडा."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "स्टार्ट मेनू फोल्डर निवडा ज्यामध्ये तुम्हाला प्रोग्रामचे शॉर्टकट तयार करायचे आहेत. नवीन फोल्डर तयार करण्यासाठी तुम्ही नाव देखील टाकू शकता."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "शॉर्टकट तयार करू नका"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "विस्थापित करा $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "तुमच्या संगणकावरून $(^NameDA) काढा."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "तुम्हाला खात्री आहे की तुम्ही $(^Name) सेटअप सोडू इच्छिता?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "तुम्हाला खात्री आहे की तुम्ही $(^Name) अनइंस्टॉल सोडू इच्छिता?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "वापरकर्ते निवडा"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "तुम्ही कोणत्या वापरकर्त्यांसाठी $(^NameDA) स्थापित करू इच्छिता ते निवडा."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "तुम्ही $(^NameDA) फक्त स्वतःसाठी किंवा या संगणकाच्या सर्व वापरकर्त्यांसाठी स्थापित करू इच्छिता की नाही ते निवडा. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "हा संगणक वापरणाऱ्या प्रत्येकासाठी स्थापित करा"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "फक्त माझ्यासाठी स्थापित करा"
!endif
