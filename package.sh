#!/bin/sh
classdir='"\$BASEDIR"\/classes:'
generated_path='CLASSPATH=\$CLASSPATH_PREFIX:"\$BASEDIR"\/etc:'
sed "s/\($generated_path\)/\1$classdir/" target/bin/$1 >$1
mv $1 target/bin/
