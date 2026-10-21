#!/usr/bin/env bash
# Script test nhanh cac trang bang curl (Windows Git Bash / WSL)
set -e
BASE=${BASE:-http://localhost:8080}
COOKIE=$(mktemp)
LOGIN=$(mktemp)

# lay csrf token
curl -s -c "$COOKIE" "$BASE/login" -o "$LOGIN"
CSRF=$(grep -o 'name="_csrf"[^>]*value="[^"]*"' "$LOGIN" | sed 's/.*value="//; s/"//')
if [ -z "$CSRF" ]; then CSRF=$(grep -o 'value="[^"]*"[^>]*name="_csrf"' "$LOGIN" | sed 's/value="//; s/".*//'); fi
echo "CSRF=$CSRF"

# login
curl -s -b "$COOKIE" -c "$COOKIE" -X POST "$BASE/login" \
  --data-urlencode "_csrf=$CSRF" \
  --data-urlencode "username=${USER:-truongkhoa}" \
  --data-urlencode "password=${PASS:-123456}" \
  -o /dev/null -w "login status=%{http_code} redirect=%{redirect_url}\n"

for path in "$@"; do
  code=$(curl -s -b "$COOKIE" "$BASE$path" -o /tmp/page.html -w "%{http_code}")
  if [ "$code" = "200" ]; then
    echo "OK   $path ($code)"
  else
    echo "FAIL $path ($code)"
    grep -o '<h2>[^<]*</h2>' /tmp/page.html | head -2
  fi
done
rm -f "$COOKIE" "$LOGIN"
