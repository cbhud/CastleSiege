# Castle Siege 2 — The Improved Version of Castle Siege!

![Castle Siege 2](https://i.imgur.com/d3KxRQF.png)

Castle Siege 2 is the enhanced and community-driven successor to the original Castle Siege! Built with the needs of players and server owners in mind, it introduces a **simple multi-arena system**, **arena regeneration**, and **full customization** of features.

> **Note:** This is a beta release. Some features may be missing or incomplete but will be added in the future.

---

## 🔥 Key Features

- ✅ Multi-arena support
- ♻️ Arena regeneration
- 🎮 Customizable kits, messages, and scoreboards (now with gradient and PlaceholderAPI support!)
- 🧩 Full YAML configuration: `arenas.yml`, `config.yml`, `messages.yml`, `scoreboards.yml`

---

## 🚀 Getting Started

Watch the full setup tutorial on YouTube:  
📺 [Video Tutorial](https://youtu.be/vcgLJUzd73k)

### 1. Set the Main Lobby Spawn

```bash
/setlobby
```
> Requires `OP` status or `cs.admin` permission.

---

### 2. Import and Teleport to Arena World

```bash
/mv import arena normal
/mv tp arena
```
> Requires [Multiverse-Core](https://www.spigotmc.org/resources/multiverse-core.390/) installed.

---

### 3. Create an Arena

```bash
/arena create <arenaName>
```

**Example:**
```bash
/arena create arena1
```

---

### 4. Set Arena Locations

Set the pre-game lobby for the arena:
```bash
/arena setlobby
```

Set spawn locations for each role:
```bash
/arena setking
/arena setdefenders
/arena setattackers
```

---

### 5. Select Arena Regeneration Area

Use WorldEdit to define the regeneration area.

**Steps:**
1. Get a wand:
   ```bash
   //wand
   ```
2. Select two red wool markers placed in the map (as shown in the video).
3. Use WorldEdit to select the region:
   - Left-click = First position
   - Right-click = Second position
4. Copy the selection:
   ```bash
   //copy
   ```

---

### 6. Finalize Arena Setup

```bash
/arena finish
```

---

## ⚙️ Configuration Files

Customize gameplay using the following files:

- `arenas.yml` – Arena data and locations
- `config.yml` – Core plugin settings
- `scoreboards.yml` – Scoreboard layouts
- `messages.yml` – All in-game messages (supports gradients!)

---

## 📛 Placeholders

You can use the following placeholders in messages, scoreboards, or any text fields that support them:

| Placeholder | Description |
|------------|-------------|
| `%cs_timer%` | Returns the current game timer (e.g., remaining match time). |
| `%cs_starting-in%` | Returns the countdown time until the game starts. |
| `%cs_kills%` | Returns the number of kills the player has. |
| `%cs_wins%` | Returns the number of wins the player has. |
| `%cs_deaths%` | Returns the number of times the player has died. |
| `%cs_coins%` | Returns the player's coin balance. |
| `%cs_king%` | Returns the king's remaining health for the player’s team. |
| `%cs_team%` | Returns the name of the player's current team (e.g., "Attackers"). |
| `%cs_attackers_size%` | Returns the number of players currently on the attackers team. |
| `%cs_defenders_size%` | Returns the number of players currently on the defenders team. |
| `%cs_arena%` | Returns the name or ID of the arena the player is currently in. |
| `%cs_arenasize%` | Returns the number of players in the current arena. |
| `%cs_winner%` | Returns the name of the team that won the match (if available). |
| `%cs_attackers%` | Returns the display name of the attackers team (e.g., "Red Team"). |
| `%cs_defenders%` | Returns the display name of the defenders team (e.g., "Blue Team"). |
| `%cs_kit%` | Returns the name of the selected kit for the player, or "No kit selected". |

---

## 🐛 Need Help?

If you encounter any bugs or need help, contact me:

- Discord: **cbhud**
- Support Server: [Join Discord](https://discord.gg/EC3gcUsGcV)

---

Made with ❤️ for the Minecraft community.
