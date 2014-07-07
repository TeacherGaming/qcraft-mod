#!/bin/sh

SVN="/opt/subversion/bin/svn"

echo "Setting permissions..."
chmod +x deploy.sh
chmod +x gradlew

echo "Setting up IntelliJ development environment with gradle..."
gradlew setupDecompWorkspace
gradlew idea

echo "Done."
