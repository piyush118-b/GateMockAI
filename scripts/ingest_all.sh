#!/bin/bash
# ─── GateMockAI Bulk Ingestion Script ────────────────────────────────────────
# Ingests all PDFs in the data/ folder sequentially via the REST API.
# Run from project root: bash scripts/ingest_all.sh

set -e
BASE_URL="http://localhost:8085"
DATA_DIR="$(dirname "$0")/../data"
COOKIE_JAR="/tmp/gate_ingest_cookies.txt"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║         GateMockAI — Bulk PDF Ingestion              ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# ── Login ────────────────────────────────────────────────────────────────────
echo "🔐 Logging in as admin..."
HTTP=$(curl -s -c "$COOKIE_JAR" -X POST "$BASE_URL/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin%40gate.com&password=Admin%40123" \
  -o /dev/null -w "%{http_code}")
if [ "$HTTP" != "302" ]; then
  echo "❌ Login failed (HTTP $HTTP). Is the backend running on port 8085?"
  exit 1
fi
echo "✅ Login successful"
echo ""

# ── Ingestion table ──────────────────────────────────────────────────────────
# Define PDF → paperId, examName, year, branch
declare -a PDFS=(
  "2dc0da1f-6ffa-4c2a-a829-a2c17d0c7e74.pdf|gate_cse_2025|GATE CSE 2025|2025|CSE"
  "p1.pdf|gate_cse_2024|GATE CSE 2024|2024|CSE"
  "pp2.pdf|gate_cse_2023|GATE CSE 2023|2023|CSE"
  "CS124S5.pdf|gate_cse_2022|GATE CSE 2022|2022|CSE"
)

SUCCESS=0
FAILED=0

for entry in "${PDFS[@]}"; do
  IFS='|' read -r FILENAME PAPER_ID EXAM_NAME YEAR BRANCH <<< "$entry"
  PDF_PATH="$DATA_DIR/$FILENAME"

  if [ ! -f "$PDF_PATH" ]; then
    echo "⚠️  Skipping '$FILENAME' — file not found"
    continue
  fi

  SIZE=$(du -sh "$PDF_PATH" | cut -f1)
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "📄 Ingesting: $FILENAME ($SIZE)"
  echo "   paperId: $PAPER_ID | examName: $EXAM_NAME"
  echo "   ⏳ Sending to Gemini 3.5 Flash... (may take 2-4 minutes)"
  echo ""

  RESPONSE=$(curl -s -b "$COOKIE_JAR" \
    -X POST "$BASE_URL/api/pipeline/ingest" \
    -F "questionPaper=@$PDF_PATH" \
    -F "paperId=$PAPER_ID" \
    -F "examName=$EXAM_NAME" \
    -F "year=$YEAR" \
    -F "branch=$BRANCH" \
    -w "\nHTTP_CODE:%{http_code}" 2>&1)

  HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
  BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE:")

  if [ "$HTTP_CODE" = "200" ]; then
    TOTAL_Q=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('totalQuestions','?'))" 2>/dev/null || echo "?")
    echo "   ✅ SUCCESS — $TOTAL_Q questions extracted"
    echo "   Response: $BODY" | head -5
    SUCCESS=$((SUCCESS+1))
  else
    echo "   ❌ FAILED (HTTP $HTTP_CODE)"
    echo "   Error: $BODY" | head -3
    FAILED=$((FAILED+1))
  fi
  echo ""
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Ingestion Complete: ✅ $SUCCESS succeeded | ❌ $FAILED failed"
echo ""
