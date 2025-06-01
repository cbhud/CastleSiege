
# 🏰 Castle Siege 2 — The Improved Version of Castle Siege!

![Castle Siege 2](https://i.imgur.com/Zyt5Cpz.jpeg)

Castle Siege 2 is a complete overhaul and upgrade of the original Castle Siege minigame. Built from the ground up for modern servers, it offers full customization, multi-arena support, and exciting, balanced team-based gameplay.

> ⚠️ **Note:** This is a beta release (Beta-3). Some features such as Hardcore Mode, BossBar support, and Player Stats are not yet implemented.

#  Your goal? Assassinate the King or protect the throne. It's all about thrilling battles and fun for everyone on server!

---

## 🔥 Features

- ⚔️ **Two Teams**: Attackers try to assassinate the King; Defenders protect the throne.
- 🧑‍🤝‍🧑 **Multi-Arena Support**: Host multiple matches in parallel across different worlds.
- 🎯 **8 Customizable Kits**: 4 per team, each with items, abilities, and kill rewards.
- 🎮 **Game Modes**: Normal Mode is supported. Hardcore Mode (no respawns) is **coming soon**.
- 💰 **Coins System**: Earn coins from kills and wins. Use them to unlock kits.
- 🪓 **Throwable Weapons**: Bombardiers throw TNT, Berserkers throw axes.
- ⚡ **Kill Rewards**: Grant players effects like Speed or Regeneration on kill (configurable).
- 🔄 **Map Regeneration**: Breakable map elements (OAK_FENCES) reset after each game.
- 🎨 **Fully Customizable**: Kits, messages, scoreboards, kill effects, and more.
- 🧾 **Placeholders**: Integrate custom data into messages, holograms, and scoreboards.
- ⏱️ **Autostart System**: Games start automatically with adjustable settings.
- 🗺️ **Map Included**: A beautiful premade map is bundled with the plugin.

---

## 🚀 Getting Started

# 📺 **Video Tutorial**: [Watch on YouTube](https://youtu.be/vcgLJUzd73k)

Once you download plugin folder, follow tutorial steps to set up Castle Siege 2 on your server:
Note: You will receive all the necessary dependencies and map when you download the plugin.
### ✅ 1. Set the Main Lobby

```bash
/setlobby
```
> Requires `cs.admin` permission or OP.

---

### 🗺️ 2. Import & Teleport to the Arena World

```bash
/mv import <arenaName> normal
/mv tp <arenaName>
```
> Requires [Multiverse-Core](https://www.spigotmc.org/resources/multiverse-core.390/).

---

### 🏗️ 3. Create and Configure an Arena

```bash
/arena create <arenaName>
/arena setlobby
/arena setking
/arena setattackers
/arena setdefenders
```

> ⚠️ You must use `Attackers` and `Defenders` as internal team names, even if you use display names.

---

### 📐 4. Set Arena Regeneration Region

1. Use WorldEdit or FAWE:
   - Select 2 points using `//wand` on red wool markers provided in the map.
2. Copy the selection:
   ```bash
   //copy
   ```

---

### ✅ 5. Finalize the Arena

```bash
/arena finish
```

---

## 📄 Commands


### 🔧 Admin Commands

| Command | Description |
|--------|-------------|
| `/arena <create|setlobby|setking|setattackers|setdefenders|finish>` | Manage arenas |
| `/setlobby` | Set global lobby |
| `/coins <add|remove|set> <player> <amount>` | Manage player coins |

### 👤 Player Commands

| Command | Description |
|---------|-------------|
| `/leave` | Leave the current arena |
| `/stats` or `/stats <player>` *(Coming Soon)* | View player stats |
| `/cs type` *(Coming Soon)* | Toggle between Normal and Hardcore mode |

---

## 🧰 Configuration Files

- `config.yml` – Core plugin settings
- `arenas.yml` – Arena locations and settings
- `kits.yml` – Define kits, ability items, and unlock costs
- `custom_items.yml` – Define ability items used in kits
- `killrewards.yml` – Effects granted on kill
- `messages.yml` – Customize all plugin messages (supports gradients!)
- `scoreboards.yml` – Customize per-phase scoreboards

> 💡 All files support reloads and are fully commented for ease of use.

---

## 🎯 Kits

- 4 Kits per team (total: 8), customizable in `kits.yml`.
- Each kit includes:
   - Items
   - An ability item (customizable via `custom_items.yml`)
   - Kill rewards (`killrewards.yml`)

Players can unlock kits using in-game coins or admin commands.

---

## 💣 Throwable Weapons

- **Berserkers** can throw axes (NOTE only GOLDEN_AXE is throwable).
- **Bombardiers** can throw TNT:
   - Destroys **OAK_FENCES** and damages nearby players.
   - Regenerates after the match via FAWE/WorldEdit.

---

## 🧩 Placeholders

Use these with PlaceholderAPI, holograms, scoreboards, and messages:

| Placeholder | Description |
|------------|-------------|
| `%cs_timer%` | Current game timer |
| `%cs_starting-in%` | Countdown until game starts |
| `%cs_kills%` | Player's kill count |
| `%cs_wins%` | Player's win count |
| `%cs_deaths%` | Player's death count |
| `%cs_coins%` | Player's coin balance |
| `%cs_king%` | King’s remaining health |
| `%cs_team%` | Player’s team name |
| `%cs_attackers_size%` | Attackers team size |
| `%cs_defenders_size%` | Defenders team size |
| `%cs_arena%` | Arena name |
| `%cs_arenasize%` | Player count in arena |
| `%cs_winner%` | Winning team name |
| `%cs_attackers%` | Display name for attackers |
| `%cs_defenders%` | Display name for defenders |
| `%cs_kit%` | Player’s selected kit |

---

## 📦 Map

- 📍 Coordinates: `X: 0 Y: 105 Z: 0`
- 📥 Download: **Included with plugin**
- 👷 Built by: `MATIASXD10` — [Discord Contact](https://discord.com/users/MATIASXD10)

---

## 🧪 Coming Soon (Beta Roadmap)

- 📊 Player stats system
- 🧠 Hardcore mode (no respawns)
- 🩸 BossBar support for King health
- GUI kit selector
- Stats leaderboard

---

## 🙋 Need Help?

If you find bugs, have ideas, or need support:

- 💬 Discord: **cbhud**
- 💻 Support Server: [Join Here](https://discord.gg/EC3gcUsGcV)

> 💡 Please avoid using reviews for bug reports or feature requests — open a ticket or contact me directly.

---

Made with ❤️ by **cbhud** for the Minecraft community.
