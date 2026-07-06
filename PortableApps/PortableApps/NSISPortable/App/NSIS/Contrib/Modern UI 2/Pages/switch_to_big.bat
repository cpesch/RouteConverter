echo off
rem Switch UI to big version for PortableApps.com Updater
title Switching to big UI...
del Welcome.nsh
del Finish.nsh
copy Welcome__BIG.nsh Welcome.nsh
copy Finish__BIG.nsh Finish.nsh