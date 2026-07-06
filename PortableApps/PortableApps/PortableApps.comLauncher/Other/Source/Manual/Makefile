# Makefile for Sphinx documentation
#

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
PAPER         =

# Internal variables.
PAPEROPT_a4     = -D latex_paper_size=a4
PAPEROPT_letter = -D latex_paper_size=letter
ALLSPHINXOPTS   = -d _build/doctrees $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) .

.PHONY: help release clean html dirhtml pickle json htmlhelp qthelp latex changes linkcheck doctest

help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  html      to make standalone HTML files"
	@echo "  dirhtml   to make HTML files named index.html in directories"
	@echo "  pickle    to make pickle files"
	@echo "  json      to make JSON files"
	@echo "  htmlhelp  to make HTML files and a HTML help project"
	@echo "  qthelp    to make HTML files and a qthelp project"
	@echo "  latex     to make LaTeX files, you can set PAPER=a4 or PAPER=letter"
	@echo "  changes   to make an overview of all changed/added/deprecated items"
	@echo "  linkcheck to check all external links for integrity"
	@echo "  doctest   to run all doctests embedded in the documentation (if enabled)"

release:
	@# clean
	-rm -rf _build
	-rm -rf ../../../App/Manual
	@# html
	$(SPHINXBUILD) -b html $(ALLSPHINXOPTS) ../../../App/Manual
	@# partial clean
	-rm ../../../App/Manual/.buildinfo
	-rm ../../../App/Manual/objects.inv
	-rm -rf _build
	-rm _ext/paldocs.pyc

clean:
	-rm -rf ../../../App/Manual/*
	-rm -rf _build/*

html:
	$(SPHINXBUILD) -b html $(ALLSPHINXOPTS) ../../../App/Manual/html
	@echo
	@echo "Build finished. The HTML pages are in ../../../App/Manual/html."

dirhtml:
	$(SPHINXBUILD) -b dirhtml $(ALLSPHINXOPTS) ../../../App/Manual/dirhtml
	@echo
	@echo "Build finished. The HTML pages are in ../../../App/Manual/dirhtml."

pickle:
	$(SPHINXBUILD) -b pickle $(ALLSPHINXOPTS) ../../../App/Manual/pickle
	@echo
	@echo "Build finished; now you can process the pickle files."

json:
	$(SPHINXBUILD) -b json $(ALLSPHINXOPTS) ../../../App/Manual/json
	@echo
	@echo "Build finished; now you can process the JSON files."

htmlhelp:
	$(SPHINXBUILD) -b htmlhelp $(ALLSPHINXOPTS) ../../../App/Manual/htmlhelp
	@echo
	@echo "Build finished; now you can run HTML Help Workshop with the" \
	      ".hhp project file in ../../../App/Manual/htmlhelp."

qthelp:
	$(SPHINXBUILD) -b qthelp $(ALLSPHINXOPTS) ../../../App/Manual/qthelp
	@echo
	@echo "Build finished; now you can run "qcollectiongenerator" with the" \
	      ".qhcp project file in ../../../App/Manual/qthelp, like this:"
	@echo "# qcollectiongenerator ../../../App/Manual/qthelp/PortableAppscomLauncher.qhcp"
	@echo "To view the help file:"
	@echo "# assistant -collectionFile ../../../App/Manual/qthelp/PortableAppscomLauncher.qhc"

latex:
	$(SPHINXBUILD) -b latex $(ALLSPHINXOPTS) ../../../App/Manual/latex
	@echo
	@echo "Build finished; the LaTeX files are in ../../../App/Manual/latex."
	@echo "Run \`make all-pdf' or \`make all-ps' in that directory to" \
	      "run these through (pdf)latex."

changes:
	$(SPHINXBUILD) -b changes $(ALLSPHINXOPTS) ../../../App/Manual/changes
	@echo
	@echo "The overview file is in ../../../App/Manual/changes."

linkcheck:
	$(SPHINXBUILD) -b linkcheck $(ALLSPHINXOPTS) ../../../App/Manual/linkcheck
	@echo
	@echo "Link check complete; look for any errors in the above output " \
	      "or in ../../../App/Manual/linkcheck/output.txt."

doctest:
	$(SPHINXBUILD) -b doctest $(ALLSPHINXOPTS) ../../../App/Manual/doctest
	@echo "Testing of doctests in the sources finished, look at the " \
	      "results in ../../../App/Manual/doctest/output.txt."
