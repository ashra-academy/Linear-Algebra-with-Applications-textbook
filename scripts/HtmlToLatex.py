# -*- coding: UTF-8 -*-

from bs4 import BeautifulSoup, NavigableString, Tag
import io
from optparse import OptionParser
import os
import re
import subprocess
import sys
import urllib.parse

UNICODE_CHARS = {
    
    # Greek letters
    u'α' : "$\\alpha$",
    u'β' : "$\\beta$",
    u'γ' : "$\\gamma$",
    u'δ' : "$\\delta$",
    u'κ' : "$\\kappa$",
    u'λ' : "$\\lambda$",
    u'μ' : "$\\mu$",
    u'µ' : "$\\mu$", # Micro symbol? Seems to be used like mu
    u'θ' : "$\\theta$",
    u'π' : "$\\pi$",
    u'ρ' : "$\\rho$",
    u'σ' : "$\\sigma$",
    u'Σ' : "$\\Sigma$",
    u'ϕ' : "$\\phi$",
    u'φ' : "$\\Phi$",
    u'Ω' : "$\\Omega$",
    
    
    # Binary Operators
    u'·' : "$\\cdot$",
    u'×' : "$\\times$",
    u'±' : "$\\pm$",
    u'≤' : "$\\leq$",
    u'≥' : "$\\geq$",
    u'≠' : "$\\neq$",
    u'≅' : "$\\cong$", # Or should this be approx?
    u'≈' : "$\\approx$",
    u'−' : "${}-{}$", # minus sign
    u'→' : "$\\to$",
    u'⇒' : "$\\implies$",
    u'↦' : "$\\mapsto$",
    u'←' : "$\\gets$",
    u'↔' : "$\\leftrightarrow$",
    u'∘' : "$\\circ$",
    u'○' : "$\\bigcirc$",
    u'⊕' : "$\\oplus$",
    u'∩' : "$\\cap$",
    u'∪' : "$\\cup$",
    u'⊇' : "$\\supseteq$",
    u'⊂' : "$\\subset$",
    u'∊' : "$\\in$",
    u'∉' : "$\\notin$",
    u'⊥' : "$\\perp$",
    u'•' : "$\\cdot$", # Bullet point symbol seems to be used like cdot
    
    # Unary Operators
    u'∑' : "$\\sum$",
    u'∫' : "$\\int$",
    u'√' : "$\\sqrt{}$",
    u'′' : "$\\prime$",
    u'″' : "$\dprime$",
    u'‴' : "$\trprime$",
    
    # Fractions
    u'½' : "$\\frac{1}{2}$",
    u'⅓' : "$\\frac{1}{3}$",
    u'⅔' : "$\\frac{2}{3}$",
    u'¼' : "$\\frac{1}{4}$",
    u'¾' : "$\\frac{3}{4}$",
    u'⅕' : "$\\frac{1}{5}$",
    
    # Misc
    u'°' : "\\textdegree ", # Not a math command
    u'‖' : "$\\vectlength$",
    u'é' : "\\'{e}",
    u'É' : "\\'{E}",
    u'ö' : "\\\"{o}",
    u'“' : "``",
    u'”' : "''",
    u'©' : "\\textcopyright ",
    u'…' : "\\dots ",
    u'¢' : "\\textcent ",
    u'ℂ' : "\\mathbb{C} ", # Complex set
    u'ℤ' : "\\mathbb{Z} ", # Integer set
    u'∞' : "\\infty ",
    u'⋅' : "$\\cdots$", # cdot? Seems to be used like cdots
    u'†' : "\\dagger ",
    
    # Dashes and spaces
    u'—' : "---", # em dash
    u'–' : "--", # en dash
    u' ' : "~", # en space... looks like it is used as nbsp
    u' ' : "~", # em space... looks like it is used as nbsp
    u' ' : "$\quad$", # four-per-em space -- is that quad?
    u' ' : " ", # thin space, seems to appear between f (x)
    u' ' : " " # Punctuation space
    
    }

INPUT_FILENAME = None
INPUT_DIR = None
SOLUTION_FILENAME = None
SOLUTION_INPUT_DIR = None

OUTPUT_FILENAME = None
FIGURE_DIR = None
FIGURE_DIR_REL = None

TOP_DOCUMENT = None
SOLUTION_LIST = None


