#!/usr/bin/env bash
# repo typically stays constant
repo=Friends-of-Apache-NetBeans/netbeans-installers
dist=dist
# If release_tag argument is not given, use latest according to github
if [[ $# -lt 1 ]]; then 
    release_tag=$(gh release list -R ${repo} --json tagName | jq '.[] .tagName' | sed -e's/"//g' | head -1)
else 
    release_tag=$1; shift
fi
if [[ $# -ge 1 ]]; then
    dist=$1
fi

echo "using release tag ${release_tag}"
echo "using repo tag ${repo}"
echo "using dist dir ${dist}"
# cleanup
rm -rf ${dist}
mkdir -p ${dist}
# get the effective propertes
gh release download -R ${repo} --clobber ${release_tag}  -D ${dist} -p "*.properties"
if [[ -e ${dist}/build.properties ]]; then
    cat ${dist}/build.properties >> ${dist}/effective.properties
fi
# get the assest data using the API
gh api -H "'Accept: application/vnd.github+json'" -H "'X-GitHub-Api-Version: 2022-11-28'" \
   /repos/${repo}/releases/tags/${release_tag} | jq '.assets[] | .name+";"+.browser_download_url+";"+.digest+";"+(.download_count|tostring)' \
   | sed -e 's/"//g' | grep -v yaml | grep -v properties > ${dist}/assets.txt

nb_release=$(cat dist/effective.properties | grep netbeans.version | cut -d= -f2)

java AssetsProcessor.java ${release_tag} ${dist}



