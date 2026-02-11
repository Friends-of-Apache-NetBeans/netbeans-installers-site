#!/usr/bin/env bash
release_tag=$1
rm -rf dist
mkdir -p dist
gh release download --clobber ${release_tag} -R Friends-of-Apache-NetBeans/netbeans-installers -D dist -p build.properties
gh release view -R Friends-of-Apache-NetBeans/netbeans-installers ${release_tag} --json assets |\
    jq  '.assets[] | .name +";"+.url+";"+.digest' | sed -e 's/"//g' | grep -v yaml | grep -v properties > dist/assets.txt

nb_release=$(cat dist/build.properties | grep netbeans.version | cut -d= -f2)
jdkversion=$(cat dist/build.properties | grep jdk.version | cut -d= -f2)

if [[ ${nb_release} =~ .*rc*. ]]; then
    echo release candidate ${nb_release}
    datafile=_data/rc${nb_release}.yaml
    cat - <<EOF > rc${nb_release}.md
---
layout: prerelease
sb: rc${nb_release}
---
EOF
else
    echo general availability release ${nb_release}
    datafile=_data/nb${nb_release}.yaml
    cat - <<EOF > nb${nb_release}.md
---
layout: release
sb: nb${nb_release}
---
EOF
fi

rm -f ${datafile}
## old version of attach created a nbxx.yaml file which should not be used anymore.

java ProcessAssets.java >> ${datafile}



