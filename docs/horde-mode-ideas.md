# Horde Mode Ideas

Brainstorm notes for a possible horde / monster-pressure layer around the guns
mod. This is not a final design yet.

## 1. Pressure

Pressure is the set of mechanics that increases monster spawning and overall
night danger.

Possible pressure inputs:

- night time;
- player activity;
- gunfire;
- number of nearby players;
- how long the area has survived;
- whether monsters can reach the player or base;
- difficulty / progression / balance state.

Pressure can affect:

- spawn frequency;
- group size;
- spawn distance;
- chance of special monsters;
- intensity of siege behavior.

### Smell Field

Current direction: horde monsters should not behave like perfectly informed
pathfinding agents. They should behave more like a mass following the smell of
players.

Core idea:

- players emit smell;
- smell is stored in a temporary server-side field, not in world blocks;
- smell spreads outward through walkable space, similar in spirit to light
  falloff;
- monsters read nearby smell values and move toward stronger smell;
- vanilla pathfinding can still be used as a helper, but should not be the main
  authority for long-range horde movement.

Implementation sketch:

- recalculate smell every 1-3 seconds;
- calculate per player or per local player group;
- store values in memory, keyed by packed `BlockPos` values;
- only store smell for positions where a mob can plausibly stand / move;
- do not flood-fill the entire 3D cube around the player;
- use a radius and a hard `maxVisitedNodes` limit to cap cost;
- suggested first values: radius around 48 blocks, update around every 40 ticks,
  with tuning after testing.

Walkable smell positions:

- feet position is passable;
- head position is passable;
- block below can support the mob;
- later exceptions may be needed for ladders, stairs, water, and special horde
  blocks.

Movement behavior:

- if a monster sees the player, it can move/chase directly;
- otherwise it follows the smell gradient;
- if it loses sight of the player, the last seen position can emit temporary
  smell;
- if no useful smell exists nearby, it can drift toward the last known player
  direction or join local pressure behavior.

Obstacle interaction:

- open corridors naturally carry smell;
- closed doors may leak smell at a higher cost;
- thick walls should block or strongly reduce smell;
- if strong smell exists behind a breakable obstacle, monsters may attack that
  obstacle;
- if there is no route, monsters should accumulate near high-smell surfaces,
  creating siege pressure and corpse mass over time.

### Horde Spawning

Current direction: horde spawning should not rely only on vanilla natural spawn.
At night, temporary horde sources appear around players and emit monsters until
morning or until their internal resource is spent.

Spawner / source behavior:

- sources appear when darkness / horde night begins;
- sources appear within a configurable distance band around players;
- sources should prefer relevant attack space, not random deep caves;
- normal caves should usually be ignored;
- dark basements or internal dark spaces near a player base can be valid later,
  because players should not be completely safe inside neglected interiors;
- sources can be physical and destructible, making raids against them dangerous
  but profitable;
- sources disappear at dawn;
- sources may also have a limited resource / charge pool and expire early when
  depleted.

Physical horde source details to decide later:

- whether sources are invisible event anchors, physical blocks, entities, or
  structure-like nests;
- whether players can find and destroy them during the night;
- how much health / durability a source should have;
- whether destroying a source stops only one wave, the whole night, or just
  reduces pressure;
- what risks should exist around attacking a source directly;
- what rewards, if any, come from destroying a source;
- how source visuals and sounds warn the player without turning the whole mode
  into a waypoint hunt.

Spawned monster behavior:

- spawned monsters should immediately belong to the horde event;
- they should get the relevant player / player group as their main target;
- they should move by smell / pressure rather than acting like independent
  vanilla mobs;
- vanilla pathfinding can be used when it produces useful local movement;
- if pathfinding is not useful, monsters should still drift toward smell/player
  pressure and accumulate at obstacles.

Night progression:

