#!/bin/bash

export WEB_SERVER_ROOT=$HOME/apps/web-server/
nohup java -jar $WEB_SERVER_ROOT/releases/web-server.jar 2>&1 &