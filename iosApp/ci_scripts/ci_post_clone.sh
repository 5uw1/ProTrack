#!/bin/sh

set -e

echo "=== Installing OpenJDK 17 for Kotlin Multiplatform ==="

# Install OpenJDK 17 via Homebrew
brew install openjdk@17

# Create symlink in user's Library (no sudo required in Xcode Cloud)
mkdir -p "$HOME/Library/Java/JavaVirtualMachines"
ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" "$HOME/Library/Java/JavaVirtualMachines/openjdk-17.jdk"

echo "=== Verifying Java Installation ==="
export PATH="$(brew --prefix openjdk@17)/bin:$PATH"
java -version
/usr/libexec/java_home -v 17 || true
