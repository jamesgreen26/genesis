package shipwrights.genesis.compat.vs;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/// Applies the mixins of {@code genesis.vs.mixins.json}, which target Valkyrien Skies, only when it's loaded. Mixins run
/// before mods are, so this asks the loading mod list rather than {@link shipwrights.genesis.compat.Compat}, and uses
/// none of Valkyrien Skies' classes
public final class VsMixinPlugin implements IMixinConfigPlugin {

    private boolean vsLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        vsLoaded = LoadingModList.get().getModFileById("valkyrienskies") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return vsLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
