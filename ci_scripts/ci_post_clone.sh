#!/bin/sh

set -e

echo "=== Installing OpenJDK 17 for Kotlin Multiplatform ==="

# Install OpenJDK 17 via Homebrew
brew install openjdk@17

# Create a symlink so /usr/libexec/java_home discovers the JDK
sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk

echo "=== Verifying Java Installation ==="
java -version
/usr/libexec/java_home -v 17
