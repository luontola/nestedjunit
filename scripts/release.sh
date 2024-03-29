#!/bin/bash
set -eu

echo "Version number of the release to create, in MAJOR.MINOR.PATCH format: "
read RELEASE_VERSION
if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: invalid format"
    exit 1
fi

echo "Version number of the next development increment, in MAJOR.MINOR.PATCH format: "
read NEXT_VERSION
if [[ ! "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: invalid format"
    exit 1
fi

set -x


# Release

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$RELEASE_VERSION"

git add pom.xml
git commit -m "Release $RELEASE_VERSION"

mvn clean deploy \
    -Psonatype-oss-release
mvn nexus:staging-close \
    -Dnexus.description="NestedJUnit $RELEASE_VERSION"

# Tag only after we know the release succeeded, so we can retry with
# just a `git reset --hard HEAD^` without having to delete a tag also.
git tag -s -m "Release $RELEASE_VERSION" "v$RELEASE_VERSION"


# Next increment

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="$NEXT_VERSION-SNAPSHOT"

git add pom.xml
git commit -m "Prepare for next development increment"

set +x
echo "Done. Next steps:"
echo "- Go to https://oss.sonatype.org/ and release the staging repository."
echo "- git push; git push --tags"
