# (Dataplanets) Creature Specification
A key feature of Dataplanets is that the planets should be alive and interesting.

One of the planned ways of doing this is to have Space-faring species and procedural generated creatures.

This document will outline how these NPCs will interact with the world, the player, and each other

Due to development constraints with this main repo, a lot of the faction content will suddenly appear later
## Section 1: Space-faring species
- Space-faring species should be datapackable
- these species should have a thematic building style
  - The Neum - Self-replicating robots, they generally go for brutalist functional architecture
  - The Pali - Humanoids, they generally go for typical sci-fi esque architecture
- These species have a "mind" the stores information:
  - A map of opinions, for each entity and player they interact with, a number is stored
  - A list of "rumour tags" that the entity has access to
    - a rumour tag is a list of rumours sorted by opinion required to share them
    - a rumour tag may contain a rumour that requires the entity to have an opinion of 5 of the entity requesting rumours
  - A faction assigned on the entity being created (see Section 3)

## Section 2: procedural creatures
- procedural creatures may exist on/in:
  - a planet with an atmosphere
  - a planet with an ocean
  - a feature or structure on a planet that has liquid water
- A procedural creature is configured:
  - as a fish style entity if spawning in water
  - as a quadruped or biped style entity if spawning on land
  - with different colours and sizes of each limb
  - with a set of features and goals
    - some creatures may be milked (this could cause anger)
    - some creatures may have meat and leather/wool drops
    - some creatures could be neutral, passive or aggressive
    - some creatures may be able to barter (or trade)
  - these creatures may have a chance to learn things as the days pass
    - creatures in factions may be able to generate knowledge points
    - these points can be used within factions to climb a basic tech tree
    - this tech tree could consist of learning to barter or trade, become aggressive etc
    - eventually these creatures could exist on space stations

## Section 3: Factions and Conflicts
- When a planet is generated, several factions are created on the planet based on the creatures that exist
- each faction consists of one (or more) creature or species
- each faction can have one or more conflicts with another faction on the planet
  - a conflict makes the members of each faction in the conflict aggressive to each other
  - conflicts can be marked as resolved with knowledge points from both factions
- when a faction is created:
  - A "population" number is assigned
  - this number can fluctuate (randomly) without player interaction
  - if this number drops below 0, the faction is removed from the data and they can no longer spawn
  - if all of a creatures factions are removed, then that creature can no longer spawn