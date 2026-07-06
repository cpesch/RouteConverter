@ECHO OFF

REM Command file for Sphinx documentation

if "%SPHINXBUILD%" == "" set SPHINXBUILD=sphinx-build
set ALLSPHINXOPTS=-d _build/doctrees %SPHINXOPTS% .
if NOT "%PAPER%" == "" (
	set ALLSPHINXOPTS=-D latex_paper_size=%PAPER% %ALLSPHINXOPTS%
)

if "%1" == "" goto help

if "%1" == "help" (
	:help
	echo.Please use `make ^<target^>` where ^<target^> is one of
	echo.  release   to make a release version of the documentation
	echo.  html      to make standalone HTML files
	echo.  dirhtml   to make HTML files named index.html in directories
	echo.  pickle    to make pickle files
	echo.  json      to make JSON files
	echo.  htmlhelp  to make HTML files and a HTML help project
	echo.  qthelp    to make HTML files and a qthelp project
	echo.  latex     to make LaTeX files, you can set PAPER=a4 or PAPER=letter
	echo.  changes   to make an overview over all changed/added/deprecated items
	echo.  linkcheck to check all external links for integrity
	echo.  doctest   to run all doctests embedded in the documentation if enabled
	goto end
)

if "%1" == "release" ( :: clean, html, partial clean
	:: clean
	rmdir /q /s _build
	rmdir /q /s ..\..\..\App\Manual
	:: html
	%SPHINXBUILD% -b html %ALLSPHINXOPTS% ../../../App/Manual
	:: partial clean
	del /q ..\..\..\App\Manual\.buildinfo
	del /q ..\..\..\App\Manual\objects.inv
	rmdir /q /s _build
	del /q _ext\paldocs.pyc
	goto end
)
if "%1" == "clean" (
	for /d %%i in (_build\*) do rmdir /q /s %%i
	del /q /s _build\*
	for /d %%i in (..\..\..\App\Manual\*) do rmdir /q /s %%i
	del /q /s ..\..\..\App\Manual\*
	goto end
)

if "%1" == "html" (
	%SPHINXBUILD% -b html %ALLSPHINXOPTS% ../../../App/Manual/html
	echo.
	echo.Build finished. The HTML pages are in ../../../App/Manual/html.
	goto end
)

if "%1" == "dirhtml" (
	%SPHINXBUILD% -b dirhtml %ALLSPHINXOPTS% ../../../App/Manual/dirhtml
	echo.
	echo.Build finished. The HTML pages are in ../../../App/Manual/dirhtml.
	goto end
)

if "%1" == "pickle" (
	%SPHINXBUILD% -b pickle %ALLSPHINXOPTS% ../../../App/Manual/pickle
	echo.
	echo.Build finished; now you can process the pickle files.
	goto end
)

if "%1" == "json" (
	%SPHINXBUILD% -b json %ALLSPHINXOPTS% ../../../App/Manual/json
	echo.
	echo.Build finished; now you can process the JSON files.
	goto end
)

if "%1" == "htmlhelp" (
	%SPHINXBUILD% -b htmlhelp %ALLSPHINXOPTS% ../../../App/Manual/htmlhelp
	echo.
	echo.Build finished; now you can run HTML Help Workshop with the ^
.hhp project file in ../../../App/Manual/htmlhelp.
	goto end
)

if "%1" == "qthelp" (
	%SPHINXBUILD% -b qthelp %ALLSPHINXOPTS% ../../../App/Manual/qthelp
	echo.
	echo.Build finished; now you can run "qcollectiongenerator" with the ^
.qhcp project file in ..\..\..\App\Manual\qthelp, like this:
	echo.^> qcollectiongenerator ..\..\..\App\Manual\qthelp\PortableAppscomLauncher.qhcp
	echo.To view the help file:
	echo.^> assistant -collectionFile ..\..\..\App\Manual\qthelp\PortableAppscomLauncher.ghc
	goto end
)

if "%1" == "latex" (
	%SPHINXBUILD% -b latex %ALLSPHINXOPTS% ../../../App/Manual/latex
	echo.
	echo.Build finished; the LaTeX files are in ../../../App/Manual/latex.
	goto end
)

if "%1" == "changes" (
	%SPHINXBUILD% -b changes %ALLSPHINXOPTS% ../../../App/Manual/changes
	echo.
	echo.The overview file is in ../../../App/Manual/changes.
	goto end
)

if "%1" == "linkcheck" (
	%SPHINXBUILD% -b linkcheck %ALLSPHINXOPTS% ../../../App/Manual/linkcheck
	echo.
	echo.Link check complete; look for any errors in the above output ^
or in ../../../App/Manual/linkcheck/output.txt.
	goto end
)

if "%1" == "doctest" (
	%SPHINXBUILD% -b doctest %ALLSPHINXOPTS% ../../../App/Manual/doctest
	echo.
	echo.Testing of doctests in the sources finished, look at the ^
results in ../../../App/Manual/doctest/output.txt.
	goto end
)

:end
