#!/bin/sh
#
# $Id$

# set up our classpath
CP=$ANT_HOME/lib/ant.jar
CP=$CP:/usr/share/java/servlet-2.2.jar
CP=$CP:$ANT_HOME/../lib/jaxp.jar
CP=$CP:$ANT_HOME/../lib/crimson.jar
CP=$CP:$JAVA_HOME/lib/tools.jar

# execute ANT to perform the requested build target
java -classpath $CP:$CLASSPATH org.apache.tools.ant.Main "$@"
