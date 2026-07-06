;Language: Urdu (1056)
;By John T. Haller (machine translated)

!insertmacro LANGFILE "Urdu" = "اُردُو" "Urdu" ; See \Include\LangFile.nsh for a description of these parameters

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "$(^NameDA) سیٹ اپ میں خوش آمدید"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "سیٹ اپ $(^NameDA) کی انسٹالیشن میں آپ کی رہنمائی کرے گا۔$\r$\n$\r$\nیہ تجویز کی جاتی ہے کہ آپ سیٹ اپ شروع کرنے سے پہلے تمام دیگر ایپلیکیشنز کو بند کر دیں۔ یہ آپ کے کمپیوٹر کو ریبوٹ کیے بغیر متعلقہ سسٹم فائلوں کو اپ ڈیٹ کرنا ممکن بنائے گا۔$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "$(^NameDA) ان انسٹال میں خوش آمدید"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "سیٹ اپ $(^NameDA) کی ان انسٹالیشن میں آپ کی رہنمائی کرے گا۔$\r$\n$\r$\nان انسٹالیشن شروع کرنے سے پہلے یقینی بنائیں کہ $(^NameDA) نہیں چل رہا ہے۔$\r$\n$\r $\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "لائسنس کا معاہدہ"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "براہ کرم $(^NameDA) کو انسٹال کرنے سے پہلے لائسنس کی شرائط کا جائزہ لیں۔"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں تو جاری رکھنے کے لیے میں متفق ہوں پر کلک کریں۔ آپ کو $(^NameDA) کو انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں تو نیچے دیے گئے چیک باکس پر کلک کریں۔ آپ کو $(^NameDA) کو انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔ $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں، تو ذیل میں پہلا آپشن منتخب کریں۔ آپ کو $(^NameDA) کو انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔ $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "لائسنس کا معاہدہ"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "براہ کرم $(^NameDA) کو ان انسٹال کرنے سے پہلے لائسنس کی شرائط کا جائزہ لیں۔"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں تو جاری رکھنے کے لیے میں متفق ہوں پر کلک کریں۔ آپ کو $(^NameDA) کو ان انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں تو نیچے دیے گئے چیک باکس پر کلک کریں۔ آپ کو $(^NameDA) کو ان انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔ $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "اگر آپ معاہدے کی شرائط کو قبول کرتے ہیں، تو ذیل میں پہلا آپشن منتخب کریں۔ آپ کو $(^NameDA) کو ان انسٹال کرنے کا معاہدہ قبول کرنا ہوگا۔ $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "باقی معاہدے کو دیکھنے کے لیے صفحہ نیچے دبائیں۔"
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "اجزاء کا انتخاب کریں۔"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "منتخب کریں کہ آپ $(^NameDA) کی کون سی خصوصیات انسٹال کرنا چاہتے ہیں۔"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "اجزاء کا انتخاب کریں۔"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "منتخب کریں کہ آپ $(^NameDA) کی کون سی خصوصیات اَن انسٹال کرنا چاہتے ہیں۔"
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "تفصیل"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "اس کی تفصیل دیکھنے کے لیے اپنے ماؤس کو کسی جزو کے اوپر رکھیں۔"
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "اس کی تفصیل دیکھنے کے لیے ایک جز کو منتخب کریں۔"
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "انسٹال مقام کا انتخاب کریں۔"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "وہ فولڈر منتخب کریں جس میں $(^NameDA) انسٹال کرنا ہے۔"
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "ان انسٹال لوکیشن کا انتخاب کریں۔"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "وہ فولڈر منتخب کریں جہاں سے $(^NameDA) کو ان انسٹال کرنا ہے۔"
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "انسٹال کرنا"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "براہ کرم انتظار کریں جب تک $(^NameDA) انسٹال ہو رہا ہو۔"
  ${LangFileString} MUI_TEXT_FINISH_TITLE "تنصیب مکمل"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "سیٹ اپ کامیابی سے مکمل ہو گیا۔"
  ${LangFileString} MUI_TEXT_ABORT_TITLE "تنصیب ختم کر دی گئی۔"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "سیٹ اپ کامیابی سے مکمل نہیں ہوا۔"
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "ان انسٹال کرنا"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "براہ کرم انتظار کریں جب تک $(^NameDA) کو اَن انسٹال کیا جا رہا ہو۔"
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "ان انسٹالیشن مکمل"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "ان انسٹال کامیابی کے ساتھ مکمل ہو گیا۔"
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "ان انسٹالیشن روک دی گئی۔"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "اَن انسٹال کامیابی سے مکمل نہیں ہوا۔"
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) سیٹ اپ مکمل ہو رہا ہے۔"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) آپ کے کمپیوٹر پر انسٹال ہو گیا ہے۔$\r$\n$\r$\nسیٹ اپ کو بند کرنے کے لیے Finish پر کلک کریں۔"
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) کی تنصیب مکمل کرنے کے لیے آپ کے کمپیوٹر کو دوبارہ شروع کرنا ضروری ہے۔ کیا آپ ابھی ریبوٹ کرنا چاہتے ہیں؟"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) اَن انسٹال مکمل ہو رہا ہے۔"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) کو آپ کے کمپیوٹر سے ان انسٹال کر دیا گیا ہے۔$\r$\n$\r$\nسیٹ اپ کو بند کرنے کے لیے ختم پر کلک کریں"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) کی ان انسٹالیشن کو مکمل کرنے کے لیے آپ کے کمپیوٹر کو دوبارہ شروع کرنا ضروری ہے۔ کیا آپ ابھی ریبوٹ کرنا چاہتے ہیں؟"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "اب دوبارہ شروع"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "میں بعد میں دستی طور پر ریبوٹ کرنا چاہتا ہوں۔"
  ${LangFileString} MUI_TEXT_FINISH_RUN "چلائیں $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "ریڈمی دکھائیں۔"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "ختم کرنا"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "اسٹارٹ مینو فولڈر کا انتخاب کریں۔"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "$(^NameDA) شارٹ کٹس کے لیے اسٹارٹ مینو فولڈر کا انتخاب کریں۔"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "اسٹارٹ مینو فولڈر کو منتخب کریں جس میں آپ پروگرام کے شارٹ کٹس بنانا چاہیں گے۔ آپ نیا فولڈر بنانے کے لیے نام بھی درج کر سکتے ہیں۔"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "شارٹکٹ مت بنائیں"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "ان انسٹال کریں $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "اپنے کمپیوٹر سے $(^NameDA) کو ہٹا دیں۔"
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "کیا آپ واقعی $(^Name) سیٹ اپ چھوڑنا چاہتے ہیں؟"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "کیا آپ واقعی $(^Name) ان انسٹال کو چھوڑنا چاہتے ہیں؟"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "صارفین کو منتخب کریں۔"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "منتخب کریں کہ آپ کن صارفین کے لیے $(^NameDA) انسٹال کرنا چاہتے ہیں۔"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "منتخب کریں کہ آیا آپ $(^NameDA) صرف اپنے لیے یا اس کمپیوٹر کے تمام صارفین کے لیے انسٹال کرنا چاہتے ہیں۔ $(^ اگلا کلک کریں)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "اس کمپیوٹر پر کسی کے بھی استعمال کے لیے انسٹال کریں"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Install just for me"
!endif
