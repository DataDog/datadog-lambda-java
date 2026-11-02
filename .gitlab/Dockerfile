FROM registry.ddbuild.io/images/docker:27.3.1

ARG TARGETARCH

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget curl gnupg unzip zip jq uuid-runtime

# Install AWS CLI for the target architecture.
RUN AWS_ARCH=$([ "$TARGETARCH" = "arm64" ] && echo aarch64 || echo x86_64) && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip && ./aws/install
