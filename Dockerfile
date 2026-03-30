FROM gradle:8.7.0-jdk17

ENV ANDROID_SDK_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
ENV ANDROID_API_LEVEL=android-35
ENV ANDROID_BUILD_TOOLS_VERSION=35.0.0
ENV ANDROID_HOME=/usr/local/android-sdk-linux
ENV ANDROID_NDK_VERSION=26.3.11579264
ENV ANDROID_VERSION=35
ENV ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}/
ENV PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

# Use root for system-level installations (this is a build-only container)
USER root

# Install nasm (required for x86/x86_64 native ASM_NASM builds in TMessagesProj JNI)
RUN apt-get update && apt-get install -y --no-install-recommends nasm && rm -rf /var/lib/apt/lists/*

RUN mkdir "$ANDROID_HOME" .android && \
    cd "$ANDROID_HOME" && \
    curl -o sdk.zip $ANDROID_SDK_URL && \
    unzip sdk.zip && \
    rm sdk.zip

RUN yes | ${ANDROID_HOME}/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --update
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "build-tools;30.0.3" \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" \
    "ndk;$ANDROID_NDK_VERSION" \
    "cmake;3.10.2.4988404"
RUN if [ -f "$ANDROID_HOME/build-tools/30.0.3/dx" ]; then \
        cp $ANDROID_HOME/build-tools/30.0.3/dx $ANDROID_HOME/build-tools/35.0.0/dx && \
        cp $ANDROID_HOME/build-tools/30.0.3/lib/dx.jar $ANDROID_HOME/build-tools/35.0.0/lib/dx.jar; \
    fi
ENV PATH=${ANDROID_NDK_HOME}:$PATH
ENV PATH=${ANDROID_NDK_HOME}/prebuilt/linux-x86_64/bin/:$PATH

ENTRYPOINT ["/bin/bash", "-c", "\
    set -e && \
    echo '=== Preparing build environment ===' && \
    mkdir -p /home/source/TMessagesProj/build/outputs/apk && \
    mkdir -p /home/source/TMessagesProj/build/outputs/native-debug-symbols && \
    echo '--- Reading version from gradle.properties ---' && \
    APP_VERSION_NAME=$(grep '^APP_VERSION_NAME=' /home/source/gradle.properties | cut -d= -f2- | tr -d '[:space:]') && \
    APP_VERSION_CODE=$(grep '^APP_VERSION_CODE=' /home/source/gradle.properties | cut -d= -f2- | tr -d '[:space:]') && \
    echo \"Building version: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})\" && \
    echo '--- Copying source to build directory ---' && \
    cp -R /home/source/. /home/gradle && \
    cd /home/gradle && \
    chmod +x gradlew && \
    echo '--- Cleaning stale build artifacts to prevent version caching ---' && \
    rm -rf TMessagesProj/build TMessagesProj_App/build TMessagesProj_AppStandalone/build \
           TMessagesProj_AppHuawei/build TMessagesProj_AppHockeyApp/build TMessagesProj_AppTests/build \
           TMessagesProj/.cxx build && \
    rm -rf .gradle 2>/dev/null || true && \
    echo '--- Running Gradle clean to ensure fresh build ---' && \
    ./gradlew clean --no-daemon && \
    echo '=== Building Standalone APK ===' && \
    echo \"Using version: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})\" && \
    ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandalone --no-daemon --stacktrace && \
    echo '=== Building Release APK ===' && \
    ./gradlew :TMessagesProj_App:assembleAfatRelease --no-daemon --stacktrace && \
    echo '=== Copying build outputs ===' && \
    mkdir -p /home/source/TMessagesProj/build/outputs/apk/release && \
    mkdir -p /home/source/TMessagesProj/build/outputs/apk/standalone && \
    if ! find /home/gradle/TMessagesProj_App/build/outputs/apk/ -name '*.apk' -print -quit | grep -q .; then \
        echo 'ERROR: TMessagesProj_App (Release) build completed without producing any APK files' && exit 1; \
    fi && \
    find /home/gradle/TMessagesProj_App/build/outputs/apk/ -name '*.apk' -exec cp {} /home/source/TMessagesProj/build/outputs/apk/release/ \\; && \
    if ! find /home/gradle/TMessagesProj_AppStandalone/build/outputs/apk/ -name '*.apk' -print -quit | grep -q .; then \
        echo 'ERROR: TMessagesProj_AppStandalone build completed without producing any APK files' && exit 1; \
    fi && \
    find /home/gradle/TMessagesProj_AppStandalone/build/outputs/apk/ -name '*.apk' -exec cp {} /home/source/TMessagesProj/build/outputs/apk/standalone/ \\; && \
    echo '=== Build complete ===' && \
    echo \"Version: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})\" && \
    echo 'Release APKs:' && ls -lh /home/source/TMessagesProj/build/outputs/apk/release/ && \
    echo 'Standalone APKs:' && ls -lh /home/source/TMessagesProj/build/outputs/apk/standalone/ \
"]
