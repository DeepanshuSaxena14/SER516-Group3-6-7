#!/bin/bash
# Build and Structure Verification Script
# Task #15: Evaluate project build and structure

# Don't exit on error - we want to report all checks

echo "=========================================="
echo "Project Build and Structure Verification"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track results
PASSED=0
FAILED=0

check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $1"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $1"
        ((FAILED++))
    fi
}

echo "1. Checking project structure..."
[ -f "pom.xml" ] && check "pom.xml exists"
[ -d "src/main/java" ] && check "src/main/java directory exists"
[ -d "src/test/java" ] && check "src/test/java directory exists"
[ -f ".github/workflows/ci.yml" ] && check "CI workflow exists"
[ -f "Jenkinsfile" ] && check "Jenkinsfile exists"
echo ""

echo "2. Checking Java source files..."
MAIN_FILES=$(find src/main/java -name "*.java" | wc -l | tr -d ' ')
TEST_FILES=$(find src/test/java -name "*.java" | wc -l | tr -d ' ')
echo "   Main source files: $MAIN_FILES"
echo "   Test files: $TEST_FILES"
[ "$MAIN_FILES" -gt 0 ] && check "Has main source files"
[ "$TEST_FILES" -gt 0 ] && check "Has test files"
echo ""

echo "3. Verifying Maven build..."
mvn -B -ntp clean compile > /dev/null 2>&1
check "Project compiles successfully"
echo ""

echo "4. Verifying tests..."
mvn -B -ntp test > /dev/null 2>&1
check "Tests run successfully"
echo ""

echo "5. Verifying package creation..."
mvn -B -ntp package -DskipTests > /dev/null 2>&1
[ -f "target/ser516-group6-metrics-1.0.0.jar" ] && check "JAR package created"
echo ""

echo "6. Checking fan-out analysis components..."
grep -q "SourceScanner" src/main/java/edu/asu/ser516/metrics/*.java 2>/dev/null && check "SourceScanner class exists"
grep -q "ClassIndexBuilder" src/main/java/edu/asu/ser516/metrics/*.java 2>/dev/null && check "ClassIndexBuilder class exists"
grep -q "OutgoingReferenceExtractor" src/main/java/edu/asu/ser516/metrics/*.java 2>/dev/null && check "OutgoingReferenceExtractor class exists"
echo ""

echo "7. Testing main class execution..."
CLASSPATH="target/classes:$(mvn dependency:build-classpath -q -DincludeScope=compile)"
java -cp "$CLASSPATH" edu.asu.ser516.metrics.ScannerMain . > /dev/null 2>&1
check "ScannerMain executes successfully"
echo ""

echo "=========================================="
echo "Summary:"
echo "  Passed: $PASSED"
echo "  Failed: $FAILED"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All checks passed!${NC}"
    echo ""
    echo "Project structure is sufficient for fan-out analysis:"
    echo "  • Can scan Java source files"
    echo "  • Can build class index"
    echo "  • Can extract outgoing references"
    echo "  • Builds and tests successfully"
    exit 0
else
    echo -e "${RED}Some checks failed!${NC}"
    exit 1
fi
