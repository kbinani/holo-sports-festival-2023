set -ue

(
  cd "$(dirname "$0")"
  (
    cd ./Paper/patches
    git reset HEAD .
    git clean -xdf
  )

  for dir in $(ls ./patches); do
    number=$(ls ./Paper/patches/$dir/ | cut -c 1-4 | sort -nr | head -1 | bc)
    for patch in $(ls ./patches/$dir | sort -n); do
      number=$((number + 1))
      trailing=$(echo "$patch" | cut -c 6-)
      prefix=$(printf %04d $number)
      cp "./patches/$dir/$patch" "./Paper/patches/$dir/${prefix}-${trailing}"
    done
  done

  (
    cd ./Paper
    ./gradlew applyPatches
    ./gradlew createReobfBundlerJar
  )
)
