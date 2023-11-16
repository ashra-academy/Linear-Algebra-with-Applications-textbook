#!/usr/bin/python3


import os.path
import re
import sys

BEGIN_RE = re.compile(r"^\\begin{(.+?)}")
END_RE = re.compile(r"^\\end{(.+?)}[ \t]*$")

def main():
    
    input_file = sys.argv[1]
    #output_file = sys.argv[2]
    
    #if (os.path.exists(output_file)):
    #    sys.stderr.write("Output file already exists\n")
    #    sys.exit(1)
    
    input_fhan = open(input_file, "r")
    
    input_data = ''.join(input_fhan.readlines())
    
    input_fhan.close()
    
    lines = input_data.split("\n")
    
    pos = 0
    while pos < len(lines):
        
        line = lines[pos]
        
        m = BEGIN_RE.match(line)
        if m:
            name = m.group(1)
            if name[-1] == "*":
                name = name[:-1]
                
            if name == "proof" or name == "theorem" or name == "definition" or name == "corollary" or \
                    name == "lemma" or name == "example" or name == "solution" or \
                    name == "ex" or name == "sol":
                removeBlankLinesAfter(lines, pos)
                
            if name == "equation":
                pos -= removeBlankLinesBefore(lines, pos)
            
        m = END_RE.match(line)
        if m:
            name = m.group(1)
            if name[-1] == "*":
                name = name[:-1]
                
            if name == "proof" or name == "theorem" or name == "definition" or name == "corollary" or \
                    name == "lemma" or name == "example" or name == "solution" or \
                    name == "ex" or name == "sol":
                pos -= removeBlankLinesBefore(lines, pos)
                
            if name == "equation":
                removeBlankLinesAfter(lines, pos)
                
        pos += 1
        
    
    #output_fhan = open(output_file, "w")
    output_fhan = open(input_file, "w")
    
    output_fhan.write("\n".join(lines))
    output_fhan.close()


def removeBlankLinesBefore(lines, pos):
    
    numLinesDeleted = 0
    
    pos -= 1
    
    while pos > 0 and lines[pos] == "":
        del lines[pos]
        numLinesDeleted += 1
        pos -= 1
    
    return numLinesDeleted


def removeBlankLinesAfter(lines, pos):
    
    numLinesDeleted = 0
    
    pos += 1
    
    while pos < len(lines) and lines[pos] == "":
        del lines[pos]
        numLinesDeleted += 1
    
    return numLinesDeleted


if __name__ == '__main__':
    main()
