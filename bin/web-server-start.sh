#!/bin/bash

export WEB_SERVER_ROOT=$HOME/apps/web-server/
nohup java -jar $WEB_SERVER_ROOT/web-server.jar  2>&1 &
