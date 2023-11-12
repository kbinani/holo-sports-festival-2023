set -ue

(
  cd "$(dirname "$0")"
  ./gradlew assemble
)
