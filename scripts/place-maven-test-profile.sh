# install gsed for mac
testProfile=`cat ./scripts/mavenTestProfile.xml`;
gsed -e "s/<profiles>/<profiles>$(testProfile)/" $1