def main():
    
    global INPUT_FILENAME
    global SOLUTION_FILENAME
    global OUTPUT_FILENAME
    global TOP_DOCUMENT
    global SOLUTION_LIST
    
    if not doOptions():
        return
    
    html_fhan = io.open(INPUT_FILENAME, "r", encoding="utf8")
    solution_fhan = io.open(SOLUTION_FILENAME, "r", encoding="utf8")
    out_fhan = io.open(OUTPUT_FILENAME, "w", encoding="utf8")
    
    # Seems to make any content after an img tag be its child!
    #soup = BeautifulSoup(html_fhan, "html.parser")
    TOP_DOCUMENT = BeautifulSoup(html_fhan, "lxml")
    solution_document = BeautifulSoup(solution_fhan, "lxml")
    
    # Find OL with solutions before parsing main document
    reader_div = solution_document.find("div", id="reader")
    subsection_div = reader_div.find("div", None, False, class_="subsection")
    # TODO: It looks like the OL is under the DIV, but a non-recursive search fails.  Why?
    SOLUTION_LIST = subsection_div.find("ol")
    
    
    reader_div = TOP_DOCUMENT.find("div", id="reader")
    
    section_div = reader_div.find("div", None, False, class_="section")
    
    if section_div:
        parseSection(reader_div, section_div, out_fhan)
        return
    
    appendix_div = reader_div.find("div", None, False, class_="appendix")
    
    if appendix_div:
        parseAppendix(reader_div, out_fhan)
        return
    
    sys.stderr.write("File does not appear to be a section or appendix.\n")


def doOptions():
    
    global INPUT_FILENAME
    global INPUT_DIR
    global SOLUTION_FILENAME
    global SOLUTION_INPUT_DIR
    global OUTPUT_FILENAME
    global FIGURE_DIR
    global FIGURE_DIR_REL
    
    parser = OptionParser()
    
    parser.add_option("-i", "--input", dest="input_file",
                      help="HTML file to read from", metavar="INPUT_FILE")
    parser.add_option("-o", "--output", dest="output_file",
                      help="Latex file to write to", metavar="OUTPUT_FILE")
    parser.add_option("-f", "--figures", dest="figure_dir",
                      help="Save figures to this directory", metavar="FIGURE_DIR")
    parser.add_option("-r", "--figuresrel", dest="figure_rel",
                      help="Relative path to figure directory to use in Latex", metavar="FIGURE_REL")
    parser.add_option("-s", "--solutions", dest="solution_file",
                      help="HTML file that contains solutions to exercises", metavar="SOLUTION_FILE")
    
    (options, args) = parser.parse_args()
    
    if not options.input_file or not options.output_file or not options.figure_dir or not options.figure_rel \
            or not options.solution_file:
        parser.print_help()
        return False
    
    if not os.path.isfile(options.input_file):
        sys.stderr.write("ERROR: Input file %s does not exist." % (options.input_file, ))
        return False
    
    if not os.path.isfile(options.solution_file):
        sys.stderr.write("ERROR: Solution file %s does not exist." % (options.solution_file, ))
        return False
    
    #if os.path.exists(options.output_file):
    #    sys.stderr.write("ERROR: Output file %s exists.  Refusing to overwrite." % (options.output_file, ))
    #    return False
    
    if not os.path.isdir(options.figure_dir):
        sys.stderr.write("ERROR: Figure directory %s does not exist." % (options.figure_dir, ))
        return False
    
    INPUT_FILENAME = options.input_file
    INPUT_DIR = os.path.dirname(INPUT_FILENAME)
    SOLUTION_FILENAME = options.solution_file
    SOLUTION_INPUT_DIR = os.path.dirname(SOLUTION_FILENAME)
    OUTPUT_FILENAME = options.output_file
    FIGURE_DIR = options.figure_dir
    FIGURE_DIR_REL = options.figure_rel
    
    return True


def parseSection(reader_div, section_div, out_fhan):
    
    section_head = section_div.find("div", None, False, class_="sectionhead")
    
    # Some sections seem to be missing the head div and just have the header content as part
    # of the body
    if not section_head:
        
        label_text = "sec:UNKNOWN"
        
        nd = section_div.find("span", None, False, class_=["sectitle", "ahead"])
        section_head_head = nd.find("span", class_="secthd")
        
    else:
        label_text = "sec:" + re.sub("^sh_", "", section_head['id'])
        section_head_head = section_head.find("span", class_="secthd")
    
    out_fhan.write("\\section{")
    parseParagraph(section_head_head, out_fhan, False)
    out_fhan.write("}\n")
    out_fhan.write("\\label{%s}\n" % (label_text, ))
    
    section_body = section_div.find("div", None, False, class_="sectbody")
    # Some sections have the body outside of the section div
    if not section_body:
        section_body = reader_div.find("div", None, False, class_="sectbody")
    
    parseBody(section_body, out_fhan)
    
    for subsection_div in section_div.find_all("div", None, False, class_="subsection"):
        
        subsection_head = subsection_div.find("span", None, False, class_=["sectitle", "bhead"])
        
        # Sometimes a sub section won't have any text, just sub-sub sections
        if not subsection_head:
            
            out_fhan.write("\\subsection*{}\n")
            
            for subsubsection_div in subsection_div.find_all("div", None, False, class_="subsection"):
                
                subsubsection_head = subsubsection_div.find("span", None, False, class_=["sectitle", "titlegreenb"])
                
                out_fhan.write("\\subsubsection*{")
                parseParagraph(subsubsection_head, out_fhan, False)
                out_fhan.write("}\n")
                
                subsubsection_head.decompose()
                
                parseBody(subsubsection_div, out_fhan)
                
        else:
            
            out_fhan.write("\\subsection*{")
            parseParagraph(subsection_head, out_fhan, False)
            out_fhan.write("}\n")
            
            subsection_head.decompose()
            
            parseBody(subsection_div, out_fhan)


