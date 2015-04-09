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

import com.google.common.base.CaseFormat;
import dan200.QCraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;
import java.util.*;

public class TileEntityQuantumComputer extends TileEntity
{
    public static final EntanglementRegistry<TileEntityQuantumComputer> ComputerRegistry = new EntanglementRegistry<TileEntityQuantumComputer>();
    public static final EntanglementRegistry<TileEntityQuantumComputer> ClientComputerRegistry = new EntanglementRegistry<TileEntityQuantumComputer>();
    public static final int RANGE = 8;

    public static EntanglementRegistry<TileEntityQuantumComputer> getEntanglementRegistry( World world )
    {
        if( !world.isRemote )
        {
            return ComputerRegistry;
        }
        else
        {
            return ClientComputerRegistry;
        }
    }

    public static class AreaShape
    {
        public int m_xMin;
        public int m_xMax;
        public int m_yMin;
        public int m_yMax;
        public int m_zMin;
        public int m_zMax;

        public boolean equals( AreaShape o )
        {
            return
                o.m_xMin == m_xMin &&
                o.m_xMax == m_xMax &&
                o.m_yMin == m_yMin &&
                o.m_yMax == m_yMax &&
                o.m_zMin == m_zMin &&
                o.m_zMax == m_zMax;
        }
    }

    public static class AreaData
    {
        public AreaShape m_shape;
        public Block[] m_blocks;
        public int[] m_metaData;

        public NBTTagCompound encode()
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger( "xmin", m_shape.m_xMin );
            nbttagcompound.setInteger( "xmax", m_shape.m_xMax );
            nbttagcompound.setInteger( "ymin", m_shape.m_yMin );
            nbttagcompound.setInteger( "ymax", m_shape.m_yMax );
            nbttagcompound.setInteger( "zmin", m_shape.m_zMin );
            nbttagcompound.setInteger( "zmax", m_shape.m_zMax );

            NBTTagList blockNames = new NBTTagList();
            for( int i=0; i<m_blocks.length; ++i )
            {
                String name = null;
                Block block = m_blocks[i];
                if( block != null )
                {
                    name = Block.blockRegistry.getNameForObject( block );
                }
                if( name != null && name.length() > 0 )
                {
                    blockNames.appendTag( new NBTTagString( name ) );
                }
                else
                {
                    blockNames.appendTag( new NBTTagString( "null" ) );
                }
            }
            nbttagcompound.setTag( "blockNames", blockNames );

            nbttagcompound.setIntArray( "metaData", m_metaData );
            return nbttagcompound;
        }

