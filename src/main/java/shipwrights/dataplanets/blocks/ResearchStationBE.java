package shipwrights.dataplanets.blocks;

import shipwrights.dataplanets.registry.DPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ResearchStationBE extends BlockEntity {

    ListTag datapoints = new ListTag();
    int progress = 0;

    public ResearchStationBE(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("datapoints",datapoints);
        tag.putInt("progress",progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag){
        super.load(tag);
        datapoints = (ListTag) tag.get("datapoints");
        progress = tag.getInt("progress");
    }

    public boolean addDatapoint(String biome, int sectorX, int sectorZ,long time)
    {
        boolean exists = false;
        for (int i = 0; i < datapoints.size(); i++) {
            CompoundTag point = datapoints.getCompound(i);
            if(point.getInt("sectorX")==sectorX && point.getInt("sectorZ")==sectorZ)
            {
                if(point.getString("biome").equals(biome))
                {
                    exists=true;
                    break;
                }
            }
        }
        if(!exists)
        {
            CompoundTag tag = new CompoundTag();
            tag.putLong("time",time);
            tag.putInt("sectorX",sectorX);
            tag.putInt("sectorZ",sectorZ);
            tag.putString("biome",biome);
            datapoints.add(tag);
            setChanged();
            return true;
        }
        return false;
    }

    public void addGenericDatapoint()
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("time",level.getGameTime()-50000L);
        datapoints.add(tag);
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("datapoints",datapoints);
        tag.putInt("progress",progress);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    public static void tick(Level level, BlockPos pos, BlockState blockState, ResearchStationBE be)
    {
        if(level.getDayTime()%100==0)
        {
            if(be.datapoints==null)be.datapoints=new ListTag();

            be.progress+=be.datapoints.size();
            if(be.progress>=1000)
            {
                if(level.getBlockEntity(pos.above()) instanceof Container container)
                {
                    boolean found = false;
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        container.getItem(i);
                        if(container.getItem(i).isEmpty())
                        {
                            container.setItem(i,new ItemStack(DPItems.THEORY));
                            found = true;
                            break;
                        }
                    }
                    if(found)
                    {
                        be.progress=0;
                    }
                }
            }



            List<CompoundTag> tags = new ArrayList<>();
            for (int i = 0; i < be.datapoints.size(); i++) {
                CompoundTag tag = be.datapoints.getCompound(i);
                if(level.getGameTime()>tag.getLong("time")+ 100000L)
                {
                    tags.add(tag);
                }
            }
            tags.forEach(a->be.datapoints.remove(a));
            level.sendBlockUpdated(pos,blockState,blockState,2);
            be.setChanged();
        }
    }
}
