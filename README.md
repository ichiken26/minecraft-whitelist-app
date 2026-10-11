# Minecraft Whitelist Manager

Private-network-only Nuxt administration app and Paper plugin for an existing Minecraft server.

## Features
- Queue Java and Floodgate Bedrock accounts by username and edition
- Validate authenticated player UUID on login
- Add confirmed UUID to whitelist, optionally grant OP
- Keep whitelist enabled at all times
- Show queued and completed approvals

## Requirements
- Paper server with a compatible Java runtime
- Floodgate for Bedrock players
- Node.js and Gradle
- Tailscale for private admin access

## Configuration
Copy the provided environment template, use a **locally determined private bind address**, and set a strong administrator password. Do not commit real hostnames, user IDs, IP addresses, credentials, world data, or internal network details.

Deploy the Paper plugin to the server's plugin directory, then deploy the Nuxt application as a separate service. See `deploy/kokage-manager.service` as a starting template; adjust service user, directories, and permissions locally. Do not expose the admin interface publicly.

## Important limitations
- The source has not been verified by a production build or end-to-end testing.
- Removing an approval record does **not** remove Minecraft whitelist or OP permissions.
- Plugin approval depends on verified identity and the correct Paper/Floodgate versions.
- Keep sensitive deployment details in private local configuration only.
