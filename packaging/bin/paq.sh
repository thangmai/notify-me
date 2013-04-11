#!/bin/bash

. ./packaging/VERSION

echo "FULL VERSION: $FULL_VERSION"
echo "FSNAME:       $FSNAME"

ROOT=`pwd`

TARGET=target
PREFIX=$TARGET/debian
ARTIFACTS=$TARGET/artifacts

CONTROL="DEBIAN/control"

ARCHIVE=$PAQNAME-$SOFTWARE-SNAPSHOT-standalone.jar
DEST="artifacts"

rm -rf $PREFIX
##lein uberjar

mkdir -p $PREFIX
mkdir -p $ARTIFACTS

cp -r packaging/debian/* $PREFIX/
sed -i -e  "s/FULL_VERSION/$FULL_VERSION/" $PREFIX/$CONTROL
sed -i -e  "s/ARCHITECTURE/$ARCHITECTURE/" $PREFIX/$CONTROL

cp target/$ARCHIVE $PREFIX/opt/notify-me/

echo "java -jar $ARCHIVE" >> $PREFIX/opt/notify-me/run.sh
chmod a+x $PREFIX/opt/notify-me/run.sh

cd $PREFIX
tar czvf data.tar.gz etc opt
cd $ROOT
tar czvf $PREFIX/control.tar.gz -C $PREFIX/DEBIAN/ .
echo 2.0 > $PREFIX/debian-binary

cd $ROOT

ar -r $TARGET/$FSNAME $PREFIX/debian-binary $PREFIX/data.tar.gz $PREFIX/control.tar.gz
#rm -fr $PREFIX

#dpkg-deb --build $PREFIX $TARGET/$FSNAME


