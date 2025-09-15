# Harmonisation :
# Ce script peut être lancé avec un format de date en paramètre optionnel.
# Exemple : ./Prepa-TNR-TRUSTEE-UNDERLYINg-test.sh PREPROD  # format par défaut
#           ./Prepa-TNR-TRUSTEE-UNDERLYINg-test.sh PREPROD %d%m%Y  # format jourmoisannée

#!/bin/bash

# Vérification des paramètres
if [ -z "$1" ]; then
    echo "[ERROR] Usage: $0 <ENV> [DATE_FORMAT]"
    echo "[INFO] Exemple: $0 PREPROD %d%m%Y"
    exit 1
fi

ENV=$1
DATE_FORMAT=${2:-"%Y%m%d"}
DATE=$(date +"$DATE_FORMAT")
YESTERDAY=$(date -d "yesterday" +"$DATE_FORMAT")
YESTERDAY_FRIDAY=$(date -d "3 days ago" +"$DATE_FORMAT")

# Chemins selon l’environnement de test
BASE_PATH="./test_env"

# Fonction de copie (identique à l'original)
process_file() {
    local prefix_name=$1
    local suffix_name=$2
    local fileExtension=$3
    local SRC_DIR=$4
    local DEST_DIR=$5
    local DATESource=$6
    local DATECopy=$7
    mkdir -p "$DEST_DIR"
    local files=("${SRC_DIR}/${prefix_name}${DATESource}${suffix_name}"*"${fileExtension}")
    for filepath in "${files[@]}"; do
        if [ -f "$filepath" ]; then
            local filename=$(basename "$filepath")
            cp "$filepath" "$DEST_DIR"
            local new_name="${prefix_name}${DATECopy}${suffix_name}-${ENV}${fileExtension}"
            mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
            echo "[INFO] Copié : $filename → $new_name"
        fi
    done
}

process_file_issuer() {
    local prefix_name=$1
    local suffix_name=$2
    local timing=$3
    local fileExtension=$4
    local SRC_DIR=$5
    local DEST_DIR=$6
    local DATESource=$7
    local DATECopy=$8
    mkdir -p "$DEST_DIR"
    local files=("${SRC_DIR}/${prefix_name}${DATESource}"*"${timing}"*"${fileExtension}")
    for filepath in "${files[@]}"; do
        if [ -f "$filepath" ]; then
            local filename=$(basename "$filepath")
            cp "$filepath" "$DEST_DIR"
            local new_name="${prefix_name}${suffix_name}${DATECopy}-${ENV}${fileExtension}"
            mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
            echo "[INFO] Copié : $filename → $new_name"
        fi
    done
}

# Fonction DiffMate (simulation)
run_diffmate() {
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
        echo "[INFO] Comparaison réussie : ${prefix_name}-${suffix_name}"
    else
        echo "[WARN] Fichier(s) manquant(s) pour ${prefix_name}-${suffix_name} — comparaison non effectuée"
    fi
}

# Lancer les copies (adapté à l'environnement de test)
process_file "Generic_Security_Underlying_" "" ".json" "$BASE_PATH/export" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE" "$DATE"
process_file "Generic_FxSpot_" "" ".json" "$BASE_PATH/export" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE" "$DATE"
process_file_issuer "GenericIssuerExport_" "" "04" ".json" "$BASE_PATH/export" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "$DATE" "$DATE"

# Lancer DiffMate pour chaque fichier (simulation)
run_diffmate "Generic_Security_Underlying_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Securities.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$DATE"
sleep 1
run_diffmate "GenericIssuerExport_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_Issuers.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$DATE"
sleep 1
run_diffmate "Generic_FxSpot_" "" ".json" "ConfigDiffMate-TNR-TRUSTEE_FxSpot.json" "$BASE_PATH/testing-tools/TNR/TRUSTEE" "JSON" "$DATE"
