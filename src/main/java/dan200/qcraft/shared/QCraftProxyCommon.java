/*
Copyright 2014 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


package dan200.qcraft.shared;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import dan200.QCraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.RecipeSorter;

import java.io.*;

import static net.minecraftforge.oredict.RecipeSorter.Category.SHAPED;

public abstract class QCraftProxyCommon implements IQCraftProxy
{
    public QCraftProxyCommon()
    {
    }

    // IQCraftProxy implementation

    @Override
    public void preLoad()
    {
        registerItems();
    }

    @Override
    public void load()
    {
        registerTileEntities();
        registerForgeHandlers();
    }

    @Override
    public abstract boolean isClient();

    @Override
    public abstract Object getQuantumComputerGUI( InventoryPlayer inventory, TileEntityQuantumComputer computer );

    @Override
    public abstract void showItemTransferGUI( EntityPlayer entityPlayer, TileEntityQuantumComputer computer );

    @Override
    public abstract void travelToServer( LostLuggage.Address address );

    @Override
    public boolean isPlayerWearingGoggles( EntityPlayer player )
    {
        ItemStack headGear = player.inventory.armorItemInSlot( 3 );
        if( headGear != null &&
                headGear.getItem() == QCraft.Items.quantumGoggles )
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPlayerWearingQuantumGoggles( EntityPlayer player )
    {
        ItemStack headGear = player.inventory.armorItemInSlot( 3 );
        if( headGear != null &&
                headGear.getItem() == QCraft.Items.quantumGoggles &&
                headGear.getItemDamage() == ItemQuantumGoggles.SubTypes.Quantum )
        {
            return true;
        }
        return false;
    }

    @Override
    public abstract boolean isLocalPlayerWearingGoggles();

    @Override
    public abstract boolean isLocalPlayerWearingQuantumGoggles();

    @Override
    public abstract void renderQuantumGogglesOverlay( float width, float height );

    @Override
    public abstract void renderAOGogglesOverlay( float width, float height );

    @Override
    public abstract void spawnQuantumDustFX( World world, double x, double y, double z );

    private void registerItems()
    {
        // Register our own creative tab
        QCraft.creativeTab = new CreativeTabQuantumCraft( CreativeTabs.getNextID(), "qCraft" );

        // BLOCKS

        // Quantum ore blocks
        QCraft.Blocks.quantumOre = new BlockQuantumOre( false );
        GameRegistry.registerBlock( QCraft.Blocks.quantumOre, "quantumore" );

        QCraft.Blocks.quantumOreGlowing = new BlockQuantumOre( true );
        GameRegistry.registerBlock( QCraft.Blocks.quantumOreGlowing, "quantumoreglowing" );

        // Quantum logic block
        QCraft.Blocks.quantumLogic = new BlockQuantumLogic();
        GameRegistry.registerBlock( QCraft.Blocks.quantumLogic, ItemQuantumLogic.class, "quantumlogic" );

        // qBlock block
        QCraft.Blocks.qBlock = new BlockQBlock();
        GameRegistry.registerBlock( QCraft.Blocks.qBlock, ItemQBlock.class, "qblock" );

        // Quantum Computer block
        QCraft.Blocks.quantumComputer = new BlockQuantumComputer();
        GameRegistry.registerBlock( QCraft.Blocks.quantumComputer, ItemQuantumComputer.class, "quantumcomputer" );

        // Quantum Portal block
        QCraft.Blocks.quantumPortal = new BlockQuantumPortal();
        GameRegistry.registerBlock( QCraft.Blocks.quantumPortal, "quantumportal" );

        // ITEMS

        // Quantum Dust item
        QCraft.Items.quantumDust = new ItemQuantumDust();
        GameRegistry.registerItem( QCraft.Items.quantumDust, "dust" );

        // EOS item
        QCraft.Items.eos = new ItemEOS();
        GameRegistry.registerItem( QCraft.Items.eos, "essence" );

        // Quantum Goggles item
        QCraft.Items.quantumGoggles = new ItemQuantumGoggles();
        GameRegistry.registerItem( QCraft.Items.quantumGoggles, "goggles" );
        
        // Dummy item to contain (modded) items that were sent to this server, but don't exist here
        QCraft.Items.missingItem = new ItemMissing();
        GameRegistry.registerItem( QCraft.Items.missingItem, "missing" );

        // RECIPES

        // Automated Observer recipe
        ItemStack observer = new ItemStack( QCraft.Blocks.quantumLogic, 1, BlockQuantumLogic.SubType.ObserverOff );
        GameRegistry.addRecipe( observer, new Object[]{
            "XXX", "XYX", "XZX",
            Character.valueOf( 'X' ), Blocks.stone,
            Character.valueOf( 'Y' ), new ItemStack( QCraft.Items.eos, 1, ItemEOS.SubType.Observation ),
            Character.valueOf( 'Z' ), Items.redstone
        } );

        // EOS recipe
        ItemStack eos = new ItemStack( QCraft.Items.eos, 1, ItemEOS.SubType.Superposition );
        GameRegistry.addRecipe( eos, new Object[]{
            "XX", "XX",
            Character.valueOf( 'X' ), QCraft.Items.quantumDust,
        } );

        // EOO recipe
        ItemStack eoo = new ItemStack( QCraft.Items.eos, 1, ItemEOS.SubType.Observation );
        GameRegistry.addRecipe( eoo, new Object[]{
            " X ", "X X", " X ",
            Character.valueOf( 'X' ), QCraft.Items.quantumDust,
        } );

        // EOE recipe
        ItemStack eoe = new ItemStack( QCraft.Items.eos, 1, ItemEOS.SubType.Entanglement );
        GameRegistry.addRecipe( eoe, new Object[]{
            "X X", " Y ", "X X",
            Character.valueOf( 'X' ), QCraft.Items.quantumDust,
            Character.valueOf( 'Y' ), eos,
        } );

        // qBlock recipes
        GameRegistry.addRecipe( new QBlockRecipe() );
        RecipeSorter.register( "qCraft:qBlock", QBlockRecipe.class, SHAPED, "after:minecraft:shapeless" );

        GameRegistry.addRecipe( new EntangledQBlockRecipe() );
        RecipeSorter.register( "qCraft:entangled_qBlock", EntangledQBlockRecipe.class, SHAPED, "after:minecraft:shapeless" );

        // Quantum Computer recipe
        ItemStack regularQuantumComputer = ItemQuantumComputer.create( -1, 1 );
        GameRegistry.addRecipe( regularQuantumComputer, new Object[] {
            "XXX", "XYX", "XZX",
            Character.valueOf( 'X' ), Items.iron_ingot,
            Character.valueOf( 'Y' ), QCraft.Items.quantumDust,
            Character.valueOf( 'Z' ), Blocks.glass_pane,
        } );

        // Entangled Quantum Computer
        ItemStack entangledQuantumComputer = ItemQuantumComputer.create( 0, 1 );
        GameRegistry.addRecipe( new EntangledQuantumComputerRecipe() );
        RecipeSorter.register( "qCraft:entangled_computer", EntangledQuantumComputerRecipe.class, SHAPED, "after:minecraft:shapeless" );

        // Quantum Goggles recipe
        ItemStack quantumGoggles = new ItemStack( QCraft.Items.quantumGoggles, 1, ItemQuantumGoggles.SubTypes.Quantum );
        GameRegistry.addRecipe( quantumGoggles, new Object[] {
            "XYX",
            Character.valueOf( 'X' ), Blocks.glass_pane,
            Character.valueOf( 'Y' ), QCraft.Items.quantumDust,
        } );

        // Anti-observation goggles recipe
        ItemStack aoGoggles = new ItemStack( QCraft.Items.quantumGoggles, 1, ItemQuantumGoggles.SubTypes.AntiObservation );
        GameRegistry.addRecipe( aoGoggles, new Object[] {
            "XYX",
            Character.valueOf( 'X' ), Blocks.glass_pane,
            Character.valueOf( 'Y' ), new ItemStack( QCraft.Items.eos, 1, ItemEOS.SubType.Observation ),
        } );

        if( QCraft.enableWorldGenReplacementRecipes )
        {
            // Quantum dust recipe
            GameRegistry.addRecipe( new ItemStack( QCraft.Items.quantumDust, 2 ), new Object[] {
                "XY",
                Character.valueOf( 'X' ), Items.redstone,
                Character.valueOf( 'Y' ), new ItemStack( Items.dye, 1, 10 ) // Lime green
            } );
        }
    }

    private void registerTileEntities()
    {
        // Tile Entities
        GameRegistry.registerTileEntity( TileEntityQBlock.class, "qblock" );
        GameRegistry.registerTileEntity( TileEntityQuantumComputer.class, "qcomputer" );
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
        FMLCommonHandler.instance().bus().register( handlers );
        if( QCraft.enableWorldGen )
        {
            GameRegistry.registerWorldGenerator( new QuantumOreGenerator(), 1 );
        }
        NetworkRegistry.INSTANCE.registerGuiHandler( QCraft.instance, handlers );

        ConnectionHandler connectionHandler = new ConnectionHandler();
        MinecraftForge.EVENT_BUS.register( connectionHandler );
        FMLCommonHandler.instance().bus().register( connectionHandler );
    }

    public class ForgeHandlers implements
        IGuiHandler
    {
        private ForgeHandlers()
        {
        }

        // IGuiHandler implementation

        @Override
        public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            TileEntity tile = world.getTileEntity( x, y, z );
            switch( id )
            {
                case QCraft.quantumComputerGUIID:
                {
                    if( tile != null && tile instanceof TileEntityQuantumComputer )
                    {
                        TileEntityQuantumComputer computer = (TileEntityQuantumComputer) tile;
                        return new ContainerQuantumComputer( player.inventory, computer );
                    }
                    break;
                }
            }
            return null;
        }

        @Override
        public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            TileEntity tile = world.getTileEntity( x, y, z );
            switch( id )
            {
                case QCraft.quantumComputerGUIID:
                {
                    if( tile != null && tile instanceof TileEntityQuantumComputer )
                    {
                        TileEntityQuantumComputer drive = (TileEntityQuantumComputer) tile;
                        return getQuantumComputerGUI( player.inventory, drive );
                    }
                    break;
                }
            }
            return null;
        }

        // Forge event responses

        @SubscribeEvent
        public void onPlayerLogin( PlayerEvent.PlayerLoggedInEvent event )
        {
            EntityPlayer player = event.player;
            if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
            {
                QCraft.clearUnverifiedLuggage( player ); // Shouldn't be necessary, but can't hurt
                QCraft.requestLuggage( player );
            }
        }

        @SubscribeEvent
        public void onPlayerLogout( PlayerEvent.PlayerLoggedOutEvent event )
        {
            EntityPlayer player = event.player;
            if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
            {
                QCraft.clearUnverifiedLuggage( player );
            }
        }
    }

    public static NBTTagCompound loadNBTFromPath( File file )
    {
        try
        {
            if( file != null && file.exists() )
            {
                InputStream input = new BufferedInputStream( new FileInputStream( file ) );
                try
                {
                    return CompressedStreamTools.readCompressed( input );
                }
                finally
                {
                    input.close();
                }
            }
        }
        catch( IOException e )
        {
            QCraft.log( "Warning: failed to load QCraft entanglement info" );
        }
        return null;
    }

    public static void saveNBTToPath( File file, NBTTagCompound nbt )
    {
        try
        {
            if( file != null )
            {
                file.getParentFile().mkdirs();
                OutputStream output = new BufferedOutputStream( new FileOutputStream( file ) );
                try
                {
                    CompressedStreamTools.writeCompressed( nbt, output );
                }
                finally
                {
                    output.close();
                }
            }
        }
        catch( IOException e )
        {
            QCraft.log( "Warning: failed to save QCraft entanglement info" );
        }
    }
}
