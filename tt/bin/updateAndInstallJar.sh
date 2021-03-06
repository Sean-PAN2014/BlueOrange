#!/bin/bash
#ident  "%W%"
# $1 app_name
# $2 skip_test
# DONE

APP_NAME=$1
SKIP_TEST=$2

BASEDIR=`dirname $0`
MVN_ARG="-Dmaven.test.skip=true"

if [ "${SKIP_TEST}" == "no" ];then
	MVN_ARG="" 
fi

. ${BASEDIR}/env.source

APP_SVN_DIR=${SVN_WORKING_DIR}/${APP_NAME}

# svn update to a tmp
if [ ! -d "${APP_SVN_DIR}" ]; then
	mkdir -p ${APP_SVN_DIR}
fi

echo "svn co ${APP_SVN_URL} ${APP_SVN_DIR}"
svn co ${APP_SVN_URL} ${APP_SVN_DIR}

# run mvn -Dmaven.test.skip=true
mvn clean install deploy -f ${APP_SVN_DIR}/pom.xml ${MVN_ARG}

STATUS=$?
if [ ${STATUS} -ne 0 ]; then
	echo "Maven build failed for ${APP_NAME}"
	exit ${STATUS}
fi
