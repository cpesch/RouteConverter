echo off
title Switching to original UI...
del Welcome.nsh
del Finish.nsh
copy Welcome__ORIGINAL.nsh Welcome.nsh
copy Finish__ORIGINAL.nsh Finish.nsh