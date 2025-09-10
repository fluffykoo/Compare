#!/bin/bash

#Determine le dossier ou se trouve le script (et donc le jar)

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

#VÃ©rfie qu'au moins un argument est donnÃ©

if [ "$#" -lt 1 ];then
	echo "Usage:"
	echo "For JSON :  json <file1> <file2> <reportfolder> <config.json>"
	echo "For REQ  :  req <file1> <file2> <reportfolder>"
	echo "For TXT  :  txt <file1> <file2> <reportfolder> <config.json> [terminal]"
	exit 1
fi

#Lance le JAR depuis son chemin absolu avec encodage UTF-8 forcé
java -Dfile.encoding=UTF-8 -jar "$DIR/diffmate2.3.2.jar" "$@"
