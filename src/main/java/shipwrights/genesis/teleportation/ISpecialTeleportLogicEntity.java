package shipwrights.genesis.teleportation;

public interface ISpecialTeleportLogicEntity {
	void genesis$beforeTeleport();

	void genesis$afterTeleport(ISpecialTeleportLogicEntity old);
}
