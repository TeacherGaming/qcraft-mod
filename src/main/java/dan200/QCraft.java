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


package dan200;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.registry.GameRegistry;
import dan200.qcraft.shared.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.IOException;
import java.security.PublicKey;
import java.util.*;
import net.minecraft.item.Item;

///////////////
// UNIVERSAL //
///////////////

@Mod( modid = "qCraft", name = "qCraft", version = "${version}" )
public class QCraft
{
    // Static Settings
    // GUI IDs
    // ComputerCraft uses ID 100-102
    // CCTurtle uses ID 103
    // JuniorDev uses ID 104
    public static final int quantumComputerGUIID = 105;

    // Configuration options
    public static boolean enableQBlockOcclusionTesting = false;
    public static boolean enableWorldGen = true;
    public static boolean enableWorldGenReplacementRecipes = false;

    public static boolean letAdminsCreatePortals = true;
    public static boolean letPlayersCreatePortals = true;
    public static boolean letAdminsEditPortalServerList = true;
    public static boolean letPlayersEditPortalServerList = false;
    public static boolean letAdminsVerifyPortalServers = true;
    public static boolean letPlayersVerifyPortalServers = false;
    public static int maxPortalSize = 5;
    public static int maxQTPSize = 8;

    // Blocks and Items
    public static class Blocks
    {
        public static BlockQuantumOre quantumOre;
        public static BlockQuantumOre quantumOreGlowing;
        public static BlockQuantumLogic quantumLogic;
        public static BlockQBlock qBlock;
        public static BlockQuantumComputer quantumComputer;
        public static BlockQuantumPortal quantumPortal;
    }

    public static class Items
    {
        public static ItemQuantumDust quantumDust;
        public static ItemEOS eos;
        public static ItemQuantumGoggles quantumGoggles;
        public static ItemMissing missingItem;
    }

    // Networking
    public static FMLEventChannel networkEventChannel;
    public static LostLuggage.Address travelNextTick;

    // Other stuff
    public static CreativeTabs creativeTab;

    public static CreativeTabs getCreativeTab()
    {
        return creativeTab;
    }

    private static enum LuggageVerificationResult
    {
        UNTRUSTED,
        TRUSTED,
        LOCALKEY
    }

    // Implementation
    @Mod.Instance( value = "qCraft" )
    public static QCraft instance;

    @SidedProxy(
        clientSide = "dan200.qcraft.client.QCraftProxyClient",
        serverSide = "dan200.qcraft.server.QCraftProxyServer"
    )
    public static IQCraftProxy proxy;

    public QCraft()
    {
    }

