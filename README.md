# LandClaimRules

LandClaimRules is a lightweight Lands addon that prevents players from creating unsupported claim shapes by enforcing a dual-ring support rule whenever they add a new chunk to their land.

## How it works

- **Claim inspection**: Every `ChunkPreClaimEvent` is intercepted at the highest priority after the host Lands plugin is loaded.
- **Dual-ring rule**: The listener checks the number of already-claimed chunks in the immediate 3×3 ring (`ring1`) and the wider 5×5 area (`ring12`). A new chunk is only allowed when `ring1` already contains more than `m` chunks (default `1`) and `ring12` has more than `n` chunks (default `5`).
- **Feedback & logging**: Denials cancel the claim, log the refusal (including the land name, world, and coordinates), and send the player a status message showing how many supporting chunks they still need.
- **Safety on first claim**: The very first claim in a world is always permitted so that expansion can begin.

## Installation

1. Build the project with `mvn clean package`.
2. Drop the resulting `LandClaimRules-<version>.jar` into your server’s `plugins/` directory.
3. Restart or reload the server after installing the `Lands` plugin dependency (LandClaimRules disables itself if Lands is missing).

## Development notes

- Entry point: `io.github.loliiiico.LandClaimRules` registers the listener once Lands has loaded.
- The rule implementation lives in `io.github.loliiiico.rules.DualRingSupportRule` with configurable thresholds via its constructor.
- The plugin currently hardcodes the `1/5` thresholds, so adjust that class if you need different support requirements.

## Compatibility

- Requires Minecraft server `api-version: 1.21`.
- Depends on the `Lands` plugin provided by angenommen (https://www.spigotmc.org/resources/lands.26732/).
