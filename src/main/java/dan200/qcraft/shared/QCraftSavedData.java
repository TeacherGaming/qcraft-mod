package dan200.qcraft.shared;

import cpw.mods.fml.common.FMLCommonHandler;
import dan200.QCraft;
import java.io.File;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 *
 * @author Robijnvogel
 */
abstract class QCraftSavedData extends WorldSavedData{

    public QCraftSavedData(String name) {
        super(name);
    }

    public File getSaveLocation(World world) {
        File rootDir = FMLCommonHandler.instance().getMinecraftServerInstance().getFile( "." );
        File saveDir = null;
        if( QCraft.isServer() )
        {
            saveDir = new File( rootDir, world.getSaveHandler().getWorldDirectoryName() );
        }
        else
        {
            saveDir = new File( rootDir, "saves/" + world.getSaveHandler().getWorldDirectoryName() );
        }
        return new File( saveDir, "quantum/" );
    }
}
