package dan200.qcraft.shared;

import dan200.QCraft;
import static dan200.qcraft.shared.QCraftProxyCommon.saveNBTToPath;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;

/**
 *
 * @author Robijnvogel
 */
public class EncryptionSavedData extends QCraftSavedData {

    private static final String DATA_NAME = "qCraft_EncSavedData";

    public EncryptionSavedData() {
        super(DATA_NAME);
    }

    public EncryptionSavedData(String s) {
        super(s);
    }

    @Override
    public File getSaveLocation(World world) {
        return new File(super.getSaveLocation(world), "encryption.bin");
    }

    public static EncryptionSavedData get(World world) {
        MapStorage storage = world.mapStorage;
        EncryptionSavedData instance = (EncryptionSavedData) storage.loadData(EncryptionSavedData.class, DATA_NAME);

        if (instance == null) {
            instance = new EncryptionSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    @Override
    public void writeToNBT(NBTTagCompound encryptionnbt) {

        NBTTagCompound encryption = new NBTTagCompound();
        EncryptionRegistry.Instance.writeToNBT(encryption);
        encryptionnbt.setTag("encryption", encryption);

        saveNBTToPath(getSaveLocation(QCraft.getDefWorld()), encryptionnbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound encryptionnbt) {
        // Reset
        EncryptionRegistry.Instance.reset();

        // Load NBT
        if (encryptionnbt != null) {
            if (encryptionnbt.hasKey("encryption")) {
                NBTTagCompound encryption = encryptionnbt.getCompoundTag("encryption");
                EncryptionRegistry.Instance.readFromNBT(encryption);
            }
        }
    }
}
