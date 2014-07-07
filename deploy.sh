#!/bin/sh

SVN="/opt/subversion/bin/svn"

echo "Building with gradle..."
rm -rf build/libs
chmod -R +rw src/main/resources
gradlew build

echo "Deleting old deployment..."
rm -rf deploy
mkdir deploy

echo "Making new deployment..."
INPUTJAR=`ls -1 build/libs`
OUTPUTJAR=`ls -1 build/libs | sed s/\-//g`
cp build/libs/$INPUTJAR deploy/$OUTPUTJAR

echo "Done."
