package shipwrights.genesis.space.transformProvider;

public class BuiltinTransformProviders {
    public static void register() {
        StaticTransformProvider.register();
        OrbitingTransformProvider.register();
    }
}
