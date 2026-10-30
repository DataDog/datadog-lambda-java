stages:
  - build
  - sign
  - publish

default:
  retry:
    max: 1
    when:
      - runner_system_failure

variables:
  # TODO: switch to registry.ddbuild.io/ci/datadog-lambda-java once the "ci image" job
  # has built and signed it on main. Until then, reuse the identical image from datadog-lambda-java-ci.
  CI_DOCKER_TARGET_IMAGE: registry.ddbuild.io/ci/datadog-lambda-java-ci
  CI_DOCKER_TARGET_VERSION: latest

build layer:
  stage: build
  tags: ["arch:amd64"]
  image: ${CI_DOCKER_TARGET_IMAGE}:${CI_DOCKER_TARGET_VERSION}
  variables:
    LAYER_FILE: "dd-trace-java-$CI_PIPELINE_ID.zip"
  artifacts:
    expire_in: 2 weeks
    paths:
      - .layers/${LAYER_FILE}
  retry: 2
  script:
    - .gitlab/scripts/pull_and_zip_layers.sh

{{ range $environment := (ds "environments").environments }}

{{ if eq $environment.name "prod" }}
sign layer:
  stage: sign
  tags: ["arch:amd64"]
  image: ${CI_DOCKER_TARGET_IMAGE}:${CI_DOCKER_TARGET_VERSION}
  rules:
    - if: '$CI_COMMIT_TAG =~ /^v.*/'
      when: manual
  needs:
    - build layer
  dependencies:
    - build layer
  variables:
    LAYER_FILE: "dd-trace-java-$CI_PIPELINE_ID.zip"
  artifacts: # Re-specify artifacts so the modified signed file is passed
    expire_in: 2 weeks
    paths:
      - .layers/${LAYER_FILE}
  before_script:
    - EXTERNAL_ID_NAME={{ $environment.external_id }} ROLE_TO_ASSUME={{ $environment.role_to_assume }} AWS_ACCOUNT={{ $environment.account }} source .gitlab/scripts/get_secrets.sh
  script:
    - .gitlab/scripts/sign_layers.sh {{ $environment.name }}
{{ end }}

publish layer {{ $environment.name }}:
  stage: publish
  tags: ["arch:amd64"]
  image: ${CI_DOCKER_TARGET_IMAGE}:${CI_DOCKER_TARGET_VERSION}
  rules:
    - if: '"{{ $environment.name }}" =~ /^(sandbox|staging)/'
      when: manual
      allow_failure: true
    - if: '$CI_COMMIT_TAG =~ /^v.*/'
  needs:
    {{ if eq $environment.name "prod" }}
    - sign layer
    {{ else }}
    - build layer
    {{ end }}
  dependencies:
    {{ if eq $environment.name "prod" }}
    - sign layer
    {{ else }}
    - build layer
    {{ end }}
  parallel:
    matrix:
      - REGION:
        {{ range (ds "regions").regions }}
        - {{ .code }}
        {{- end}}
  variables:
    STAGE: {{ $environment.name }}
    LAYER_FILE: "dd-trace-java-$CI_PIPELINE_ID.zip"
  before_script:
    - EXTERNAL_ID_NAME={{ $environment.external_id }} ROLE_TO_ASSUME={{ $environment.role_to_assume }} AWS_ACCOUNT={{ $environment.account }} source .gitlab/scripts/get_secrets.sh
  script:
    - .gitlab/scripts/publish_layers.sh

{{- end }} # environments end
