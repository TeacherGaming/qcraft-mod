package dan200.qcraft.shared;

import dan200.QCraft;
import static dan200.qcraft.shared.QCraftProxyCommon.saveNBTToPath;
import java.io.File;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;

/**
 *
 * @author Robijnvogel
 */
public class EntanglementSavedData extends QCraftSavedData {

    private static final String DATA_NAME = "qCraft_EntSavedData";

    public EntanglementSavedData() {
        super(DATA_NAME);
    }

    public EntanglementSavedData(String s) {
        super(s);
    }

    @Override
    public File getSaveLocation(World world) {
        return new File(super.getSaveLocation(world), "entanglements.bin");
    }

    public static EntanglementSavedData get(World world) {
        MapStorage storage = world.mapStorage;
        EntanglementSavedData instance = (EntanglementSavedData) storage.loadData(EntanglementSavedData.class, DATA_NAME);

        if (instance == null) {
            instance = new EntanglementSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    @Override
    public void writeToNBT(NBTTagCompound rootnbt) {

        NBTTagCompound qblocks = new NBTTagCompound();
        TileEntityQBlock.QBlockRegistry.writeToNBT(qblocks);
        rootnbt.setTag("qblocks", qblocks);

        NBTTagCompound qcomputers = new NBTTagCompound();
        TileEntityQuantumComputer.ComputerRegistry.writeToNBT(qcomputers);
        rootnbt.setTag("qcomputers", qcomputers);

        NBTTagCompound portals = new NBTTagCompound();
        PortalRegistry.PortalRegistry.writeToNBT(portals);
        rootnbt.setTag("portals", portals);

        saveNBTToPath(getSaveLocation(QCraft.getDefWorld()), rootnbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound rootnbt) {
        // Reset
        TileEntityQBlock.QBlockRegistry.reset();
        TileEntityQuantumComputer.ComputerRegistry.reset();
        PortalRegistry.PortalRegistry.reset();

        // Load NBT
        if (rootnbt != null) {
            if (rootnbt.hasKey("qblocks")) {
                NBTTagCompound qblocks = rootnbt.getCompoundTag("qblocks");
                TileEntityQBlock.QBlockRegistry.readFromNBT(qblocks);
            }
            if (rootnbt.hasKey("qcomputers")) {
                NBTTagCompound qcomputers = rootnbt.getCompoundTag("qcomputers");
                TileEntityQuantumComputer.ComputerRegistry.readFromNBT(qcomputers);
            }
            if (rootnbt.hasKey("portals")) {
                NBTTagCompound portals = rootnbt.getCompoundTag("portals");
                PortalRegistry.PortalRegistry.readFromNBT(portals);
            }
        }
    }
}
