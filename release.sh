#!/bin/bash
set -e

VERSION=${1:-}
if [ -z "$VERSION" ]; then
    read -p "Version (e.g. 1.2.0): " VERSION
fi

BRANCHES=("main" "mc/1.21.4" "mc/1.21.1" "mc/1.20.4")

echo "Releasing v$VERSION across ${#BRANCHES[@]} branches..."

for BRANCH in "${BRANCHES[@]}"; do
    echo "  -> $BRANCH"
    git checkout "$BRANCH" -q
    sed -i "s/^mod_version=.*/mod_version=$VERSION/" gradle.properties
    git add gradle.properties
    git diff --cached --quiet || git commit -m "Release $VERSION" -q
done

git checkout main -q
git tag "v$VERSION"
git push origin --all -q
git push origin "v$VERSION" -q

echo "Done! v$VERSION is releasing on Modrinth."
