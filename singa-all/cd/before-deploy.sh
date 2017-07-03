#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ] && [ ! -z "$TRAVIS_TAG" ]; then
    openssl aes-256-cbc -K $encrypted_277d978f8812_key -iv $encrypted_277d978f8812_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
    gpg --fast-import cd/codesigning.asc
fi