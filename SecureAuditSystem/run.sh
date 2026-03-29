#!/bin/bash
# ============================================================
# run.sh - Run the Secure Audit System
# ============================================================

LIB_DIR="lib"
JAR_NAME="SecureAuditSystem.jar"
MAIN_CLASS="com.audit.menu.MainMenu"

MYSQL_JAR=$(find "$LIB_DIR" -name "mysql-connector*.jar" 2>/dev/null | head -1)
if [ -z "$MYSQL_JAR" ]; then
    echo "[ERROR] MySQL JDBC connector not found in lib/"
    exit 1
fi

if [ ! -f "$JAR_NAME" ]; then
    echo "[ERROR] $JAR_NAME not found. Run ./build.sh first."
    exit 1
fi

java -cp "$JAR_NAME:$MYSQL_JAR" $MAIN_CLASS