        public static AreaData decode( NBTTagCompound nbttagcompound )
        {
            AreaData storedData = new AreaData();
            storedData.m_shape = new AreaShape();
            storedData.m_shape.m_xMin = nbttagcompound.getInteger( "xmin" );
            storedData.m_shape.m_xMax = nbttagcompound.getInteger( "xmax" );
            storedData.m_shape.m_yMin = nbttagcompound.getInteger( "ymin" );
            storedData.m_shape.m_yMax = nbttagcompound.getInteger( "ymax" );
            storedData.m_shape.m_zMin = nbttagcompound.getInteger( "zmin" );
            storedData.m_shape.m_zMax = nbttagcompound.getInteger( "zmax" );

            int size =
                ( storedData.m_shape.m_xMax - storedData.m_shape.m_xMin + 1 ) *
                ( storedData.m_shape.m_yMax - storedData.m_shape.m_yMin + 1 ) *
                ( storedData.m_shape.m_zMax - storedData.m_shape.m_zMin + 1 );
            storedData.m_blocks = new Block[ size ];
            if( nbttagcompound.hasKey( "blockData" ) )
            {
                int[] blockIDs = nbttagcompound.getIntArray( "blockData" );
                for( int i=0; i<size; ++i )
                {
                    storedData.m_blocks[i] = Block.getBlockById( blockIDs[i] );
                }
            }
            else
            {
                NBTTagList blockNames = nbttagcompound.getTagList( "blockNames", Constants.NBT.TAG_STRING );
                for( int i=0; i<size; ++i )
                {
                    String name = blockNames.getStringTagAt( i );
                    if( name.length() > 0 && !name.equals( "null" ) )
                    {
                        storedData.m_blocks[i] = Block.getBlockFromName( name );
                    }
                }
            }
            storedData.m_metaData = nbttagcompound.getIntArray( "metaData" );
            return storedData;
        }
    }

    // Shared state
    private boolean m_powered;
    private int m_entanglementFrequency;
    private int m_timeSinceEnergize;

    // Area Teleportation state
    private AreaData m_storedData;

    // Server Teleportation state
    private String m_portalID;
    private boolean m_portalNameConflict;
    private String m_remoteServerAddress;
    private String m_remoteServerName;
    private String m_remotePortalID;

    public TileEntityQuantumComputer()
    {
        m_powered = false;
        m_entanglementFrequency = -1;
        m_timeSinceEnergize = 0;

        m_storedData = null;

        m_portalID = null;
        m_portalNameConflict = false;
        m_remoteServerAddress = null;
        m_remoteServerName = null;
        m_remotePortalID = null;
    }

    private EntanglementRegistry<TileEntityQuantumComputer> getEntanglementRegistry()
    {
        return getEntanglementRegistry( worldObj );
    }

    private PortalRegistry getPortalRegistry()
    {
        return PortalRegistry.getPortalRegistry( worldObj );
    }

    @Override
    public void validate()
    {
        super.validate();
        register();
    }

    @Override
    public void invalidate()
    {
        unregister();
        super.invalidate();
    }

    public void onDestroy()
    {
        PortalLocation location = getPortal();
        if( location != null && isPortalDeployed( location ) )
        {
            undeployPortal( location );
        }
        unregisterPortal();
    }

    // Entanglement

    private void register()
    {
        if( m_entanglementFrequency >= 0 )
        {
            getEntanglementRegistry().register( m_entanglementFrequency, this );
        }
    }

    private void unregister()
    {
        if( m_entanglementFrequency >= 0 )
        {
            getEntanglementRegistry().unregister( m_entanglementFrequency, this );
        }
    }

    public void setEntanglementFrequency( int frequency )
    {
        if( m_entanglementFrequency != frequency )
        {
            unregister();
            m_entanglementFrequency = frequency;
            register();
        }
    }

    public int getEntanglementFrequency()
    {
        return m_entanglementFrequency;
    }

    private TileEntityQuantumComputer findEntangledTwin()
    {
        if( m_entanglementFrequency >= 0 )
        {
            List<TileEntityQuantumComputer> twins = ComputerRegistry.getEntangledObjects( m_entanglementFrequency );
            if( twins != null )
            {
                Iterator<TileEntityQuantumComputer> it = twins.iterator();
                while( it.hasNext() )
                {
                    TileEntityQuantumComputer computer = it.next();
                    if( computer != this )
                    {
                        return computer;
                    }
                }
            }
        }
        return null;
    }

    // Area Teleportation

    public void setStoredData( AreaData data )
    {
        m_storedData = data;
    }

    public AreaData getStoredData()
    {
        return m_storedData;
    }

    private boolean isPillarBase( int x, int y, int z, int side )
    {
        if( y < 0 || y >= 256 )
        {
            return false;
        }

        TileEntity entity = worldObj.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock quantum = (TileEntityQBlock) entity;
            int[] types = quantum.getTypes();
            for( int i = 0; i < 6; ++i )
            {
                if( i == side )
                {
                    if( types[ i ] != 31 ) // GOLD
                    {
                        return false;
                    }
                }
                else
                {
                    if( types[ i ] != 21 ) // OBSIDIAN
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean isPillar( int x, int y, int z )
    {
        if( y < 0 || y >= 256 )
        {
            return false;
        }

        Block block = worldObj.getBlock( x, y, z );
        if( block == Blocks.obsidian )
        {
            return true;
        }
        return false;
    }

    private boolean isGlass( int x, int y, int z )
    {
        if( y < 0 || y >= 256 )
        {
            return false;
        }

        Block block = worldObj.getBlock( x, y, z );
        if( block.getMaterial() == Material.glass && !(block instanceof BlockPane) )
        {
            return true;
        }
        return false;
    }

    private AreaShape calculateShape()
    {
        AreaShape shape = new AreaShape();
        shape.m_xMin = -99;
        shape.m_xMax = -99;
        shape.m_yMin = 0;
        shape.m_yMax = 0;
        shape.m_zMin = -99;
        shape.m_zMax = -99;
        for( int i = 0; i < RANGE; ++i )
        {
            if( shape.m_xMin == -99 && isPillarBase( xCoord - i - 1, yCoord, zCoord, 5 ) )
            {
                shape.m_xMin = -i;
            }
            if( shape.m_xMax == -99 && isPillarBase( xCoord + i + 1, yCoord, zCoord, 4 ) )
            {
                shape.m_xMax = i;
            }
            if( shape.m_zMin == -99 && isPillarBase( xCoord, yCoord, zCoord - i - 1, 3 ) )
            {
                shape.m_zMin = -i;
            }
            if( shape.m_zMax == -99 && isPillarBase( xCoord, yCoord, zCoord + i + 1, 2 ) )
            {
                shape.m_zMax = i;
            }
        }

        if( shape.m_xMin != -99 &&
                shape.m_xMax != -99 &&
                shape.m_zMin != -99 &&
                shape.m_zMax != -99 )
        {
            // Find Y Min
            for( int i = 1; i < RANGE; ++i )
            {
                if( isPillar( xCoord + shape.m_xMin - 1, yCoord - i, zCoord ) &&
                        isPillar( xCoord + shape.m_xMax + 1, yCoord - i, zCoord ) &&
                        isPillar( xCoord, yCoord - i, zCoord + shape.m_zMin - 1 ) &&
                        isPillar( xCoord, yCoord - i, zCoord + shape.m_zMax + 1 ) )
                {
                    shape.m_yMin = -i;
                }
                else
                {
                    break;
                }
            }

            // Find Y Max
            for( int i = 1; i < RANGE; ++i )
            {
                if( isPillar( xCoord + shape.m_xMin - 1, yCoord + i, zCoord ) &&
                        isPillar( xCoord + shape.m_xMax + 1, yCoord + i, zCoord ) &&
                        isPillar( xCoord, yCoord + i, zCoord + shape.m_zMin - 1 ) &&
                        isPillar( xCoord, yCoord + i, zCoord + shape.m_zMax + 1 ) )
                {
                    shape.m_yMax = i;
                }
                else
                {
                    break;
                }
            }

            // Check glass caps
            int top = yCoord + shape.m_yMax + 1;
            if( isGlass( xCoord + shape.m_xMin - 1, top, zCoord ) &&
                    isGlass( xCoord + shape.m_xMax + 1, top, zCoord ) &&
                    isGlass( xCoord, top, zCoord + shape.m_zMin - 1 ) &&
                    isGlass( xCoord, top, zCoord + shape.m_zMax + 1 ) )
            {
                return shape;
            }
        }
        return null;
    }

    private AreaData storeArea()
    {
        AreaShape shape = calculateShape();
        if( shape == null )
        {
            return null;
        }

        AreaData storedData = new AreaData();
        int minX = shape.m_xMin;
        int maxX = shape.m_xMax;
        int minY = shape.m_yMin;
        int maxY = shape.m_yMax;
        int minZ = shape.m_zMin;
        int maxZ = shape.m_zMax;

        int size = ( maxX - minX + 1 ) * ( maxY - minY + 1 ) * ( maxZ - minZ + 1 );
        int index = 0;

        storedData.m_shape = shape;
        storedData.m_blocks = new Block[ size ];
        storedData.m_metaData = new int[ size ];
        for( int y = minY; y <= maxY; ++y )
        {
            for( int x = minX; x <= maxX; ++x )
            {
                for( int z = minZ; z <= maxZ; ++z )
                {
                    int worldX = xCoord + x;
                    int worldY = yCoord + y;
                    int worldZ = zCoord + z;
                    if( !( worldX == xCoord && worldY == yCoord && worldZ == zCoord ) )
                    {
                        TileEntity tileentity = worldObj.getTileEntity( worldX, worldY, worldZ );
                        if( tileentity != null )
                        {
                            return null;
                        }

                        Block block = worldObj.getBlock( worldX, worldY, worldZ );
                        int meta = worldObj.getBlockMetadata( worldX, worldY, worldZ );
                        storedData.m_blocks[ index ] = block;
                        storedData.m_metaData[ index ] = meta;
                    }
                    index++;
                }
            }
        }

        return storedData;
    }

    private void notifyBlockOfNeighborChange( int x, int y, int z )
    {
        worldObj.notifyBlockOfNeighborChange( x, y, z, worldObj.getBlock( x, y, z ) );
    }

    private void notifyEdgeBlocks( AreaShape shape )
    {
        // Notify the newly transported blocks on the edges of the area that their neighbours have changed
        int minX = shape.m_xMin;
        int maxX = shape.m_xMax;
        int minY = shape.m_yMin;
        int maxY = shape.m_yMax;
        int minZ = shape.m_zMin;
        int maxZ = shape.m_zMax;
        for( int x = minX; x <= maxX; ++x )
        {
            for( int y = minY; y <= maxY; ++y )
            {
                notifyBlockOfNeighborChange( xCoord + x, yCoord + y, zCoord + minZ );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + y, zCoord + maxZ );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + y, zCoord + minZ );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + y, zCoord + maxZ + 1 );
            }
        }
        for( int x = minX; x <= maxX; ++x )
        {
            for( int z = minZ; z <= maxZ; ++z )
            {
                notifyBlockOfNeighborChange( xCoord + x, yCoord + minY, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + maxY, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + minY - 1, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + x, yCoord + maxY + 1, zCoord + z );
            }
        }
        for( int y = minY; y <= maxY; ++y )
        {
            for( int z = minZ; z <= maxZ; ++z )
            {
                notifyBlockOfNeighborChange( xCoord + minX, yCoord + y, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + maxX, yCoord + y, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + minX - 1, yCoord + y, zCoord + z );
                notifyBlockOfNeighborChange( xCoord + maxX + 1, yCoord + y, zCoord + z );
            }
        }
    }

    private Set<EntityItem> getEntityItemsInArea( AreaShape shape )
    {
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                (double) ( xCoord + shape.m_xMin ),
                (double) ( yCoord + shape.m_yMin ),
                (double) ( zCoord + shape.m_zMin ),
                (double) ( xCoord + shape.m_xMax + 1 ),
                (double) ( yCoord + shape.m_yMax + 1 ),
                (double) ( zCoord + shape.m_zMax + 1 )
        );

        List list = worldObj.getEntitiesWithinAABBExcludingEntity( null, aabb );
        Set<EntityItem> set = new HashSet<EntityItem>();
        for( int i = 0; i < list.size(); ++i )
        {
            Entity entity = (Entity) list.get( i );
            if( entity instanceof EntityItem && !entity.isDead )
            {
                set.add( (EntityItem) entity );
            }
        }
        return set;
    }

    private void killNewItems( Set<EntityItem> before, Set<EntityItem> after )
    {
        Iterator<EntityItem> it = after.iterator();
        while( it.hasNext() )
        {
            EntityItem item = it.next();
            if( !item.isDead && !before.contains( item ) )
            {
                item.setDead();
            }
        }
    }

    private void clearArea( AreaShape shape )
    {
        // Cache the loose entities
        Set<EntityItem> before = getEntityItemsInArea( shape );

        // Set the area around us to air, notifying the adjacent blocks
        int minX = shape.m_xMin;
        int maxX = shape.m_xMax;
        int minY = shape.m_yMin;
        int maxY = shape.m_yMax;
        int minZ = shape.m_zMin;
        int maxZ = shape.m_zMax;
        for( int y = minY; y <= maxY; ++y )
        {
            for( int x = minX; x <= maxX; ++x )
            {
                for( int z = minZ; z <= maxZ; ++z )
                {
                    int worldX = xCoord + x;
                    int worldY = yCoord + y;
                    int worldZ = zCoord + z;
                    if( !( worldX == xCoord && worldY == yCoord && worldZ == zCoord ) )
                    {
                        worldObj.setBlockToAir( worldX, worldY, worldZ );
                    }
                }
            }
        }

        // Kill the new entities
        Set<EntityItem> after = getEntityItemsInArea( shape );
        killNewItems( before, after );

        // Notify edge blocks
        notifyEdgeBlocks( shape );
    }

    private void unpackArea( AreaData storedData )
    {
        // Cache the loose entities
        Set<EntityItem> before = getEntityItemsInArea( storedData.m_shape );

        // Set the area around us to the stored data
        int index = 0;
        int minX = storedData.m_shape.m_xMin;
        int maxX = storedData.m_shape.m_xMax;
        int minY = storedData.m_shape.m_yMin;
        int maxY = storedData.m_shape.m_yMax;
        int minZ = storedData.m_shape.m_zMin;
        int maxZ = storedData.m_shape.m_zMax;
        for( int y = minY; y <= maxY; ++y )
        {
            for( int x = minX; x <= maxX; ++x )
            {
                for( int z = minZ; z <= maxZ; ++z )
                {
                    int worldX = xCoord + x;
                    int worldY = yCoord + y;
                    int worldZ = zCoord + z;
                    if( !( worldX == xCoord && worldY == yCoord && worldZ == zCoord ) )
                    {
                        Block block = storedData.m_blocks[ index ];
                        if( block != null )
                        {
                            int meta = storedData.m_metaData[ index ];
                            worldObj.setBlock( worldX, worldY, worldZ, block, meta, 2 );
                        }
                        else
                        {
                            worldObj.setBlockToAir( worldX, worldY, worldZ );
                        }
                    }
                    index++;
                }
            }
        }

        // Kill the new entities
        Set<EntityItem> after = getEntityItemsInArea( storedData.m_shape );
        killNewItems( before, after );

        // Notify edge blocks
        notifyEdgeBlocks( storedData.m_shape );
    }

    private boolean checkCooling()
    {
        for( int i = 0; i < 6; ++i )
        {
            int x = xCoord + Facing.offsetsXForSide[ i ];
            int y = yCoord + Facing.offsetsYForSide[ i ];
            int z = zCoord + Facing.offsetsZForSide[ i ];
            Block block = worldObj.getBlock( x, y, z );
            if( block != null && (block.getMaterial() == Material.ice || block.getMaterial() == Material.packedIce) )
            {
                return true;
            }
        }
        return false;
    }

    public static enum TeleportError
    {
        Ok,
        NoTwin,
        FrameIncomplete,
        DestinationFrameIncomplete,
        FrameMismatch,
        FrameObstructed,
        InsufficientCooling,
        AreaNotTransportable,
        DestinationNotTransportable,
        FrameDeployed,
        NameConflict;

        public static String decode( TeleportError error )
        {
            if( error != Ok )
            {
                return "gui.qcraft:computer.error_" + CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_UNDERSCORE, error.toString() );
            }
            return null;
        }
    }

    private TeleportError canTeleport()
    {
        // Check entangled:
        TileEntityQuantumComputer twin = null;
        if( m_entanglementFrequency >= 0 )
        {
            // Find entangled twin:
            twin = findEntangledTwin();

            // Check the twin exists:
            if( twin == null )
            {
                return TeleportError.NoTwin;
            }
        }

        // Check the shape is big enough:
        AreaShape localShape = calculateShape();
        if( localShape == null )
        {
            return TeleportError.FrameIncomplete;
        }

        if( twin != null )
        {
            // Check the twin shape matches
            AreaShape twinShape = twin.calculateShape();
            if( twinShape == null )
            {
                return TeleportError.DestinationFrameIncomplete;
            }
            if( !localShape.equals( twinShape ) )
            {
                return TeleportError.FrameMismatch;
            }
        }
        else
        {
            // Check the stored shape matches
            if( m_storedData != null )
            {
                if( !localShape.equals( m_storedData.m_shape ) )
                {
                    return TeleportError.FrameMismatch;
                }
            }
        }

        // Check cooling
        if( !checkCooling() )
        {
            return TeleportError.InsufficientCooling;
        }

        // Store the two areas:
        AreaData localData = storeArea();
        if( localData == null )
        {
            return TeleportError.AreaNotTransportable;
        }

        if( twin != null )
        {
            AreaData twinData = twin.storeArea();
            if( twinData == null )
            {
                return TeleportError.DestinationNotTransportable;
            }
        }

        return TeleportError.Ok;
    }

    private TeleportError tryTeleport()
    {
        TeleportError error = canTeleport();
        if( error == TeleportError.Ok )
        {
            if( m_entanglementFrequency >= 0 )
            {
                // Store the two areas:
                TileEntityQuantumComputer twin = findEntangledTwin();
                if( twin != null )
                {
                    AreaData localData = storeArea();
                    AreaData twinData = twin.storeArea();
                    if( localData != null && twinData != null )
                    {
                        // Unpack the two areas:
                        unpackArea( twinData );
                        twin.unpackArea( localData );
                    }
                }
            }
            else
            {
                // Store the local area:
                AreaData localData = storeArea();

                // Unpack the stored area:
                if( m_storedData != null )
                {
                    unpackArea( m_storedData );
                }
                else
                {
                    clearArea( localData.m_shape );
                }

                m_storedData = localData;
            }

            // Effects
            worldObj.playSoundEffect( xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "mob.endermen.portal", 1.0F, 1.0F );
        }
        return error;
    }

    // Server Teleportation

    private void registerPortal()
    {
        PortalLocation location = findPortal();
        if( location != null )
        {
            if( m_portalID == null )
            {
                m_portalID = getPortalRegistry().getUnusedID();
                worldObj.markBlockForUpdate( xCoord, yCoord, zCoord );
            }
            if( !getPortalRegistry().register( m_portalID, location ) )
            {
                m_portalNameConflict = true;
            }
            else
            {
                m_portalNameConflict = false;
            }
        }
    }

    private void unregisterPortal()
    {
        if( m_portalID != null )
        {
            if( !m_portalNameConflict )
            {
                getPortalRegistry().unregister( m_portalID );
            }
            m_portalNameConflict = false;
        }
    }

    public void setPortalID( String id )
    {
        unregisterPortal();
        m_portalID = id;
        registerPortal();
    }

    public String getPortalID()
    {
        return m_portalID;
    }

    public void setRemoteServerAddress( String address )
    {
        m_remoteServerAddress = address;
        m_remoteServerName = getPortalRegistry().getServerName( address );
    }

    public String getRemoteServerAddress()
    {
        return m_remoteServerAddress;
    }

    public String getRemoteServerName()
    {
        return m_remoteServerName;
    }

    public void cycleRemoteServerAddress( String previousAddress )
    {
        m_remoteServerAddress = getPortalRegistry().getServerAddressAfter( previousAddress );
        m_remoteServerName = getPortalRegistry().getServerName( m_remoteServerAddress );
    }

    public void setRemotePortalID( String id )
    {
        m_remotePortalID = id;
    }

    public String getRemotePortalID()
    {
        return m_remotePortalID;
    }

    public boolean isTeleporter()
    {
        if( m_entanglementFrequency >= 0 )
        {
            return false;
        }
        if( !QCraft.canAnybodyCreatePortals() )
        {
            return false;
        }
        PortalLocation location = getPortal();
        if( location != null )
        {
            return true;
        }
        return false;
    }

    public boolean isTeleporterEnergized()
    {
        return canDeactivatePortal() == TeleportError.Ok;
    }

    private boolean isPortalCorner( int x, int y, int z, int dir )
    {
        if( y < 0 || y >= 256 )
        {
            return false;
        }

        TileEntity entity = worldObj.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock quantum = (TileEntityQBlock) entity;
            int[] types = quantum.getTypes();
            for( int i = 0; i < 6; ++i )
            {
                if( i == dir || i == Facing.oppositeSide[ dir ] )
                {
                    if( types[ i ] != 31 ) // GOLD
                    {
                        return false;
                    }
                }
                else
                {
                    if( types[ i ] != 21 ) // OBSIDIAN
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static class PortalLocation
    {
        public int m_dimensionID;
        public int m_xOrigin;
        public int m_yOrigin;
        public int m_zOrigin;
        public int m_xLength;
        public int m_yLength;
        public int m_zLength;

        public NBTTagCompound encode()
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger( "dimensionID", m_dimensionID );
            nbttagcompound.setInteger( "xOrigin", m_xOrigin );
            nbttagcompound.setInteger( "yOrigin", m_yOrigin );
            nbttagcompound.setInteger( "zOrigin", m_zOrigin );
            nbttagcompound.setInteger( "xLength", m_xLength );
            nbttagcompound.setInteger( "yLength", m_yLength );
            nbttagcompound.setInteger( "zLength", m_zLength );
            return nbttagcompound;
        }

        public static PortalLocation decode( NBTTagCompound nbttagcompound )
        {
            PortalLocation location = new PortalLocation();
            if( nbttagcompound.hasKey( "dimensionID" ) )
            {
                location.m_dimensionID = nbttagcompound.getInteger( "dimensionID" );
            }
            else
            {
                location.m_dimensionID = 0;
            }
            location.m_xOrigin = nbttagcompound.getInteger( "xOrigin" );
            location.m_yOrigin = nbttagcompound.getInteger( "yOrigin" );
            location.m_zOrigin = nbttagcompound.getInteger( "zOrigin" );
            location.m_xLength = nbttagcompound.getInteger( "xLength" );
            location.m_yLength = nbttagcompound.getInteger( "yLength" );
            location.m_zLength = nbttagcompound.getInteger( "zLength" );
            return location;
        }
    }

    private boolean portalExistsAt( PortalLocation location )
    {
        if( location.m_xLength > 0 )
        {
            // Check walls
            for( int y = location.m_yOrigin; y < location.m_yOrigin + location.m_yLength; ++y )
            {
                if( !isGlass( location.m_xOrigin - 1, y, location.m_zOrigin ) )
                {
                    return false;
                }
                if( !isGlass( location.m_xOrigin + location.m_xLength, y, location.m_zOrigin ) )
                {
                    return false;
                }
            }
            // Check ceiling and floor
            for( int x = location.m_xOrigin; x < location.m_xOrigin + location.m_xLength; ++x )
            {
                if( !isGlass( x, location.m_yOrigin - 1, location.m_zOrigin ) )
                {
                    return false;
                }
                if( !isGlass( x, location.m_yOrigin + location.m_yLength, location.m_zOrigin ) )
                {
                    return false;
                }
            }
            // Check corners
            if( !isPortalCorner( location.m_xOrigin - 1, location.m_yOrigin - 1, location.m_zOrigin, 2 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin + location.m_xLength, location.m_yOrigin - 1, location.m_zOrigin, 2 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin - 1, location.m_yOrigin + location.m_yLength, location.m_zOrigin, 2 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin + location.m_xLength, location.m_yOrigin + location.m_yLength, location.m_zOrigin, 2 ) )
            {
                return false;
            }
        }
        if( location.m_zLength > 0 )
        {
            // Check walls
            for( int y = location.m_yOrigin; y < location.m_yOrigin + location.m_yLength; ++y )
            {
                if( !isGlass( location.m_xOrigin, y, location.m_zOrigin - 1 ) )
                {
                    return false;
                }
                if( !isGlass( location.m_xOrigin, y, location.m_zOrigin + location.m_zLength ) )
                {
                    return false;
                }
            }
            // Check ceiling and floor
            for( int z = location.m_zOrigin; z < location.m_zOrigin + location.m_zLength; ++z )
            {
                if( !isGlass( location.m_xOrigin, location.m_yOrigin - 1, z ) )
                {
                    return false;
                }
                if( !isGlass( location.m_xOrigin, location.m_yOrigin + location.m_yLength, z ) )
                {
                    return false;
                }
            }
            // Check corners
            if( !isPortalCorner( location.m_xOrigin, location.m_yOrigin - 1, location.m_zOrigin - 1, 4 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin, location.m_yOrigin - 1, location.m_zOrigin + location.m_zLength, 4 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin, location.m_yOrigin + location.m_yLength, location.m_zOrigin - 1, 4 ) )
            {
                return false;
            }
            if( !isPortalCorner( location.m_xOrigin, location.m_yOrigin + location.m_yLength, location.m_zOrigin + location.m_zLength, 4 ) )
            {
                return false;
            }
        }
        return true;
    }

    private PortalLocation getPortalAt( int x, int y, int z, int xlen, int ylen, int zlen )
    {
        PortalLocation result = new PortalLocation();
        result.m_dimensionID = worldObj.provider.dimensionId;
        result.m_xOrigin = x;
        result.m_yOrigin = y;
        result.m_zOrigin = z;
        result.m_xLength = xlen;
        result.m_yLength = ylen;
        result.m_zLength = zlen;
        if( portalExistsAt( result ) )
        {
            return result;
        }
        return null;
    }

    private PortalLocation getPortal()
    {
        if( m_portalID != null )
        {
            if( !m_portalNameConflict )
            {
                PortalLocation portal = getPortalRegistry().getPortal( m_portalID );
                if( portal != null )
                {
                    return portal;
                }
            }
            else
            {
                PortalLocation portal = findPortal();
                if( portal != null )
                {
                    return portal;
                }
            }
        }
        return null;
    }

    private PortalLocation findPortal()
    {
        for( int dir = 0; dir < 6; ++dir )
        {
            // See if this adjoining block is part of a portal:
            int x = xCoord + Facing.offsetsXForSide[ dir ];
            int y = yCoord + Facing.offsetsYForSide[ dir ];
            int z = zCoord + Facing.offsetsZForSide[ dir ];
            if( !isGlass( x, y, z ) && !isPortalCorner( x, y, z, 2 ) && !isPortalCorner( x, y, z, 4 ) )
            {
                continue;
            }

            // Try all possible portals it could be part of:
            PortalLocation result;
            result = getPortalAt( x, y + 1, z, 2, 3, 0 );
            if( result != null ) return result;
            result = getPortalAt( x - 1, y + 1, z, 2, 3, 0 );
            if( result != null ) return result;
            result = getPortalAt( x, y - 3, z, 2, 3, 0 );
            if( result != null ) return result;
            result = getPortalAt( x - 1, y - 3, z, 2, 3, 0 );
            if( result != null ) return result;

            result = getPortalAt( x, y + 1, z, 0, 3, 2 );
            if( result != null ) return result;
            result = getPortalAt( x, y + 1, z - 1, 0, 3, 2 );
            if( result != null ) return result;
            result = getPortalAt( x, y - 3, z, 0, 3, 2 );
            if( result != null ) return result;
            result = getPortalAt( x, y - 3, z - 1, 0, 3, 2 );
            if( result != null ) return result;

            for( int yy = y - 3; yy <= y + 1; ++yy )
            {
                result = getPortalAt( x + 1, yy, z, 2, 3, 0 );
                if( result != null ) return result;
                result = getPortalAt( x - 2, yy, z, 2, 3, 0 );
                if( result != null ) return result;
                result = getPortalAt( x, yy, z + 1, 0, 3, 2 );
                if( result != null ) return result;
                result = getPortalAt( x, yy, z - 2, 0, 3, 2 );
                if( result != null ) return result;
            }
        }
        return null;
    }

    private boolean isPortalClear( PortalLocation portal )
    {
        for( int y = portal.m_yOrigin; y < portal.m_yOrigin + portal.m_yLength; ++y )
        {
            for( int x = portal.m_xOrigin; x < portal.m_xOrigin + portal.m_xLength; ++x )
            {
                if( !worldObj.isAirBlock( x, y, portal.m_zOrigin ) )
                {
                    return false;
                }
            }
            for( int z = portal.m_zOrigin; z < portal.m_zOrigin + portal.m_zLength; ++z )
            {
                if( !worldObj.isAirBlock( portal.m_xOrigin, y, z ) )
                {
                    return false;
                }
            }
        }
        return true;
    }

    private void deployPortal( PortalLocation portal )
    {
        for( int y = portal.m_yOrigin; y < portal.m_yOrigin + portal.m_yLength; ++y )
        {
            for( int x = portal.m_xOrigin; x < portal.m_xOrigin + portal.m_xLength; ++x )
            {
                worldObj.setBlock( x, y, portal.m_zOrigin, QCraft.Blocks.quantumPortal, 0, 2 );
            }
            for( int z = portal.m_zOrigin; z < portal.m_zOrigin + portal.m_zLength; ++z )
            {
                worldObj.setBlock( portal.m_xOrigin, y, z, QCraft.Blocks.quantumPortal, 0, 2 );
            }
        }
    }

    private void undeployPortal( PortalLocation portal )
    {
        for( int y = portal.m_yOrigin; y < portal.m_yOrigin + portal.m_yLength; ++y )
        {
            for( int x = portal.m_xOrigin; x < portal.m_xOrigin + portal.m_xLength; ++x )
            {
                worldObj.setBlockToAir( x, y, portal.m_zOrigin );
            }
            for( int z = portal.m_zOrigin; z < portal.m_zOrigin + portal.m_zLength; ++z )
            {
                worldObj.setBlockToAir( portal.m_xOrigin, y, z );
            }
        }
    }

    private boolean isPortalDeployed( PortalLocation portal )
    {
        for( int y = portal.m_yOrigin; y < portal.m_yOrigin + portal.m_yLength; ++y )
        {
            for( int x = portal.m_xOrigin; x < portal.m_xOrigin + portal.m_xLength; ++x )
            {
                if( worldObj.getBlock( x, y, portal.m_zOrigin ) != QCraft.Blocks.quantumPortal )
                {
                    return false;
                }
            }
            for( int z = portal.m_zOrigin; z < portal.m_zOrigin + portal.m_zLength; ++z )
            {
                if( worldObj.getBlock( portal.m_xOrigin, y, z ) != QCraft.Blocks.quantumPortal )
                {
                    return false;
                }
            }
        }
        return true;
    }

    private TeleportError canActivatePortal()
    {
        if( m_entanglementFrequency >= 0 )
        {
            return TeleportError.FrameIncomplete;
        }
        if( !QCraft.canAnybodyCreatePortals() )
        {
            return TeleportError.FrameIncomplete;
        }

        PortalLocation location = getPortal();
        if( location == null )
        {
            return TeleportError.FrameIncomplete;
        }
        if( isPortalDeployed( location ) )
        {
            return TeleportError.FrameDeployed;
        }
        if( m_portalNameConflict )
        {
            return TeleportError.NameConflict;
        }
        if( !isPortalClear( location ) )
        {
            return TeleportError.FrameObstructed;
        }
        if( !checkCooling() )
        {
            return TeleportError.InsufficientCooling;
        }
        return TeleportError.Ok;
    }

    private TeleportError tryActivatePortal()
    {
        TeleportError error = canActivatePortal();
        if( error == TeleportError.Ok )
        {
            // Deploy
            PortalLocation location = getPortal();
            if( location != null )
            {
                deployPortal( location );
            }

            // Effects
            worldObj.playSoundEffect( xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "mob.endermen.portal", 1.0F, 1.0F );
        }
        return error;
    }

    public TeleportError canDeactivatePortal()
    {
        if( m_entanglementFrequency >= 0 )
        {
            return TeleportError.FrameIncomplete;
        }
        PortalLocation location = getPortal();
        if( location == null )
        {
            return TeleportError.FrameIncomplete;
        }
        if( !isPortalDeployed( location ) )
        {
            return TeleportError.FrameIncomplete;
        }
        return TeleportError.Ok;
    }

    private TeleportError tryDeactivatePortal()
    {
        TeleportError error = canDeactivatePortal();
        if( error == TeleportError.Ok )
        {
            // Deploy
            PortalLocation location = getPortal();
            if( location != null )
            {
                undeployPortal( location );
            }

            // Effects
            worldObj.playSoundEffect( xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "mob.endermen.portal", 1.0F, 1.0F );
        }
        return error;
    }


    // Common

    public void setRedstonePowered( boolean powered )
    {
        if( m_powered != powered )
        {
            m_powered = powered;
            if( !worldObj.isRemote )
            {
                if( m_powered && m_timeSinceEnergize >= 4 )
                {
                    tryEnergize();
                }
            }
        }
    }

    public TeleportError canEnergize()
    {
        TeleportError error = canTeleport();
        if( error == TeleportError.Ok )
        {
            return error;
        }

        TeleportError serverError = canActivatePortal();
        if( serverError == TeleportError.Ok )
        {
            return serverError;
        }

        TeleportError deactivateError = canDeactivatePortal();
        if( deactivateError == TeleportError.Ok )
        {
            return deactivateError;
        }

        if( error.ordinal() >= serverError.ordinal() )
        {
            return error;
        }
        else
        {
            return serverError;
        }
    }

    public TeleportError tryEnergize()
    {
        TeleportError error = tryTeleport();
        if( error == TeleportError.Ok )
        {
            m_timeSinceEnergize = 0;
            return error;
        }

        TeleportError serverError = tryActivatePortal();
        if( serverError == TeleportError.Ok )
        {
            m_timeSinceEnergize = 0;
            return serverError;
        }

        TeleportError deactivateError = tryDeactivatePortal();
        if( deactivateError == TeleportError.Ok )
        {
            return deactivateError;
        }

        if( error.ordinal() >= serverError.ordinal() )
        {
            return error;
        }
        else
        {
            return serverError;
        }
    }

    @Override
    public void updateEntity()
    {
        m_timeSinceEnergize++;

        if( !worldObj.isRemote )
        {
            // Try to register conflicted portal
            if( m_portalNameConflict )
            {
                registerPortal();
            }

            // Validate existing portal
            PortalLocation location = getPortal();
            if( location != null )
            {
                if( !portalExistsAt( location ) )
                {
                    if( isPortalDeployed( location ) )
                    {
                        undeployPortal( location );
                    }
                    unregisterPortal();
                    location = null;
                }
                else if( !checkCooling() )
                {
                    if( isPortalDeployed( location ) )
                    {
                        undeployPortal( location );
                    }
                }
            }
            else
            {
                unregisterPortal();
                location = null;
            }

            // Find new portal
            if( location == null )
            {
                registerPortal();
                location = getPortal();
            }

            // Try teleporting entities through portal
            if( location != null && isPortalDeployed( location ) )
            {
                // Search for players
                AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                    (double) ( location.m_xOrigin ),
                    (double) ( location.m_yOrigin ),
                    (double) ( location.m_zOrigin ),
                    (double) ( location.m_xOrigin + Math.max( location.m_xLength, 1 ) ),
                    (double) ( location.m_yOrigin + location.m_yLength ),
                    (double) ( location.m_zOrigin + Math.max( location.m_zLength, 1 ) )
                );

                List entities = worldObj.getEntitiesWithinAABB( EntityPlayer.class, aabb );
                if( entities != null && entities.size() > 0 )
                {
                    Iterator it = entities.iterator();
                    while( it.hasNext() )
                    {
                        Object next = it.next();
                        if( next != null && next instanceof EntityPlayer )
                        {
                            EntityPlayer player = (EntityPlayer)next;
                            if( player.timeUntilPortal <= 0 && player.ticksExisted >= 200 &&
                                player.ridingEntity == null && player.riddenByEntity == null )
                            {
                                // Teleport them:
                                teleportPlayer( player );
                            }
                        }
                    }
                }
            }
        }
    }

    private void teleportPlayer( EntityPlayer player )
    {
        if( m_remoteServerAddress != null )
        {
            queryTeleportPlayerRemote( player );
        }
        else
        {
            teleportPlayerLocal( player, m_remotePortalID );
        }
    }

    private void queryTeleportPlayerRemote( EntityPlayer player )
    {
        QCraft.requestQueryGoToServer( player, this );
        player.timeUntilPortal = 50;
    }

    public void teleportPlayerRemote( EntityPlayer player, boolean takeItems )
    {
        teleportPlayerRemote( player, m_remoteServerAddress, m_remotePortalID, takeItems );
    }

    public static void teleportPlayerRemote( EntityPlayer player, String remoteServerAddress, String remotePortalID, boolean takeItems )
    {
        // Log the trip
        QCraft.log( "Sending player " + player.getDisplayName() + " to server \"" + remoteServerAddress + "\"" );

        NBTTagCompound luggage = new NBTTagCompound();
        if( takeItems )
        {
            // Remove and encode the items from the players inventory we want them to take with them
            NBTTagList items = new NBTTagList();
            InventoryPlayer playerInventory = player.inventory;
            for( int i = 0; i < playerInventory.getSizeInventory(); ++i )
            {
                ItemStack stack = playerInventory.getStackInSlot( i );
                if( stack != null && stack.stackSize > 0 )
                {
                    // Ignore entangled items
                    if( stack.getItem() == Item.getItemFromBlock( QCraft.Blocks.quantumComputer ) && ItemQuantumComputer.getEntanglementFrequency( stack ) >= 0 )
                    {
                        continue;
                    }
                    if( stack.getItem() == Item.getItemFromBlock( QCraft.Blocks.qBlock ) && ItemQBlock.getEntanglementFrequency( stack ) >= 0 )
                    {
                        continue;
                    }

                    // Store items
                    NBTTagCompound itemTag = new NBTTagCompound();
                    stack.writeToNBT( itemTag );
                    items.appendTag( itemTag );

                    // Remove items
                    playerInventory.setInventorySlotContents( i, null );
                }
            }

            if( items.tagCount() > 0 )
            {
                QCraft.log( "Removed " + items.tagCount() + " items from " + player.getDisplayName() + "'s inventory." );
                playerInventory.markDirty();
                luggage.setTag( "items", items );
            }
        }

        // Set the destination portal ID
        if( remotePortalID != null )
        {
            luggage.setString( "destinationPortal", remotePortalID );
        }

        try
        {
            // Cryptographically sign the luggage
            luggage.setString( "uuid", UUID.randomUUID().toString() );
            byte[] luggageData = CompressedStreamTools.compress( luggage );
            byte[] luggageSignature = EncryptionRegistry.Instance.signData( luggageData );
            NBTTagCompound signedLuggage = new NBTTagCompound();
            signedLuggage.setByteArray( "key", EncryptionRegistry.Instance.encodePublicKey( EncryptionRegistry.Instance.getLocalKeyPair().getPublic() ) );
            signedLuggage.setByteArray( "luggage", luggageData );
            signedLuggage.setByteArray( "signature", luggageSignature );

            // Send the player to the remote server with the luggage
            byte[] signedLuggageData = CompressedStreamTools.compress( signedLuggage );
            QCraft.requestGoToServer( player, remoteServerAddress, signedLuggageData );
        }
        catch( IOException e )
        {
            throw new RuntimeException( "Error encoding inventory" );
        }
        finally
        {
            // Prevent the player from being warped twice
            player.timeUntilPortal = 200;
        }
    }

    public void teleportPlayerLocal( EntityPlayer player )
    {
        teleportPlayerLocal( player, m_remotePortalID );
    }

    public static void teleportPlayerLocal( EntityPlayer player, String portalID )
    {
        PortalLocation location = (portalID != null) ?
            PortalRegistry.PortalRegistry.getPortal( portalID ) :
            null;

        if( location != null )
        {
            double xPos = (double)location.m_xOrigin + 0.5 + ( (location.m_xLength > 0) ? (double)(location.m_xLength - 1) * 0.5 : 0.0 );
            double yPos = (double)location.m_yOrigin;
            double zPos = (double)location.m_zOrigin + 0.5 + ( (location.m_zLength > 0) ? (double)(location.m_zLength - 1) * 0.5 : 0.0 );
            if( location.m_dimensionID == player.dimension )
            {
                player.timeUntilPortal = 40;
                player.setPositionAndUpdate( xPos, yPos, zPos );
            }
            else if( player instanceof EntityPlayerMP )
            {
                player.timeUntilPortal = 40;
                MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(
                    (EntityPlayerMP)player,
                    location.m_dimensionID,
                    new QuantumTeleporter(
                        MinecraftServer.getServer().worldServerForDimension( location.m_dimensionID ),
                        xPos, yPos, zPos
                    )
                );
            }
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        // Read properties
        super.readFromNBT( nbttagcompound );
        m_powered = nbttagcompound.getBoolean( "p" );
        m_timeSinceEnergize = nbttagcompound.getInteger( "tse" );
        m_entanglementFrequency = nbttagcompound.getInteger( "f" );
        if( nbttagcompound.hasKey( "d" ) )
        {
            m_storedData = AreaData.decode( nbttagcompound.getCompoundTag( "d" ) );
        }
        if( nbttagcompound.hasKey( "portalID" ) )
        {
            m_portalID = nbttagcompound.getString( "portalID" );
        }
        m_portalNameConflict = nbttagcompound.getBoolean( "portalNameConflict" );
        if( nbttagcompound.hasKey( "remoteIPAddress" ) )
        {
            m_remoteServerAddress = nbttagcompound.getString( "remoteIPAddress" );
        }
        if( nbttagcompound.hasKey( "remoteIPName" ) )
        {
            m_remoteServerName = nbttagcompound.getString( "remoteIPName" );
        }
        else
        {
            m_remoteServerName = m_remoteServerAddress;
        }
        if( nbttagcompound.hasKey( "remotePortalID" ) )
        {
            m_remotePortalID = nbttagcompound.getString( "remotePortalID" );
        }
    }

    @Override
    public void writeToNBT( NBTTagCompound nbttagcompound )
    {
        // Write properties
        super.writeToNBT( nbttagcompound );
        nbttagcompound.setBoolean( "p", m_powered );
        nbttagcompound.setInteger( "tse", m_timeSinceEnergize );
        nbttagcompound.setInteger( "f", m_entanglementFrequency );
        if( m_storedData != null )
        {
            nbttagcompound.setTag( "d", m_storedData.encode() );
        }
        if( m_portalID != null )
        {
            nbttagcompound.setString( "portalID", m_portalID );
        }
        nbttagcompound.setBoolean( "portalNameConflict", m_portalNameConflict );
        if( m_remoteServerAddress != null )
        {
            nbttagcompound.setString( "remoteIPAddress", m_remoteServerAddress );
        }
        if( m_remoteServerName != null )
        {
            nbttagcompound.setString( "remoteIPName", m_remoteServerName );
        }
        if( m_remotePortalID != null )
        {
            nbttagcompound.setString( "remotePortalID", m_remotePortalID );
        }
    }

    @Override
    public Packet getDescriptionPacket()
    {
        // Communicate networked state
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setInteger( "f", m_entanglementFrequency );
        if( m_portalID != null )
        {
            nbttagcompound.setString( "portalID", m_portalID );
        }
        nbttagcompound.setBoolean( "portalNameConflict", m_portalNameConflict );
        if( m_remoteServerAddress != null )
        {
            nbttagcompound.setString( "remoteIPAddress", m_remoteServerAddress );
        }
        if( m_remoteServerName != null )
        {
            nbttagcompound.setString( "remoteIPName", m_remoteServerName );
        }
        if( m_remotePortalID != null )
        {
            nbttagcompound.setString( "remotePortalID", m_remotePortalID );
        }
        return new S35PacketUpdateTileEntity( this.xCoord, this.yCoord, this.zCoord, 0, nbttagcompound );
    }

    @Override
    public void onDataPacket( NetworkManager net, S35PacketUpdateTileEntity packet )
    {
        switch( packet.func_148853_f() ) // actionType
        {
            case 0:
            {
                // Read networked state
                NBTTagCompound nbttagcompound = packet.func_148857_g(); // data
                setEntanglementFrequency( nbttagcompound.getInteger( "f" ) );
                if( nbttagcompound.hasKey( "portalID" ) )
                {
                    m_portalID = nbttagcompound.getString( "portalID" );
                }
                else
                {
                    m_portalID = null;
                }
                m_portalNameConflict = nbttagcompound.getBoolean( "portalNameConflict" );
                if( nbttagcompound.hasKey( "remoteIPAddress" ) )
                {
                    m_remoteServerAddress = nbttagcompound.getString( "remoteIPAddress" );
                }
                else
                {
                    m_remoteServerAddress = null;
                }
                if( nbttagcompound.hasKey( "remoteIPName" ) )
                {
                    m_remoteServerName = nbttagcompound.getString( "remoteIPName" );
                }
                else
                {
                    m_remoteServerName = null;
                }
                if( nbttagcompound.hasKey( "remotePortalID" ) )
                {
                    m_remotePortalID = nbttagcompound.getString( "remotePortalID" );
                }
                else
                {
                    m_remotePortalID = null;
                }
                break;
            }
            default:
            {
                break;
            }
        }
    }
}
