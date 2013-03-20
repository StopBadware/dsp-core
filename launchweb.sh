#! /bin/sh
cp logback.xml.template ./target/classes/logback.xml
sh package.sh webapp
sh target/bin/webapp
