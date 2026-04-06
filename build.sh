#!/bin/bash
# ============================================================
# build.sh - Compile and package the Secure Audit System
# ============================================================

echo "============================================================"
echo "  SECURE AUDIT LOGGING & MONITORING SYSTEM - BUILD SCRIPT"
echo "============================================================"

# --- Config ---
SRC_DIR="src"
OUT_DIR="out"
LIB_DIR="lib"
JAR_NAME="SecureAuditSystem.jar"
MAIN_CLASS="com.audit.menu.MainMenu"

# --- Check for mysql connector jar ---
MYSQL_JAR=$(find "$LIB_DIR" -name "mysql-connector*.jar" 2>/dev/null | head -1)
if [ -z "$MYSQL_JAR" ]; then
    echo "[ERROR] MySQL JDBC connector not found in lib/"
    echo "  Download from: https://dev.mysql.com/downloads/connector/j/"
    echo "  Place the .jar inside the lib/ directory and re-run."
    exit 1
fi
echo "[OK] Found MySQL connector: $MYSQL_JAR"

# --- Clean & create output dir ---
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# --- Find all .java files ---
find "$SRC_DIR" -name "*.java" > sources.txt
echo "[INFO] Compiling $(wc -l < sources.txt) source files..."

# --- Compile ---
javac -cp "$MYSQL_JAR" -d "$OUT_DIR" @sources.txt
if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed."
    rm sources.txt
    exit 1
fi
rm sources.txt
echo "[OK] Compilation successful."

# --- Package into JAR ---
jar cfe "$JAR_NAME" "$MAIN_CLASS" -C "$OUT_DIR" .
echo "[OK] JAR created: $JAR_NAME"

echo ""
echo "To run the application:"
echo "  java -cp \"$JAR_NAME:$MYSQL_JAR\" $MAIN_CLASS"
echo ""
echo "Or use:  ./run.sh"