- store a persistent horde night number in world/server state;
- each new horde night should be more dangerous than the previous one;
- night number affects monster count, source strength, special monster chance,
  boss chance, and wave composition;
- every N nights may become a special milestone / boss night;
- the design can assume many players will not survive to very high night counts,
  such as night 30, but the system should still behave predictably if they do.

Milestone progression details to decide later:

- which nights introduce new monster variants;
- when siege-focused enemies start appearing;
- when source strength, source count, or source durability increases;
- whether every Nth night has a boss, special wave, or unusual source behavior;
- how much progression is global world state versus local player/base pressure.

Waves:

- a horde night can be divided into 3-5 waves;
- waves create pressure peaks and short breathing windows;
- breathing windows let players craft ammo, repair defenses, eat, and reposition;
- remaining monsters from the previous wave may still be present during a break,
  but source output should drop;
- the first implementation should fit waves into the normal vanilla night rather
  than changing world time.

Night duration:

- do not change vanilla night duration for the first implementation;
- horde wave timers should run on top of the vanilla day/night cycle;
- changing night length is possible later, but it is a separate system and may
  interact badly with other mechanics.

Suggested tuning variables:

```json
{
  "horde": {
    "spawning": {
      "enabled": true,
      "startAfterNight": 1,
      "sourceMinDistance": 48,
      "sourceMaxDistance": 96,
      "sourcesPerPlayerBase": 2,
      "sourcesPerPlayerGrowth": 0.15,
      "sourceMinSeparation": 32,
      "sourceChargesBase": 30,
      "sourceChargesPerNight": 3,
      "sourceSpawnIntervalTicksBase": 160,
      "sourceSpawnIntervalTicksMin": 40,
      "sourceSpawnIntervalNightReduction": 4,
      "sourceGroupSizeBase": 2,
      "sourceGroupSizePerNight": 0.15,
      "maxActiveHordeMonstersPerPlayer": 80,
      "maxActiveHordeMonstersGlobal": 300,
      "despawnSourcesAtDawn": true,
      "sourceCanBeDestroyed": true,
      "sourceHealthBase": 60,
      "sourceHealthPerNight": 5
    },
    "progression": {
      "nightDifficultyBase": 1.0,
      "nightDifficultyLinearGrowth": 1.0,
      "nightDifficultyPower": 1.15,
      "specialChanceBase": 0.0,
      "specialChancePerNight": 0.02,
      "bossEveryNthNight": 5,
      "bossChanceAfterMilestone": 1.0
    },
    "waves": {
      "enabled": true,
      "wavesPerNight": 4,
      "waveDurationTicks": 1600,
      "breakDurationTicks": 300,
      "finalWaveMultiplier": 1.5,
      "breakSpawnMultiplier": 0.15
    }
  }
}
```

These values are placeholders for balancing, not final numbers.

## 2. New or Changed Monsters

The horde needs monsters that are more interesting than vanilla zombies.

Directions:

- new monster types;
- modified vanilla monsters;
- stronger variants of existing monsters;
- monsters with clearer battlefield roles;
- monsters that force different weapon choices.

Examples to explore later:

- faster melee attackers;
- armored or very durable enemies;
- special support mobs;
- enemies that punish static defenses;
- enemies that create priority targets during a fight.

Possible monster variations to define later:

- runner: fast, fragile pressure unit that punishes open doors and exposed
  players;
- brute: slow, durable wall-pressure unit with stronger siege damage;
- armored enemy: resists weak weapons and rewards better ammo choice;
- ranged attacker: pressures firing positions, towers, and exposed repair work;
- siege thrower: damages walls or defensive structures from short range;
- support / warlock enemy: buffs nearby horde mobs, weakens defenses, or
  increases local pressure;
- climber / crawler: pressures simple vertical walls or low barricades;
- fireproof / hazard-resistant enemy: prevents lava or other traps from solving
  every wave by themselves.

### First Horde Monster

