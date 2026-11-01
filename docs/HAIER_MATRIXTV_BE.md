# Haier MatrixTV BE setup and recovery

This repository preserves one personal CleanYourTV configuration for a Haier MatrixTV BE with Google TV 14, firmware `UKNV.260313.001`. The package choices were observed from 31 October to 1 November 2026 with one reboot. Check your firmware and apps before using the list on another TV. Nothing in this repository automatically changes a TV.

## What this copy adds

- [`examples/haier-matrixtv-be/debloat.txt`](../examples/haier-matrixtv-be/debloat.txt): nine TV packages disabled on the tested set, with keep and restore notes.
- [`haier/theme/config.json`](../haier/theme/config.json): Scenes wallpapers, frosted dock, four apps, Live TV button and quick settings gear mappings.
- `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java`: the Home Settings chip respects `settings_key_panel` and opens `QuickSettingsActivity` when that setting is true. This one-line fix was installed and verified on the original local launcher; this repository applies it to the newer upstream source.

This repository keeps CleanYourTV's source, Git history, and MIT license. The model example was also submitted upstream as [PR #2964](https://github.com/Gathin23/cleanyourtv/pull/2964). The private original-state backup remains only on the owner's computer.

## Remote key findings

| Button | Event | Configuration | Verified behavior |
|---|---|---|---|
| TV | Linux scan 225 (`KEY_BRIGHTNESSUP`) | `scan:225:com.mediatek.wwtv.tvcenter` | Opens Live TV |
| Settings gear | Android `KEYCODE_NOTIFICATION` (83), scan 240 | `KEYCODE_NOTIFICATION:com.gathin.tvhome/.QuickSettingsActivity` | Opens quick settings panel |
| Three-line | Android `KEYCODE_3D_MODE` (206), scan 240 | No mapping | Left separate from the gear |

Mapping scan 240 catches both gear and three-line buttons. The tested configuration uses Android keycode 83 for the gear. `InputKeyService` is required for the two shortcuts. With that service active, holding Volume Up or Down changed volume by one step; pressing repeatedly worked. The owner chose to keep the shortcuts. `consume_volume_keys: true` did not restore held repeat.

## Using the files on Windows

Work from this repository root. Install Python, Android SDK, and JDK as described in the upstream [README](../README.md). A TV must have ADB debugging enabled and be reachable on the same network. Replace the sample address in these commands with the TV's current IP.

```powershell
python -m venv tools\venv
tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
powershell -ExecutionPolicy Bypass -File tools\disable.ps1 -TvHost 192.168.1.50 examples\haier-matrixtv-be\debloat.txt
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50 -InputKeyService
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.50 haier\theme
```

Before disabling `com.google.android.tungsten.setupwraith`, check whether it owns a saved Wi-Fi network; the disable script includes a guard. The install script only disables stock Home after the new launcher starts. The tested TV's stock Home is `com.google.android.apps.tv.launcherx`; Live TV is `com.mediatek.wwtv.tvcenter`. This folder is documentation and source only; these commands have not been run from this new checkout.

## Restoring

The owner's exact original-state record stays private in the original project folder. That folder also has a private `work\restore-this-tv.ps1` script for the record. Do not publish those files. If this new checkout is used to apply the example to another TV, CleanYourTV creates its own local recovery record. To undo that run, first turn off CleanYourTV's `InputKeyService` in TV Accessibility settings, then use:

```powershell
powershell -ExecutionPolicy Bypass -File tools\restore.ps1 -TvHost 192.168.1.50 -Stock com.google.android.apps.tv.launcherx examples\haier-matrixtv-be\debloat.txt
```

The restore script replays recorded package states when its local record exists. Verify that Google TV Home opens after the restore. It leaves the CleanYourTV app installed, which can then be removed through TV Settings > Apps. See the [model example](../examples/haier-matrixtv-be/README.md) for package-level cautions and the limited test duration.
