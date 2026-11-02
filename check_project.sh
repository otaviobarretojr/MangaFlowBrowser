#!/usr/bin/env bash
set -euo pipefail
node --check app/src/main/assets/reader/blocker.js
node --check app/src/main/assets/reader/reader.js
python3 - <<'PY'
import json,xml.etree.ElementTree as ET
json.load(open('app/src/main/assets/reader/manifest.json'))
for p in ['app/src/main/AndroidManifest.xml', 'app/src/main/res/values/styles.xml', 'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml', 'app/src/main/res/values/colors.xml']:
    ET.parse(p)
print('Manifest, recursos XML e scripts: OK')
PY
