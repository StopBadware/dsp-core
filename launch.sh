#! /bin/sh
cp logback.xml.template ./target/classes/logback.xml
sh package.sh $1
sh target/bin/$1
