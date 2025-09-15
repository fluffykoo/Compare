# Harmonisation : 
# la date du jour est utilisée pour nommer et traiter les fichiers (format paramétrable).
# Exemples d'utilisation :
#   ./Prepa-TNR-TRUSTEE.sh PREPROD              # format par défaut (année, mois, jour)
#   ./Prepa-TNR-TRUSTEE.sh PREPROD %d%m%Y      # format jourmoisannée


#!/bin/bash

# Vérification de l'environnement 
if [ -z "$1" ]; then
	echo "Usage: $0 <ENV> (ex: PREPROD, UAT) [DATE_FORMAT]"
	exit 1
fi
ENV=$1
DATE_FORMAT="%Y%m%d"
if [ -n "$2" ]; then
	DATE_FORMAT="$2"
fi

# =====================
# Gestion des dates
# =====================

# Format de date paramétrable (par défaut : %Y%m%d)
DATE=$(date +"$DATE_FORMAT")
YESTERDAY=$(date -d "yesterday" +"$DATE_FORMAT")
YESTERDAY_FRIDAY=$(date -d "3 days ago" +"$DATE_FORMAT")

# =====================
# Définition des chemins selon l'environnement
# =====================
case "$ENV" in
	PREPROD)
		BASE_PATH="/test/mmd/share";;
	UAT)
		BASE_PATH="/homo/mmd/share";;
	*)
		echo "Environnement inconnu : $ENV"; exit 2;;
esac

# =====================
# Fonctions de copie
# =====================

# Fonction de copie et de renommage de fichiers
process_file() {
	#echo ""copions les fichiers
	# $1 : préfixe, $2 : suffixe, $3 : extension, $4 : dossier source, $5 : dossier destination, $6 : date
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
			echo "[INFO] Copié : $filename → $new_name"
		else
			echo "[WARN] Fichier non trouvé : $filepath"
		fi
	done
}

process_file_issuer() {
	# $1 : préfixe, $2 : suffixe, $3 : timing, $4 : extension, $5 : dossier source, $6 : dossier destination, $7 : date #echo ""copions les fichiers
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
			echo "[INFO] Copié : $filename → $new_name"
		else
			echo "[WARN] Fichier non trouvé : $filepath"
		fi
	done
}

# Fonction de copie et renommage pour fichiers req
process_file_req() {
	# $1 : préfixe, $2 : suffixe, $3 : extension, $4 : dossier source, $5 : dossier destination, $6 : date #echo ""copions les fichiers
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
			local new_name="P${prefix_name}${suffix_name}${DATEL}-$ENV${fileExtension}"
			mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
			echo "[INFO] Copié : $filename → $new_name"
		else
			echo "[WARN] Fichier non trouvé : $filepath"
		fi
	done
}
# =====================
# Fonctions de comparaison DiffMate
# =====================

# Lancement de DiffMate (TXT/JSON)
run_diffmate() {
	# $1 : préfixe, $2 : suffixe, $3 : extension, $4 : config, $5 : dossier destination, $6 : type
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local configfile=$4
	local DEST_DIR=$5
	local difftype=$6
	echo ""
	echo "[INFO] Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
	local preprod_file="$DEST_DIR/${prefix_name}${DATE}${suffix_name}-$ENV${fileExtension}"
	local prod_file="$DEST_DIR/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
	local config_file="$BASE_PATH/testing-tools/diffmate/$configfile"
	echo "[DEBUG] preprod_file = $preprod_file"
	echo "[DEBUG] prod_file = $prod_file"
	echo "[DEBUG] config_file = $config_file"
	if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
		if [ "$difftype" == "JSON" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" $difftype "$prod_file" "$preprod_file" "$DEST_DIR" "$config_file"
		elif [ "$difftype" == "TXT" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" $difftype "$prod_file" "$preprod_file" "$config_file" "$DEST_DIR"
		else
			echo "[ERROR] Erreur de type de comparaison"
		fi
		echo "[INFO] Comparaison reussie : ${prefix_name}-${suffix_name}"
	else
		echo "[ERROR] Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee"
		echo "[ERROR] Prod : $prod_file | Preprod : $preprod_file | Config : $config_file"
	fi
}

# Lancement de DiffMate pour fichiers req
run_diffmate_req() {
	# $1 : préfixe, $2 : suffixe, $3 : extension, $4 : config, $5 : dossier destination, $6 : type
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local configfile=$4
	local DEST_DIR=$5
	local difftype=$6
	echo ""
	echo "[INFO] Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
	local preprod_file="$DEST_DIR/${prefix_name}${DATE}${suffix_name}-$ENV${fileExtension}"
	local prod_file="$DEST_DIR/${prefix_name}${DATE}${suffix_name}-PROD${fileExtension}"
	local config_file="$BASE_PATH/testing-tools/diffmate/$configfile"
	echo "[DEBUG] preprod_file = $preprod_file"
	echo "[DEBUG] prod_file = $prod_file"
	echo "[DEBUG] config_file = $config_file"
	if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
		if [ "$difftype" == "JSON" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" $difftype "$prod_file" "$preprod_file" "$DEST_DIR" "$config_file"
		elif [ "$difftype" == "TXT" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" $difftype "$prod_file" "$preprod_file" "$config_file" "$DEST_DIR"
		elif [ "$difftype" == "REQ" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" $difftype "$prod_file" "$preprod_file" "$DEST_DIR"
		else
			echo "[ERROR] Erreur de type de comparaison"
		fi
		echo "[INFO] Comparaison reussie : ${prefix_name}-${suffix_name}"
	else
		echo "[ERROR] Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee"
		echo "[ERROR] Prod : $prod_file | Preprod : $preprod_file | Config : $config_file"
	fi
}

# =====================
# Appels des fonctions
# =====================

# Copie des fichiers REQ 
process_file_req "_M_B_DE_E_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_DE_ID_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_P_EOD_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_P_E_I_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_DE_EOD_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_req "_M_SM_" "" ".req" "$BASE_PATH/import/vendor/bbg/persec/dl781904/processed" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"

# Copie des fichiers JSON
process_file "Trustee_Security_Main_" "" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"
process_file_issuer "GenericIssuerExport_" "Main_" "08" ".json" "$BASE_PATH/export/ALTO2SGSS" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE"

# Lancer les comparaisons avec Diffmate pour chaque fichiers req
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