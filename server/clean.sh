set -ue

(
  cd "$(dirname "$0")"
  rm -rf ./build/libs/*.jar
)
