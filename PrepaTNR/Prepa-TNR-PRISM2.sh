#!/bin/bash

#mode debub
DebugInit=0
DebugProcessFile=0
DebugDiffmate=0


# VÃ©rification de l'environnement 
if [ -z "$1" ]; then
  echo "Usage: $0 <ENV> (ex: PREPROD, UAT)"
  exit 1
fi

ENV=$1
#ce serait bien de mettre le format de date en parametre ...
# On stocke toutes les dates de référence dans un format brut unique (ici %Y%m%d)
DATE_RAW=$(date +"%Y%m%d")
#YESTERDAY_RAW correspond à la date d'hier au format brut
YESTERDAY_RAW=$(date -d "yesterday" +"%Y%m%d")
#FRIDAY_RAW correspond à la date de vendredi dernier (3 jours avant aujourd'hui) au format brut
FRIDAY_RAW=$(date -d "3 days ago" +"%Y%m%d")


# Chemins selon l'environnement
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


transform_date() {
	local DATE="$1"
	local formatDate="$2"
	# Conversion de la date depuis le format %Y%m%d vers le format cible
	date -d "$(echo $DATE | sed 's/\(....\)\(..\)\(..\)/\1-\2-\3/')" +"$formatDate"
}


# Fonction de copie 
process_file() {
	# Harmonisation : on passe la date de référence et le format en argument
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local SRC_DIR=$4
	local DEST_DIR=$5
	local date_ref=$6
	local formatDate=$7
	local DATEL=$(transform_date "$date_ref" "$formatDate")
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${DATEL}"*"${suffix_name}"*"${fileExtension}" )
  for filepath in "${files[@]}"; do
    if [ -f "$filepath" ]; then
      local filename=$(basename "$filepath")
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    else
      echo "fichier non copié : $filepath"
    fi
  done
}

process_file_scope() {
	# Harmonisation : on passe la date de référence et le format en argument
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local SRC_DIR=$4
	local DEST_DIR=$5
	local date_ref=$6
	local formatDate=$7
	local DATEL=$(transform_date "$date_ref" "$formatDate")
	mkdir -p "$DEST_DIR"
	local files=( "${SRC_DIR}/${prefix_name}${fileExtension}${DATEL}" )
  for filepath in "${files[@]}"; do
    if [ -f "$filepath" ]; then
      local filename=$(basename "$filepath")
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${suffix_name}-${ENV}${fileExtension}${DATEL}"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    else
      echo "fichier non copié : $filepath"
    fi
  done
}

run_diffmate() {
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local configfile=$4
	local DEST_DIR=$5
	local difftype=$6
	local date_ref=$7
	local formatDate=$8
	local DATEL=$(transform_date "$date_ref" "$formatDate")
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
		if [ "${difftype}" == "JSON" ]; then
			if [ $DebugDiffmate = 1 ] ; then
				echo "Type = JSON"
				echo "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
					"$prod_file" \
					"$test_file" \
					"$DEST_DIR" \
					"$config_file"
			fi
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
				"$prod_file" \
				"$test_file" \
				"$DEST_DIR" \
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
		echo "Comparaison reussie : ${prefix_name}-${suffix_name}"
		echo "Results available on : $DEST_DIR"
	else
		echo "Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuee"
		echo "Prod file should be in : $prod_file"
		echo "$ENV file should be in : $test_file"
		echo "Config_file file should be in : $config_file"
	fi
}

run_diffmate_scope() {
	local prefix_name=$1
	local suffix_name=$2
	local fileExtension=$3
	local configfile=$4
	local DEST_DIR=$5
	local difftype=$6
	local date_ref=$7
	local formatDate=$8
	local DATEL=$(transform_date "$date_ref" "$formatDate")
	local preprod_file="${DEST_DIR}/${prefix_name}${suffix_name}-${ENV}${fileExtension}${DATEL}"
	local prod_file="${DEST_DIR}/${prefix_name}${suffix_name}-PROD${fileExtension}${DATEL}"
	local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
	echo "Lancement de DiffMate pour ${prefix_name}-${suffix_name}"
	if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
		echo "difftype = ${difftype=}"
		if [ "${difftype}" == "JSON" ]; then
			"$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} \
				"$prod_file" \
				"$preprod_file" \
				"$DEST_DIR" \
				"$config_file"
		elif [ "${difftype}" == "TXT" ]; then
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



# Lancer les copies (harmonisé)
process_file "" "_SG0001_ASSETS_REF_DATA" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"
process_file "" "_SG0001_TRANSCO_EXPORT" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"
process_file "" "_SG0001_ASSETS_PRICES" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"
process_file "" "_SG0001_AGENTS" ".txt" "$BASE_PATH/export/prism/processed" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"
# Pour PRISM_CONTROL, on veut la date au format %d%m%Y
process_file "PRISM_CONTROL_" "" ".txt" "$BASE_PATH/export/prism/reports" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%d%m%Y"
process_file_scope "PrismInput" "Supp" ".txt_" "$BASE_PATH/import/vendor/vdf/req/suppliment_sent_req" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"
process_file_scope "PrismInput" "Main" ".txt_" "$BASE_PATH/import/vendor/vdf/req/Prism_sent_reqs" "$BASE_PATH/testing-tools/TNR/PRISM" "$YESTERDAY_RAW" "%Y%m%d"


# Lancer DiffMate pour chaque fichier (harmonisé)
run_diffmate "" "_SG0001_ASSETS_REF_DATA" ".txt" "ConfigDiffMate-TNR-PRISM-RefData.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 60
run_diffmate "" "_SG0001_TRANSCO_EXPORT" ".txt" "ConfigDiffMate-TNR-PRISM-Transco.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 60
run_diffmate "" "_SG0001_ASSETS_PRICES" ".txt" "ConfigDiffMate-TNR-PRISM-AssetPrice.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 60
run_diffmate "" "_SG0001_AGENTS" ".txt" "ConfigDiffMate-TNR-PRISM-Agent.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 60
run_diffmate "PRISM_CONTROL_" "" ".txt" "ConfigDiffMate-TNR-PRISM-PrismControl.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%d%m%Y"
sleep 60
run_diffmate_scope "PrismInputMain" "" ".txt_" "ConfigDiffMate-TNR-PRISM-PrismInput.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"
sleep 60
run_diffmate_scope "PrismInputSupp" "" ".txt_" "ConfigDiffMate-TNR-PRISM-PrismInput.json" "$BASE_PATH/testing-tools/TNR/PRISM" "TXT" "$YESTERDAY_RAW" "%Y%m%d"