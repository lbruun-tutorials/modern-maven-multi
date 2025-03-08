#!/bin/bash
#
#  Execute Maven in a GitHub workflow. Suitable for library projects.
#
#  The POM is expected to use the "Maven CI Friendly" paradigm.
#  (https://maven.apache.org/maven-ci-friendly.html)
#
#  'mvn verify' will be executed by default. However, if executing from a SemVer-like tag
#  then 'mvn deploy' is executed instead.
#

set -e



# Constants


# SemVer regular expression:
#  - The tag can optionally start with 'v' but the 'v' doesn't become part of the Maven version string.
#
#  - We allow the X.Y.Z version to have a pre-release suffix, e.g. "3.2.0-RC1" but if
#    so we tell Maven that this is a SNAPSHOT release. In other words: tag "3.2.0-RC1" will
#    be published as version "3.2.0-RC1-SNAPSHOT" and will therefore go into the OSS Sonatype snapshot repo,
#    not Maven Central.
#
semver_regex='^v?([0-9]+\.[0-9]+\.[0-9]+(\-[0-9a-zA-Z.]+)*)$'



# Defaults
mvn_phase="verify"
mvn_ci_revision=""
mvn_ci_sha1_short="${GITHUB_SHA::7}"
mvn_ci_changelist=""
mvn_profiles_active=""



if [ "$GITHUB_REF_TYPE" = "tag" ]; then
  # Does tag look like a SemVer string?
  if [[ "$GITHUB_REF_NAME" =~ $semver_regex ]]; then
    if [ "$GITHUB_EVENT_NAME" != "release" ]; then
      echo "A SemVer-like tag, \"$GITHUB_REF_NAME\", was pushed to GitHub independently of the GitHub Releases feature."
      echo "Releases must be created from the GitHub UI Releases page."
      exit 1
    else
      semver=${BASH_REMATCH[1]}
      semver_prerelease=${BASH_REMATCH[2]}

      mvn_phase="deploy"
      mvn_ci_revision="$semver"
      mvn_ci_sha1_short=""
      mvn_profiles_active="-Prelease-to-central"

      # Test for SemVer pre-releases. We turn those into SNAPSHOTs
      if [ -n "$semver_prerelease" ]; then
        # Unless "SNAPSHOT" is already the Semver Pre-release string, then..
        if [[ ! "$semver_prerelease" =~ SNAPSHOT$ ]]; then
          mvn_ci_changelist="-SNAPSHOT"  # effectively, this gets appended to the full Maven version string
        fi
      fi
    fi
  else
    # Tagged with something not resembling a SemVer string.
    # This may be a mistake. But we accept it as there may be valid reasons for creating Git tags not related to
    # versioning. However, at the very least the tag must not start with lower-case 'v' and then a digit.
    echo "Tag \"$GITHUB_REF_NAME\" is not SemVer"
    if [ "$GITHUB_EVENT_NAME" = "release" ]; then
      echo "Tag \"$GITHUB_REF_NAME\" was created from the GitHub UI Releases page but has incorrect format. You should delete the release in the UI."
      exit 1
    else
      if [[ "$GITHUB_REF_NAME" =~ ^v[0-9] ]]; then
        echo "Tag \"$GITHUB_REF_NAME\" looks like a versioning tag, but not started from GitHub UI Releases page."
        echo "The tag can easily be mistaken for a versioning tag and should be removed from git"
        exit 1
      fi
    fi
  fi
fi

# Execute maven
#
#
set -x
MVNW_VERBOSE=true ./mvnw \
  --show-version \
  --batch-mode \
  --no-transfer-progress \
  -Dci.project.url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}" \
  -Dchangelist=$mvn_ci_changelist  -Dsha1=$mvn_ci_sha1_short  -Drevision=$mvn_ci_revision \
  $mvn_profiles_active \
  $mvn_phase

