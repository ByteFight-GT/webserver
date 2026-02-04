#!/bin/bash

mkdir -p src/main/resources/certs

openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in private_key.pem -out src/main/resources/certs/public_key.pem