#!/bin/bash
# common_diffmate_utils.sh
# Utilitaires communs pour scripts DiffMate (copie, renommage, logs, lancement)
# Usage : source ce fichier dans vos scripts

# Debug flags (0=off, 1=on)
DEBUG_INIT=0
DEBUG_PROCESS=0
DEBUG_DIFFMATE=0

# Fonction de log
log() {
  local level="$1"; shift
  echo "[$level] $*"
}

# Fonction de transformation de date (formatage)
transform_date() {
  local DATE="$1"
  local FORMAT="$2"
  date -d "$(echo $DATE | sed 's/\(....\)\(..\)\(..\)/\1-\2-\3/')" +"$FORMAT"
}

# Fonction générique de copie/renommage
process_file() {
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local SRC_DIR=$4
  local DEST_DIR=$5
  local DATEL=$6
  mkdir -p "$DEST_DIR"
  local files=("${SRC_DIR}/${prefix_name}${DATEL}${suffix_name}"*"${fileExtension}")
  [ $DEBUG_PROCESS = 1 ] && log INFO "Recherche fichiers : ${files[*]}"
  for filepath in "${files[@]}"; do
    if [ -f "$filepath" ]; then
      local filename=$(basename "$filepath")
      cp "$filepath" "$DEST_DIR"
      local new_name="${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      log INFO "Copié : $filename → $new_name"
    else
      log WARN "Fichier non trouvé : $filepath"
    fi
  done
}

# Fonction générique de lancement DiffMate
run_diffmate() {
  local prefix_name=$1
  local suffix_name=$2
  local fileExtension=$3
  local configfile=$4
  local DEST_DIR=$5
  local difftype=$6
  local DATEL=$7
  local prod_file="${DEST_DIR}/${prefix_name}${DATEL}${suffix_name}-PROD${fileExtension}"
  local test_file="${DEST_DIR}/${prefix_name}${DATEL}${suffix_name}-${ENV}${fileExtension}"
  local config_file="${BASE_PATH}/testing-tools/diffmate/${configfile}"
  [ $DEBUG_DIFFMATE = 1 ] && log INFO "DiffMate : $prod_file vs $test_file (config: $config_file)"
  if [[ -f "$prod_file" && -f "$test_file" ]]; then
    "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" ${difftype} "$prod_file" "$test_file" "$DEST_DIR" "$config_file"
    log INFO "Comparaison réussie : $prefix_name$suffix_name ($DATEL)"
  else
    log ERROR "Fichier(s) manquant(s) pour $prefix_name$suffix_name ($DATEL)"
    log ERROR "Prod : $prod_file | Test : $test_file | Config : $config_file"
  fi
}