Current preferred first monster: a simple custom horde zombie / breacher. It can
reuse zombie-like behavior and presentation at first, but should be its own
entity type so it can have custom attributes, goals, spawning rules, and horde
state.

Design role:

- common front-line horde monster;
- more purposeful than a vanilla zombie;
- not a special boss or rare enemy;
- exists to make the first horde loop work end to end;
- teaches the player that horde mobs can pressure defenses.

Core behavior:

- spawns only from custom horde spawners / horde sources;
- should not be added to normal vanilla natural spawn tables at first;
- belongs to a horde event or horde source after spawning;
- gets a target player or player group from the horde system;
- uses normal chase behavior when it can see or path to the player;
- uses the smell field when direct pathing is not enough;
- drifts toward stronger smell instead of acting like an omniscient pathfinder;
- accumulates at high-smell obstacles when blocked;
- can apply siege damage to simple obstacles, doors, barricades, and soft blocks;
- stronger block breaking can be reserved for later heavy / siege monsters.

Suggested initial parameters:

- based on `Zombie` or a zombie-like `Monster` subclass;
- higher follow range than vanilla zombies;
- modestly higher health than vanilla zombies;
- normal or slightly slower movement speed, depending on testing;
- low individual siege damage so one monster is not a wall drill;
- several monsters hitting one point should become dangerous;
- despawns or loses pressure behavior after the horde event ends.

Implementation notes:

- register a dedicated entity type such as `guns:horde_zombie`;
- register attributes explicitly: health, movement speed, attack damage, armor,
  knockback resistance, and follow range;
- create custom goals for horde target selection, smell following, and siege
  attacking;
- keep natural spawning disabled and spawn it only through horde source logic;
- renderer can initially reuse a zombie-like model / texture to keep the first
  version focused on behavior;
- add a spawn egg only for development/testing if useful.

## 3. Defense Mechanics

Players need defensive tools beyond vanilla blocks.

This includes passive and active defense:

- reinforced walls;
- barricades;
- doors or gates;
- traps;
- spikes;
- turrets;
- catapults;
- other siege-defense structures.

The goal is to make base defense an active part of the loop, not just a static
Minecraft wall that solves everything forever.

## 4. Horde Attack Mechanics

The horde should have its own attack tools and battlefield roles.

Possible categories:

- default melee fighters;
- skeleton archers or other ranged attackers;
- very heavy monsters;
- casters / warlocks / support enemies;
- siege attackers;
- stone throwers that can damage or break walls;
- enemies designed to pressure defensive structures.

This is separate from spawn pressure: pressure decides how much danger arrives,
while attack mechanics decide what that danger can actually do.

## 5. Defense Bypass Mechanics

The horde needs ways to eventually work around simple defenses without using an
artificial rule like "the player must leave one valid path open."

Possible bypass mechanics:

- filling moats or pits with monster bodies / temporary mass;
- chewing through doors;
- damaging or breaking walls;
- climbing walls;
- climbing over other monsters;
- creating bridges or ramps from bodies or corrupted material;
- forcing the player to maintain defenses during the night.

The important design goal: isolation should be allowed, but not perfectly stable.

### Siege Damage

Current preferred first implementation: do not try to make monsters use vanilla
player mining progress directly. Instead, store a small temporary siege damage
value for attacked block positions.

Core idea:

- normal blocks stay normal Minecraft blocks;
- horde attacks add siege damage to a `BlockPos`;
- damage persists longer than vanilla mining cracks;
- damage slowly decays after a configurable delay if monsters stop attacking;
- if damage reaches the block's siege durability, the block breaks or becomes
  rubble;
- damage data is deleted when it decays to zero or when the block changes.

This gives the desired behavior where a lone monster hitting a wall every few
seconds may not make meaningful progress, but a group focusing the same point
can eventually break through.

Suggested early behavior:

