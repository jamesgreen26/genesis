package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.entities.NeumEntity;
import shipwrights.dataplanets.entities.NeumEntityRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public class DPEntities {

    public static EntityEntry<NeumEntity> NEUM = DataplanetsMod.REGISTRATE.entity("neum",NeumEntity::new, MobCategory.CREATURE)
            .attributes(Mob::createMobAttributes)
            .renderer(()-> NeumEntityRenderer::new)
            .lang("Neum").register();

    /// do not delete
    public static void init() {}
}
