# Harmonisation :
# Ce script peut être lancé avec un format de date en paramètre optionnel.
# Exemple : ./Prepa-TNR-TRUSTEE-UNDERLYING.sh PREPROD  # format par défaut %Y%m%d
#           ./Prepa-TNR-TRUSTEE-UNDERLYING.sh PREPROD %d%m%Y  # format jourmoisannée


# =====================
# Initialisation et paramètres
# =====================
#!/bin/bash

# Vérification de l'environnement 

# Vérification des paramètres

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

# Chemins selon l’environnement
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


# =====================
# Fonctions de copie
# =====================
process_file() {
	#echo ""copions les fichiers
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local SRC_DIR=$4
	local DEST_DIR=$5
	local DATESource=$6
	local DATECopy=$7
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${DATESource}${suffix_name}"*"${fileExtension}" )
	#echo "$files"
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${DATECopy}${suffix_name}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
	echo "[INFO] Copié : $filename → $new_name"
    fi
  done
}

process_file_issuer() {
	#echo "copions les fichiers issuers"
	local prefix_name=$1
	local suffix_name=$2
	local timing=$3
	local fileExtension=$4
	local SRC_DIR=$5
	local DEST_DIR=$6
	local DATESource=$7
	local DATECopy=$8
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${DATESource}"*"${timing}"*"${fileExtension}" )
	#echo "$files"
  for filepath in "${files[@]}"; do
	#echo "boucle for : $files"
    if [ -f "$filepath" ]; then
	#echo " le fichier existe : $filename"
      local filename=$(basename "$filepath")
	  #echo "$filename"
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${suffix_name}${DATECopy}-${ENV}${fileExtension}"
	  #echo "$new_name"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
	echo "[INFO] Copié : $filename → $new_name"
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
	local DATESource=$6
	#echo "$DATESource"
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/C${prefix_name}${DATESource}"*"${fileExtension}" )
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
	echo "[INFO] Copié : $filename → $new_name"
    fi
  done
}

# =====================
# Fonctions de comparaison DiffMate
# =====================
run_diffmate() {
  #local base_name=$1
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local configfile=$4
  local DEST_DIR=$5
  local difftype=$6
  local datefile=$7
	echo ""
	echo "[INFO] Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
	local preprod_file="${DEST_DIR}/${prefix_name}${datefile}${suffix_name}-${ENV}${fileExtension}"
	echo "[DEBUG] preprod_file = $preprod_file"
	local prod_file="${DEST_DIR}/${prefix_name}${datefile}${suffix_name}-PROD${fileExtension}"
	echo "[DEBUG] prod_file = $prod_file"
	local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
	echo "[DEBUG] config_file = $config_file"
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
	echo "[INFO] Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
	local preprod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-${ENV}${fileExtension}"
	echo "[DEBUG] preprod_file = $preprod_file"
	local prod_file="${DEST_DIR}/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
	echo "[DEBUG] prod_file = $prod_file"
	local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
	echo "[DEBUG] config_file = $config_file"
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
	echo "[INFO] Comparaison réussie : ${prefix_name}-${suffix_name}"
  else
	echo "[WARN] Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuée"
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

#a faire du mardi au samedi à partir de 5h10
#a faire à partir de  23h10 en preprod  (D-1)  #Generic_FxSpot_YYYYMMDD_HHMMSS.json
#a faire à partir de 4h45 en preprod  (D-1) #Generic_Security_Underlying_YYYYMMDD_HHMMSS.json
#a faire à partir de 5h en preprod (D) #GenericIssuerExport_Underlying (GenericIssuerExport_202507*_04*.json) 


# =====================
# Appels des fonctions
# =====================
process_file "Generic_Security_Underlying_" "" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$YESTERDAY" "$YESTERDAY"
process_file "Generic_FxSpot_" "" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$YESTERDAY" "$YESTERDAY" "$YESTERDAY"
process_file_issuer "GenericIssuerExport_" "Underlying_" "04" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$YESTERDAY" "$YESTERDAY"

# Lancer DiffMate pour chaque fichier
run_diffmate "Generic_Security_Underlying_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Securities.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$YESTERDAY"
sleep 60
run_diffmate "GenericIssuerExport_Underlying_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Issuers.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$YESTERDAY"
sleep 60
run_diffmate "Generic_FxSpot_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_FxSpot.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$YESTERDAY"