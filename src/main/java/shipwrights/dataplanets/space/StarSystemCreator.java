package shipwrights.dataplanets.space;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import shipwrights.dataplanets.compat.Compat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class StarSystemCreator {

    public static Pair<CompoundTag, String> makeSystem(DimensionDataStorage storage,int minPlanetCount, int maxPlanetCount)
    {
        String uuid = UUID.randomUUID().toString();
        return inventSystem(storage,uuid, minPlanetCount, maxPlanetCount);
    }

    private static String truth(boolean truth)
    {
        if(truth) return "true";
        return "false";
    }

    private static final String[] CODE = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};

    private static Pair<CompoundTag, String> inventSystem(DimensionDataStorage storage ,String uuid, int minPlanetCount, int maxPlanetCount)
    {
        RandomSource random = RandomSource.create();
        String systemName = CODE[random.nextInt(CODE.length)]+CODE[random.nextInt(CODE.length)]+random.nextInt(1000);
        int rocketTier = random.nextInt(3)+1;


        CompoundTag systemData = new CompoundTag();
        systemData.putString("systemName",systemName);
        systemData.putInt("rocketTier",rocketTier);
        systemData.putInt("planets",random.nextInt(minPlanetCount, maxPlanetCount));



        StringBuilder langFile = new StringBuilder("{\"dataplanets.").append(systemName).append("\":\"");
        String rebuild = systemName.toUpperCase();
        rebuild = rebuild.substring(0,2)+" "+rebuild.substring(2);
        langFile.append(rebuild).append("\",");


        for (int i = 0; i < systemData.getInt("planets"); i++) {
            CompoundTag planetData = new CompoundTag();
            String subname = CODE[i];
            String planetName = systemName+subname;
            planetData.putString("name",planetName);
            planetData.putFloat("gravity",(float)random.nextInt(1500)/100f);
            planetData.putBoolean("hasAtmosphere",random.nextInt(4)==0);
            planetData.putInt("yearDays",random.nextInt(1,1000));


            int initalTemperature = random.nextInt(1000);

            //a planet with an atmosphere is more likely to have a reasonable temperature
            if(planetData.getBoolean("hasAtmosphere") && initalTemperature<100)
            {
                initalTemperature+=100;
            }
            if(random.nextInt(10)==0)
            {
                planetData.putString("planetType","gaseous");
            }

            //temperature limits solar power - that's wierd right?
            //a higher solar power would imply a higher temperature from the sun.
            //(this is a massive simplification when you consider different radiation types...)
            //however higher temperatures can be generated in a planet without its star (tidal flexing/heating, volcanic activity etc.)
            //hence, as temperature isn't necesarily dependent on the star, its this way round
            //MAYBE: does this make sense?
            planetData.putInt("temperature",initalTemperature);

            //solar power 50 is REALLY high, Mercury has 19 at 440K
            //however, the planet needs a temeprature of 1000 to get 49 (upper bounds)
            //that's rather warm.
            //nicely, a planet with temperature 401 has a chance of getting 20 (still 1-20 however)
            planetData.putInt("solarPower",random.nextInt((planetData.getInt("temperature")/15)+1));

            planetData.putBoolean("hasOxygen",random.nextInt(4)==0);

            String effects;
            if(planetData.getBoolean("hasAtmosphere"))
            {
                effects="minecraft:overworld";
            }
            else
            {
                effects="minecraft:the_end";
            }

            planetData.putString("effects",effects);


            if(planetData.getInt("temperature")>600)
            {
                planetData.putString("generalBlock", "minecraft:magma_block");
            }
            else
            {
                planetData.putString("generalBlock", Compat.SURFACE_BLOCKS[random.nextInt(Compat.SURFACE_BLOCKS.length)]);
            }


            if(planetData.getBoolean("hasAtmosphere") && random.nextInt(5)==0)
            {
                if(planetData.getInt("temperature")<373)
                {
                    if(planetData.getInt("temperature")>273)
                    {
                        planetData.putString("seaBlock","minecraft:water");
                        planetData.putString("planetType","ocean");
                    }
                    else
                    {
                        planetData.putString("seaBlock","minecraft:packed_ice");
                        planetData.putString("planetType","icy");
                    }

                }
                else
                {
                    planetData.putString("seaBlock","minecraft:air");
                }

            }
            else
            {
                planetData.putString("seaBlock","minecraft:air");
            }





            planetData.putFloat("nr1", (float) random.nextInt(1800) /1000f);
            planetData.putFloat("nr2", (float) random.nextInt(2000) /1000f);
            planetData.putFloat("nr3", (float) random.nextInt(2000) /1000f);


            planetData.putFloat("radiusClient",(((50f-planetData.getInt("solarPower")) /10f)+random.nextFloat()));
            planetData.putInt("scaleClient",random.nextInt(5,20));

            planetData.putInt("seaLevel",random.nextInt(63,210));


            ListTag biomeList = new ListTag();
            for (int j = 0; j < 3; j++) {

                CompoundTag biomeCompound = new CompoundTag();
                biomeCompound.putFloat("downfall",random.nextFloat());
                biomeCompound.putBoolean("hasRain", planetData.getBoolean("hasAtmosphere") && random.nextInt(5)==0);
                if(planetData.getBoolean("hasAtmosphere"))
                {
                    int offsetTemp = planetData.getInt("temperature")-(random.nextInt(450,550));
                    biomeCompound.putFloat("climate",(offsetTemp)/500f);
                }
                else
                {
                    int offsetTemp = planetData.getInt("temperature")-(random.nextInt(400,600));
                    biomeCompound.putFloat("climate",(offsetTemp)/500f);
                }
                biomeCompound.putInt("skyColour",random.nextInt(16777215));
                biomeCompound.putInt("fogColour",random.nextInt(16777215));
                biomeCompound.putInt("waterColour",random.nextInt(16777215));
                biomeCompound.putInt("waterFogColour",random.nextInt(16777215));
                biomeCompound.putInt("grassColour",random.nextInt(16777215));
                biomeCompound.putInt("foliageColour",random.nextInt(16777215));
                biomeCompound.putFloat("temp",random.nextFloat()/2f);
                biomeCompound.putFloat("humid",random.nextFloat()/2f);
                biomeCompound.putFloat("erode",random.nextFloat()/2f);
                biomeCompound.putFloat("wierd",random.nextFloat()/2f);
                biomeCompound.putString("generalBlock",planetData.getString("generalBlock"));
                biomeCompound.putInt("temperature",planetData.getInt("temperature")+random.nextInt(-20,20));
                //biomeCompound.put("biome_ores",planetData.getList("planet_ores",ListTag.TAG_STRING));
                genOre(biomeCompound,random);
                //biomeCompound.put("rock_blocks",planetData.getList("rock_blocks",ListTag.TAG_STRING));
                genRocks(biomeCompound,random);
                //biomeCompound.put("lakeFluids",planetData.getList("lakeFluids",ListTag.TAG_STRING));
                genLakes(biomeCompound,random);
                byte[] flavour = new byte[10];
                for(int z=0; z<flavour.length; z++)
                {
                    if(random.nextBoolean())
                    {
                        flavour[z]=1;
                    }
                    else
                    {
                        flavour[z]=0;
                    }
                }
                biomeCompound.putByteArray("flavour",flavour);
                biomeCompound.putString("name",planetData.getString("name")+random.nextInt(10000));
                if(random.nextInt(10)==0)
                {
                    if(planetData.getBoolean("hasAtmosphere") && planetData.getBoolean("hasOxygen"))
                    {
                        biomeCompound.putString("treeTrunk","minecraft:oak_log");
                        biomeCompound.putString("treeLeaves","minecraft:oak_leaves");
                    }
                    else
                    {
                        biomeCompound.putString("treeTrunk","minecraft:blackstone");
                        biomeCompound.putString("treeLeaves","minecraft:basalt");
                    }
                }


                biomeList.add(biomeCompound);
            }

            planetData.put("biomes",biomeList);

            langFile.append("\"level.").append(planetName).append("\":\"");
            rebuild = planetName.toUpperCase();
            rebuild = rebuild.substring(0,2)+" "+rebuild.substring(2,rebuild.length()-1)+" "+rebuild.toLowerCase().charAt(rebuild.length()-1);
            langFile.append(rebuild).append("\",");


            systemData.put(planetName,planetData);
        }
        CompoundTag tag = getDynamicDataOrNew(storage);
        tag.put(systemName,systemData);
        writeToDynamic(storage,tag);
        return new Pair<>(systemData,systemName);
    }

    public static CompoundTag getDynamicDataOrNew() {
        return getDynamicDataOrNew(ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage());
    }

    public static CompoundTag getDynamicDataOrNew(DimensionDataStorage storage) {
        return storage.computeIfAbsent(DynamicSavedData::new, DynamicSavedData::new, "dataplanets_dynamic_data").getData().copy();
    }

    private static void writeToDynamic(CompoundTag tag) {
        writeToDynamic(ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage(), tag);
    }

    private static void writeToDynamic(DimensionDataStorage storage, CompoundTag tag) {
        storage.computeIfAbsent(DynamicSavedData::new, DynamicSavedData::new, "dataplanets_dynamic_data").setData(tag);
    }


    private static void genOre(CompoundTag biomeData,RandomSource random) {
        ListTag ores;
        if(biomeData.contains("biome_ores"))
        {
            ores = (ListTag) biomeData.get("biome_ores");
        }
        else
        {
            ores = new ListTag();
        }
        for (int i = 0; i < random.nextInt(1,5); i++) {
            ResourceLocation[] candidate = BuiltInRegistries.BLOCK.keySet().stream().filter(a->a.getPath().contains("_ore")).toArray(ResourceLocation[]::new);
            ResourceLocation orerl = candidate[random.nextInt(candidate.length)];
            String ore = orerl.toString();
            ores.add(StringTag.valueOf(ore));
            biomeData.put("biome_ores",ores);
        }

    }

    private static void genLakes(CompoundTag biomeData, RandomSource randomSource)
    {
        for (int i = 0; i < 1; i++) {

            List<Fluid> materials = BuiltInRegistries.FLUID.stream().filter(a->
            {
                int temp = a.getFluidType().getTemperature();

                //if the fluid temperature is >300 it's not set as a liquid at room temperature so is *probably* molten.
                //in this case, we need the planet to be warmer than its liquid state
                if(temp<=biomeData.getInt("temperature") && temp>300)
                {
                    return true;
                }
                //if the fluid temeperature <301 you probably need to cool it to get it as a liquid, or its liquid at room temperature
                //in this case, we want the planet to be colder than its liquid state
                if(biomeData.getInt("temperature")<=temp && temp<301)
                {
                    return true;
                }
                //since we only have 1 number (what temperature it is when it's a liquid) we can't infer when something freezes solid
                //or when something evaporates.

                return false;
            }).toList();

            String matName;
            String fluidName;

            //these first two conditions should only trigger if a planet is really, really hot or really, really cold.
            //and they should only trigger at all if we are in a gamestate where we have no other fluids
            if(materials.isEmpty() && biomeData.getInt("temperature")<301)
            {
                fluidName = "minecraft:packed_ice";
            }
            else if(materials.isEmpty() && biomeData.getInt("temperature")>300)
            {
                fluidName = "minecraft:lava";
            }
            else
            {
                fluidName = BuiltInRegistries.FLUID.getKey(materials.get(randomSource.nextInt(materials.size()))).toString();
            }

            ListTag lakes;
            if(biomeData.contains("lakeFluids"))
            {
                lakes = (ListTag) biomeData.get("lakeFluids");
            }
            else
            {
                lakes = new ListTag();
            }
            lakes.add(StringTag.valueOf(fluidName));

            biomeData.put("lakeFluids",lakes);

        }


    }
    private static void genRocks(CompoundTag biomeData, RandomSource randomSource)
    {
        ListTag rocks;
        if(biomeData.contains("rock_blocks"))
        {
            rocks = (ListTag) biomeData.get("rock_blocks");
        }
        else
        {
            rocks = new ListTag();
        }
        for (int i = 0; i < randomSource.nextInt(1,4); i++) {
            String matName = Compat.SURFACE_BLOCKS[randomSource.nextInt(Compat.SURFACE_BLOCKS.length)];
            rocks.add(StringTag.valueOf(matName));
        }



        biomeData.put("rock_blocks",rocks);


    }

    static boolean writeCond = false;
    private static void writeLinesCond(File file, List<String> strings) throws IOException {
        if(writeCond)
        {
            FileUtils.writeLines(file,strings);
        }
    }
}
