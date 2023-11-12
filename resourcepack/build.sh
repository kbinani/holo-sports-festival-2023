set -ue

(
  rm -f holo-sports-festival-2023.zip

  cd "$(dirname "$0")"
  (
    cd contents
    zip -q ../holo-sports-festival-2023.zip -r *
  )

  sha1sum holo-sports-festival-2023.zip
)
