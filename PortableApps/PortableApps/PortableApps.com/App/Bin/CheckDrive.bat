@echo off
cls
color f0
title Checking %2 (%1) for errors...
echo Checking %2 (%1) for errors...
echo.
chkdsk %1
echo.
pause