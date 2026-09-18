# MagicMaster

Vanilla enchanting tables are stingy. You grind out 30 levels, click the bottom
slot, and get Sharpness 4 and Unbreaking 2. That's it. That's the reward.

MagicMaster makes the table roll like it's working at a much higher level while
charging you exactly what it charged before. Paper 1.21.11.

## How it works

Three systems. Each one has its own on/off switch and they don't depend on each
other.

### Multiply Level

```
effective level = table level x multiplier
```

Then it rolls the enchantment again at the effective level using the game's own
selection code, and merges that into what the table already produced.

Merge means it adds and raises. It never removes. If the table gave you
Sharpness 3 and the re-roll says Sharpness 5, you get 5. If the re-roll comes
up with Looting 2 and you didn't have Looting, you get Looting 2. Nothing gets
deleted to make space, including enchantments some other plugin put there.

### Enchantment Plus

```
extra = floor((effective level - threshold) / interval) x additional-enchantments
```

| Effective level | Extras |
| --- | --- |
| 15 | 0 |
| 29 | 0 |
| 30 | 1 |
| 44 | 1 |
| 45 | 2 |
| 60 | 3 |

The extras come out of the same pool the table would have used for that exact
item. A pickaxe can pull Fortune. It can't pull Sharpness, because a table can't
put Sharpness on a pickaxe. If Silk Touch is already there, Fortune won't show
up next to it.

When the pool runs dry it adds fewer. Ask for 20 extras on a bow and you'll get
four, because a bow only has four things left to give.

### Level Plus

Same formula, but it raises the levels of what's already on the item.

```
extra levels = floor((effective level - threshold) / interval) x levels-per-interval
```

It stops at each enchantment's actual maximum. Sharpness 4 plus 3 is Sharpness 5.
Unbreaking 1 plus 3 is Unbreaking 3, because 3 is as high as Unbreaking
goes. Something already sitting at max doesn't move.

The level shown next to the offer in the table updates too, so you can see what
you're about to get before you click.

### All together, default settings

Diamond sword, bottom slot, level 30 offer:

```
effective level    30 x 2.00 = 60
extra enchantments floor((60 - 15) / 15) x 1 = 3
extra levels       floor((60 - 15) / 15) x 1 = 3
you pay            the level 30 offer, unchanged
```

## Picking a multiplier

```
item override  ->  tool material  ->  default
```

An item override always wins. Tool material covers everything without one. The
default catches the rest.

```yaml
items:
  DIAMOND_SWORD:
    enabled: true
    multiplier: 4.00     # diamond swords get 4x

material-mode:
  materials:
    DIAMOND:
      enabled: true
      multiplier: 2.00   # every other diamond tool gets 2x
```

Set `enabled: false` on an override and it parks without deleting. That item
falls back like it was never listed.

**Tool materials** are swords, pickaxes, axes, shovels and hoes, in wood, stone,
iron, gold, diamond and netherite. Nothing else. A golden apple isn't a gold
tool. Armour, bows, crossbows and tridents use the default multiplier unless you
give them an item override.

**Books:** `BOOK` and `ENCHANTED_BOOK` share one setting. A book also accepts
anything the table can roll instead of only what fits a tool, same as vanilla.

## Commands

| Command | What it does |
| --- | --- |
| `/magicmaster set <path> <value>` | Change any setting except the three master switches |
| `/magicmaster toggle <system>` | Flip Multiply Level, Enchantment Plus or Level Plus |
| `/magicmaster info` | Current settings as a tree |
| `/magicmaster reload` | Re-read config.yml |
| `/magicmaster help` | The list above |

`/mm` works too.

The three master switches are only reachable through `toggle`. Typing
`/magicmaster set multiply-level enabled false` gets rejected and points you at
the right command, on purpose.

```
/magicmaster set multiply-level default-multiplier 2
/magicmaster set multiply-level items DIAMOND_SWORD enabled true
/magicmaster set multiply-level items DIAMOND_SWORD multiplier 4
/magicmaster set multiply-level material-mode materials DIAMOND multiplier 3
/magicmaster set enchantment-plus additional-enchantments 1
/magicmaster set level-plus levels-per-interval 1
```

Setting a multiplier on an item that isn't in the config yet creates it switched
on, so it works immediately.

## Install

Grab the jar, drop it in `plugins/`, restart. `config.yml` writes itself on first
start.

