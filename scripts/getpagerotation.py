#!/usr/bin/python

# 
# (c) 2018 Lyryx Learning Inc
#
# This work is licensed under the Creative Commons
# Attribution-NonCommercial-ShareAlike 4.0 International License. To view a copy
# of this license, visit http://creativecommons.org/licenses/by-nc-sa/4.0/
# or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042,
# USA.

import re
import sys

if len(sys.argv) < 2:
    print "Usage: " + sys.argv[0] + " input.ps"
    sys.exit(1)

REpage = re.compile('^%%Page:[ ]+([0-9]+)[ ]+([0-9]+)$');

fhan = open(sys.argv[1], 'r')

curPage = 0
rotations = []

for line in fhan:
    
    if line[-1] == '\n':
        line = line[:-1]
    if len(line) > 0 and line[-1] == '\r':
        line = line[:-1]

    if line == '':
        continue

    m = REpage.match(line)
    if m != None:
        curPage = int(m.group(2))
        continue

    if line.find("{ThisPage}<</Rotate 90>>/PUT") >= 0:
        rotations.append(curPage)

fhan.close()

lastPage = curPage

if len(rotations) == 0:
    print "1-%i" % (lastPage)
    sys.exit(0)

lastRotate = 0

for curPage in rotations:
    
    if curPage-1 == lastRotate+1:
        sys.stdout.write("%i " % ((curPage-1), ))
    elif curPage != lastRotate+1:
        sys.stdout.write("%i-%i " % ((lastRotate+1), (curPage-1)))
    
    sys.stdout.write("%ileft " % (curPage, ))
    lastRotate = curPage

if lastRotate+1 == lastPage:
    sys.stdout.write("%i" % (lastPage, ))
elif lastRotate != lastPage:
    sys.stdout.write("%i-%i" % ((lastRotate+1), lastPage))

sys.stdout.write("\n")