def parseAppendix(reader_div, out_fhan):
    
    # apxbody div seems to be empty
    
    out_fhan.write("\\section*{Appendix ???}\n\\label{sec:UNKNOWN}\n")
    
    for subsection_div in reader_div.find_all("div", None, False, class_="apx-section"):
        
        subsection_head = subsection_div.find("span", None, False, class_=["sectitle", "bmchap"])
        
        # First apx-section does not have a header
        if subsection_head:
            
            out_fhan.write("\\subsection*{")
            parseParagraph(subsection_head, out_fhan, False)
            out_fhan.write("}\n")
            
            subsection_head.decompose()
        
        subsection_body_div = subsection_div.find("div", None, False, class_="sectbody")
        
        parseBody(subsection_body_div, out_fhan)


def parseBody(parentNd, out_fhan):
    
    for nd in parentNd.children:
        
        if isinstance(nd, NavigableString):
            out_fhan.write(parseString(nd))
            continue
        
        
        if nd.name == "p":
            parseParagraph(nd, out_fhan)
        elif nd.name == "img":
            parseImg(nd, out_fhan)
        elif nd.name == "ol":
            parseOL(nd, out_fhan)
        elif nd.name == "ul":
            parseUL(nd, out_fhan)
        elif nd.name == "div":
            
            if "figure" in nd['class']:
                parseFigure(nd, out_fhan)
            elif "definition" in nd['class']:
                parseDefinition(nd, out_fhan)
            elif "example" in nd['class']:
                parseExample(nd, out_fhan)
            elif "theorem" in nd['class']:
                parseTheorem(nd, out_fhan)
            elif "proof" in nd['class']:
                parseProof(nd, out_fhan)
            elif "corollary" in nd['class']:
                parseCorollary(nd, out_fhan)
            elif "equation" in nd['class']:
                parseEquation(nd, out_fhan)
            elif "exercises" in nd['class']:
                parseExercises(nd, out_fhan)
            elif "blockquote" in nd['class']:
                parseQuotation(nd, out_fhan)
            elif "newpageo" in nd['class']:
                # Don't include page numbers in the output
                pass
            elif "table" in nd['class']:
                
                if hasGraphicDivWithImage(nd):
                    parseBody(nd.find("div", None, False, class_="graphic"), out_fhan)
                else:
                    out_fhan.write(" MISSING\\_TABLE ")
                    sys.stderr.write("UNKNOWN TABLE: %s\n" % (str(nd), ))
                
            elif divIsSolutionMarker(nd):
                # Ignore for now
                # out_fhan.write("% HAS SOLUTION MARKER\n")
                pass
            elif "subsection" in nd['class']:
                
                didBody = False
                
                if "subsection" in nd.parent['class']:
                    
                    subsection_head = nd.find("span", None, False, class_=["sectitle", "greentl"])
                    
                    if subsection_head:
            
                        out_fhan.write("\\subsubsection*{")
                        parseParagraph(subsection_head, out_fhan, False)
                        out_fhan.write("}\n")
                        
                        subsection_head.decompose()
                        
                        parseBody(nd, out_fhan)
                        didBody = True
                
                
                # Detect solution environment...
                solve_nd = nd.find("span", class_="solve")
                
                if not didBody and solve_nd:
                    
                    # Sanity checking: 1. solve span must be first in paragraph
                    if solve_nd.previous_sibling:
                        sys.stderr.write("NOT SURE HOW TO HANDLE SOLVE NODE\n")
                    
                    # 2. Paragraph must be direct child of subsection
                    if nd != solve_nd.parent.parent:
                        sys.stderr.write("NOT SURE HOW TO HANDLE SOLVE NODE (3)\n")
                    
                    # 3. Paragraph must be first in subsection
                    prevsib = solve_nd.parent.previous_sibling
                    while prevsib and isinstance(prevsib, NavigableString) and re.match("^[ \t\n]+$", str(prevsib)):
                        prevsib = prevsib.previous_sibling
                        
                    if prevsib and isinstance(prevsib, Tag):
                        sys.stderr.write("NOT SURE HOW TO HANDLE SOLVE NODE (2)\n")
                    
                    # Find img tag just after solve span
                    nextsib = solve_nd.next_sibling
                    while nextsib and isinstance(nextsib, NavigableString) and re.match("^[ \t\n]+$", str(nextsib)):
                        nextsib = nextsib.next_sibling
                    
                    # If found, remove it
                    if nextsib and isinstance(nextsib, Tag) and \
                            nextsib.name == "img" and nextsib['src'].endswith("/arrow.png"):
                        nextsib.decompose()

                    # Remove solve span as well                    
                    solve_nd.decompose()
                    
                    out_fhan.write("\\begin{solution}\n")
                    parseBody(nd, out_fhan)
                    out_fhan.write("\\end{solution}\n")
                    
                    didBody = True
                
                
                if not didBody:
                    sys.stderr.write("UNHNADLED SUB-SUBSECTION: %s\n" % (str(nd), ))
                    parseBody(nd, out_fhan)
                
                
            elif "graphic" in nd['class']:
                
                imgTag = nd.find("img") # Seems to only work with recursion
                
                parseImg(imgTag, out_fhan)
                
            else:
                out_fhan.write(" MISSING\\_DIV ")
                sys.stderr.write("UNKNOWN DIV TYPE: %s\n" % (str(nd['class']), ))
        
        elif nd.name == "span":
            
            if "scale" in nd['class']:
                
                out_fhan.write("% Header class=scale\n")
                out_fhan.write("\\textbf{")
                parseParagraph(nd, out_fhan, False)
                out_fhan.write("}")
                
            elif "typenum" in nd['class']:
                # A list whose labels are "Type I...II...III..."
                parseParagraph(nd, out_fhan, False)
            else:
                out_fhan.write(" MISSING\\_SPAN ")
                sys.stderr.write("UNKNOWN SPAN TYPE: %s\n" % (str(nd['class']), ))
            
        else:        
            out_fhan.write(" MISSING\\_TAG ")
            sys.stderr.write("UNKNOWN TAG: %s\n" % (str(nd), ))


