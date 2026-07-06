; Require at least Unicode NSIS 2.46
!ifndef NSIS_UNICODE
	!error "You must compile the PortableApps.com Launcher with Unicode NSIS."
!endif

!if ${NSIS_VERSION} == v2.45
	!error "The PortableApps.com Launcher requires Unicode NSIS 2.46 or later."
!else
	!verbose push
	!verbose 4
	!echo "(If you get a compile error with !searchparse, please upgrade to Unicode NSIS 2.46 or later and try again.)"
	!verbose pop
!endif
!searchparse ${NSIS_VERSION} "v" V
!if ${V} < 2.46
	!error "You only have Unicode NSIS ${V}, but Unicode NSIS 2.46 or later is required for proper Windows 7 support. Please upgrade to Unicode NSIS 2.46 or later and try again."
!endif
!undef V
