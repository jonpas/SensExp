#!/bin/bash

# Duplicate this file to change information
mosquitto_sub -v -h <server-uri> -p <port> -t '<topic>' -u <username> -P <password>
