## Genesis
 - Clear registered planets on server stop
 - Add mining laser for better asteroid mining

## Dataplanets
 - Add somewhere to lookup planet data
   - Uses
     - gravity
     - oxygen
     - temperature
   - Requirements
     - saving planet data to json
     - reading planet data from json
     - adding planet data at runtime
 - Reimplement temperature, gravity, oxygen with new planet data lookup
 - Add planets to space in Genesis
   - Requirements
     - add planet when first registered
       - ensure that clients are aware of registered planets if called only on the server
     - write planet to json (in genesis format)
- Distinct biomes
  - Features
    - distinct surface rules
    - distinct features
    - distinct terrain shape
- Better block palettes—possibly revamp data format
  - Get rid of fallable blocks (sand/gravel) - they have too much lag potential
  - Add more interesting blocks
  - Ensure that block properties give good variety/spread
- Optimize density functions (how?)