    @Mod.EventHandler
    public void preInit( FMLPreInitializationEvent event )
    {
        // Load config

        Configuration config = new Configuration( event.getSuggestedConfigurationFile() );
        config.load();

        // Setup general

        Property prop = config.get( Configuration.CATEGORY_GENERAL, "enableQBlockOcclusionTesting", enableQBlockOcclusionTesting );
        prop.comment = "Set whether QBlocks should not be observed if their line of sight to the player is obstructed. WARNING: This has a very high performance cost if you have lots of QBlocks in your world!!";
        enableQBlockOcclusionTesting = prop.getBoolean( enableQBlockOcclusionTesting );

        prop = config.get( Configuration.CATEGORY_GENERAL, "enableWorldGen", enableWorldGen );
        prop.comment = "Set whether Quantum Ore will spawn in new chunks";
        enableWorldGen = prop.getBoolean( enableWorldGen );

        prop = config.get( Configuration.CATEGORY_GENERAL, "enableWorldGenReplacementRecipes", enableWorldGenReplacementRecipes );
        prop.comment = "Set whether Quantum Dust can be crafted instead of mined";
        enableWorldGenReplacementRecipes = prop.getBoolean( enableWorldGenReplacementRecipes );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letAdminsCreatePortals", letAdminsCreatePortals );
        prop.comment = "Set whether server admins can energize portals";
        letAdminsCreatePortals = prop.getBoolean( letAdminsCreatePortals );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letPlayersCreatePortals", letPlayersCreatePortals );
        prop.comment = "Set whether players can energize portals.";
        letPlayersCreatePortals = prop.getBoolean( letPlayersCreatePortals );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letAdminsEditPortalServerList", letAdminsEditPortalServerList );
        prop.comment = "Set whether server admins can edit the list of Servers which portals can teleport to";
        letAdminsEditPortalServerList = prop.getBoolean( letAdminsEditPortalServerList );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letPlayersEditPortalServerList", letPlayersEditPortalServerList );
        prop.comment = "Set whether players can edit the list of Servers which portals can teleport to";
        letPlayersEditPortalServerList = prop.getBoolean( letPlayersEditPortalServerList );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letAdminsVerifyPortalServers", letAdminsVerifyPortalServers );
        prop.comment = "Set whether server admins can verify an inter-server portal link";
        letAdminsVerifyPortalServers = prop.getBoolean( letAdminsVerifyPortalServers );

        prop = config.get( Configuration.CATEGORY_GENERAL, "letPlayersVerifyPortalServers", letPlayersVerifyPortalServers );
        prop.comment = "Set whether players can verify an inter-server portal link";
        letPlayersVerifyPortalServers = prop.getBoolean( letPlayersVerifyPortalServers );
        
        prop = config.get( Configuration.CATEGORY_GENERAL, "maxPortalSize", maxPortalSize );
        prop.comment = "Set the maximum height and width for the Quantum Portal inside the frame in blocks. [min: 3, max: 16, def: 5]";
        int temp = prop.getInt( maxPortalSize );
        if (temp < 3) {
            maxPortalSize = 3;
        } else if (temp > 16) {
            maxPortalSize = 16;
        } else {
            maxPortalSize = prop.getInt( maxPortalSize );
        }
        prop.set(maxPortalSize);
        
        prop = config.get( Configuration.CATEGORY_GENERAL, "maxQTPSize", maxQTPSize );
        prop.comment = "Set the maximum distance from the Quantum Computer that the quantization or teleportation field can extend in blocks. (3 means that there are 2 blocks between the computer and the pillar) [min: 1, max: 16, def: 8]";
        temp = prop.getInt( maxQTPSize );
        if (temp < 1) {
            maxQTPSize = 1;
        } else if (temp > 16) {
            maxQTPSize = 16;
        } else {
            maxQTPSize = prop.getInt( maxQTPSize );
        }
        prop.set(maxQTPSize);
        //if more configs like these last two get added, it might be a good idea to include a method that checks the maximum and minimum instead of copying code over and over

        // None

        // Save config
        config.save();

        // Setup network
        networkEventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel( "qCraft" );
        networkEventChannel.register( new PacketHandler() );

        proxy.preLoad();
    }

    @Mod.EventHandler
    public void init( FMLInitializationEvent event )
    {
        proxy.load();
    }

    @Mod.EventHandler
    public void serverLoad( FMLServerStartingEvent event )
    {
        event.registerServerCommand( new QCraftCommand() );
        EncryptionSavedData.get(getDefWorld()); //trigger load of inter-server portal validations from disk to memory
        EntanglementSavedData.get(getDefWorld()); //trigger load of entanglements and portals from disk to memory
    }

    public static boolean isClient()
    {
        return proxy.isClient();
    }

    public static boolean isServer()
    {
        return !isClient();
    }

    public static World getDefWorld() {
        return proxy.getDefWorld(); //gets the server or client world dim 0 handler
    }

    public static void openQuantumComputerGUI( EntityPlayer player, TileEntityQuantumComputer computer )
    {
        player.openGui(QCraft.instance, QCraft.quantumComputerGUIID, player.worldObj, computer.xCoord, computer.yCoord, computer.zCoord);
    }

    private static FMLProxyPacket encode( QCraftPacket packet )
    {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes( buffer );
        return new FMLProxyPacket( buffer, "qCraft" );
    }

