#!/bin/bash

#mode debub
DebugInit=0
DebugProcessFile=0
DebugDiffmate=1


# Vérification de l'environnement 
if [ -z "$1" ]; then
  echo "Usage: $0 <ENV> (ex: PREPROD, UAT, PROD)"
  exit 1
fi

ENV=$1
#ce serait bien de mettre le format de date en parametre ...
# On stocke la date de référence dans un format brut unique (ici %Y%m%d)
YESTERDAY_RAW=$(date -d "yesterday" +"%Y%m%d")
ENV_FILE_TO_COMPARE="PROD"
echo "YESTERDAY_RAW : $YESTERDAY_RAW"
echo "ENV_FILE_TO_COMPARE : ${ENV_FILE_TO_COMPARE}"

# Chemins selon l’environnement
case "$ENV" in
  PREPROD)
    BASE_PATH="/test/mmd/share"
    ;;
  UAT)
    BASE_PATH="/homo/mmd/share"
    ;;
  PROD)
    BASE_PATH="/expl/mmd/share"
	;;
  *)
    echo "Environnement inconnu : $ENV"
    exit 2
    ;;
esac


transform_date() {
	local DATE="$1"
	local formatDate="$2"
	# Conversion de la date depuis le format %Y%m%d vers le format cible
	date -d "$(echo $DATE | sed 's/\(....\)\(..\)\(..\)/\1-\2-\3/')" +"$formatDate"
}


# Fonction de copie 
process_file() {
	#echo ""copions les fichiers
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local SRC_DIR=$4
	local DEST_DIR=$5
	local DATEL=$6
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${DATEL}"*"${suffix_name}"*"${fileExtension}" )
		
	if [ $DebugProcessFile = 1 ] ; then 
		echo "prefix_name : $prefix_name  & suffix_name : $suffix_name & fileExtension : $fileExtension"
		echo "SRC_DIR : $SRC_DIR & DEST_DIR : $DEST_DIR & DATEL : $DATEL & formatDate : $formatDate"
		echo "files : $files"
		echo " "
	fi
	
  for filepath in "${files[@]}"; do
	if [ $DebugProcessFile = 1 ] ; then 
		echo "boucle for : $files"
	fi
		if [ -f "$filepath" ]; then
			if [ $DebugProcessFile = 1 ] ; then  
				echo " le fichier existe : $filename" 
				echo "$filename"
				echo "$new_name"
			fi
			local filename=$(basename "$filepath")
			cp "$filepath" "$DEST_DIR"
			local new_name="${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
			mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
			echo "Copié : $filename → $new_name"
		else
			echo "fichier non copié : $filename → $new_name"
		fi
  done
}


run_diffmate() {
  #local base_name=$1
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local configfile=$4
  local DEST_DIR=$5
  local difftype=$6
  local formatDate=$7
  local DATEL=$(transform_date "$DATE" "$formatDate")
  local test_file="${DEST_DIR}/${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
  local prod_file="${DEST_DIR}/${prefix_name}${DATEL}${suffix_name}-PROD${fileExtension}"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  
  
  if [ $DebugDiffmate = 1 ] ; then 
		echo ""
		echo "Lancement de DiffMate pour prefix_name : $prefix_name  & suffix_name : $suffix_name & fileExtension : $fileExtension"
		echo "configfile : $configfile & DEST_DIR : $DEST_DIR & difftype : $difftype "
		echo "test_file = $test_file"		
		echo "prod_file = $prod_file"	
		echo "calculated config_file = $config_file"
		echo " "
	fi
  
  if [[ -f "$prod_file" && -f "$test_file" ]]; then
	#echo "difftype = ${difftype=}"
	if [ "${difftype}" == "JSON" ]; then
		if [ $DebugDiffmate = 1 ] ; then
			echo "Type = JSON"
			echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file"\
			"$DEST_DIR" \
			"$config_file" 
		fi
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file"\
			"$DEST_DIR"\
			"$config_file" 

		
	elif [ "${difftype}" == "TXT" ]; then
		if [ $DebugDiffmate = 1 ] ; then
			echo "Type = TXT"
			echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file" \
			"$config_file" \
			"$DEST_DIR"
		fi	
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file" \
			"$config_file" \
			"$DEST_DIR"
	else
		echo "Erreur de type de comparaison"
  fi
    echo "Comparaison reussie : ${prefix_name}-${suffix_name}" \
	"Results available on : $DEST_DIR"
  else
    echo "Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee" \
	"Prod file should be in : $prod_file" \
	"$ENV file should be in : $test_file" \
	"Config_file file should be in : $config_file"
  fi
}




