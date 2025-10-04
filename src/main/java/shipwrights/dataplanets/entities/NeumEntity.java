package shipwrights.dataplanets.entities;

import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import shipwrights.dataplanets.factions.Questable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NeumEntity extends PathfinderMob {

    public NeumEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this,Player.class,1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0F));
    }

    @Override
    protected InteractionResult mobInteract(Player p_21472_, InteractionHand p_21473_) {
        if(p_21472_ instanceof ServerPlayer spe)
        {
            Questable.randomQuestToPlayer(spe);
        }
        return super.mobInteract(p_21472_, p_21473_);
    }
}
