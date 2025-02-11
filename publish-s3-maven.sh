#!/bin/bash

# Check if a profile argument is provided
if [[ -z "$1" ]]; then
  echo "Usage: $0 <aws-profile>"
  exit 1
fi

# AWS profile to use for fetching credentials
AWS_PROFILE="$1"

# Fetch credentials using AWS SSO and configure
echo "Logging in to AWS SSO..."
aws sso login --profile "$AWS_PROFILE"

# Fetch and export credentials in environment variable format
echo "Fetching temporary AWS credentials..."
credentials=$(aws configure export-credentials --profile "$AWS_PROFILE" --format env-no-export)

if [[ $? -ne 0 ]]; then
  echo "Failed to fetch credentials. Please ensure your profile is correctly configured."
  exit 1
fi

# Parse and export the credentials as environment variables
eval "$credentials"

# Confirm the variables are set
echo "AWS_ACCESS_KEY_ID: $AWS_ACCESS_KEY_ID"
echo "AWS_SECRET_ACCESS_KEY: [hidden]"
echo "AWS_SESSION_TOKEN: [hidden]"

# Optionally, verify with a simple AWS command
aws sts get-caller-identity --profile "$AWS_PROFILE"
if [[ $? -eq 0 ]]; then
  echo "AWS authentication successful."
else
  echo "AWS authentication failed."
  exit 1
fi

# Run Gradle publish task, passing AWS environment variables
echo "Running Gradle publish task..."

# Pass the environment variables to the Gradle process
export AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY"
export AWS_SESSION_TOKEN="$AWS_SESSION_TOKEN"

# Publish
./gradlew publish

# Publish repackaged version (-mod)
./gradlew publish -Prepackage=true -x test -x lintDebug

# Check for success or failure of the gradle publish task
if [[ $? -eq 0 ]]; then
  echo "Gradle publish completed successfully."
else
  echo "Gradle publish failed."
  exit 1
fi