Var CustomCodeFound
Var CustomCodeUseTempNSI

Function CustomTrim
	Exch $R1 ; Original string
	Push $R2
 
CustomLoop:
	StrCpy $R2 "$R1" 1
	StrCmp "$R2" " " CustomTrimLeft
	StrCmp "$R2" "$\r" CustomTrimLeft
	StrCmp "$R2" "$\n" CustomTrimLeft
	StrCmp "$R2" "$\t" CustomTrimLeft
	GoTo CustomLoop2
CustomTrimLeft:	
	StrCpy $R1 "$R1" "" 1
	Goto CustomLoop
 
CustomLoop2:
	StrCpy $R2 "$R1" 1 -1
	StrCmp "$R2" " " CustomTrimRight
	StrCmp "$R2" "$\r" CustomTrimRight
	StrCmp "$R2" "$\n" CustomTrimRight
	StrCmp "$R2" "$\t" CustomTrimRight
	GoTo CustomDone
CustomTrimRight:	
	StrCpy $R1 "$R1" -1
	Goto CustomLoop2
 
CustomDone:
	Pop $R2
	Exch $R1
FunctionEnd

 
!define CustomTrim "!insertmacro CustomTrim"
 
!macro CustomTrim ResultVar String
  Push "${String}"
  Call CustomTrim
  Pop "${ResultVar}"
!macroend

Function CustomCodeFindUnicode
    ${TrimNewLines} '$R9' $R9
    ;DetailPrint "test for line $R8 `$R9`"
	${CustomTrim} $R9 $R9
    ${If} $R9 == "unicode false"
	${OrIf} $R9 == "unicode true"
        StrCpy $CustomCodeFound 1         ;set flag
        Push "StopLineFind"     ;stop find
    ${else}
        Push 0                  ;ignore -> continue
    ${endIf}
FunctionEnd

${SegmentFile}

