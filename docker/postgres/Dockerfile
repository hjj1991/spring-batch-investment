FROM timescale/timescaledb:latest-pg16

USER root

RUN apk add --no-cache \
        build-base \
        ca-certificates \
        curl \
        py3-pip \
        unzip \
        postgresql16-contrib \
        postgresql16-dev \
        postgresql-pgvector \
    && pip3 install --break-system-packages --no-cache-dir pgxnclient \
    && pgxn install pgmq \
    && apk del build-base py3-pip

USER postgres
