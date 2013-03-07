#!/bin/sh
classdir='"\$BASEDIR"\/classes:'
generated_path='CLASSPATH=\$CLASSPATH_PREFIX:"\$BASEDIR"\/etc:'
sed "s/\($generated_path\)/\1$classdir/" target/bin/webapp >webapp
mv webapp target/bin/