run_diffmate_projet() {
  local Ref_prefix_name=$1
  local Ref_suffix_name=$2
  local Ref_fileExtension=$3
  local Test_prefix_name=$4
  local Test_suffix_name=$5
  local Test_fileExtension=$6
  local configfile=$7
  local DEST_DIR=$8
  local difftype=$9
  local formatDate=${11}
  local DATEL=$(transform_date "${10}" "$formatDate")
  local prod_file="${DEST_DIR}/${Ref_prefix_name}${DATEL}${Ref_suffix_name}${Ref_fileExtension}"
  local test_file="${DEST_DIR}/${Test_prefix_name}${DATEL}${Test_suffix_name}${Test_fileExtension}"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  
  
  if [ $DebugDiffmate = 1 ] ; then 
		echo ""
		echo "Lancement de DiffMate pour prefix_name : $prod_file & $test_file for the date $DATEL"
		echo "configfile : $configfile & DEST_DIR : $DEST_DIR & difftype : $difftype "
		echo "test_file = $test_file"		
		echo "prod_file = $prod_file"	
		echo "calculated config_file = $config_file"
		echo " "
	fi
  
  if [[ -f "$prod_file" && -f "$test_file" ]]; then
	#echo "difftype = ${difftype=}"
	if [ "${difftype}" == "JSON" ]; then
		if [ $DebugDiffmate = 1 ] ; then
			echo "Type = JSON"
			echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file"\
			"$DEST_DIR" \
			"$config_file" 
		fi
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file"\
			"$DEST_DIR"\
			"$config_file" 

		
	elif [ "${difftype}" == "TXT" ]; then
		if [ $DebugDiffmate = 1 ] ; then
			echo "Type = TXT"
			echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file" \
			"$config_file" \
			"$DEST_DIR"
		fi	
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
			"$prod_file" \
			"$test_file" \
			"$config_file" \
			"$DEST_DIR"
	else
		echo "Erreur de type de comparaison"
  fi
    echo "Comparaison reussie : ${prefix_name}-${suffix_name}" \
	"Results available on : $DEST_DIR"
  else
    echo "Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee" \
	"Prod file should be in : $prod_file" \
	"Test file should be in : $test_file" \
	"Config_file file should be in : $config_file"
  fi
}



#YESTERDAY=$(date -d "yesterday" +"%Y%m%d")

# Lancer les copies 
#process_file "" "_SG0001_ASSETS_REF_DATA" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"
#process_file "" "_SG0001_ASSETS_NEW_REF_DATA" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"
#process_file "" "_SG0001_ASSETS_PRICES" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"
#process_file "" "_SG0001_ASSETS_NEW_PRICES" ".txt" "$BASE_PATH/export/prism/out" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"
#process_file "" "_SG0001_AGENTS" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"
#process_file "" "_SG0001_NEW_AGENTS" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy" "$YESTERDAY"



#YESTERDAY=$(date -d "yesterday" +"%d%m%Y")
# Lancer DiffMate pour chaque fichier (harmonisé)
run_diffmate_projet "" "_SG0001_ASSETS_REF_DATA-${ENV_FILE_TO_COMPARE}" ".txt" "" "_SG0001_ASSETS_NEW_REF_DATA-${ENV_FILE_TO_COMPARE}" ".txt" "ConfigDiffMate-PRISM-RefData-Audrey.json" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy/PROD-Files" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 10
run_diffmate_projet "" "_SG0001_ASSETS_PRICES-${ENV_FILE_TO_COMPARE}" ".txt" "" "_SG0001_ASSETS_NEW_PRICES-${ENV_FILE_TO_COMPARE}" ".txt" "ConfigDiffMate-PRISM-Prices-Audrey.json" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy/PROD-Files" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 10
run_diffmate_projet "" "_SG0001_AGENTS-${ENV_FILE_TO_COMPARE}" ".txt" "" "_SG0001_NEW_AGENTS-${ENV_FILE_TO_COMPARE}" ".txt" "ConfigDiffMate-PRISM-Agents-Audrey.json" "$BASE_PATH/testing-tools/Test_Audrey/PRISM-SilverCopy/PROD-Files" "TXT" "$YESTERDAY_RAW" "%Y%m%d"