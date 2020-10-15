#!/bin/bash


while True; do aws lambda invoke --function-name $1  --payload 1 out.json ; done
