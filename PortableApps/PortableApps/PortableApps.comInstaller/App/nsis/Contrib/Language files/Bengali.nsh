;Language: Bengali (1093)
;By John T. Haller (machine translated)

!insertmacro LANGFILE "Bengali" "Bengali" "বাংলা" "Bangla"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "$(^NameDA) সেটআপে স্বাগতম"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "$(^NameDA) এর ইনস্টলেশনের মাধ্যমে সেটআপ আপনাকে গাইড করবে।$\r$\n$\r$\nসেটআপ শুরু করার আগে আপনি অন্য সমস্ত অ্যাপ্লিকেশন বন্ধ করার পরামর্শ দেওয়া হচ্ছে। এটি আপনার কম্পিউটার রিবুট না করেই প্রাসঙ্গিক সিস্টেম ফাইল আপডেট করা সম্ভব করে তুলবে।$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "$(^NameDA) আনইনস্টল এ স্বাগতম"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "সেটআপ আপনাকে $(^NameDA) আনইনস্টল করার জন্য গাইড করবে।$\r$\n$\r$\nআনইন্সটল শুরু করার আগে, নিশ্চিত করুন যে $(^NameDA) চলছে না।$\r$\n$\r $\n$_ক্লিক করুন"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "লাইসেন্স চুক্তি"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "অনুগ্রহ করে $(^NameDA) ইনস্টল করার আগে লাইসেন্সের শর্তাবলী পর্যালোচনা করুন।"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন তবে চালিয়ে যেতে আমি রাজি ক্লিক করুন৷ $(^NameDA) ইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে।"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন, নীচের চেক বক্সে ক্লিক করুন৷ $(^NameDA) ইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে। $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন তবে নীচের প্রথম বিকল্পটি নির্বাচন করুন৷ $(^NameDA) ইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে। $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "লাইসেন্স চুক্তি"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "অনুগ্রহ করে $(^NameDA) আনইনস্টল করার আগে লাইসেন্সের শর্তাবলী পর্যালোচনা করুন।"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন তবে চালিয়ে যেতে আমি রাজি ক্লিক করুন৷ $(^NameDA) আনইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে।"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন, নীচের চেক বক্সে ক্লিক করুন৷ $(^NameDA) আনইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে। $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "আপনি যদি চুক্তির শর্তাদি স্বীকার করেন তবে নীচের প্রথম বিকল্পটি নির্বাচন করুন৷ $(^NameDA) আনইনস্টল করার জন্য আপনাকে অবশ্যই চুক্তিটি গ্রহণ করতে হবে। $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "চুক্তির বাকি অংশ দেখতে পেজ ডাউন টিপুন।"
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "উপাদান নির্বাচন করুন"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "আপনি $(^NameDA) এর কোন বৈশিষ্ট্যগুলি ইনস্টল করতে চান তা চয়ন করুন৷"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "উপাদান নির্বাচন করুন"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "আপনি $(^NameDA) এর কোন বৈশিষ্ট্যগুলি আনইনস্টল করতে চান তা চয়ন করুন৷"
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "বর্ণনা"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "একটি উপাদানের বর্ণনা দেখতে আপনার মাউসকে তার উপর রাখুন।"
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "তার বিবরণ দেখতে একটি উপাদান নির্বাচন করুন."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "ইনস্টল অবস্থান নির্বাচন করুন"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "যে ফোল্ডারে $(^NameDA) ইনস্টল করতে হবে সেটি বেছে নিন।"
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "আনইনস্টল অবস্থান নির্বাচন করুন"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "যে ফোল্ডার থেকে $(^NameDA) আনইনস্টল করতে হবে সেটি বেছে নিন।"
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "ইনস্টল করা হচ্ছে"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "অনুগ্রহ করে অপেক্ষা করুন যখন $(^NameDA) ইনস্টল করা হচ্ছে।"
  ${LangFileString} MUI_TEXT_FINISH_TITLE "ইনস্টলেশন সম্পূর্ণ"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "সেটআপ সফলভাবে সম্পন্ন হয়েছে।"
  ${LangFileString} MUI_TEXT_ABORT_TITLE "ইনস্টলেশন পরিত্যাগ"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "সেটআপ সফলভাবে সম্পন্ন করা হয় নি."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "আনইনস্টল হচ্ছে"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "অনুগ্রহ করে অপেক্ষা করুন যখন $(^NameDA) আনইনস্টল হচ্ছে।"
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "আনইনস্টলেশন সম্পূর্ণ"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "আনইনস্টল সফলভাবে সম্পন্ন হয়েছে."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "আনইনস্টলেশন বাতিল করা হয়েছে"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "আনইনস্টল সফলভাবে সম্পন্ন হয়নি।"
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) সেটআপ সম্পূর্ণ করা হচ্ছে"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) আপনার কম্পিউটারে ইনস্টল করা হয়েছে।$\r$\n$\r$\nসেটআপ বন্ধ করতে ফিনিস এ ক্লিক করুন।"
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) এর ইনস্টলেশন সম্পূর্ণ করার জন্য আপনার কম্পিউটার পুনরায় চালু করতে হবে। আপনি কি এখন রিবুট করতে চান?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) আনইনস্টল সম্পূর্ণ করা হচ্ছে"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) আপনার কম্পিউটার থেকে আনইনস্টল করা হয়েছে।$\r$\n$\r$\nসেটআপ বন্ধ করতে ফিনিস এ ক্লিক করুন।"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) আনইনস্টল সম্পূর্ণ করার জন্য আপনার কম্পিউটার পুনরায় চালু করতে হবে। আপনি কি এখন রিবুট করতে চান?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "রিবুট করো এখনি"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "আমি নিজে পরে পুনরায় চালু করতে চাই"
  ${LangFileString} MUI_TEXT_FINISH_RUN "$(^NameDA) চালান"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "রিডমি দেখান"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "শেষ করুন"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "স্টার্ট মেনু ফোল্ডার নির্বাচন করুন"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "$(^NameDA) শর্টকাটের জন্য একটি স্টার্ট মেনু ফোল্ডার বেছে নিন।"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "স্টার্ট মেনু ফোল্ডারটি নির্বাচন করুন যেখানে আপনি প্রোগ্রামের শর্টকাট তৈরি করতে চান। আপনি একটি নতুন ফোল্ডার তৈরি করতে একটি নাম লিখতে পারেন৷"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "সংক্ষিপ্ত করো না"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "আনইনস্টল $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "আপনার কম্পিউটার থেকে $(^NameDA) সরান।"
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "আপনি কি $(^Name) সেটআপ ছেড়ে দেওয়ার বিষয়ে নিশ্চিত?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "আপনি কি $(^Name) আনইনস্টল বন্ধ করার বিষয়ে নিশ্চিত?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "ব্যবহারকারী নির্বাচন করুন"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "আপনি কোন ব্যবহারকারীদের জন্য $(^NameDA) ইনস্টল করতে চান তা চয়ন করুন।"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "আপনি শুধুমাত্র নিজের জন্য বা এই কম্পিউটারের সমস্ত ব্যবহারকারীর জন্য $(^NameDA) ইনস্টল করতে চান কিনা তা নির্বাচন করুন৷ $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "এই কম্পিউটার ব্যবহার করে যে কারও জন্য ইনস্টল করুন"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "শুধু আমার জন্য ইনস্টল করুন"
!endif
