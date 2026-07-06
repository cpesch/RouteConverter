# pttb - Pin To TaskBar


Pin To TaskBar for command line:

  - Minimal reverse engineering of syspin.exe from https://www.technosys.net/products/utils/pintotaskbar
  - With only "Pin to taskbar" and "Unpin from taskbar" functionalities included
  - By default, pttb force Re-Pin any .exe/.lnk that is already pinned, but as a result: the programs gets re-pinned in last position
  - Tested on Windows 10 Pro 64bit - Version 20H2 / build 19042.964 / locale en-US
  - Syspin.exe was decompiled using Retargetable Decompiler from https://retdec.com
  - Another helpful reverse engineering project of syspin.exe in C++ (much more faithful to the source) : https://github.com/airwolf2026/Win10Pin2TB


Compiled with MSYS2/MinGW-w64:

	$ gcc -o pttb pttb.c -Lmingw64/x86_64-w64-mingw32/lib -lole32 -loleaut32 -luuid -s -O3 -Wl,--gc-sections -nostartfiles --entry=pttb


Usage:

	> pttb Path\to\.exe\or\.lnk\to\PinToTaskbar
	> pttb -u Path\to\.exe\or\.lnk\to\UnPinFromTaskBar
	> pttb -r (Refresh taskbar by Pinning & UnPinning itself, in order to reflect changes made to:
	           HKEY_CURRENT_USER\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\Taskband)

Notes:

  - 1st tried the registry method described here:
    - https://stackoverflow.com/questions/31720595/pin-program-to-taskbar-using-ps-in-windows-10
    - Doesn't work anymore
  - Then tried the PEB method described here:
    - https://alexweinberger.com/main/pinning-network-program-taskbar-programmatically-windows-10/
    - Doesn't work anymore either
  - So pttb ended up being developed with the PE injection method used by syspin.exe from https://www.technosys.net
    - Thanks Microsoft for making it a bit more difficult, I learned quite a bit with this little project

To view the source code in your browser with original tabbing (4 white-spaces instead of browsers default 8), add '/?ts=4' to url.  
Works on Firefox/Chrome based browser, except for edge..  
https://github.com/0x546F6D/pttb_-_Pin_To_TaskBar/blob/main/pttb.c/?ts=4
