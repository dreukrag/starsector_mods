#!/usr/bin/env bash
# Build a mod's jar: compiles mods/<mod>/src/**/*.java into mods/<mod>/jars/<mod>.jar.
#
# Usage:
#   tools/build_mod.sh <mod_folder_name>
#
# Classpath:
#   - If STARSECTOR_CORE points at <Starsector>/starsector-core, the game's own jars are used
#     (most accurate: exactly what the game runs).
#   - Otherwise it compiles against reference/starfarer-api/src plus the game's third-party
#     libraries downloaded from Maven Central into .build/libs (cached, gitignored).
#
# Output targets Java 16 bytecode by default (override with JAVA_RELEASE=<n>). Starting with 0.98.5a the
# game's own API sources use Java 16 syntax (pattern-matching instanceof), so the game runs on Java 16+
# and 16 is the lowest level that can compile against them.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
MOD="${1:?usage: tools/build_mod.sh <mod_folder_name>}"
MOD_DIR="$REPO/mods/$MOD"
[ -d "$MOD_DIR/src" ] || { echo "error: $MOD_DIR/src not found" >&2; exit 1; }

OUT="$REPO/.build/classes/$MOD"
rm -rf "$OUT" && mkdir -p "$OUT" "$MOD_DIR/jars"

if [ -n "${STARSECTOR_CORE:-}" ]; then
  CP="$(find "$STARSECTOR_CORE" -maxdepth 1 -name '*.jar' | tr '\n' ':')"
  SRCPATH=()
else
  LIBS="$REPO/.build/libs"
  mkdir -p "$LIBS"
  M=https://repo1.maven.org/maven2
  for u in \
    org/lwjgl/lwjgl/lwjgl/2.9.3/lwjgl-2.9.3.jar \
    org/lwjgl/lwjgl/lwjgl_util/2.9.3/lwjgl_util-2.9.3.jar \
    org/json/json/20090211/json-20090211.jar \
    log4j/log4j/1.2.17/log4j-1.2.17.jar \
    com/thoughtworks/xstream/xstream/1.4.10/xstream-1.4.10.jar \
    javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar; do
    f="$LIBS/$(basename "$u")"
    [ -f "$f" ] || curl -sSfL -o "$f" "$M/$u"
  done
  CP="$(find "$LIBS" -name '*.jar' | tr '\n' ':')"
  # API sources are Windows-1252 encoded; only classes actually referenced get compiled.
  SRCPATH=(-sourcepath "$REPO/reference/starfarer-api/src")
fi

# Compile-only jars of other mods (e.g. LunaLib), listed per mod in tools/compile_libs/<mod>.txt as
# "<file name> <url>" lines. Downloaded once into .build/extlibs; never packaged into the mod's jar.
LIBLIST="$REPO/tools/compile_libs/$MOD.txt"
if [ -f "$LIBLIST" ]; then
  EXT="$REPO/.build/extlibs"
  mkdir -p "$EXT"
  while read -r name url; do
    [ -z "${name:-}" ] || [ "${name:0:1}" = "#" ] && continue
    [ -f "$EXT/$name" ] || curl -sSfL -o "$EXT/$name" "$url"
    CP="$CP:$EXT/$name"
  done < "$LIBLIST"
fi

mapfile -t SOURCES < <(find "$MOD_DIR/src" -name '*.java')
set +e
javac -encoding Cp1252 -proc:none -nowarn --release "${JAVA_RELEASE:-16}" \
  -cp "$CP" "${SRCPATH[@]}" -d "$OUT" "${SOURCES[@]}" 2>&1 \
  | grep -vE '^(Picked up|warning:|Note:)|bootstrap class path|^[0-9]+ warnings?$'
JAVAC_STATUS=${PIPESTATUS[0]}
set -e
if [ "$JAVAC_STATUS" -ne 0 ]; then
  echo "error: compilation failed; existing jar left unchanged" >&2
  exit 1
fi

# Package only the mod's own classes, never the API classes compiled from -sourcepath.
JAR="$MOD_DIR/jars/$MOD.jar"
rm -f "$JAR"
(cd "$OUT" && mapfile -t CLASSES < <(cd "$MOD_DIR/src" && find . -name '*.java' | sed 's/\.java$//') \
  && ls "${CLASSES[@]/%/.class}" >/dev/null \
  && jar cf "$JAR" $(for c in "${CLASSES[@]}"; do ls "$c.class" "$c"\$*.class 2>/dev/null; done))
echo "built $(realpath --relative-to="$REPO" "$JAR")"
jar tf "$JAR" | grep '\.class$'
