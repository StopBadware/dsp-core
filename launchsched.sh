#! /bin/sh
cp logback.xml.template ./target/classes/logback.xml
sh package.sh scheduler
sh target/bin/scheduler
