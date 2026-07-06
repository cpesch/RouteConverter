;Language: Burmese / Myanmar (1109)
;By John T. Haller (machin translation)

!insertmacro LANGFILE "Burmese" "Myanmar (Burmese)" "မြန်မာဘာသာ" "mranmabhasa"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "$(^NameDA) စနစ်ထည့်သွင်းခြင်းမှ ကြိုဆိုပါတယ်။"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "စနစ်ထည့်သွင်းမှုတွင် သင့်အား $(^NameDA) ၏ တပ်ဆင်မှုမှတစ်ဆင့် လမ်းညွှန်ပေးမည်ဖြစ်သည်။$\r$\n$\r$\nစနစ်ထည့်သွင်းခြင်းမစတင်မီ အခြားအပလီကေးရှင်းအားလုံးကို ပိတ်ရန် အကြံပြုအပ်ပါသည်။ ၎င်းသည် သင့်ကွန်ပျူတာကို ပြန်လည်စတင်ရန် မလိုအပ်ဘဲ သက်ဆိုင်ရာ စနစ်ဖိုင်များကို အပ်ဒိတ်လုပ်ရန် စွမ်းဆောင်နိုင်မည်ဖြစ်သည်။$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "$(^NameDA) uninstall မှကြိုဆိုပါတယ်။"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "စနစ်ထည့်သွင်းမှုသည် $(^NameDA) ကို ဖြုတ်ချခြင်းမှတစ်ဆင့် သင့်အား လမ်းညွှန်ပေးပါမည်။$\r$\n$\r$\nမတပ်ဆင်မီ $(^NameDA) မလည်ပတ်မီ သေချာပါစေ။$\r$\n$\r $\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "လိုင်စင်သဘောတူညီချက်"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "$(^NameDA) မထည့်သွင်းမီ လိုင်စင်စည်းကမ်းချက်များကို ပြန်လည်သုံးသပ်ပါ။"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက ဆက်လက်ဆောင်ရွက်ရန် I Agree ကိုနှိပ်ပါ။ $(^NameDA) ထည့်သွင်းရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက အောက်ပါ checkbox ကိုနှိပ်ပါ။ $(^NameDA) ထည့်သွင်းရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။ $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက၊ အောက်ဖော်ပြပါ ပထမရွေးချယ်မှုကို ရွေးချယ်ပါ။ $(^NameDA) ထည့်သွင်းရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။ $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "လိုင်စင်သဘောတူညီချက်"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "$(^NameDA) ကို မဖြုတ်မီ လိုင်စင် စည်းကမ်းချက်များကို ပြန်လည်သုံးသပ်ပါ။"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက ဆက်လက်ဆောင်ရွက်ရန် I Agree ကိုနှိပ်ပါ။ $(^NameDA) ကိုဖြုတ်ရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက အောက်ပါ checkbox ကိုနှိပ်ပါ။ $(^NameDA) ကိုဖြုတ်ရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။ $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "သဘောတူညီချက်၏ စည်းကမ်းချက်များကို သင်လက်ခံပါက၊ အောက်ဖော်ပြပါ ပထမရွေးချယ်မှုကို ရွေးချယ်ပါ။ $(^NameDA) ကိုဖြုတ်ရန် သဘောတူညီချက်ကို သင်လက်ခံရပါမည်။ $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "ကျန်သဘောတူညီချက်များကိုကြည့်ရှုရန် စာမျက်နှာအောက်သို့ နှိပ်ပါ။"
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "အစိတ်အပိုင်းများကို ရွေးပါ။"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "သင်ထည့်သွင်းလိုသော $(^NameDA) ၏ အင်္ဂါရပ်များကို ရွေးပါ။"
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "အစိတ်အပိုင်းများကို ရွေးပါ။"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "သင်ဖြုတ်လိုသော $(^NameDA) ၏ အင်္ဂါရပ်များကို ရွေးပါ။"
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "ဖော်ပြချက်"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "၎င်း၏ဖော်ပြချက်ကိုကြည့်ရန် သင့်မောက်စ်ကို အစိတ်အပိုင်းတစ်ခုပေါ်တွင် နေရာချထားပါ။"
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "၎င်း၏ဖော်ပြချက်ကိုကြည့်ရန် အစိတ်အပိုင်းတစ်ခုကို ရွေးပါ။"
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "တပ်ဆင်တည်နေရာကိုရွေးချယ်ပါ။"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "$(^NameDA) ထည့်သွင်းရန် ဖိုင်တွဲကို ရွေးပါ။"
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "ဖယ်ရှားရန်တည်နေရာကို ရွေးပါ။"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "$(^NameDA) ကို ဖြုတ်ရန် ဖိုင်တွဲကို ရွေးပါ။"
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "တပ်ဆင်ခြင်း။"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "$(^NameDA) ကို ထည့်သွင်းနေချိန်တွင် စောင့်ပါ။"
  ${LangFileString} MUI_TEXT_FINISH_TITLE "တပ်ဆင်မှု ပြီးပါပြီ။"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "စနစ်ထည့်သွင်းမှု အောင်မြင်စွာပြီးမြောက်ခဲ့သည်။"
  ${LangFileString} MUI_TEXT_ABORT_TITLE "တပ်ဆင်မှုကို ဖျက်သိမ်းထားသည်။"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "စနစ်ထည့်သွင်းမှု မပြီးမြောက်ခဲ့ပါ။"
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "ဖြုတ်ခြင်း။"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "$(^NameDA) ကို ဖြုတ်နေချိန် စောင့်ပါ။"
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Uninstallation အပြီးသတ်"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Uninstall ကို အောင်မြင်စွာ ပြီးမြောက်ခဲ့သည်။"
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "ဖြုတ်ချခြင်းကို ဖျက်သိမ်းထားသည်။"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Uninstall ကို အောင်မြင်စွာ မပြီးမြောက်ခဲ့ပါ။"
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) စနစ်ထည့်သွင်းမှုကို ပြီးအောင်လုပ်နေသည်။"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) ကို သင့်ကွန်ပြူတာတွင် ထည့်သွင်းပြီးပါပြီ။$\r$\n$\r$\nတပ်ဆင်မှုကို ပိတ်ရန် အပြီးသတ်ကို နှိပ်ပါ။"
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) တပ်ဆင်မှုကို အပြီးသတ်ရန်အတွက် သင့်ကွန်ပျူတာကို ပြန်လည်စတင်ရပါမည်။ ယခု ပြန်လည်စတင်လိုပါသလား။"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) ကို အပြီးအစီး ဖြုတ်ချခြင်း"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) ကို သင့်ကွန်ပြူတာမှ ဖယ်ရှားလိုက်ပါပြီ။$\r$\n$\r$\nတပ်ဆင်မှုကို ပိတ်ရန် အပြီးသတ်ကို နှိပ်ပါ။"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) အား ဖြုတ်ချခြင်းကို အပြီးသတ်ရန်အတွက် သင့်ကွန်ပျူတာကို ပြန်လည်စတင်ရပါမည်။ ယခု ပြန်လည်စတင်လိုပါသလား။"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "ယခု ပြန်ဖွင့်ပါ။"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "ကျွန်ုပ်သည် နောက်ပိုင်းတွင် ကိုယ်တိုင် ပြန်လည်စတင်လိုပါသည်။"
  ${LangFileString} MUI_TEXT_FINISH_RUN "$(^NameDA) ကိုဖွင့်ပါ"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "ဖတ်ပြပါ။"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "ပြီးအောင်"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "စတင်မီနူးဖိုင်တွဲကို ရွေးပါ။"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "$(^NameDA) ဖြတ်လမ်းလင့်ခ်များအတွက် စတင်မီနူးဖိုင်တွဲကို ရွေးပါ။"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "ပရိုဂရမ်၏ ဖြတ်လမ်းလင့်ခ်များကို သင်ဖန်တီးလိုသည့် စတင်မီနူးဖိုင်တွဲကို ရွေးချယ်ပါ။ ဖိုင်တွဲအသစ်တစ်ခုဖန်တီးရန် အမည်တစ်ခုကိုလည်း ထည့်သွင်းနိုင်သည်။"
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "ဖြတ်လမ်းများ မဖန်တီးပါနှင့်"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "$(^NameDA) ကို ဖြုတ်ပါ"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "သင့်ကွန်ပျူတာမှ $(^NameDA) ကို ဖယ်ရှားပါ။"
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "$(^Name) စနစ်ထည့်သွင်းခြင်းမှ ထွက်လိုသည်မှာ သေချာပါသလား။"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "$(^Name) Uninstall မှထွက်လိုသည်မှာ သေချာပါသလား။"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "အသုံးပြုသူများကို ရွေးချယ်ပါ။"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "$(^NameDA) ထည့်သွင်းလိုသော အသုံးပြုသူများကို ရွေးချယ်ပါ။"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "သင်ကိုယ်တိုင် $(^NameDA) ကို သင်ကိုယ်တိုင် သို့မဟုတ် ဤကွန်ပြူတာ၏ အသုံးပြုသူအားလုံးအတွက်သာ တပ်ဆင်လိုသည်ဖြစ်စေ ရွေးချယ်ပါ။ $(^Next ကိုနှိပ်ပါ)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "ဤကွန်ပြူတာအသုံးပြုသူတိုင်းအတွက် ထည့်သွင်းပါ။"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "ကျွန်ုပ်အတွက်သာ ထည့်သွင်းပါ။"
!endif
