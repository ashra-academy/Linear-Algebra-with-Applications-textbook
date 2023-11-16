#!/bin/bash

LOGFILE="$1"

grep -q 'There were undefined references.' "$LOGFILE" >/dev/null 2>/dev/null
if [[ "$?" -eq 0 ]]; then
	echo "*******"
	echo "There were undefined references."
	echo "*******"
	exit 1
fi

grep -q 'There were multiply-defined labels.' "$LOGFILE"  >/dev/null 2>/dev/null
if [[ "$?" -eq 0 ]]; then
	echo "*******"
	echo "There were multiply-defined labels."
	echo "*******"
	exit 1
fi

IFS=$'\n'

FOUND_UNICODE=0
OUTPUT=""

for fname in `find . -name \*.tex`; do
	LINES=`grep -P -n "[^\x00-\x7F]" "$fname" 2>&1`
	if [[ "$?" -eq 0 ]]; then
		FOUND_UNICODE=1
		OUTPUT="$OUTPUT"$'\n'"$fname:$LINES"
	fi
done

if [[ "$FOUND_UNICODE" -eq 1 ]]; then
	echo "*******"
	echo "Some files contain unicode characters."
	echo "*******"
	echo "$OUTPUT"
	exit 1	
fi

exit 0
