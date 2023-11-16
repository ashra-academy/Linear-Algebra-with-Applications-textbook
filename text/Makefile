#use Texlive 2015
export PATH := /usr/local/texlive/2015/bin/x86_64-linux:$(PATH)

TEXNAME = Nicholson-LAWA
TEXJOB =

export TEXINPUTS=.:../styles:

# Aliases to the main files that we want to build

.PHONY: all
all: $(TEXNAME).pdf

.PHONY: interior
interior: interior.pdf


# The commands to run to build .dvi from .tex
# This rebuilds the .dvi until the .aux, .toc, and .out files
# do not change (assuming they exist at all).  This will make
# sure that references are defined.
define BUILDTEX
	while : ; do \
		[ -f "$*.aux" ] && cp -a "$*.aux" "$*.aux.old" ; \
		[ -f "$*.toc" ] && cp -a "$*.toc" "$*.toc.old" ; \
		[ -f "$*.out" ] && cp -a "$*.out" "$*.out.old" ; \
		latex $(TEXJOB) "$<" || exit $$?; \
		if [ -f "$*.aux" ]; then \
			diff -q "$*.aux" "$*.aux.old" || continue ; \
		fi ; \
		if [ -f "$*.toc" ]; then \
			diff -q "$*.toc" "$*.toc.old" || continue ; \
		fi ; \
		if [ -f "$*.out" ]; then \
			diff -q "$*.out" "$*.out.old" || continue ; \
		fi ; \
		break ; \
	done
	[ -f "$*.aux.old" ] && rm -f "$*.aux.old" || exit 0
	[ -f "$*.toc.old" ] && rm -f "$*.toc.old" || exit 0
	[ -f "$*.out.old" ] && rm -f "$*.out.old" || exit 0
	../scripts/finalchecks.sh "$*.log"
endef


# Runs ps2pdf and rotates landscape pages for Amazon
define ROTATEDPDF
	ps2pdf "$<" "$@.tmp"
	pdftk "$@.tmp" cat `python ../scripts/getpagerotation.py $<` output "$@"
	rm -f "$@.tmp"
endef



# Special cases for interior, solutions, etc that use "latex --jobname ..."

interior.dvi: TEXJOB = --jobname interior
interior.dvi: $(TEXNAME).tex
	$(BUILDTEX)

#interior.pdf: interior.ps
#	$(ROTATEDPDF)

interior-1: interior-1.pdf
interior-1.dvi: $(TEXNAME).tex
# Build .aux file for references 
	latex --jobname interior $<
# Now build the interior
	latex --jobname interior-1 $<
	latex --jobname interior-1 $<
	latex --jobname interior-1 $<

interior-2: interior-2.pdf
interior-2.dvi: $(TEXNAME).tex
# Build .aux file for references
	latex --jobname interior $<
# Now build the interior
	latex --jobname interior-2 $<
	latex --jobname interior-2 $<
	latex --jobname interior-2 $<



# Default rules for building .dvi, .ps, and .pdf
%.dvi : %.tex
	$(BUILDTEX)

%.ps : %.dvi
	dvips -o "$@" "$<"

%.pdf : %.ps
# For all
	ps2pdf -r300 -dPDFSETTINGS=/printer -dColorConversionStrategy=/LeaveColorUnchanged -dGrayImageResolution=300 -dColorImageResolution=300 "$<"
# For main PDF
	pdftk LAWA_FrontMatter_2021A.pdf Nicholson-LAWA.pdf BackCover_2021A.pdf cat output LinearAlgebraWithApplications_2021A.pdf
# For interior
#	pdftk LOTs_2021A.pdf interior.pdf cat output interior_2021A.pdf
#	pdftk LOTs_2021A.pdf interior-1.pdf cat output interior_2021A.pdf
#	pdftk LOTs_2021A.pdf interior-2.pdf cat output interior_2021A.pdf


# Intermediate and target files that Latex will output
suffixes := .dvi .ps .pdf .out .out.old .log .toc .toc.old .aux .aux.old .bbl .blg .tps .glo .ist .acn .idx .ilg .ind

.PHONY: clean
clean:
	rm -f $(foreach f,$(suffixes),$(TEXNAME)$(f)) ex.tex  $(foreach f,$(suffixes),interior$(f))
	rm -f $(foreach f,$(suffixes),interior-1$(f))
	rm -f $(foreach f,$(suffixes),interior-2$(f))
	rm -f LinearAlgebrawithApplications_2021A.pdf
	rm -f interior_2021A.pdf


