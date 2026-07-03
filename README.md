# 🏆 Particles Spigot Plugin

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-5E7C16?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21.11" />
  <img src="https://img.shields.io/badge/Paper-API-00A8E8?style=for-the-badge" alt="Paper API" />
  <img src="https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Author-USMCsky-6A4C93?style=for-the-badge&logo=github&logoColor=white" alt="Author USMCsky" />
</p>

A lightweight Paper/Spigot plugin that adds ambient particle effects around living entities to make outdoor areas feel more alive and cinematic.

## Features
- **Leaf drift** under tree canopies using falling dust particles.
- **Flower ambience** with color-matched particle bursts near nearby flowers.
- **Movement dust** around entities moving on the ground.
- **Always-on effects** with no setup or configuration required.
- **Lightweight scheduler** that runs automatically across loaded worlds.

## Built For
- **Platform:** Paper / Spigot-compatible servers
- **Minecraft version:** 1.21.11
- **Language level:** Java 21

## How It Works
Once installed, the plugin automatically scans loaded worlds and adds subtle particle effects around living entities:

- **Tree ambience:** drifting leaf particles appear when entities are near leaf blocks.
- **Flower ambience:** soft colored particles appear when entities are near flowers.
- **Movement ambience:** light dust particles appear when grounded entities are moving.

This creates a more lively outdoor atmosphere without requiring players or admins to manually trigger anything.

## Installation
1. Download or build the plugin JAR.
2. Place the JAR in your server's `plugins/` folder.
3. Start or restart your Paper/Spigot server.
4. The plugin will begin working automatically after the server finishes loading.

## Usage
This plugin does **not currently provide any commands or permissions**.

After installation, usage is automatic:
- Start the server with the plugin installed.
- Go to areas with trees, flowers, or moving mobs/players.
- The plugin will spawn ambient particle effects automatically.

## Compatibility
- Paper 1.21.11
- Spigot-compatible servers using the Paper API level declared by the plugin

## Developer Notes
The plugin entry point is `com.Main` and it loads after the world is available (`POSTWORLD`). It applies effects to living entities in loaded worlds on a repeating scheduler.

## License
Released under the **MIT License**. See `LICENSE.txt` for details.