def parseExercises(parentNd, out_fhan):
    
    global SOLUTION_LIST
    
    olElem = parentNd.find("ol", None, False, class_="number")
    
    # output section name?
    sec_title = parentNd.find("span", None, False, class_="sectitle")
    sec_title_head = sec_title.find("span", None, False, class_="ex")
    
    sec_title_string = sec_title_head.string
    
    m = re.match(r"^EXERCISES ([0-9]+).([0-9]+)$", sec_title_string, re.IGNORECASE)
    if m:
        sec_title_string = "Exercises for \\ref{sec:" + m.group(1) + "_" + m.group(2) + "}"
    
    out_fhan.write("\\section*{%s}\n" % (sec_title_string))
    
    for nd in olElem.children:
        
        if not isinstance(nd, Tag):
            continue
        
        if nd.name == "div":
            
            if divIsSolutionMarker(nd):
                # Ignore for now
                # out_fhan.write("% HAS SOLUTION MARKER\n")
                pass
            elif "newpageo" in nd['class']:
                # Don't include page numbers in the output
                pass
            else:
                out_fhan.write(" MISSING\\_DIV ")
                sys.stderr.write("UNKNOWN DIV CLASS: %s\n" % (str(nd['class'])));
            
        elif nd.name == "li":
            out_fhan.write("\\begin{ex}\n")
            parseBody(nd, out_fhan)
            
            # TODO: Also doesn't seem to work when set to non-recursive
            solution_li = SOLUTION_LIST.find("li", value=nd['value'])
            if solution_li:
                out_fhan.write("\\begin{sol}\n")
                
                # First try to see if the solution contains lettered parts.
                # If so, this function will output the list properly
                if not parseSolutionsAsList(solution_li, out_fhan):
                    # Else just output as a regular body
                    parseBody(solution_li, out_fhan)
                    
                out_fhan.write("\\end{sol}\n")
            
            out_fhan.write("\\end{ex}\n\n")
        else:
            out_fhan.write(" MISSING\\_TAG ")
            sys.stderr.write("UNKNOWN TAG: %s\n" % (str(nd), ));


