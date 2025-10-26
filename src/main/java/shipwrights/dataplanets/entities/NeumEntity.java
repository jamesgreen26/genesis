package shipwrights.dataplanets.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NeumEntity extends PathfinderMob {

    String block = "minecraft:stone";
    public NeumEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
        setCustomName(Component.literal("1101 The Neum"));
    }

    @Override
    public void spawnAnim() {
        super.spawnAnim();
        block = "minecraft:iron_block";
        shouldBeSaved();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        //this.goalSelector.addGoal(1, new LookAtPlayerGoal(this,Player.class,1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.2F));
    }

    @Override
    protected InteractionResult mobInteract(Player p_21472_, InteractionHand p_21473_) {
        if(level().isClientSide)
        {
            //Questable.randomQuestToPlayer(spe);
            GenericNPCScreen.open(this);
        }
        return super.mobInteract(p_21472_, p_21473_);
    }

    @Override
    public boolean save(CompoundTag arg) {
        arg.putString("block",block);
        return super.save(arg);
    }

    @Override
    public void load(CompoundTag arg) {
        super.load(arg);
        block = arg.getString("block");
    }
}
