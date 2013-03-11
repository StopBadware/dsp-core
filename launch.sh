#! /bin/sh
cp logback.xml.template ./target/classes/logback.xml
sh package.sh
sh target/bin/webapp
