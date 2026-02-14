FROM mcr.microsoft.com/devcontainers/java:17-bookworm

# Always start as root for provisioning
USER root
ENV DEBIAN_FRONTEND=noninteractive

# ---- System dependencies ----
# Remove yarn repository to avoid GPG key errors
RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        unzip \
        zip \
        libglu1-mesa \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ---- Android SDK env ----
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# ---- Android command line tools ----
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip -o /tmp/cmdline-tools.zip \
    && unzip /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# ---- SDK packages ----
RUN yes | sdkmanager --licenses \
    && sdkmanager --update \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-36" \
        "build-tools;36.0.0"

# ---- Permissions (CRITICAL) ----
RUN chown -R vscode:vscode ${ANDROID_SDK_ROOT}

# Drop privileges
USER vscode
