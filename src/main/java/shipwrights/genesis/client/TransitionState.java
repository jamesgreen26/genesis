package shipwrights.genesis.client;

public enum TransitionState {
    NONE, SPACE_TRAVEL, WORMHOLE_TRAVEL;

    public static TransitionState CURRENT = NONE;
}