def parseSolutionsAsList(parentNd, out_fhan):
    
    # parentNd contains only P tags and whitespace?
    for nd in parentNd.children:
        
        if isinstance(nd, Tag):
            
            if not nd.name == "p":
                return False
            
        elif isinstance(nd, NavigableString):
            
            if not re.match("^[ \t\n]+$", str(nd)):
                return False
            
        else:
            
            return False
    
    # Each P tag starts with <b>(letter)</b>
    childPTags = parentNd.find_all("p", None, False)
    
    for nd in childPTags:
        
        firstChild = nd.contents[0]
        if not isinstance(firstChild, Tag):
            return False
        
        if firstChild.name != "b":
            return False
        
        if len(firstChild.contents) != 1:
            return False
        
        if not re.match("^\([a-zA-Z]\)$", firstChild.string):
            return False
        
    # Looks like the right pattern, so output an enumerate with items
    out_fhan.write("\\begin{enumerate}[label={\\alph*.}]\n")
    
    counterVal = 1
    
    for nd in childPTags:
    
        firstChild = nd.contents[0]
        m = re.match("^\(([a-zA-Z])\)$", firstChild.string)
        
        letterVal = ord(m.group(1).lower()) - ord('a') + 1
        
        firstChild.decompose()
        
        if counterVal != letterVal:
            #out_fhan.write("\\setcounter{\\@enumctr}{%i}\n" % (letterVal - 1, ))
            out_fhan.write("\\setcounter{enumi}{%i}\n" % (letterVal - 1, ))
            counterVal = letterVal
        
        out_fhan.write("\\item ")
        parseParagraph(nd, out_fhan)
        
        counterVal = counterVal + 1
    
    out_fhan.write("\\end{enumerate}\n")
    
    return True


def parseParagraph(parentNd, out_fhan, addExtraSpace=True):
    
    for nd in parentNd.children:
        
        if isinstance(nd, NavigableString):
            out_fhan.write(parseString(nd))
            continue
        
        if nd.name == "i":
            out_fhan.write("\\textit{")
            parseParagraph(nd, out_fhan, False)
            out_fhan.write("}")
        elif nd.name == "b":
            out_fhan.write("\\textbf{")
            parseParagraph(nd, out_fhan, False)
            out_fhan.write("}")
        elif nd.name == "sup":
            out_fhan.write("\\textsuperscript{")
            parseParagraph(nd, out_fhan, False)
            out_fhan.write("}")
        elif nd.name == "sub":
            out_fhan.write("\\textsubscript{")
            parseParagraph(nd, out_fhan, False)
            out_fhan.write("}")
        elif nd.name == "a":
            parseATag(nd, out_fhan)
        elif nd.name == "img":
            parseImg(nd, out_fhan)
        elif nd.name == "br":
            out_fhan.write("\\\\\n")
        elif nd.name == "span":
            
            if "smallcaps" in nd['class']:
                out_fhan.write("\\textsc{")
                parseParagraph(nd, out_fhan, False)
                out_fhan.write("}")
            elif "typenum" in nd['class']:
                # A list whose labels are "Type I...II...III..."
                parseParagraph(nd, out_fhan, False)
            else:
                out_fhan.write(" MISSING\\_SPAN ")
                sys.stderr.write("UNKNOWN SPAN TYPE: %s\n" % (str(nd['class']), ))
            
        else:
            out_fhan.write(" MISSING\\_TAG ")
            sys.stderr.write("UNKNOWN TAG: %s\n" % (str(nd), ))
       
    if addExtraSpace:
        # Make sure there is enough whitespace to end the paragraph in Latex
        out_fhan.write("\n\n");


def hasGraphicDivWithImage(nd):
    
    childTags = nd.find_all(None, None, False)
    if len(childTags) == 1 and childTags[0].name == "div" and "graphic" in childTags[0]['class']:
        
        grandchildTags = childTags[0].find_all(None, None, False)
        if len(grandchildTags) == 1 and grandchildTags[0].name == "img":
            return True
        
    return False


