#!/bin/bash

# Vérification de l'environnement 
if [ -z "$1" ]; then
  echo "Usage: $0 <ENV> (ex: PREPROD, UAT)"
  exit 1
fi

ENV=$1
#ce serait bien de mettre le format de date en parametre ...
DATE=$(date +"%Y%m%d")
YESTERDAY=$(date -d "yesterday" +"%Y%m%d")
YESTERDAY_FRIDAY=$(date -d "3 days ago" +"%Y%m%d")

# Chemins selon l’environnement
case "$ENV" in
  PREPROD)
    BASE_PATH="/test/mmd/share"
    ;;
  UAT)
    BASE_PATH="/homo/mmd/share"
    ;;
  *)
    echo "Environnement inconnu : $ENV"
    exit 2
    ;;
esac

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
	local files=( "${SRC_DIR}/${prefix_name}${DATEL}${suffix_name}"*"${fileExtension}" )
	echo "$files"
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    fi
  done
}

process_file_issuer() {
	#echo ""copions les fichiers
	local prefix_name=$1
	local suffix_name=$2
	local timing=$3
	local fileExtension=$4
	local SRC_DIR=$5
	local DEST_DIR=$6
	local DATEL=$7
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${DATEL}"*"${timing}"*"${fileExtension}" )
	#echo "$files"
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${suffix_name}${DATE}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    fi
  done
}

process_file_req() {
	#echo ""copions les fichiers
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local SRC_DIR=$4
	local DEST_DIR=$5
	#echo "$DEST_DIR"
	local DATEL=$6
	#echo "$DATEL"
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/C${prefix_name}${DATEL}"*"${fileExtension}" )
	#echo "$files"""
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="P${prefix_name}${suffix_name}${DATEL}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    fi
  done
}

# Fonction DiffMate 
run_diffmate() {
  #local base_name=$1
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local configfile=$4
  local DEST_DIR=$5
  local difftype=$6
  echo ""
  echo "Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
  local preprod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-${ENV}${fileExtension}"
  echo "preprod_file = $preprod_file"
  local prod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
  echo "prod_file = $prod_file"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  echo "config_file = $config_file"
  if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
	#echo "difftype = ${difftype=}"
	if [ "${difftype}" == "JSON" ]; then
	#	echo "Type = JSON"
		echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file"\
		"$DEST_DIR" \
		"$config_file" 
		"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file"\
		"$DEST_DIR"\
		"$config_file" 
	elif [ "${difftype}" == "TXT" ]; then
	#	echo "Type = TXT"
		echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file" \
		"$config_file" \
		"$DEST_DIR"
		
		"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file" \
		"$config_file" \
		"$DEST_DIR"
	else
		echo "Erreur de type de comparaison"
  fi
    echo "Comparaison reussie : ${prefix_name}-${suffix_name}"
  else
    echo "Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee"
  fi
}

run_diffmate_req() {
  #local base_name=$1
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local configfile=$4
  #echo "configfile = $configfile"
  local DEST_DIR=$5
  local difftype=$6
  echo ""
  echo "Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
  local preprod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-${ENV}${fileExtension}"
  echo "preprod_file = $preprod_file"
  local prod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
  echo "prod_file = $prod_file"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  echo "config_file = $config_file"
  if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
	echo "difftype = ${difftype=}"
	if [ "${difftype}" == "JSON" ]; then
	#	echo "Type = JSON"
	#	echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} "$prod_file" "$preprod_file" "$DEST_DIR" "$config_file" 
		
		"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file"\
		"$DEST_DIR"\
		"$config_file" 
	elif [ "${difftype}" == "TXT" ]; then
	#	echo "Type = TXT" 
	#	echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} "$prod_file" "$preprod_file" "$config_file" "$DEST_DIR" 
		
		"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file" \
		"$config_file" \
		"$DEST_DIR"
	elif [ "${difftype}" == "REQ" ]; then
	#	echo "Type = REQ" 
	#	echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} "$prod_file" "$preprod_file" "$DEST_DIR" 
	"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
		"$prod_file" \
		"$preprod_file" \
		"$DEST_DIR"
	else
		echo "Erreur de type de comparaison"
  fi
    echo "Comparaison reussie : ${prefix_name}-${suffix_name}"
  else
    echo "Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee"
  fi
}


# Lancer les copies
#if [ "$(date +%u)" -eq 1 ]; then
    #echo "Nous sommes lundi"
#	echo "Hier etait vendredi : $YESTERDAY_FRIDAY"
	#echo "$BASE_PATH/import/client/SGBT/Processed/"
	#echo "$BASE_PATH/testing-tools/TNR/SIGMA"
	#echo "YESTERDAY_FRIDAY"
#	process_file "socgen01_ora_" "" ".txt" "$BASE_PATH/import/client/SGBT/Processed" "$BASE_PATH/testing-tools/TNR/SIGMA" "$YESTERDAY_FRIDAY"
#else
    #echo "Nous ne sommes pas lundi"
#	echo "Hier n'etait pas vendredi: $YESTERDAY" 
#	process_file "socgen01_ora_" "" ".txt" "$BASE_PATH/import/client/SGBT/Processed" "$BASE_PATH/testing-tools/TNR/SIGMA" "$YESTERDAY"
	#echo "$BASE_PATH/import/client/SGBT/Processed/"
	#echo "$BASE_PATH/testing-tools/TNR/SIGMA"
	#echo "YESTERDAY_FRIDAY"
#fi


# Lancer les copies 
process_file_req "_M_B_DE_E_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_DE_ID_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_P_EOD_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_P_E_I_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_DE_EOD_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_SM_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file "Trustee_Security_Main_" "" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_issuer "GenericIssuerExport_" "Main_" "08" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"


# Lancer DiffMate pour chaque fichier
run_diffmate_req "P_M_B_DE_E_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate_req "P_M_DE_ID_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate_req "P_M_P_EOD_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate_req "P_M_P_E_I_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate_req "P_M_DE_EOD_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate_req "P_M_SM_" "" ".req" "" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "REQ"
sleep 60
run_diffmate "Trustee_Security_Main_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Securities.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON"
sleep 60
run_diffmate "GenericIssuerExport_Main_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Issuers.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON"