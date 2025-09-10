
#!/bin/bash

# Harmonisation : chaque fonction prend la date en argument
if [ -z "$1" ]; then
  echo "Usage: $0 <ENV> (ex: PREPROD, UAT)"
  exit 1
fi

ENV=$1

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

SRC_DIR="$BASE_PATH/export/prism/processed"
DEST_DIR="$BASE_PATH/testing-tools/TNR/PRISM"
mkdir -p "$DEST_DIR"

# Fonction de copie harmonisée
process_file() {
  local base_name=$1
  local date_fmt=$2
  local date_val=$(date -d "yesterday" +"$date_fmt")
  local files=( "$SRC_DIR/${date_val}_$base_name.txt" )
  for filepath in "${files[@]}"; do
    if [ -f "$filepath" ]; then
      local filename=$(basename "$filepath")
      cp "$filepath" "$DEST_DIR"
      local new_name="${date_val}_${base_name}-${ENV}.txt"
      mv "$DEST_DIR/$filename" "$DEST_DIR/$new_name"
      echo "Copié : $filename → $new_name"
    fi
  done
}

# Fonction DiffMate harmonisée
run_diffmate() {
  local base_name=$1
  local date_fmt=$2
  local date_val=$(date -d "yesterday" +"$date_fmt")
  local preprod_file="${DEST_DIR}/${date_val}_${base_name}-${ENV}.txt"
  local prod_file="${DEST_DIR}/${date_val}_${base_name}-PROD.txt"
  local config_file="${BASE_PATH}/testing-tools/diffmate/ConfigDiffMate-TNR-PRISM-${base_name}.json"
  case "$base_name" in
    SG0001_ASSETS_REF_DATA)
      config_file="${BASE_PATH}/testing-tools/diffmate/ConfigDiffMate-TNR-PRISM-RefData.json"
      ;;
    SG0001_TRANSCO_EXPORT)
      config_file="${BASE_PATH}/testing-tools/diffmate/ConfigDiffMate-TNR-PRISM-Transco.json"
      ;;
    SG0001_ASSETS_PRICES)
      config_file="${BASE_PATH}/testing-tools/diffmate/ConfigDiffMate-TNR-PRISM-AssetPrice.json"
      ;;
    SG0001_AGENTS)
      config_file="${BASE_PATH}/testing-tools/diffmate/ConfigDiffMate-TNR-PRISM-Agent.json"
      ;;
    *)
      echo "Aucun fichier de configuration DiffMate trouvé pour : $base_name"
      return
      ;;
  esac
  if [[ -f "$prod_file" && -f "$preprod_file" ]]; then
    "$BASE_PATH/testing-tools/diffmate/run_diffmate.sh" TXT \
      "$prod_file" \
      "$preprod_file" \
      "$config_file" \
      "$DEST_DIR"
    echo "Comparaison reussie : $base_name"
  else
    echo "Fichier(s) manquant(s) pour $base_name — comparaison non effectuee"
  fi
}

# Lancer les copies avec format explicite
process_file "SG0001_ASSETS_REF_DATA" "%Y%m%d"
process_file "SG0001_TRANSCO_EXPORT" "%Y%m%d"
process_file "SG0001_ASSETS_PRICES" "%Y%m%d"
process_file "SG0001_AGENTS" "%Y%m%d"

# Lancer DiffMate pour chaque fichier avec format explicite
run_diffmate "SG0001_ASSETS_REF_DATA" "%Y%m%d"
run_diffmate "SG0001_TRANSCO_EXPORT" "%Y%m%d"
run_diffmate "SG0001_ASSETS_PRICES" "%Y%m%d"
run_diffmate "SG0001_AGENTS" "%Y%m%d"
