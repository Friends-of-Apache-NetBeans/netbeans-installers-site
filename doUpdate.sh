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

java ProcessAssets.java ${release_tag}



