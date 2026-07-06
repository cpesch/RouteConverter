;Language: Amharic (1118)
;By John T Haller (machine generated)

!insertmacro LANGFILE "Amharic" = "አማርኛ" "Amarenna" ; See \Include\LangFile.nsh for a description of these parameters

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "እንኳን ወደ $(^NameDA) ማዋቀር በደህና መጡ"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "ማዋቀር በ$(^NameDA) ጭነት ውስጥ ይመራዎታል።$\r$\n$\r$\nማዋቀር ከመጀመርዎ በፊት ሁሉንም ሌሎች መተግበሪያዎችን እንዲዘጉ ይመከራል። ይህ ኮምፒተርዎን እንደገና ማስጀመር ሳያስፈልግ ተዛማጅ የሆኑ የስርዓት ፋይሎችን ማዘመን ያስችላል።$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "እንኳን ወደ $(^NameDA) ማራገፍ በደህና መጡ"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "ማዋቀር በ$(^NameDA) ማራገፊያ ይመራዎታል።$\r$\n$\r$\nማራገፉን ከመጀመርዎ በፊት $(^NameDA) እየሰራ አለመሆኑን ያረጋግጡ።$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "የፈቃድ ስምምነት"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "እባክዎ $(^NameDA) ከመጫንዎ በፊት የፍቃድ ውሉን ይከልሱ።"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "የስምምነቱን ውሎች ከተቀበሉ፣ ለመቀጠል እስማማለሁ የሚለውን ጠቅ ያድርጉ። $(^NameDA) ለመጫን ስምምነቱን መቀበል አለብህ።"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "የስምምነቱን ውሎች ከተቀበሉ, ከታች ያለውን አመልካች ሳጥን ጠቅ ያድርጉ. $(^NameDA) ለመጫን ስምምነቱን መቀበል አለብህ። $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "የስምምነቱን ውሎች ከተቀበሉ, ከታች ያለውን የመጀመሪያውን አማራጭ ይምረጡ. $(^NameDA) ለመጫን ስምምነቱን መቀበል አለብህ። $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "የፈቃድ ስምምነት"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "እባክዎ $(^NameDA) ከማራገፍዎ በፊት የፍቃድ ውሉን ይከልሱ።"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "የስምምነቱን ውሎች ከተቀበሉ፣ ለመቀጠል እስማማለሁ የሚለውን ጠቅ ያድርጉ። $(^NameDA)ን ለማራገፍ ስምምነቱን መቀበል አለብህ።"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "የስምምነቱን ውሎች ከተቀበሉ, ከታች ያለውን አመልካች ሳጥን ጠቅ ያድርጉ. $(^NameDA)ን ለማራገፍ ስምምነቱን መቀበል አለብህ። $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "የስምምነቱን ውሎች ከተቀበሉ, ከታች ያለውን የመጀመሪያውን አማራጭ ይምረጡ. $(^NameDA)ን ለማራገፍ ስምምነቱን መቀበል አለብህ። $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "የቀረውን ስምምነቱን ለማየት ገጹን ወደታች ይጫኑ።"
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "ክፍሎችን ይምረጡ"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "የትኛውን የ$(^NameDA) መጫን እንደሚፈልጉ ይምረጡ።"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "ክፍሎችን ይምረጡ"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "የትኛውን የ$(^NameDA) ማራገፍ እንደሚፈልጉ ይምረጡ።"
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "መግለጫ"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "መግለጫውን ለማየት መዳፊትዎን በአንድ አካል ላይ ያስቀምጡት።"
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "መግለጫውን ለማየት አንድ አካል ይምረጡ።"
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "የመጫኛ ቦታን ይምረጡ"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "$(^NameDA) የሚጭኑበት አቃፊ ይምረጡ።"
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "የማራገፍ አካባቢን ይምረጡ"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "$(^NameDA) የሚያራግፍበትን አቃፊ ይምረጡ።"
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "በመጫን ላይ"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "እባክዎ $(^NameDA) ሲጫን ይጠብቁ።"
  ${LangFileString} MUI_TEXT_FINISH_TITLE "መጫኑ ተጠናቋል"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "ማዋቀር በተሳካ ሁኔታ ተጠናቅቋል።"
  ${LangFileString} MUI_TEXT_ABORT_TITLE "መጫኑ ተቋርጧል"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "ማዋቀር በተሳካ ሁኔታ አልተጠናቀቀም።"
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "በማራገፍ ላይ"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "እባክዎ $(^NameDA) ሲራገፍ ይጠብቁ።"
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "ማራገፍ ተጠናቅቋል"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "ማራገፍ በተሳካ ሁኔታ ተጠናቅቋል።"
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "ማራገፍ ተቋርጧል"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "ማራገፍ በተሳካ ሁኔታ አልተጠናቀቀም።"
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "የ$(^NameDA) ማዋቀርን በማጠናቀቅ ላይ"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) በኮምፒውተርዎ ላይ ተጭኗል።$\r$\n$\r$\nማዋቀርን ለመዝጋት ጨርስን ይንኩ።"
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "የ$(^NameDA) መጫኑን ለማጠናቀቅ ኮምፒውተርዎ እንደገና መጀመር አለበት። አሁን ዳግም ማስጀመር ይፈልጋሉ?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "የ$(^NameDA) ማራገፍን በማጠናቀቅ ላይ"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) ከኮምፒዩተርዎ ተራግፏል።$\r$\n$\r$\nማዋቀርን ለመዝጋት ጨርስን ጠቅ ያድርጉ።"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "የ$(^NameDA) ማራገፉን ለማጠናቀቅ ኮምፒውተርዎ እንደገና መጀመር አለበት። አሁን ዳግም ማስጀመር ይፈልጋሉ?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "አሁን እንደገና አስጀምር"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "በኋላ ላይ ራሴን ዳግም ማስጀመር እፈልጋለሁ"
  ${LangFileString} MUI_TEXT_FINISH_RUN "ጀምር $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "Readme አሳይ"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "ጨርስ"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "የጀምር ምናሌ አቃፊን ይምረጡ"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "ለ$(^NameDA) አቋራጮች የጀምር ሜኑ አቃፊን ይምረጡ።"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "የፕሮግራሙን አቋራጮች ለመፍጠር የሚፈልጉትን የጀምር ሜኑ አቃፊ ይምረጡ። አዲስ አቃፊ ለመፍጠር ስም ማስገባትም ይችላሉ።"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "አቋራጮችን አትፍጠር"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "$(^NameDA) አራግፍ"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "$(^NameDA)ን ከኮምፒውተርህ አስወግድ።"
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "እርግጠኛ ነህ የ$(^Name) ማዋቀርን ማቋረጥ ትፈልጋለህ?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "እርግጠኛ ነዎት $(^Name) ማራገፍን ማቆም ይፈልጋሉ?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "ተጠቃሚዎችን ይምረጡ"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "የትኛዎቹ ተጠቃሚዎች $(^NameDA) መጫን እንደሚፈልጉ ይምረጡ።"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "ለራስህ ወይም ለሁሉም የዚህ ኮምፒውተር ተጠቃሚዎች $(^NameDA) መጫን ትፈልግ እንደሆነ ምረጥ። $(^ቀጥልን ጠቅ ያድርጉ)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "ይህን ኮምፒውተር ለሚጠቀም ለማንኛውም ሰው ጫን"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "ለእኔ ብቻ ጫን"
!endif
