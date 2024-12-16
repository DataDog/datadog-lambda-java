FROM registry.ddbuild.io/images/docker:24.0.5

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl gnupg unzip zip jq

# Install AWS CLI
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip && ./aws/install