- siege damage is only active during horde events;
- ordinary horde monsters can damage doors, barricades, and soft blocks;
- stronger siege monsters can damage harder blocks such as cobblestone;
- several monsters hitting the same block naturally stack progress;
- already-damaged blocks should be preferred as attack targets;
- damage should start decaying after roughly 10-30 seconds without hits;
- decay should be slow enough that a brief pause does not reset the siege;
- daytime decay or player repair can clear remaining damage faster.

Visuals:

- vanilla block breaking overlay can be used as the first visual layer;
- stored siege damage maps to crack stages 0-9;
- the overlay is only a display effect, not the source of truth;
- persistent visible cracked block variants are a possible later improvement,
  but they are not needed for the first prototype.

Important constraints:

- avoid changing vanilla mining behavior for players;
- avoid storing damage for unlimited blocks across the world;
- keep damage scoped to horde areas and recently attacked positions;
- respect block blacklists / protected blocks;
- remove stale damage entries aggressively enough to keep save data small.

### Unstable Player Fluids

Problem: players can create very cheap permanent fluid defenses. A lava source
placed on a wall can kill unlimited monsters forever, and water can create
permanent flow systems, moats, mob pushers, and infinite source setups.

Current direction: generated / natural world fluids should keep normal Minecraft
behavior, but fluid source blocks placed by players from buckets should be
unstable when used as active flowing defenses.

Core rules:

- generated lakes, rivers, aquifers, Nether lava, and other natural fluids stay
  unchanged;
- only source blocks created by player bucket placement are tracked;
- tracked water and lava sources have a configurable active lifetime;
- the lifetime only decreases while the source is flowing or sustaining nearby
  flowing fluid;
- if the source is contained and not flowing, the timer pauses;
- if the source starts flowing again later, it continues using the remaining
  lifetime rather than resetting;
- when the remaining lifetime reaches zero, the source is removed;
- flowing fluid should then disappear using normal Minecraft fluid behavior.

Intended player-facing behavior:

- a water or lava reserve inside a sealed tank can be stored;
- opening the tank or placing the source on a wall spends the source over time;
- players can still use fluids tactically;
- players cannot create free permanent lavafalls or water defenses from a
  single bucket;
- permanent defensive / utility fluids may return later through expensive modded
  infrastructure, such as tanks, pumps, channels, basins, or stabilizers.

Water source conversion:

- normal `waterSourceConversion` behavior should remain for natural / untracked
  water;
- player-placed temporary water should not participate in creating permanent
  infinite water sources;
- when vanilla fluid logic tries to create a new water source from nearby source
  blocks, source conversion should be denied if any contributing nearby source
  is tracked player-placed water;
- this should behave like a local extra rule, not like globally setting
  `waterSourceConversion` to false.

Implementation notes:

- hook bucket placement through `BucketItem#emptyContents`;
- after a successful server-side placement of water or lava source, register the
  placed `BlockPos` as a tracked player fluid source;
- store tracked sources in persistent world/server state so restarting the world
  does not turn temporary player fluids into normal permanent fluids;
- tracked data should include dimension, position, fluid type, and remaining
  active lifetime ticks;
- use a server-level tick to update only tracked positions, not to scan the
  world;
- if the block at a tracked position is no longer the expected source fluid,
  delete the tracking entry;
- a source can be considered active if it has adjacent flowing fluid of the same
  type or can otherwise sustain a visible flow;
- the first prototype can use adjacent flowing fluid as the active-flow test.

Technical source-conversion hook:

- in Minecraft 26.1.2, `FlowingFluid#getNewLiquid` counts horizontal source
  neighbors, calls `canConvertToSource`, then returns `getSource(false)` when a
  new source should form;
- `WaterFluid#canConvertToSource` reads the `WATER_SOURCE_CONVERSION` gamerule;
- a mixin can redirect or wrap the `canConvertToSource` check inside
  `FlowingFluid#getNewLiquid`;
- if the current fluid is water and nearby contributing source blocks include
  tracked player-placed water, return false for this conversion check;
