FROM ubuntu:latest
LABEL authors="tylerkwok"

ENTRYPOINT ["top", "-b"]