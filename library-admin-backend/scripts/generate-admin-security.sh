#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 '<admin-password>'"
  exit 1
fi

PASSWORD="$1"
CRYPTO_JAR="${HOME}/.m2/repository/org/springframework/security/spring-security-crypto/6.3.4/spring-security-crypto-6.3.4.jar"
SPRING_JCL_JAR="${HOME}/.m2/repository/org/springframework/spring-jcl/6.1.14/spring-jcl-6.1.14.jar"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}

trap cleanup EXIT

if [[ ! -f "$CRYPTO_JAR" ]]; then
  echo "spring-security-crypto jar not found at: $CRYPTO_JAR"
  echo "Run 'mvn -f library-admin-backend/pom.xml test' first."
  exit 1
fi

if [[ ! -f "$SPRING_JCL_JAR" ]]; then
  echo "spring-jcl jar not found at: $SPRING_JCL_JAR"
  echo "Run 'mvn -f library-admin-backend/pom.xml test' first."
  exit 1
fi

JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"

cat > "$TMP_DIR/AdminHashPrinter.java" <<'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AdminHashPrinter {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
EOF

javac -cp "$CRYPTO_JAR:$SPRING_JCL_JAR" -d "$TMP_DIR" "$TMP_DIR/AdminHashPrinter.java"

ADMIN_PASSWORD_HASH="$(
  java -cp "$TMP_DIR:$CRYPTO_JAR:$SPRING_JCL_JAR" AdminHashPrinter "$PASSWORD"
)"

if [[ -z "$ADMIN_PASSWORD_HASH" ]]; then
  echo "Failed to generate ADMIN_PASSWORD_HASH"
  exit 1
fi

cat <<EOF
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=$ADMIN_PASSWORD_HASH
ADMIN_JWT_SECRET=$JWT_SECRET
EOF
