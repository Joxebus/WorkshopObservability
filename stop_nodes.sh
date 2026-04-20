#!/usr/bin/env bash

kill $(ps aux | grep 'person-front' | awk '{print $2}')
kill $(ps aux | grep 'person-service' | awk '{print $2}')
