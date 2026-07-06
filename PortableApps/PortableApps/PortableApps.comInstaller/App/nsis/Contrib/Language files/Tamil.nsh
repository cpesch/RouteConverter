;Language: Tamil (1097)
;By Dr.T.Vasudevan <agnihot3@gmail.com>

!insertmacro LANGFILE "Tamil" = "Tamil" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "$(^NameDA) நிறுவல் வழிகாட்டிக்கு நல்வரவு"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "இந்த வழிகாட்டி உங்களை $(^NameDA)இன் நிறுவலுக்கு இட்டுச்செல்லும்.$\r$\n$\r$\nஇதைத் துவக்குமுன் நீங்கள் மற்ற நிரல்களை மூடும் படி  பரிந்துரைக்கப்படுகிறது. இதனால் உங்கள் கணினி அமைப்பு கோப்புகள் கணினி மறுதுவக்கத்துக்கு தேவையில்லாமல் நிறுவப்படும். $\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "$(^NameDA) நிறுவல் நீக்க வழிகாட்டிக்கு நல்வரவு"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "இந்த வழிகாட்டி உங்களை $(^NameDA)இன் நிறுவல் நீக்கத்துக்கு இட்டுச்செல்லும்.$\r$\n$\r$\nநிறுவல் நீக்கத்தை துவக்கு முன் $(^NameDA) இயங்கவில்லை என்பதை உறுதி படுத்திக்கொள்க.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "உரிம ஒப்பந்தம்"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "$(^NameDA) ஐ நிறுவு முன் உரிம ஒப்பந்தத்தை மறு பார்வை இடுக."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், தொடர சம்மதம் ஐ சொடுக்குக. $(^NameDA) ஐ நிறுவ நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும்."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், தொடர கீழ் உள்ள பெட்டியை சொடுக்கவும். $(^NameDA) ஐ நிறுவ நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும். $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், கீழே முதல் தேர்வை தேர்ந்தெடுக்கவும்.$(^NameDA) ஐ நிறுவ நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும். $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "உரிம ஒப்பந்தம்"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "$(^NameDA) ஐ நிறுவல் நீக்கு முன் உரிம ஒப்பந்தத்தை மறு பார்வை இடுக."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், தொடர சம்மதம் ஐ சொடுக்குக. $(^NameDA) ஐ நிறுவல் நீக்க நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும்."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், தொடர கீழ் உள்ள பெட்டியை சொடுக்கவும். $(^NameDA) ஐ நிறுவல் நீக்க நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும்."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "நீங்கள் எல்லா ஷரத்துக்களையும் ஒத்துக்கொண்டால், கீழே முதல் தேர்வை தேர்ந்தெடுக்கவும்.$(^NameDA) ஐ நிறுவல் நீக்க நீங்கள் எல்லா ஷரத்துக்களையும் ஒப்புக்கொள்ளவேண்டும். $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "மீதி ஒப்பந்தத்தை காண பக்கம் கீழே ஐ சொடுக்குக"
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "கூறுகளை தேர்ந்தெடுக்கவும்"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "$(^NameDA) இன் எந்த கூறுகளை நிறுவ விரும்புகிறீர்கள் என்று தேர்ந்தெடுக்கவும்"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "கூறுகளை தேர்ந்தெடுக்கவும்"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "$(^NameDA) இன் எந்த கூறுகளை நிறுவல் நீக்க விரும்புகிறீர்கள் என்று தேர்ந்தெடுக்கவும்"
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "விவரம்"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "ஒரு கூறின் விவரத்தை காண அதன் மீது சொடுக்கியை நிறுத்தவும்."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "ஒரு கூறின் விவரத்தை காண அதன் மீது சொடுக்கியை நிறுத்தவும்."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "நிறுவல் இடத்தை தேர்ந்தெடுக்கவும்."
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "$(^NameDA) ஐ எந்த அடைவில் நிறுவ வேண்டும் என தேர்ந்தெடுங்கள்."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "நிறுவல் நீக்க இடத்தை தேர்ந்தெடுக்கவும்."
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "$(^NameDA) ஐ எந்த அடைவில் நிறுவல் நீக்க வேண்டும் என தேர்ந்தெடுங்கள்."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "நிறுவுகிறது"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "$(^NameDA) நிறுவப்படுகிறது. தயை செய்து காத்திருக்கவும்."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "நிறுவல் நிறைவுற்றது."
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "அமைத்தல் வெற்றிகரமாக முடிந்தது."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "நிறுவல் கைவிடப்பட்டது"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "அமைத்தல் வெற்றிகரமாக முடிக்கப்படவில்லை."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "நிறுவல் நீக்குகிறது"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "$(^NameDA) நிறுவல் நீக்கப்படுகிறது. தயை செய்து காத்திருக்கவும்."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "நிறுவல் நீக்கம் நிறைவுற்றது"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "நிறுவல் நீக்கம் வெற்றிகரமாக முடிந்தது."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "நிறுவல் நீக்கம் கைவிடப்பட்டது"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "நிறுவல் நீக்கம் வெற்றிகரமாக முடிக்கப்படவில்லை."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) அமைப்பு வழிகாட்டி முடிவுக்கு வருகிறது"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) உங்கள் கணினியில் நிறுவப்பட்டு விட்டது.$\r$\n$\r$\nமுடி ஐ சொடுக்கி இந்த வழிகாட்டியை மூடவும்."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) இன் நிறுவலை பூர்த்தி செய்ய உங்கள் கணினி மறு துவக்கம் செய்யப்பட வேண்டும். நீங்கள் இப்போது மறுதுவக்கம் செய்ய விரும்புகிறீர்களா?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) நிறுவல் நீக்க வழிகாட்டி முடிவுக்கு வருகிறது"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) உங்கள் கணினியில் இருந்து நீக்கப்பட்டு விட்டது.$\r$\n$\r$\nமுடி ஐ சொடுக்கி இந்த வழிகாட்டியை மூடவும்."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) இன் நிறுவல் நீக்கத்தை பூர்த்தி செய்ய உங்கள் கணினி மறு துவக்கம் செய்யப்பட வேண்டும். நீங்கள் இப்போது மறுதுவக்கம் செய்ய விரும்புகிறீர்களா?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "இப்போது மறுதுவக்கம் செய்க"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "கைமுறையாக நான் பின்னால் மறுதுவக்கம் செய்து கொள்கிறேன்."
  ${LangFileString} MUI_TEXT_FINISH_RUN "&R $(^NameDA) ஐ இயக்கு"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&S என்னப்பார் ஐ காட்டுக"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&F முடி"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "துவக்க பட்டியல் அடைவை தேர்ந்தெடு"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "$(^NameDA) இன் சுருக்கு வழிகளுக்கு ஒரு துவக்க பட்டியல் அடைவை தேர்ந்தெடு."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "நிரலின் சுருக்கு வழிகளை உருவாக்க ஒரு துவக்க பட்டியல் அடைவை தேர்ந்தெடுக்கவும். நீங்கள் ஒரு புதிய அடைவை உருவாக்க ஒரு பெயரையும் உள்ளிடலாம்."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "சுருக்கு வழிகளை உருவாக்காதே"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "$(^NameDA) ஐ நிறுவல் நீக்கவும்"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "உங்கள் கணினியில் இருந்து $(^NameDA)  ஐ நீக்க."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "நீங்கள் நிச்சயம் $(^Name) இன் அமைப்பை கைவிட விரும்புகிறீர்களா?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "நீங்கள் நிச்சயம் $(^Name) இன் நிறுவல் நீக்கத்தை கைவிட விரும்புகிறீர்களா?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "பயனர்களை தேர்ந்தெடுக்கவும்"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "$(^NameDA) ஐ பயன்படுத்தும் பயனர்கள் யார் என தேர்ந்தெடுக்கவும்."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "$(^NameDA) ஐ உங்களுக்கு மட்டும் நிறுவுகிறீர்களா அல்லது இந்த கணினியின் எல்லா பயனர்களுக்கும் பயன்படுத்த நிறுவுகிறீர்களா. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "இந்த கணினியின் எந்த பயனரும் பயன்படுத்த நிறுவுக"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "எனக்கு மட்டும் நிறுவுக"
!endif