${SegmentPre}
	${GetParameters} $0
	
	${If} $0 != ""
		;File or something else passed
		;Remove quotes and leading/trailing spaces if present
		${WordReplace} "$0" '"' "" "+" $0
		${CustomTrim} $0 $0
		
		${If} ${FileExists} $0
			;File was passed
			StrCpy $CustomCodeFound 0
			${LineFind} $0 "/NUL" "1:-1" "CustomCodeFindUnicode"
			
			StrCpy $1 $CustomCodeFound
			
			${If} $1 == 0
				;Unicode not set in passed file
				
				${GetFileName} "$0" $1
				${If} $1 == "PortableApps.comLauncher.nsi"
					StrCpy $9 true
					StrCpy $8 true
					StrCpy $7 false
					Goto CustomCodeChangeScriptChoiceDone
				${EndIf}
				
				MessageBox MB_YESNO|MB_ICONQUESTION "The script you are compiling does not specify whether it should be compiled in Unicode or ANSI mode.  It will be compiled in ANSI/Win9x mode by default.  Would you like to adjust the script before compiling?$\r$\n$\r$\nNote: You'll be given an option whether to save the changes later or use a temporary file." IDYES CustomCodeAdjust IDNO CustomCodeNoAdjust
					
				CustomCodeAdjust:

				MessageBox MB_YESNO|MB_ICONQUESTION "Would you like to compile this script in Unicode mode?$\r$\n$\r$\nDetails: Unicode works better in multiple languages on Windows 2000 and up.  ANSI is only recommended for Windows 9x. For scripts previously used with Unicode-NSIS, Unicode is recommended. For scripts previously used with NSIS 2.x, ANSI is recommended unless no plugins are used and no ANSI calls are made to the Windows API." IDYES CustomCodeUseUnicode IDNO CustomCodeUseANSI
				
				CustomCodeUseUnicode:
					StrCpy $9 true
					Goto CustomCodeUnicodeChoiceDone
					
				CustomCodeUseANSI:
					StrCpy $9 false
					Goto CustomCodeUnicodeChoiceDone
					
				CustomCodeUnicodeChoiceDone:
				
				MessageBox MB_YESNO|MB_ICONQUESTION "The script you are using doesn't specify whether high resolution screen support should be enabled (high DPI mode on Windows 10). Would you like to enable high resolution support?$\r$\n$\r$\nNote: High resolution support is recommended as long as no custom pages are included in the installer with positioning specified in pixels.  Custom pages with positioning specified with form units or percentages should work correctly.  It is recommended that you test your installer on a high resolution Windows 10 machine at various scaling settings before distribution." IDYES CustomCodeUseHighDPI IDNO CustomCodeNoHighDPI
				
				CustomCodeUseHighDPI:
					StrCpy $8 true
					Goto CustomCodeHighDPIChoiceDone
					
				CustomCodeNoHighDPI:
					StrCpy $8 false
					Goto CustomCodeHighDPIChoiceDone
					
				CustomCodeHighDPIChoiceDone:
				
				MessageBox MB_YESNO|MB_ICONQUESTION "Would you like to save your Unicode/ANSI and DPI to your original script?$\r$\n$\r$\nNote: The changes will be made to the beginning of your script inserting 4 lines between the 1st and 2nd line. This may cause issues with a small number of scripts and need to be altered manually. Your original script will be preserved with a -BeforeUpdateToNSIS3 suffix." IDYES CustomCodeChangeScript IDNO CustomCodeNoChangeScript
				
				CustomCodeChangeScript:
					StrCpy $7 true
					Goto CustomCodeChangeScriptChoiceDone
					
				CustomCodeNoChangeScript:
					StrCpy $7 false
					Goto CustomCodeChangeScriptChoiceDone
					
				CustomCodeChangeScriptChoiceDone:
				
				${GetFileName} "$0" $1
				${GetParent} "$0" $2
				Delete "$2\$1-UpdatedToNSIS3.nsi"
				
				;Copy the contents to a new file with the selections
				FileOpen $4 $0 r
				FileOpen $5 "$2\$1-UpdatedToNSIS3.nsi" w
				
				;Copy the first line
				ClearErrors
				FileRead $4 $6
				IfErrors CustomCodeLoopExit
				FileWrite $5 $6
				
				;Additions to the file
				FileWrite $5 ";BEGIN: Added by NSIS Portable$\r$\n"
				${If} $9 == true
					FileWrite $5 "Unicode true$\r$\n"
				${Else}
					FileWrite $5 "Unicode false$\r$\n"
				${EndIf}
				${If} $8 == true
					FileWrite $5 "ManifestDPIAware true$\r$\n"
				${Else}
					FileWrite $5 "ManifestDPIAware false$\r$\n"
				${EndIf}
				FileWrite $5 ";END: Added by NSIS Portable$\r$\n"
				
				;Loop through and add remaining lines
				CustomCodeLoop:
					ClearErrors
					FileRead $4 $6
					IfErrors CustomCodeLoopExit
					FileWrite $5 $6
					Goto CustomCodeLoop
				CustomCodeLoopExit:
				FileClose $5
				FileClose $4
				
				${If} $7 == true
					;Rename to name of old file
					Rename $0 "$2\$1-BeforeUpdateToNSIS3.nsi"
					Delete $0
					Rename "$2\$1-UpdatedToNSIS3.nsi" $0
				${Else}
					;Update execstring to name of updated file
					StrCpy $CustomCodeUseTempNSI true
					${WordReplace} $ExecString ".nsi" ".nsi-UpdatedToNSIS3.nsi" "+" $ExecString
				${EndIf}
				
				CustomCodeNoAdjust:
			${EndIf}
		${EndIf}
	${EndIf}
!macroend

${SegmentPreExec}
	${If} $CustomCodeUseTempNSI == true
		${WordReplace} $ExecString ".nsi" "-UpdatedToNSIS3.nsi" "+" $ExecString
	${EndIf}
!macroend

${SegmentPost}
	${GetParameters} $0
	;Remove quotes and leading/trailing spaces if present
	${WordReplace} "$0" '"' "" "+" $0
	${CustomTrim} $0 $0
	
	${If} ${FileExists} $0
		Delete "$0-UpdatedToNSIS3.nsi"
	${EndIf}
!macroend
