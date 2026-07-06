echo off
rem Switch UI to big version for PortableApps.com Updater
title Switching to big UI...
del modern.exe
del modern_headerbmp.exe
del modern_headerbmpr.exe
copy modern__BIG.exe modern.exe
copy modern_headerbmp__BIG.exe modern_headerbmp.exe
copy modern_headerbmpr__BIG.exe modern_headerbmpr.exe