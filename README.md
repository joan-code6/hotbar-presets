# HotbarPresets

HotbarPresets (previously InvSorter) is a small Fabric client mod that lets you save and load hotbar presets. It provides an in-game GUI to create named hotbar configurations and bind them to hotkeys or trigger them via the GUI.
Features
- Save and load hotbar presets locally.
- Assign hotkeys to presets or trigger them from the GUI.
- Simple, minimal client-side UI.

Installation
1. Install Fabric and Fabric API for the Minecraft version you target.
2. Place the built mod JAR into your `mods/` folder.
3. Start Minecraft with the Fabric loader.

Usage
- Press the configured key (default: O) to open the HotbarPresets menu.
- Create, manage and apply hotbar presets through the GUI.

Notes & Troubleshooting
- Server compatibility: the mod performs hotbar changes by simulating local inventory interactions. For some servers these may appear as ghost items if the server rejects client-side changes. Ensure the server allows client inventory manipulation or run a compatible server-side plugin if needed.
- If you see corrupted configuration messages in logs, check ~/.minecraft/config/invsorter for malformed JSON files.

Development
- The mod sources are organized under `src/` and resources under `src/main/resources`.
- Build with Gradle: `./gradlew build` (use `gradlew.bat` on Windows).

License
All-Rights-Reserved

Contributing
If you want UI improvements or server-aware synchronization, open an issue or submit a PR with a clear implementation plan.