def parseFigure(parentNd, out_fhan):
    
    fig_graphic = parentNd.find("div", None, False, class_="graphic")
    
    # Special case: It seems that figure can't be used inside definition/example/thm, etc
    # So we bypass the begin/end figure if it contains on an image
    skipBeginFigure = False
    
    if parentNd.parent.name == "div":
        
        if "definition" in parentNd.parent['class'] or \
            "example" in parentNd.parent['class'] or \
            "theorem" in parentNd.parent['class'] or \
            "proof" in parentNd.parent['class']:
            
            if hasGraphicDivWithImage(parentNd):
                skipBeginFigure = True
            
        elif "subsection" in parentNd.parent['class']:
            
            if "definition" in parentNd.parent.parent['class'] or \
                "example" in parentNd.parent.parent['class'] or \
                "theorem" in parentNd.parent.parent['class'] or \
                "proof" in parentNd.parent.parent['class']:
                
                if hasGraphicDivWithImage(parentNd):
                    skipBeginFigure = True
    
    
    fig_caption = parentNd.find("div", None, False, class_="caption")
    
    fig_caption_text = ""
    if fig_caption:
        fig_caption_label = fig_caption.find("div", None, False, class_="label")
        if fig_caption_label:
            fig_caption_text = fig_caption_label.string
            # Don't output the figure numbering since latex will do this
            if re.match(r"^FIGURE [0-9]+$", fig_caption_text, re.IGNORECASE):
                fig_caption_text = ""
    
    fig_source_note = parentNd.find("div", None, False, class_="sourcenote")
    
    if skipBeginFigure:
        out_fhan.write("\\begin{center}\n")
    else:
        out_fhan.write("\\begin{figure}[H]\n")
        out_fhan.write("\\centering\n")
    
    parseBody(fig_graphic, out_fhan)
    
    if skipBeginFigure:
        out_fhan.write("\\captionof{figure}{\label{fig:%s}%s}\n" % (stripISBNFromId(parentNd['id']), parseString(fig_caption_text)))
    else:
        out_fhan.write("\\caption{\label{fig:%s}%s}\n" % (stripISBNFromId(parentNd['id']), parseString(fig_caption_text)))
    
    if fig_source_note:
        out_fhan.write("%s\n" % (parseString(fig_source_note.string)))
    
    if skipBeginFigure:
        out_fhan.write("\\end{center}\n")
    else:
        out_fhan.write("\\end{figure}\n")


def parseDefinition(parentNd, out_fhan):
    
    def_title = parentNd.find("span", None, False, class_="defnttl")
    
    title_str = def_title.string
    if not title_str:
        title_str = ""
    
    isNumbered = re.match(r"^Definition [0-9]+(\.[0-9]+)?$", title_str, re.IGNORECASE)
    skipNumbering = ""
    if not isNumbered:
        skipNumbering = "*"
    
    out_fhan.write("\\begin{definition%s}{" % (skipNumbering, ))
    if not isNumbered:
        parseParagraph(def_title, out_fhan, False)
    out_fhan.write("}{%s}\n" % (stripISBNFromId(parentNd['id']), ))
    
    def_title.decompose()
    
    parseBody(parentNd, out_fhan)
    
    out_fhan.write("\\end{definition%s}\n" % (skipNumbering, ))


def parseExample(parentNd, out_fhan):
    
    ex_title = parentNd.find("span", None, False, class_="sectitle")
    ex_title_head = ex_title.find("span", None, False, class_="exhead")
    
    title_str = ex_title_head.string
    if not title_str:
        title_str = ""
    
    out_fhan.write("\\begin{example}{")
    if not re.match(r"^Example [0-9]+(\.[0-9]+)?$", title_str, re.IGNORECASE):
        parseParagraph(ex_title_head, out_fhan, False)
    out_fhan.write("}{%s}\n" % (stripISBNFromId(parentNd['id']), ))
    
    ex_title.decompose()
    
    parseBody(parentNd, out_fhan)
    
    out_fhan.write("\\end{example}\n")


def parseTheorem(parentNd, out_fhan):
    
    thm_title = parentNd.find("span", None, False, class_="sectitle")
    thm_title_title = thm_title.find("span", None, False, class_="theormtl")
    
    title_str = thm_title_title.string
    if not title_str:
        title_str = ""
    
    isNumbered = re.match(r"^Theorem [0-9]+(\.[0-9]+)?$", title_str, re.IGNORECASE)
    skipNumbering = ""
    if not isNumbered:
        skipNumbering = "*"
    
    out_fhan.write("\\begin{theorem%s}{" % (skipNumbering, ))
    if not isNumbered:
        parseParagraph(thm_title_title, out_fhan, False)
    out_fhan.write("}{%s}\n" % (stripISBNFromId(parentNd['id']), ))
    
    thm_title.decompose()
    
    parseBody(parentNd, out_fhan)
    
    out_fhan.write("\\end{theorem%s}\n" % (skipNumbering, ))


def parseProof(parentNd, out_fhan):
    
    #proof_title = parentNd.find("span", None, False, class_="sectitle")
    #proof_title_title = proof_title.find("span", None, False, class_="proftl")
    
    proof_body =  parentNd.find("div", None, False, class_="proofinbox")
    
    out_fhan.write("\\begin{proof}\n")
    
    parseBody(proof_body, out_fhan)
    
    out_fhan.write("\\end{proof}\n")


