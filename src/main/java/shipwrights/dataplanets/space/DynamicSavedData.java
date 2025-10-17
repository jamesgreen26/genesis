package shipwrights.dataplanets.space;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class DynamicSavedData extends SavedData {

    private CompoundTag data;

    public DynamicSavedData(){
        this.data = new CompoundTag();
        this.setDirty();
    }

    public DynamicSavedData(CompoundTag data){
        this.data = data.getCompound("dataplanets_dynamic_data");
        this.setDirty();
    }

    public CompoundTag getData(){
        return this.data;
    }

    public void setData(CompoundTag data){
        this.data = data;
        this.setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag arg) {
        arg.put("dataplanets_dynamic_data", this.data);
        return arg;
    }
}
