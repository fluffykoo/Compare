# =====================
# Usage
# =====================
# Ce script prépare et compare les fichiers pour SIGMA.
# Usage : ./Prepa-TNR-SIGMA.sh <ENV> [DATE_FORMAT]
# Exemple : ./Prepa-TNR-SIGMA.sh PREPROD
# Exemple : ./Prepa-TNR-SIGMA.sh UAT %d%m%Y

# =====================
# Initialisation et paramètres
# =====================
#!/bin/bash

# Vérification de l'environnement et des paramètres
if [ -z "$1" ]; then
	echo "[ERROR] Usage: $0 <ENV> [DATE_FORMAT]"
	echo "[INFO] Exemple: $0 PREPROD %Y%m%d"
	echo "[INFO] Exemple: $0 UAT %d%m%Y"
	exit 1
fi

# =====================
# Gestion des dates
# =====================
ENV=$1
# Format de date paramétrable (par défaut: %Y%m%d)
DATE_FORMAT=${2:-"%Y%m%d"}
DATE=$(date +"$DATE_FORMAT")
YESTERDAY=$(date -d "yesterday" +"$DATE_FORMAT")
YESTERDAY_FRIDAY=$(date -d "3 days ago" +"$DATE_FORMAT")

# =====================
# Définition des chemins selon l'environnement
# =====================
case "$ENV" in
	PREPROD)
		BASE_PATH="/test/mmd/share"
		;;
	UAT)
		BASE_PATH="/homo/mmd/share"
		;;
	*)
		echo "[ERROR] Environnement inconnu : $ENV"
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
	#local files=( "$SRC_DIR/$DATE_YESTERDAY"*"$base_name".txt )
	#echo "$files"
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${DATE}${suffix_name}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
	echo "[INFO] Copié : $filename → $new_name"
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
	echo "[INFO] Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
  local preprod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-${ENV}${fileExtension}"
  #echo "preprod_file = $preprod_file"
  local prod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
  #echo "prod_file = $prod_file"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  #echo "config_file = $config_file"
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
	echo "[INFO] Comparaison réussie : ${prefix_name}-${suffix_name}"
  else
	echo "[WARN] Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuée"
  fi
}

# Lancer les copies
if [ "$(date +%u)" -eq 1 ]; then
    #echo "Nous sommes lundi"
	echo "Hier etait vendredi : $YESTERDAY_FRIDAY"
	#echo "$BASE_PATH/import/client/SGBT/Processed/"
	#echo "$BASE_PATH/testing-tools/TNR/SIGMA"
	#echo "YESTERDAY_FRIDAY"
	process_file "socgen01_ora_" "" ".txt" "$BASE_PATH/import/client/SGBT/Processed" "$BASE_PATH/testing-tools/TNR/SIGMA" "$YESTERDAY_FRIDAY"
else
    #echo "Nous ne sommes pas lundi"
	echo "Hier n'etait pas vendredi: $YESTERDAY" 
	process_file "socgen01_ora_" "" ".txt" "$BASE_PATH/import/client/SGBT/Processed" "$BASE_PATH/testing-tools/TNR/SIGMA" "$YESTERDAY"
	#echo "$BASE_PATH/import/client/SGBT/Processed/"
	#echo "$BASE_PATH/testing-tools/TNR/SIGMA"
	#echo "YESTERDAY_FRIDAY"
fi


process_file "GetRating_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"
process_file "GetMifidData_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"
process_file "GetEventsData_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"
process_file "GetRefData_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"
process_file "GetIndicesData_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"
process_file "GetPrices_" "" ".json" "$BASE_PATH/mmd-connector/sent" "$BASE_PATH/testing-tools/TNR/SIGMA" "$DATE"


# Lancer DiffMate pour chaque fichier
run_diffmate "socgen01_ora_" "" ".txt" "ConfigDiffMate-TNR-SIGMA-ScopeSix.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "TXT"
sleep 60
run_diffmate "GetRating_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetRating.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"
sleep 60
run_diffmate "GetMifidData_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetMifidData.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"
sleep 60
run_diffmate "GetEventsData_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetEventsData.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"
sleep 60
run_diffmate "GetRefData_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetRefData.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"
sleep 60
run_diffmate "GetPrices_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetPrices.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"

## GetIndicesData_ toujours vide : 
#run_diffmate "GetIndicesData_" "" ".json"  "ConfigDiffMate-TNR_SIGMA-GetIndicesData.json" "$BASE_PATH/testing-tools/TNR/SIGMA" "JSON"