def parseCorollary(parentNd, out_fhan):
    
    col_title = parentNd.find("span", None, False, class_="sectitle")
    col_title_title = col_title.find("span", None, False, class_="corl")
    
    title_str = col_title_title.string
    if not title_str:
        title_str = ""
    
    isNumbered = re.match(r"^Corollary [0-9]+(\.[0-9]+)?$", title_str, re.IGNORECASE)
    skipNumbering = ""
    if not isNumbered:
        skipNumbering = "*"
    
    out_fhan.write("\\begin{corollary%s}{" % (skipNumbering, ))
    if not isNumbered:
        parseParagraph(col_title_title, out_fhan, False)
    out_fhan.write("}{%s}\n" % (stripISBNFromId(parentNd['id']), ))
    
    col_title.decompose()
    
    parseBody(parentNd, out_fhan)
    
    out_fhan.write("\\end{corollary%s}\n" % (skipNumbering, ))


def parseQuotation(parentNd, out_fhan):
    
    out_fhan.write("\\begin{quotation}\n")
    
    parseBody(parentNd, out_fhan)
    
    out_fhan.write("\\end{quotation}\n")


def parseOL(parentNd, out_fhan):
    
    enum_type = ""
    if "number" in parentNd['class']:
        pass
    elif "lc-alpha" in parentNd['class']:
        enum_type = "[label={\\alph*.}]"
    elif "lc-roman" in parentNd['class']:
        enum_type = "[label={\\roman*.}]"
    elif "ucroman" in parentNd['class']:
        enum_type = "[label={\\Roman*.}]"
    #elif "ucalpha" in parentNd['class']:
    #    enum_type = "[label={\\Alph*.}]"
    else:
        sys.stderr.write("UNKNOWN LABEL TYPE: %s\n" % (str(parentNd['class']), ))
    
    out_fhan.write("\\begin{enumerate}%s\n" % (enum_type, ))
    
    for liTag in parentNd.find_all("li", None, False):
        
        out_fhan.write("\item ")
        
        parseBody(liTag, out_fhan)

    out_fhan.write("\\end{enumerate}\n")


def parseUL(parentNd, out_fhan):
    
    out_fhan.write("\\begin{itemize}\n")
    
    for liTag in parentNd.find_all("li", None, False):
        
        out_fhan.write("\item ")
        
        parseBody(liTag, out_fhan)

    out_fhan.write("\\end{itemize}\n")


def parseEquation(parentNd, out_fhan):
    
    eqn_graphic = parentNd.find("div", None, False, class_="graphic")
    
    if not eqn_graphic:
        
        eqn_asciimath = parentNd.find("div", None, False, class_="asciimath")
        
        if eqn_asciimath:
            out_fhan.write("\\begin{center}\n")
            parseParagraph(eqn_asciimath, out_fhan, False)
            out_fhan.write("\\end{center}\n")
            return
        
        eqn_inlineimg = parentNd.find("img", None, False, class_="inlineimage")
        
        if eqn_inlineimg:
            out_fhan.write("\\begin{center}\n")
            parseImg(eqn_inlineimg, out_fhan)
            out_fhan.write("\\end{center}\n")
            return
        
        sys.stderr.write("UNKNOWN EQUATION TYPE: %s\n" % (str(parentNd), ))
    
    out_fhan.write("\\begin{equation*}\n")
    parseBody(eqn_graphic, out_fhan)
    out_fhan.write("\\end{equation*}\n")


def parseImg(imgTag, out_fhan):
    
    global INPUT_DIR
    global SOLUTION_INPUT_DIR
    global FIGURE_DIR
    global FIGURE_DIR_REL
    
    src_data = urllib.parse.unquote(imgTag['src'])
    
    out_file_base = os.path.splitext(os.path.basename(src_data))[0]
    out_file_base = stripISBNFromFilename(out_file_base)
    out_file_base = re.sub("_", "", out_file_base)
    
    out_file = os.path.join(FIGURE_DIR, out_file_base + ".eps")
    
    if os.path.exists(out_file):
        sys.stderr.write("OUTPUT IMAGE EXISTS: %s\n" % (out_file, ))
    
    input_file = os.path.join(INPUT_DIR, src_data)
    if not os.path.exists(input_file):
        # TODO: Have a better way of knowing that we should use the solution file
        input_file = os.path.join(SOLUTION_INPUT_DIR, src_data)
        if not os.path.exists(input_file):
            sys.stderr.write("IMPUT IMAGE DOES NOT EXIST: %s\n" % (input_file, ))
    
    retcode = subprocess.call(["/usr/bin/convert", input_file, out_file])
    if retcode != 0:
        sys.stderr.write("ERROR RUNNING CONVERT ON %s: %i\n" % (src_data, retcode))
    
    out_fhan.write("\\includegraphics{%s}\n" % (os.path.join(FIGURE_DIR_REL, out_file_base), ))


