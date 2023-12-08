#!/bin/sh

# $1 your port#
java -cp UWAgent.jar:. UWAgent.UWInject -p $1 localhost UnixAgent P 2 cssmpi2 cssmpi3 12 who ls ps df who ls ps df who ls ps df