- otherwise preserve vanilla behavior, including the normal gamerule result;
- avoid replacing the whole `getNewLiquid` method if possible.

Open details:

- decide whether dispenser-placed bucket fluids count as player-placed;
- decide whether waterlogged blocks should be tracked in the first version or
  handled later;
- decide default lifetimes, probably starting around 400-1200 active ticks;
- decide whether removed lava leaves fire, stone, cobblestone, smoke, or only
  disappears;
- add config options for enabling unstable water, unstable lava, active
  lifetimes, and source-conversion restrictions.

### Corpse Mass

Current preferred approach for the "overflowing dam" problem: do not make living
monsters physically stand on each other. Instead, monster deaths leave temporary
mass behind, and enough mass becomes real terrain that the horde can climb over.

Rules:

- only dead monsters add mass;
- living monsters standing in a crowded block do not add mass by themselves;
- mass is accumulated at the block position where the monster died;
- 3 corpses should produce a full block;
- the block should decay after roughly 30 seconds, to be tuned during testing;
- lava should still allow mass accumulation, but needs special handling because
  corpses should sink / accumulate toward the bottom rather than float at the
  surface.

Intended effect:

- kill pits and moats still work at first;
- repeated kills in the same place gradually fill the defense with corpse mass;
- the player has to clear or maintain the defense instead of relaxing forever;
- the mechanic is based on normal blocks, so Minecraft pathfinding can understand
  the result.

## 6. Repair and Maintenance

If defenses can be damaged, players need ways to repair them.

Current direction: repairing siege damage should require a crafted repair tool,
not just bare hands. The tool should have durability and eventually break like
normal Minecraft tools.

Things to think about:

- repairing damaged walls;
- repairing traps and spikes;
- restoring turrets or catapults;
- emergency repairs during combat;
- slower repairs between waves;
- repair costs and required materials;
- whether some defenses can be permanently destroyed.

### Repair Tool

Core idea:

- players craft a repair tool before or during base preparation;
- the tool removes stored siege damage from the targeted block;
- each repair action consumes tool durability;
- the tool can break like other tools;
- repairing may also require matching materials, depending on block type;
- stronger / reinforced blocks may need better tools or more materials.

Early behavior:

- right-clicking a damaged block with the repair tool reduces siege damage;
- the repair tool only affects mod-managed siege damage, not normal block
  mining state;
- if a block has no siege damage, the tool should do nothing meaningful;
- repairs during combat should be possible but slower or less efficient;
- repairs between waves can be faster and become part of the preparation loop.

Possible later progression:

- crude repair tool for wooden barricades and soft blocks;
- iron repair tool for stone and metal defenses;
- advanced repair kit for reinforced blocks, traps, turrets, and gates;
- emergency patching that is fast but temporary;
- full restoration that is slower but more durable.

This might become a major part of the night loop: shoot, reload, repair, rebuild,
prioritize.

## 7. Rewards and Combat Economy

There should be reasons to fight through the horde instead of only surviving
until morning.

Status: TBD. This section needs a separate design pass after the first horde
loop is clearer.

Possible reward directions:

- drops from monsters;
- temporary combat bonuses;
- ammo-related rewards;
- materials for repairing defenses;
- materials for better traps, walls, or weapons;
- rare drops from special enemies;
- rewards that are useful immediately during the fight.

## 8. Balance and Logistics

This covers the practical survival economy around horde combat.

Status: TBD. This is mostly resource management and should be designed around
the actual horde loop once spawning, repair, ammo usage, and monster durability
are better understood.

Questions:

- how does the player craft enough ammo?
- how does the player get food during long fights?
- how does armor durability fit into the loop?
- how much preparation should a night require?
- how much can be crafted or repaired mid-combat?
- should rewards help sustain the current fight or mostly prepare for the next?

This area probably becomes balance work later, once the core horde loop exists.
