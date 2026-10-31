#!/usr/bin/env bash
# Generate a release signing keystore and the matching keystore.properties.
# Run once; keep the resulting .jks and keystore.properties private (gitignored).
set -euo pipefail

KEYSTORE="${1:-release.jks}"
ALIAS="${2:-smartcall}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

read -r -s -p "Keystore password: " STORE_PASS; echo
read -r -s -p "Key password (Enter = same): " KEY_PASS; echo
KEY_PASS="${KEY_PASS:-$STORE_PASS}"

keytool -genkeypair -v \
  -keystore "$ROOT_DIR/$KEYSTORE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "CN=Smart Call CRM, OU=Personal, O=Personal, L=-, S=-, C=IR"

cat > "$ROOT_DIR/keystore.properties" <<EOF
storeFile=$KEYSTORE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$KEY_PASS
EOF

echo "Created $KEYSTORE and keystore.properties (both are gitignored)."
echo "Keep them safe: the SAME keystore is required to ship app updates."
