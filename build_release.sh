#!/bin/sh
# This is for use by Travis CI, to reduce too-long lines in .travis.yml
ant \
    -lib /usr/share/java/ant-contrib.jar \
    -lib $(pwd)/launch4j \
    -lib $(pwd)/launch4j/lib \
    -Dlaunch4j.dir=$(pwd)/launch4j \
    -Dwindowmenu.jar.path=$(pwd)/pump-swing-1.0.00.jar \
    -Dstub-script-path=$(pwd)/universalJavaApplicationStub-2.0.2/src/universalJavaApplicationStub \
    -Dapple.extensions.path=$(pwd)/orange-extensions-1.3.0.jar \
    -Dceylon.home=$(pwd)/ceylon-1.3.3 \
    release
