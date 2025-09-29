package shipwrights.dataplanets.factions;

import shipwrights.dataplanets.MutableTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Quest consists of a "task" a "return" and a "reward"
 * - A task is an interpretable string, usually with a keyword like "visit"
 * - A return is also a task, a task and return need to be complete to obtain the reward
 * - A reward is an ItemStack (for now)
 */
//TODO: things in this file shouldn't be literals
public class Questable {

    public static final ResourceLocation QUESTING = ResourceLocation.fromNamespaceAndPath("dataplanets","questing");
    public static String visitLocation(MinecraftServer server)
    {
        List<ResourceKey<Level>> levels = server.levelKeys().stream().toList();
        ResourceKey<Level> levelKey = levels.get(server.overworld().random.nextInt(levels.size()));
        ServerLevel level = server.getLevel(levelKey);
        BlockPos place = level.findNearestMapStructure(MutableTags.QUEST_STRUCTURES,BlockPos.ZERO,10000,true);

        return "visit "+levelKey.location()+" "+place.getX()+" "+place.getY()+" "+place.getZ();
    }
    public static String checkLocation(MinecraftServer server)
    {
        List<ResourceKey<Level>> levels = server.levelKeys().stream().toList();
        RandomSource rs = server.overworld().random;
        ResourceKey<Level> levelKey = levels.get(rs.nextInt(levels.size()));
        BlockPos place = new BlockPos(rs.nextInt(-10000,10000),rs.nextInt(-50,200),rs.nextInt(-10000,10000));
        return "check "+levelKey.location()+" "+place.getX()+" "+place.getY()+" "+place.getZ();
    }
    public static String collectItems(MinecraftServer server)
    {
        Item f = BuiltInRegistries.ITEM.stream().filter(a->new ItemStack(a).is(MutableTags.QUEST_ITEMS)).findAny().get();
        ItemStack stack = new ItemStack(f,server.overworld().random.nextInt(1,128));
        return "collect "+BuiltInRegistries.ITEM.getKey(stack.getItem())+" "+stack.getCount();
    }

    public static String randomQuest(MinecraftServer server)
    {
        return switch (server.overworld().random.nextInt(3))
        {
            case 0 -> visitLocation(server);
            case 1 -> collectItems(server);
            case 2 -> checkLocation(server);
            default -> "Unreachable, theoretically, but I had to make a default case";
        };
    }

    public static void randomQuestToPlayer(ServerPlayer spe)
    {
        CompoundTag quests = spe.server.getCommandStorage().get(QUESTING);
        CompoundTag player = quests.getCompound(spe.getStringUUID());
        String task = randomQuest(spe.server);
        String ret = randomQuest(spe.server);
        spe.sendSystemMessage(Component.literal("Task: "+task));
        spe.sendSystemMessage(Component.literal("Return Task: "+ret));

        player.putString("task",task);
        player.putString("return",ret);

        quests.put(spe.getStringUUID(),player);
        spe.server.getCommandStorage().set(QUESTING,quests);

    }

    public static void interpret(ServerPlayer spe)
    {
        CompoundTag quests = spe.server.getCommandStorage().get(QUESTING);
        CompoundTag player = quests.getCompound(spe.getStringUUID());
        String string = player.getString("task");
        if(player.getString("task").contains("visit") || player.getString("task").contains("check"))
        {

            String[] split = string.split(" ");
            if(spe.serverLevel().dimension()==ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(split[1])))
            {
                BlockPos pos = new BlockPos(Integer.parseInt(split[2]),spe.getBlockY(),Integer.parseInt(split[4]));
                if(spe.getOnPos().distSqr(pos)<100)
                {
                    updateQuest(spe);
                }
            }
        }
        if(player.getString("task").contains("collect"))
        {
            String[] split = string.split(" ");
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(split[1]));
            int count = Integer.parseInt(split[2]);
            if(spe.getInventory().countItem(item)>=count)
            {
                updateQuest(spe);
            }
        }
    }

    public static void updateQuest(ServerPlayer spe)
    {
        CompoundTag quests = spe.server.getCommandStorage().get(QUESTING);
        CompoundTag player = quests.getCompound(spe.getStringUUID());
        if(!player.getString("return").equals("finished"))
        {
            player.putString("task",player.getString("return"));
            player.putString("return","finished");
            spe.sendSystemMessage(Component.literal("Finished the task!"));
        }
        else
        {
            player.putString("task","not started");
            String[] v = player.getString("reward").split(",");
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(v[0]));
            spe.sendSystemMessage(Component.literal("Quest completed, reward added!"));
            spe.addItem(new ItemStack(item,Integer.parseInt(v[1])));
        }
        quests.put(spe.getStringUUID(),player);
        spe.server.getCommandStorage().set(QUESTING,quests);
    }
}