def parseATag(aTag, out_fhan):
    
    global TOP_DOCUMENT
    
    if "externallink" in aTag['class']:
        out_fhan.write("\\href{%s}{%s}" % (aTag['href'], aTag.string))
        return
    
    
    if 'onclick' not in aTag.attrs.keys():
        sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
        out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
        return
    
    onclick_value = aTag['onclick']
    
    prefix_name = ""
    ref_val = ""
    
    if onclick_value.startswith("FindFigure("):
        
        m = re.search("^FindFigure\\('(.+)'\);$", onclick_value)
        if not m:
            sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
            out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
            return
        
        prefix_name = "Figure~";
        ref_val = "fig:" + stripISBNFromId(m.group(1))
        
    elif onclick_value.startswith("GotoID("):
        
        m = re.search("^GotoID\\('(.+)'\);$", onclick_value)
        if not m:
            sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
            out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
            return
        
        elem_id = m.group(1)
        
        elem = TOP_DOCUMENT.find(id=elem_id)
        if not elem:
            
            # Section name?
            m = re.search("^Section ([0-9]+)\\.([0-9]+)$", aTag.string)
            if not m: 
                sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
                out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
                return
            
            prefix_name = "Section~"
            ref_val = "sec:" + m.group(1) + "_" + m.group(2)
        
        elif "theorem" in elem['class']:
            prefix_name = "Theorem~"
            ref_val = "thm:" + stripISBNFromId(elem_id)
        elif "definition" in elem['class']:
            prefix_name = "Definition~"
            ref_val = "dfn:" + stripISBNFromId(elem_id)
        elif "example" in elem['class']:
            prefix_name = "Example~"
            ref_val = "ex:" + stripISBNFromId(elem_id)
        elif "proof" in elem['class']:
            prefix_name = "Proof~"
            ref_val = "prf:" + stripISBNFromId(elem_id)
        elif "ftnote" in elem['class']:
            
            ptag = elem.find("p", None, False)
            if ptag and not ptag.next_sibling and not ptag.previous_sibling:
                suptag = ptag.find("sup", None, False)
                if suptag and not suptag.previous_sibling:
                    suptag.decompose()
            
            out_fhan.write("\\footnote{")
            parseParagraph(ptag, out_fhan, False)
            out_fhan.write("}\n")
            
            elem.decompose()
            
            return
            
        else:
            sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
            out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
            return
        
    elif onclick_value.startswith("LoadSection("):
        
        m = re.search("^LoadSection\\('(.+)', *'(.+)'\);$", onclick_value)
        if not m:
            sys.stderr.write("UNKNOWN A TAG: %s\n" % (str(aTag), ))
            out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
            return
        
        prefix_name = "Chapter~";
        ref_val = "chap:" + m.group(1)
        
    else:
        sys.stderr.write("UNKNOWN A ONCLICK: %s\n" % (onclick_value, ))
        out_fhan.write("A\\_TAG:%s" % (aTag.string, ))
        return
        
    
    out_fhan.write("%s\\ref{%s}" % (prefix_name, ref_val, ))


def parseString(stringData):
    
    global UNICODE_CHARS
    
    # Characters that must be escaped
    stringData = re.sub("([%$_\\\\])", "\\\\\\1", stringData)
    
    # Are there any unicode characters?
    if all(ord(c) < 128 for c in stringData):
        return stringData
        
    retStr = ""
    
    for c in stringData:
        
        if ord(c) < 128:
            retStr = retStr + c
            continue
        
        if c in UNICODE_CHARS.keys():
            c = UNICODE_CHARS[c]
        else:
            sys.stderr.write("UNHANDLED UNICODE: %s\n" % (c, ))
            
        retStr = retStr + c
    
    return retStr


def divIsSolutionMarker(divNd):
    
    if "graphic" not in divNd['class']:
        return False
    
    imgNd = divNd.find("img", None, False, class_="antstar")
    
    return imgNd != None


def stripISBNFromId(idStr):
    
    m = re.match(r"^id_007109492x_001_(.+)$", idStr, re.IGNORECASE)
    
    if not m:
        return idStr
    
    return m.group(1)


def stripISBNFromFilename(filename):
    
    m = re.match(r"^007109492X_001_(.+)$", filename, re.IGNORECASE)
    
    if not m:
        return filename
    
    return m.group(1)


if __name__ == '__main__':
    main()