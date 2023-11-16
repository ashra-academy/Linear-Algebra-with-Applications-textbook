
import re
import os
import os.path

TOPDIR = "/lyryx/textbooks/lawa/trunk/text"

GRAPHICS_RE = re.compile(r"\\includegraphics{(.+?)}")

def doFile(filepath):
    
    fhan = open(filepath, 'r')
    
    for line in fhan:
        
        for m in GRAPHICS_RE.finditer(line):
            
            print("./" + m.group(1) + ".eps")
    
    fhan.close()
    

def doDir(dirname):
    
    for fname in os.listdir(os.path.join(TOPDIR, dirname)):
        
        if fname[-4:] != ".tex":
            continue
        
        abs_fname = os.path.join(TOPDIR, dirname, fname)
        
        doFile(abs_fname)
    

def main():
    
    
    for fname in os.listdir(TOPDIR):
        
        abs_fname = os.path.join(TOPDIR, fname)
        
        if os.path.isdir(abs_fname):
            doDir(fname)
        
    

if __name__ == '__main__':
    main()