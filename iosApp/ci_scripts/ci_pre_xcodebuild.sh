#!/bin/sh
# Xcode Cloud runs this right before xcodebuild. It stamps the version the same way the GitHub
# workflow does for Android and desktop – from the git tag – because the App Store rejects an
# upload whose CFBundleShortVersionString is not higher than the last approved one, and a version
# typed into Config.xcconfig by hand is exactly the number that gets forgotten.
#
# * Tag build (v1.2.3, v1.2.3-rc1): the tag, without the "v" and without a pre-release suffix,
#   since Apple allows only up to three dot-separated integers.
# * Branch build: the latest release tag, so a build from main between releases is uploaded as a
#   newer build of the version already released – never as an older one.
# * Build number: Xcode Cloud's own counter, which only ever goes up.
set -e

REPO="${CI_PRIMARY_REPOSITORY_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"
CONFIG="$REPO/iosApp/Configuration/Config.xcconfig"
cd "$REPO"

if [ -n "$CI_TAG" ]; then
  TAG="$CI_TAG"
else
  # The clone may not carry tags; fetch them so the branch build can find the last release.
  git fetch --tags --force --quiet || true
  TAG="$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || true)"
fi

VERSION="$(echo "$TAG" | sed -E 's/^v//; s/-.*$//')"
if ! echo "$VERSION" | grep -Eq '^[0-9]+(\.[0-9]+){0,2}$'; then
  echo "error: no usable version from tag '$TAG' – tag releases as vX.Y.Z" >&2
  exit 1
fi

BUILD="${CI_BUILD_NUMBER:-1}"

sed -i '' -E "s/^MARKETING_VERSION=.*/MARKETING_VERSION=$VERSION/" "$CONFIG"
sed -i '' -E "s/^CURRENT_PROJECT_VERSION=.*/CURRENT_PROJECT_VERSION=$BUILD/" "$CONFIG"

echo "=== Version: $VERSION (build $BUILD) from tag '$TAG' ==="
grep -E '^(MARKETING_VERSION|CURRENT_PROJECT_VERSION)=' "$CONFIG"
