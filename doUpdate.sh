#!/usr/bin/env bash
# repo typically stays constant
repo=Friends-of-Apache-NetBeans/netbeans-installers
# If release_tag argument is not given, use latest according to github
if [[ $# -lt 1 ]]; then 
    release_tag=$(gh release list -R ${repo} --json tagName | jq '.[] .tagName' | sed -e's/"//g' | head -1)
else 
    release_tag=$1
fi

echo "using release tag ${release_tag}"
echo "using repo tag ${repo}"
# cleanup
rm -rf dist
mkdir -p dist
# get the effective propertes
gh release download --clobber ${release_tag} -R ${repo} -D dist -p effective.properties
# get the assest data using the API
gh api -H "'Accept: application/vnd.github+json'" -H "'X-GitHub-Api-Version: 2022-11-28'" \
   /repos/${repo}/releases/tags/${release_tag} | jq '.assets[] | .name+";"+.browser_download_url+";"+.digest+";"+(.download_count|tostring)' \
   | sed -e 's/"//g' | grep -v yaml | grep -v properties > dist/assets.txt

nb_release=$(cat dist/effective.properties | grep netbeans.version | cut -d= -f2)

# if [[ ${nb_release} =~ .*rc*. ]]; then
#     echo release candidate ${nb_release}
#     datafile=_data/rc${nb_release}.yaml
#     cat - <<EOF > rc${nb_release}.md
# ---
# layout: prerelease
# sb: rc${nb_release}
# ---
# EOF
# else
#     echo general availability release ${nb_release}
#     datafile=_data/nb${nb_release}.yaml
#     cat - <<EOF > nb${nb_release}.md
# ---
# layout: release
# sb: nb${nb_release}
# ---
# EOF
# fi

# rm -f ${datafile}
# ## old version of attach created a nbxx.yaml file which should not be used anymore.

java ProcessAssets.java >> ${datafile}



