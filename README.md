![GameBox](logo.svg)
# GameBox

A mini-game hub inside Minecraft. Snake, Lights Out, Minesweeper, Sudoku,
Memory, 2048, Simon, Mastermind, 10x10, and Flappy Bird — all in one
Fabric mod, with local records, a light/dark theme, and an optional
server leaderboard to compete for high scores.

## Table of contents

- [Mini-games](#mini-games)
- [Features](#features)
- [Requirements](#requirements)
- [Installation (players)](#installation-players)
- [Server leaderboard](#server-leaderboard)
- [Contributing](#contributing)
- [License](#license)

## Mini-games

| Game | Difficulties | Description |
|---|---|---|
| Snake | Yes | The classic snake game. |
| Lights Out | Yes | Turn off every light on the board. |
| Minesweeper | Yes | Clear the board without detonating a mine. |
| Sudoku | Yes | Fill the 9x9 grid without repeating numbers. |
| Memory | Yes | Find every matching pair of cards. |
| 2048 | No | Combine tiles to reach 2048. |
| Simon | Yes | Repeat the color sequence. |
| Mastermind | Yes | Guess the secret color code. |
| 10x10 | No | Fit blocks into a grid, Tetris-style without falling pieces. |
| Flappy Bird | No | Fly through the gaps between the pipes. |

## Features

- **10 mini-games** with game logic kept separate from the UI, so each one
  stays easy to maintain and extend.
- **Local records** per player and per difficulty, with integrity
  verification on the save file.
- **Aggregate statistics**: total games played and your favorite game.
- Configurable **light/dark theme**.
- **In-game help** ("How to Play") for every mini-game.
- **Optional server leaderboard** (see below).
- **Client-side only** by default: you don't need to install it on any
  server to play, and it doesn't break compatibility with vanilla servers
  or other mods.

## Requirements

- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25 or later

> The exact Minecraft and Fabric API versions required depend on which
> file you download — check the [Releases](../../releases) section of
> this repository: each release states in its description which
> Minecraft version it was built for.

## Installation (players)

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft
   version.
2. Download the **Fabric API** version compatible with your Minecraft
   version from [Modrinth](https://modrinth.com/mod/fabric-api) or
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
3. Download the GameBox `.jar` matching your Minecraft version from this
   repository's [Releases](../../releases) section.
4. Copy both `.jar` files (Fabric API and GameBox) into your Minecraft
   installation's `mods` folder.
5. Launch the game with the Fabric profile. Press **G** at any time in a
   world to open the GameBox menu.

## Server leaderboard

GameBox includes an optional shared leaderboard system:

- If you connect to a server **that doesn't have GameBox installed**, the
  mod works exactly like it does in singleplayer: local records only, no
  change in behavior.
- If you connect to a server **that does have GameBox installed**, every
  mini-game automatically shows a panel with the server's Top 5 scores for
  that difficulty, including the skin of any players currently online.
- For a server to offer this feature, its operator only needs to install
  the same GameBox `.jar` (matching their Minecraft version) in the
  server's `mods` folder — no additional configuration required.

Each mini-game follows the same pattern: a **pure logic** class (with no
Minecraft dependencies, easy to test in isolation), a **screen** class
that handles drawing and input, and a **record** class serializable with
Gson.

## Contributing

Issues and pull requests are welcome. If you add a new mini-game, try to
follow the same logic/screen separation pattern used throughout the rest
of the project.

## License

This project is licensed under the MIT License — see the
[LICENSE](LICENSE) file for details.