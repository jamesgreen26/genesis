package shipwrights.dataplanets.space;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class DynamicSavedData extends SavedData {

    private CompoundTag data;

    public DynamicSavedData(){
        this.data = new CompoundTag();
    }

    public DynamicSavedData(CompoundTag data){
        this.data = data;
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
        return this.data;
    }
}
