package shipwrights.dataplanets;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import shipwrights.dataplanets.compat.Compat;
import shipwrights.dataplanets.registry.*;
import shipwrights.dataplanets.space.S2PSyncPacket;
import shipwrights.dataplanets.space.StarSystemCreator;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import shipwrights.dataplanets.space.UpdateDimensionsPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Dataplanets.MODID)
public class Dataplanets
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "dataplanets";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final UUID LOW_GRAVITY = UUID.fromString("662A6B8D-DA3E-4C1C-1112-96EA6097278D");

    public static String LAST_WORLD_ID = "";
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Dataplanets(FMLJavaModLoadingContext context)
    {


        // Register the commonSetup method for modloading
        Compat.loadCompat("genesis");

        DPPackets.INSTANCE.messageBuilder(S2PSyncPacket.class, 0)
                .encoder(S2PSyncPacket::encoder)
                .decoder(S2PSyncPacket::decoder)
                .consumerMainThread(S2PSyncPacket::messageConsumer)
                .add();

        DPPackets.INSTANCE.messageBuilder(UpdateDimensionsPacket.class, 1)
                .encoder(UpdateDimensionsPacket::write)
                .decoder(UpdateDimensionsPacket::read)
                .consumerMainThread(UpdateDimensionsPacket::handle)
                .add();

        DPItems.init();
        DPBlocks.init();
        DPEntities.init();


        MinecraftForge.EVENT_BUS.register(this);

        var bus = context.getModEventBus();
        Compat.modEventBusLoad(bus);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void playerLogsIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        Compat.postLoadWorld(event.getEntity().getServer().overworld().getDataStorage());
    }

    @SubscribeEvent
    public void entityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        //event.registerEntityRenderer(DPEntities.NEUM.get(), NeumEntityRenderer::new);
    }


    @SubscribeEvent
    public void place(BlockEvent.EntityPlaceEvent event)
    {
        if(event.getPlacedBlock().is(Blocks.TORCH) && event.getPlacedBlock().is(Blocks.WALL_TORCH))
        {
            if(event.getLevel() instanceof Level level)
            {
                if(level.dimension().location().getNamespace().equals("dataplanets"))
                {
                    String name = level.dimension().location().getPath();
                    CompoundTag data = StarSystemCreator.getDynamicDataOrNew(event.getLevel().getServer().overworld().getDataStorage());
                    CompoundTag planetData = data.getCompound(name.substring(0,name.length()-1)).getCompound(name);
                    if(planetData.getBoolean("hasOxygen"))
                    {
                        event.setCanceled(true);
                    }

                }
            }
        }

    }



}