    public static void requestEnergize( TileEntityQuantumComputer computer )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.EnergizeComputer;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestCycleServerAddress( TileEntityQuantumComputer computer )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.CycleServerAddress;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        packet.dataString = new String[]{ computer.getRemoteServerAddress() };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestSetNewServerAddress( TileEntityQuantumComputer computer, String name, String address )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.SetNewServerAddress;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        packet.dataString = new String[]{ name, address };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestRemoveServerAddress( TileEntityQuantumComputer computer )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.RemoveServerAddress;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        packet.dataString = new String[]{ computer.getRemoteServerAddress() };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestSetRemotePortalID( TileEntityQuantumComputer computer, String remotePortalID )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.SetComputerRemotePortalID;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        packet.dataString = new String[]{ remotePortalID };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestSetPortalID( TileEntityQuantumComputer computer, String portalID )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.SetComputerPortalID;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        packet.dataString = new String[]{ portalID };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestQueryGoToServer( EntityPlayer player, TileEntityQuantumComputer computer )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.QueryGoToServer;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord };
        networkEventChannel.sendTo( encode( packet ), (EntityPlayerMP) player );
    }

    public static void requestConfirmGoToServer( TileEntityQuantumComputer computer, String destinationServer, boolean takeItems )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.ConfirmGoToServer;
        packet.dataInt = new int[]{ computer.xCoord, computer.yCoord, computer.zCoord, takeItems ? 1 : 0 };
        packet.dataString = new String[]{ destinationServer };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static void requestGoToServer( EntityPlayer player, String remoteAddress, byte[] luggage )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.GoToServer;
        packet.dataString = new String[]{ remoteAddress };
        packet.dataByte = new byte[][] { luggage };
        networkEventChannel.sendTo( encode( packet ), (EntityPlayerMP) player );
    }

    public static void requestLuggage( EntityPlayer player )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.RequestLuggage;
        networkEventChannel.sendTo( encode( packet ), (EntityPlayerMP)player );
    }

    public static void requestDiscardLuggage( EntityPlayer player, byte[] luggage )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.DiscardLuggage;
        packet.dataByte = new byte[][] { luggage };
        networkEventChannel.sendTo( encode( packet ), (EntityPlayerMP)player );
    }

    public static void requestUnpackLuggage( LostLuggage.LuggageMatch luggage )
    {
        QCraftPacket packet = new QCraftPacket();
        packet.packetType = QCraftPacket.UnpackLuggage;

        packet.dataByte = new byte[][] { luggage.m_luggage };
        packet.dataInt = new int[]{ (luggage.m_matchedDestination ? 1 : 0) };
        networkEventChannel.sendToServer( encode( packet ) );
    }

    public static boolean isPlayerWearingGoggles( EntityPlayer player )
    {
        return proxy.isPlayerWearingGoggles( player );
    }

    public static boolean isPlayerWearingQuantumGoggles( EntityPlayer player )
    {
        return proxy.isPlayerWearingQuantumGoggles( player );
    }

    public static boolean isLocalPlayerWearingGoggles()
    {
        return proxy.isLocalPlayerWearingGoggles();
    }

    public static boolean isLocalPlayerWearingQuantumGoggles()
    {
        return proxy.isLocalPlayerWearingQuantumGoggles();
    }

    public static boolean isPlayerOpped( EntityPlayer player )
    {
        if( !player.worldObj.isRemote )
        {
            return MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
        }
        else
        {
            return false;
        }
    }

    public static boolean canAnybodyCreatePortals()
    {
        return letAdminsCreatePortals || letPlayersCreatePortals;
    }

    public static boolean canPlayerCreatePortals( EntityPlayer player )
    {
        if( isPlayerOpped( player ) )
        {
            return letAdminsCreatePortals;
        }
        else
        {
            return letPlayersCreatePortals;
        }
    }

    public static boolean canPlayerEditPortalServers( EntityPlayer player )
    {
        if( isPlayerOpped( player ) )
        {
            return letAdminsEditPortalServerList;
        }
        else
        {
            return letPlayersEditPortalServerList;
        }
    }

    public static boolean canAnybodyVerifyPortalServers()
    {
        return letAdminsVerifyPortalServers || letPlayersVerifyPortalServers;
    }

    public static boolean canEverybodyVerifyPortalServers()
    {
        return letAdminsVerifyPortalServers && letPlayersVerifyPortalServers;
    }

    public static boolean canPlayerVerifyPortalServers( EntityPlayer player )
    {
        if( isPlayerOpped( player ) )
        {
            return letAdminsVerifyPortalServers;
        }
        else
        {
            return letPlayersVerifyPortalServers;
        }
    }

    public static void renderQuantumGogglesOverlay( float width, float height )
    {
        proxy.renderQuantumGogglesOverlay( width, height );
    }

    public static void renderAOGogglesOverlay( float width, float height )
    {
        proxy.renderAOGogglesOverlay( width, height );
    }

    public static void handleServerPacket( QCraftPacket packet, EntityPlayer sender )
    {
        // Messages sent from client to server
        //System.out.println( "SERVER GOT PACKET " + packet.packetType );
        EntityPlayer entityPlayer = sender;
        World world = sender.getEntityWorld();
        switch( packet.packetType )
        {
            case QCraftPacket.EnergizeComputer:
            {
                // Pressed "energize" on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    computer.tryEnergize();
                }
                break;
            }
            case QCraftPacket.SetComputerPortalID:
            {
                // Changed the portal ID address on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( QCraft.canPlayerCreatePortals( entityPlayer ) && !computer.isTeleporterEnergized() )
                    {
                        computer.setPortalID( packet.dataString[ 0 ] );
                        world.markBlockForUpdate( x, y, z );
                    }
                }
                break;
            }
            case QCraftPacket.SetComputerRemotePortalID:
            {
                // Changed the remote portal ID on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( QCraft.canPlayerCreatePortals( entityPlayer ) && !computer.isTeleporterEnergized() )
                    {
                        computer.setRemotePortalID( packet.dataString[ 0 ] );
                        world.markBlockForUpdate( x, y, z );
                    }
                }
                break;
            }
            case QCraftPacket.CycleServerAddress:
            {
                // Pressed the change server button on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( QCraft.canPlayerCreatePortals( entityPlayer ) && !computer.isTeleporterEnergized() )
                    {
                        computer.cycleRemoteServerAddress( packet.dataString[ 0 ] );
                        world.markBlockForUpdate( x, y, z );
                    }
                }
                break;
            }
            case QCraftPacket.SetNewServerAddress:
            {
                // Entered new server details on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    String name = packet.dataString[ 0 ];
                    String address = packet.dataString[ 1 ];
                    if( QCraft.canPlayerEditPortalServers( entityPlayer ) )
                    {
                        if( name != null && address != null )
                        {
                            PortalRegistry.PortalRegistry.registerServer( name, address );
                            EntanglementSavedData.get(world).markDirty(); //Notify that this needs to be saved on world save
                        }
                    }
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( QCraft.canPlayerCreatePortals( entityPlayer ) && !computer.isTeleporterEnergized() )
                    {
                        computer.setRemoteServerAddress( address );
                        world.markBlockForUpdate( x, y, z );
                    }
                }
                break;
            }
            case QCraftPacket.RemoveServerAddress:
            {
                // Removed server details on a computer
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    String address = packet.dataString[ 0 ];
                    if( QCraft.canPlayerEditPortalServers( entityPlayer ) )
                    {
                        if( address != null )
                        {
                            PortalRegistry.PortalRegistry.unregisterServer( address );
                        }
                    }
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( QCraft.canPlayerCreatePortals( entityPlayer ) && !computer.isTeleporterEnergized() )
                    {
                        computer.setRemoteServerAddress( null );
                        world.markBlockForUpdate( x, y, z );
                    }
                }
                break;
            }
            case QCraftPacket.ConfirmGoToServer:
            {
                // Confirmed they'd like to travel through a cross-server portal
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    boolean takeItems = (packet.dataInt[ 3 ] > 0);
                    String expectedServer = packet.dataString[ 0 ];
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    String actualServer = computer.getRemoteServerAddress();
                    if( computer.isTeleporterEnergized() && expectedServer != null && actualServer != null && expectedServer.equals( actualServer ) )
                    {
                        computer.teleportPlayerRemote( entityPlayer, takeItems );
                    }
                }
                break;
            }
            case QCraftPacket.UnpackLuggage:
            {
                // Connected from another server, took luggage with them
                byte[] signedLuggageData = packet.dataByte[0];
                boolean isDestination = (packet.dataInt[0] > 0);
                boolean teleported = false;
                unpackLuggage( entityPlayer, entityPlayer, signedLuggageData, isDestination, teleported, false );
                break;
            }
        }
    }

    public static void handleClientPacket( QCraftPacket packet )
    {
        // Messages sent from server to client
        //System.out.println( "CLIENT GOT PACKET " + packet.packetType );
        EntityPlayer entityPlayer = proxy.getLocalPlayer();
        World world = entityPlayer.getEntityWorld();
        switch( packet.packetType )
        {
            case QCraftPacket.QueryGoToServer:
            {
                // Walked into a cross-server portal
                int x = packet.dataInt[ 0 ];
                int y = packet.dataInt[ 1 ];
                int z = packet.dataInt[ 2 ];
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQuantumComputer )
                {
                    TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                    if( computer.getRemoteServerAddress() != null )
                    {
                        // Prompt the user whether they want to teleport or not (and if so, with items or not)
                        proxy.showItemTransferGUI( entityPlayer, computer );
                    }
                }
                break;
            }
            case QCraftPacket.GoToServer:
            {
                // Walked into a cross-server portal, confirmed, and now being teleported
                // Read packet
                String destination = packet.dataString[ 0 ];
                byte[] luggage = packet.dataByte[ 0 ];
                LostLuggage.Address destinationAddress = resolveServerAddress( destination );

                // Store luggage
                LostLuggage.Instance.load();
                LostLuggage.Instance.storeLuggage(
                    getCurrentServerAddress(),
                    destinationAddress,
                    luggage
                );
                LostLuggage.Instance.save();

                // Travel
                travelNextTick = destinationAddress;
                break;
            }
            case QCraftPacket.RequestLuggage:
            {
                // Finished spawning, server is ready to receive luggage
                LostLuggage.Address address = getCurrentServerAddress();
                if( address != null )
                {
                    LostLuggage.Instance.load();
                    LostLuggage.Instance.removeOldLuggage();
                    Collection<LostLuggage.LuggageMatch> luggages = LostLuggage.Instance.getMatchingLuggage( address );
                    if( luggages.size() > 0 )
                    {
                        // Upload luggage to server
                        for( LostLuggage.LuggageMatch match : luggages )
                        {
                            QCraft.requestUnpackLuggage( match );
                        }
                    }
                    LostLuggage.Instance.save();
                }
                break;
            }
            case QCraftPacket.DiscardLuggage:
            {
                // Luggage was sent to server, who consumed it, and want you to get rid of it
                LostLuggage.Instance.load();
                byte[] luggage = packet.dataByte[ 0 ];
                LostLuggage.Instance.removeLuggage( luggage );
                LostLuggage.Instance.save();
                break;
            }
        }
    }

    private static LuggageVerificationResult verifyIncomingLuggage( EntityPlayer instigator, EntityPlayer entityPlayer, byte[] signedLuggageData, boolean forceVerify ) throws IOException
    {
        NBTTagCompound signedLuggage = CompressedStreamTools.func_152457_a( signedLuggageData , NBTSizeTracker.field_152451_a);
        byte[] luggageData = signedLuggage.getByteArray("luggage");
        NBTTagCompound luggage = CompressedStreamTools.func_152457_a(luggageData, NBTSizeTracker.field_152451_a);

        if( signedLuggage.hasKey( "key" ) )
        {
            boolean luggageFromLocalServer = false;

            // Extract the key
            PublicKey key = EncryptionRegistry.Instance.decodePublicKey( signedLuggage.getByteArray( "key" ) );
            if( EncryptionRegistry.Instance.getLocalKeyPair().getPublic().equals(key) )
            {
                QCraft.log( "Player " + entityPlayer.getDisplayName() + " has luggage from this server." );
                luggageFromLocalServer = true;
            }
            else if( !EncryptionRegistry.Instance.getVerifiedPublicKeys().contains(key) )
            {
                // Key is unknown, link needs to be verified
                // Verify link:
                if( forceVerify )
                {
                    QCraft.log( "Player " + entityPlayer.getDisplayName() + " has luggage from unverified server. Verifying." );
                    EncryptionRegistry.Instance.getVerifiedPublicKeys().add( key );
                    instigator.addChatMessage( new ChatComponentText(
                        "Portal Link verified."
                    ) );
                    if( instigator != entityPlayer )
                    {
                        instigator.addChatMessage( new ChatComponentText(
                            "Portal Link verified"
                        ) );
                    }
                    EncryptionSavedData.get(getDefWorld()).markDirty(); //Notify that this needs to be saved on world save
                }
                else
                {
                    QCraft.log( "Player " + entityPlayer.getDisplayName() + " has luggage from unverified server. Ignoring." );
                    entityPlayer.addChatMessage( new ChatComponentText(
                        "Portal Link failed:"
                    ) );
                    if( QCraft.canAnybodyVerifyPortalServers() )
                    {
                        if( QCraft.canEverybodyVerifyPortalServers() )
                        {
                            entityPlayer.addChatMessage( new ChatComponentText(
                                "The server link must be verified first."
                            ) );
                        }
                        else
                        {
                            entityPlayer.addChatMessage( new ChatComponentText(
                                "The server link must be verified by an admin first."
                            ) );
                        }
                        QCraft.addUnverifiedLuggage( entityPlayer, signedLuggageData );
                    }
                    else
                    {
                        entityPlayer.addChatMessage( new ChatComponentText(
                            "This server does not allow incoming inter-server portals."
                        ) );
                    }

                    boolean canVerify = QCraft.canPlayerVerifyPortalServers( entityPlayer );
                    boolean hasItems = luggage.hasKey( "items" );
                    if( canVerify && hasItems )
                    {
                        entityPlayer.addChatMessage( new ChatComponentText(
                            "Type \"/qcraft verify\" to do this now, or " +
                            "return to the original server within " + LostLuggage.MAX_LUGGAGE_AGE_HOURS + " hours to get your items back."
                        ) );
                    }
                    else if( canVerify )
                    {
                        entityPlayer.addChatMessage( new ChatComponentText(
                            "Type \"/qcraft verify\" to do this now."
                        ) );
                    }
                    else if( hasItems )
                    {
                        entityPlayer.addChatMessage( new ChatComponentText(
                            "Return to the original server within " + LostLuggage.MAX_LUGGAGE_AGE_HOURS + " hours to get your items back."
                        ) );
                    }
                    return LuggageVerificationResult.UNTRUSTED;
                }
            }
            else
            {
                QCraft.log( "Player " + entityPlayer.getDisplayName() + " has luggage from verified server." );
            }

            // Verify against key
            byte[] luggageSignature = signedLuggage.getByteArray("signature");
            if( !EncryptionRegistry.Instance.verifyData( key, luggageSignature, luggageData ) )
            {
                QCraft.log( "Player " + entityPlayer.getDisplayName() + "'s luggage failed signature check. Ignoring." );
                entityPlayer.addChatMessage( new ChatComponentText( "Portal Link failed:" ) );
                entityPlayer.addChatMessage( new ChatComponentText( "Signature violation." ) );
                QCraft.requestDiscardLuggage( entityPlayer, signedLuggageData );
                return LuggageVerificationResult.UNTRUSTED;
            }

            // Check UUID is not used before
            UUID uuid = UUID.fromString( luggage.getString( "uuid" ) );
            if( EncryptionRegistry.Instance.getReceivedLuggageIDs().contains( uuid ) )
            {
                QCraft.log( "Player " + entityPlayer.getDisplayName() + "'s luggage is a duplicate. Ignoring." );
                entityPlayer.addChatMessage( new ChatComponentText( "Portal Link failed:" ) );
                entityPlayer.addChatMessage( new ChatComponentText( "Luggage duplicate." ) );
                QCraft.requestDiscardLuggage( entityPlayer, signedLuggageData );
                return LuggageVerificationResult.UNTRUSTED;
            }
            EncryptionRegistry.Instance.getReceivedLuggageIDs().add( uuid );
            EncryptionSavedData.get(getDefWorld()).markDirty(); //Notify that this needs to be saved on world save

            // We're ok
            QCraft.requestDiscardLuggage( entityPlayer, signedLuggageData );
            if( luggageFromLocalServer )
            {
                return LuggageVerificationResult.LOCALKEY;
            }
            else
            {
                return LuggageVerificationResult.TRUSTED;
            }
        }
        else
        {
            entityPlayer.addChatMessage( new ChatComponentText( "Portal Link failed:" ) );
            entityPlayer.addChatMessage( new ChatComponentText( "Signature missing." ) );
            QCraft.requestDiscardLuggage( entityPlayer, signedLuggageData );
            return LuggageVerificationResult.UNTRUSTED;
        }
    }

    private static boolean unpackLuggage( EntityPlayer instigator, EntityPlayer entityPlayer, byte[] signedLuggageData, boolean isDestination, boolean alreadyTeleported, boolean forceVerify )
    {
        try
        {
            // Verify the luggage
            LuggageVerificationResult verificationResult = verifyIncomingLuggage( instigator, entityPlayer, signedLuggageData, forceVerify );
            if( verificationResult != LuggageVerificationResult.UNTRUSTED )
            {
                // Decompress the luggage
                NBTTagCompound signedLuggage = CompressedStreamTools.func_152457_a(signedLuggageData, NBTSizeTracker.field_152451_a);
                byte[] luggageData = signedLuggage.getByteArray( "luggage" );
                NBTTagCompound luggage = CompressedStreamTools.func_152457_a(luggageData, NBTSizeTracker.field_152451_a);

                // Unpack items
                if( luggage.hasKey( "items" ) )
                {
                    // Notify
                    if( verificationResult == LuggageVerificationResult.LOCALKEY && !isDestination )
                    {
                        entityPlayer.addChatMessage( new ChatComponentText(
                            "Previous attempted Portal Link failed, restoring inventory."
                        ) );
                    }

                    // Place every item from the luggage into the inventory
                    NBTTagList items = luggage.getTagList( "items", 10 );
                    QCraft.log( "Adding " + items.tagCount() + " items to " + entityPlayer.getDisplayName() + "'s inventory" );
                    for( int i=0; i<items.tagCount(); ++i )
                    {
                        NBTTagCompound itemNBT = items.getCompoundTagAt( i );
                        ItemStack stack = ItemStack.loadItemStackFromNBT( itemNBT );
                        
                        String oldName = itemNBT.getString("Name");
                        String newName = "";
                        if (stack != null) {
                            GameRegistry.UniqueIdentifier uniqueId = GameRegistry.findUniqueIdentifierFor(stack.getItem());
                            newName = uniqueId.modId + ":" + uniqueId.name;
                        }                        
                        if (! oldName.equals(newName)) {
                            GameRegistry.UniqueIdentifier oldUniqueId = new GameRegistry.UniqueIdentifier(oldName);
                            int newID = Item.getIdFromItem(GameRegistry.findItem(oldUniqueId.modId, oldUniqueId.name));
                            if (newID < 1) { //0 and -1 indicate an error, and lower IDs are even worse I guess :P                               
                                stack = new ItemStack(QCraft.Items.missingItem);
                                stack.stackTagCompound = itemNBT;//Wrap the item in the dummy item
                            } else {
                                itemNBT.setShort("id", (short) newID);
                                stack = ItemStack.loadItemStackFromNBT( itemNBT );
                            }
                        }
                        
                        if( !entityPlayer.inventory.addItemStackToInventory( stack ) )
                        {
                            entityPlayer.entityDropItem( stack, 1.5f );
                        }
                    }
                    entityPlayer.inventory.markDirty();
                }

                // Teleport to destination portal
                if( !alreadyTeleported && luggage.hasKey( "destinationPortal" ) )
                {
                    if( verificationResult == LuggageVerificationResult.TRUSTED ||
                        (verificationResult == LuggageVerificationResult.LOCALKEY && isDestination) )
                    {
                        // Find destination portal
                        String destination = luggage.getString( "destinationPortal" );
                        TileEntityQuantumComputer.PortalLocation location = PortalRegistry.PortalRegistry.getPortal( destination );
                        if( location != null )
                        {
                            QCraft.log( "Teleporting " + entityPlayer.getDisplayName() + " to portal \"" + destination + "\"" );
                            TileEntityQuantumComputer.teleportPlayerLocal( entityPlayer, destination );
                        }
                        else
                        {
                            entityPlayer.addChatMessage( new ChatComponentText( "Portal Link failed:" ) );
                            entityPlayer.addChatMessage( new ChatComponentText( "There is no portal on this server called \"" + destination + "\"" ) );
                        }
                        alreadyTeleported = true;
                    }
                }
            }
            return alreadyTeleported;
        }
        catch( IOException e )
        {
            throw new RuntimeException( "Error unpacking luggage" );
        }
    }

    public static void spawnQuantumDustFX( World world, double x, double y, double z )
    {
        proxy.spawnQuantumDustFX( world, x, y, z );
    }

    public static LostLuggage.Address resolveServerAddress( String addressString )
    {
        // TODO: Proxy me
        ServerAddress address = ServerAddress.func_78860_a( addressString );
        if( address != null )
        {
            return new LostLuggage.Address( address.getIP() + ":" + address.getPort() );
        }
        return null;
    }

    private static LostLuggage.Address s_currentServer = null;

    public static void setCurrentServerAddress( String addressString )
    {
        if( addressString != null )
        {
            s_currentServer = resolveServerAddress( addressString );
        }
        else
        {
            s_currentServer = null;
        }
    }

    public static LostLuggage.Address getCurrentServerAddress()
    {
        return s_currentServer;
    }

    private static Map<String, Set<byte[]>> s_unverifiedLuggage = new HashMap<String, Set<byte[]>>();

    public static void addUnverifiedLuggage( EntityPlayer player, byte[] luggage )
    {
        String username = player.getCommandSenderName();
        if( !s_unverifiedLuggage.containsKey( username ) )
        {
            s_unverifiedLuggage.put( username, new HashSet<byte[]>() );
        }

        Set<byte[]> luggageSet = s_unverifiedLuggage.get( username );
        if( !luggageSet.contains( luggage ) )
        {
            luggageSet.add( luggage );
        }
    }

    public static void clearUnverifiedLuggage( EntityPlayer player )
    {
        String username = player.getCommandSenderName();
        if( s_unverifiedLuggage.containsKey( username ) )
        {
            s_unverifiedLuggage.remove( username );
        }
    }

    public static void verifyUnverifiedLuggage( EntityPlayer instigator, EntityPlayer player )
    {
        String username = player.getCommandSenderName();
        if( s_unverifiedLuggage.containsKey( username ) )
        {
            Set<byte[]> luggageSet = s_unverifiedLuggage.remove( username );

            boolean teleported = false;
            Iterator<byte[]> it = luggageSet.iterator();
            while( it.hasNext() )
            {
                byte[] signedLuggageData = it.next();
                if( unpackLuggage( instigator, player, signedLuggageData, true, teleported, true ) )
                {
                    teleported = true;
                }
            }
        }
    }

    public static void log( String text )
    {
        FMLLog.info("[qCraft] " + text, 0); //Use FML logger instead of Vanilla MC log
    }
}
