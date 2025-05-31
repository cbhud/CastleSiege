
# Introducing the exciting **Castle Siege** minigame!

![main](https://cdn.modrinth.com/data/cached_images/03a2d0fe3ea4ac7db0fae542b2a4c19bfd0f1fd1_0.webp)
## **Your goal? Assassinate the King or protect the throne. It's all about thrilling battles and fun for everyone on server!**

## **Feature List**
- **Teams**: Engage in battle as a **Defender** or **Attacker**, each with unique objectives.
- **Kits**: Choose from **8 specialized kits** (4 per team) to match your playstyle. Kits are customizable for varied strategies.
- **Game Modes**: Play in **Normal Mode** or **Hardcore Mode** (where respawning is disabled).
- **Abilities**: Each kit includes a unique ability item, adding strategic depth to gameplay.
- **Coins**: Earn coins by eliminating opponents or winning the game. Use coins to purchase additional kits.
- **Kill Rewards**: Gain temporary effects, like speed or regeneration, upon killing enemies.
- **Throwable Weapons**: Berserkers can throw axes, while Bombardiers can throw TNT to destroy Defender barriers.
- **Map Regeneration**: The map’s defenses (like fences) automatically regenerate after each game.
- **Player Stats**: Track individual stats and achievements.
- **Fully Configurable**: All settings, kits, and messages can be customized to fit your preferences.
- **Autostart**: Games can start automatically with adjustable settings.
- **Map Included**: A custom map is provided for Castle Siege gameplay.

## **NEW Map Regeneration System and Free Map Download**
- **Coordinates**: `X: 63 Y: 105 Z: -80`
- **Download**: MAP IS INCLUDED IN DOWNLOAD WITH THE PLUGIN!
- When you first start the game system saves all original locations of OAK FENCES
- After each game, the map regenerates the oak fences that were destroyed during the match, restoring the battlefield for the next round.
- The system will only remove or place the one that've been broken or placed when game started
### **Just IN Case you have edited fences around map before the game STARTED you need to save fence locations **
### **/cs save**
### **NOTE this is for now only POSSIBLE with OAK Fences**

## Images
![w1](https://i.badlion.net/baMfLWYvpEqQccyuqxMqu9.png)
![w2](https://i.badlion.net/aeSp63pZgLDkCxuHZNM3gG.png)
![w3](https://i.badlion.net/vHmLRo2cgVXs3ZsCZmucWL.png)
![w4](https://i.badlion.net/3zVWXhXvoxVmAgWHEu2zSS.png)
![w5](https://i.badlion.net/mDuTzzpuhthJYmXWmCJ4cn.png)
![w6](https://proxy.builtbybit.com/28cfa66932e23db06d835de79a8032b5e4689b38?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fcached_images%2F305a189954328a9a09239e3aecd557e7e24e0517.png)

## **Getting Started**
To start off, configure the **timer, king's health, max players per team**, and **autostart timer** before launching the server. Then, set in-game spawn points as follows:

### **In-game Commands**
- `/cs setlobby` — Set lobby location.
- `/cs setspawn teamName` — Set team spawn location (Teams: Attackers or Defenders).
   - **Note**: You can change display names for teams, but you’ll still need to use **Attackers** or **Defenders** when setting spawns.
- `/cs setkingspawn` — Set the King's spawn location.

**Other commands:**
- `/cs start` — Start the game manually.
- `/cs endgame` — Force stop the game without a winner.
- `/cs save` — Save OAK FENCES if you changed them on map.
- `/cs type` — Change game mode between **Normal** and **Hardcore**.
- `/stats` or `/stats <username>` — Check your stats or someone else’s.
- `/coins <set | add | remove> <username> <amount>` — Adjust player coins (requires OP or `cs.admin`).
- `/kit <lock | unlock> <kit> <username>` — Lock or unlock a kit for a player (requires OP or `cs.admin`).

## **Kits**
Each kit has unique items, a custom ability, and kill effects. Kits are customizable via the `kits.yml` file, where you can adjust items and prices. Avoid changing `kitTeam` and `kitNames` as this can break dependencies. You can unlock kits with in-game coins or admin commands.

### **Attacker Kits**
![asd](https://cdn.modrinth.com/data/cached_images/c2cc07824b73698204cba981f372ca07b7af0e47.png)

### **Defender Kits**
![asd](https://cdn.modrinth.com/data/cached_images/0595bf1b2a6f990898d6610259270211d9615516.png)

## **Abilities**
![2222](https://cdn.modrinth.com/data/cached_images/8bc82beb8d1e0a35a19372d624e9f60a869e5dcc.png)

## **Throwable Weapons**
- **Throwable Axes**: Berserkers can throw axes as part of their kit.
- **Throwable TNT**: Bombardiers can throw TNT to destroy Defenders' fences and deal damage to players.

## **Map Regeneration System**
### When you first start the game system saves all original locations of OAK FENCES
### Just IN Case you have edited fences around map before the game STARTED you need to save fence locations
- After each game, the map regenerates oak fences destroyed during the match.
- The system only replaces fences broken or placed while the game was running.
- **Note**: Only works with oak fences.

## **Stats**
- **Tracking Player Stats**: Player stats such as kills, deaths, and wins are stored in the H2 FlatFile Database.
- **View Stats**: Use `/stats <username>` to view stats.

## **Config**
```yaml
maxPlayersPerTeam: 16
attackersTeamName: Vikings
defendersTeamName: Franks
auto-start-players: 8
auto-start-countdown: 60
timerMinutes: 8
coins-on-win: 3
coins-on-kill: 1
king-health: 80.0
king-name: Marcus
tntCooldown: 120
tntDamage: 6.0
scoreboard-title: "Castle Siege"
scoreboard-bottomline: "serverip.net"
title-color: GOLD
bottom-color: AQUA
main-color: YELLOW
secondary-color: WHITE
```
### **Future Updates**
I plan to release updates monthly, though frequency may vary due to other tasks.

### **Bug Reports**
If you encounter bugs or have feature suggestions, please avoid using the reviews section for feedback. Reach out to me directly on discord cbhud