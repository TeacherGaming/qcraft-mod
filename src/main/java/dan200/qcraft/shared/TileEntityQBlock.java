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

import dan200.QCraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TileEntityQBlock extends TileEntity
{
    public boolean hasJustFallen; //tracks whether this block has just solidified from a falling block entity
    public static int FUZZ_TIME = 9;

    public static Random s_random = new Random();
    public static EntanglementRegistry<TileEntityQBlock> QBlockRegistry = new EntanglementRegistry<TileEntityQBlock>();
    public static EntanglementRegistry<TileEntityQBlock> ClientQBlockRegistry = new EntanglementRegistry<TileEntityQBlock>();

    public static EntanglementRegistry<TileEntityQBlock> getEntanglementRegistry( World world )
    {
        if( !world.isRemote )
        {
            return QBlockRegistry;
        }
        else
        {
            return ClientQBlockRegistry;
        }
    }

    // Static
    private int m_entanglementFrequency;
    private int[] m_sideBlockTypes;

    // Replicated
    private long m_timeLastUpdated;

    private boolean m_currentlyObserved;
    private int m_currentDisplayedSide;

    private int m_currentlyForcedSide;
    private boolean[] m_forceObserved;

    // Client only
    public int m_timeSinceLastChange;
    private boolean m_goggles;
    private boolean m_wet;

    public TileEntityQBlock()
    {
        hasJustFallen = false;
        m_entanglementFrequency = -1;
        m_sideBlockTypes = new int[ 6 ];
        m_forceObserved = new boolean[ 6 ];
        for( int i = 0; i < 6; ++i )
        {
            m_sideBlockTypes[ i ] = 0;
            m_forceObserved[ i ] = false;
        }

        m_currentlyObserved = false;
        m_currentlyForcedSide = -1;
        m_currentDisplayedSide = -1;

        m_timeLastUpdated = -99;
        m_timeSinceLastChange = FUZZ_TIME;

        m_goggles = false;
        m_wet = false;
    }

    public EntanglementRegistry<TileEntityQBlock> getEntanglementRegistry()
    {
        return getEntanglementRegistry( worldObj );
    }

    @Override
    public void validate()
    {
        super.validate();
        if( m_entanglementFrequency >= 0 )
        {
            getEntanglementRegistry().register( m_entanglementFrequency, this, this.getWorldObj() );
        }
    }

    @Override
    public void invalidate()
    {
        if( m_entanglementFrequency >= 0 )
        {
            getEntanglementRegistry().unregister( m_entanglementFrequency, this, this.getWorldObj() );
        }
        super.invalidate();
    }

    public void setTypes( int[] types )
    {
        m_sideBlockTypes = types;
    }

    public int[] getTypes()
    {
        return m_sideBlockTypes;
    }

    public void setEntanglementFrequency( int frequency )
    {
        if( frequency != m_entanglementFrequency )
        {
            if( m_entanglementFrequency >= 0 )
            {
                getEntanglementRegistry().unregister( m_entanglementFrequency, this, this.getWorldObj() );
            }
            m_entanglementFrequency = frequency;
            if( m_entanglementFrequency >= 0 )
            {
                getEntanglementRegistry().register( m_entanglementFrequency, this, this.getWorldObj() );
            }
        }
    }

    public int getEntanglementFrequency()
    {
        return m_entanglementFrequency;
    }

    public int getSubType()
    {
        return QCraft.Blocks.qBlock.getSubType( worldObj, xCoord, yCoord, zCoord );
    }

    private void blockUpdate()
    {
        worldObj.markBlockForUpdate( xCoord, yCoord, zCoord );
        worldObj.scheduleBlockUpdate( xCoord, yCoord, zCoord, QCraft.Blocks.qBlock, QCraft.Blocks.qBlock.tickRate( worldObj ) );
        worldObj.notifyBlocksOfNeighborChange( xCoord, yCoord, zCoord, QCraft.Blocks.qBlock );
    }

    public boolean isForceObserved( int side )
    {
        return (m_forceObserved[ side ] == true);
    }

    public void setForceObserved( int side, boolean enable )
    {
        if( worldObj.isRemote )
        {
            return;
        }

        m_forceObserved[ side ] = enable;
    }

    private void setDisplayedSide( boolean observed, boolean forced, int side )
    {
        m_currentlyObserved = observed;
        m_currentlyForcedSide = forced ? side : -1;
        if( m_currentDisplayedSide != side )
        {
            int oldSide = m_currentDisplayedSide;
            int oldType = getObservedType();
            m_currentDisplayedSide = side;
            int newSide = m_currentDisplayedSide;
            int newType = getObservedType();
            if( newType != oldType || (oldSide < 0 != newSide < 0) )
            {
                m_timeSinceLastChange = 0;
                blockUpdate();
            }
        }
    }

    public int getObservedType()
    {
        if( m_currentDisplayedSide < 0 )
        {
            return m_sideBlockTypes[ 1 ];
        }
        return m_sideBlockTypes[ m_currentDisplayedSide ];
    }

    public BlockQBlock.Appearance getAppearance()
    {
        if( m_goggles )
        {
            return BlockQBlock.Appearance.Fuzz;
        }

        if( m_currentDisplayedSide < 0 || m_timeSinceLastChange < FUZZ_TIME )
        {
            return BlockQBlock.Appearance.Swirl;
        }

        int type = m_sideBlockTypes[ m_currentDisplayedSide ];
        if( type == 0 && m_wet )
        {
            return BlockQBlock.Appearance.Fuzz;
        }

        return BlockQBlock.Appearance.Block;
    }

    private boolean checkRayClear( Vec3 playerPos, Vec3 blockPos )
    {
        MovingObjectPosition position = worldObj.rayTraceBlocks( playerPos, blockPos );
        if( position == null )
        {
            return true;
        }
        else if( position.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK )
        {
            if( position.blockX == xCoord &&
                    position.blockY == yCoord &&
                    position.blockZ == zCoord )
            {
                return true;
            }
        }
        return false;
    }

    private int[] collectVotes()
    {
        // Collect votes from all observers
        int[] votes = new int[ 6 ];
        double centerX = (double) xCoord + 0.5;
        double centerY = (double) yCoord + 0.5;
        double centerZ = (double) zCoord + 0.5;

        // For each player:
        List players = worldObj.playerEntities;
        for( int i = 0; i < players.size(); ++i )
        {
            // Determine whether they're looking at the block:
            EntityPlayer player = (EntityPlayer) players.get( i );
            if( player != null )
            {
                // Check the player can see:
                if( QCraft.isPlayerWearingGoggles( player ) )
                {
                    continue;
                }
                else
                {
                    ItemStack headGear = player.inventory.armorItemInSlot( 3 );
                    if( headGear != null && headGear.getItem() == Item.getItemFromBlock( Blocks.pumpkin ) )
                    {
                        continue;
                    }
                }

                // Get position info:
                double x = player.posX - centerX;
                double y = player.posY + 1.62 - (double) player.yOffset - centerY;
                double z = player.posZ - centerZ;

                // Check distance:
                double distance = Math.sqrt( x * x + y * y + z * z );
                if( distance < 96.0 )
                {
                    // Get direction info:
                    double dx = x / distance;
                    double dy = y / distance;
                    double dz = z / distance;

                    // Get facing info:
                    float pitch = player.rotationPitch;
                    float yaw = player.rotationYaw;
                    float f3 = MathHelper.cos( -yaw * 0.017453292f - (float) Math.PI );
                    float f4 = MathHelper.sin( -yaw * 0.017453292f - (float) Math.PI );
                    float f5 = -MathHelper.cos( -pitch * 0.017453292f );
                    float f6 = MathHelper.sin( -pitch * 0.017453292f );
                    float f7 = f4 * f5;
                    float f8 = f3 * f5;
                    double fx = (double) f7;
                    double fy = (double) f6;
                    double fz = (double) f8;

                    // Compare facing and direction (must be close to opposite):
                    double dot = fx * dx + fy * dy + fz * dz;
                    if( dot < -0.4 )
                    {
                        if( QCraft.enableQBlockOcclusionTesting )
                        {
                            // Do some occlusion tests
                            Vec3 playerPos = Vec3.createVectorHelper( centerX + x, centerY + y, centerZ + z );
                            boolean lineOfSightFound = false;
                            for( int side = 0; side < 6; ++side )
                            {
                                // Only check faces that are facing the player
                                Vec3 sideNormal = Vec3.createVectorHelper(
                                        0.49 * Facing.offsetsXForSide[ side ],
                                        0.49 * Facing.offsetsYForSide[ side ],
                                        0.49 * Facing.offsetsZForSide[ side ]
                                );
                                Vec3 blockPos = Vec3.createVectorHelper(
                                        centerX + sideNormal.xCoord,
                                        centerY + sideNormal.yCoord,
                                        centerZ + sideNormal.zCoord
                                );
                                Vec3 playerPosLocal = playerPos.addVector(
                                        -blockPos.xCoord,
                                        -blockPos.yCoord,
                                        -blockPos.zCoord
                                );
                                //if( playerPosLocal.dotProduct( sideNormal ) > 0.0 )
                                {
                                    Vec3 playerPos2 = playerPos.addVector( 0.0, 0.0, 0.0 );
                                    if( checkRayClear( playerPos2, blockPos ) )
                                    {
                                        lineOfSightFound = true;
                                        break;
                                    }
                                }
                            }
                            if( !lineOfSightFound )
                            {
                                continue;
                            }
                        }

                        // Block is being observed!

                        // Determine the major axis:
                        int majoraxis = -1;
                        double majorweight = 0.0f;

                        if( -dy >= majorweight )
                        {
                            majoraxis = 0;
                            majorweight = -dy;
                        }
                        if( dy >= majorweight )
                        {
                            majoraxis = 1;
                            majorweight = dy;
                        }
                        if( -dz >= majorweight )
                        {
                            majoraxis = 2;
                            majorweight = -dz;
                        }
                        if( dz >= majorweight )
                        {
                            majoraxis = 3;
                            majorweight = dz;
                        }
                        if( -dx >= majorweight )
                        {
                            majoraxis = 4;
                            majorweight = -dx;
                        }
                        if( dx >= majorweight )
                        {
                            majoraxis = 5;
                            majorweight = dx;
                        }

                        // Vote for this axis
                        if( majoraxis >= 0 )
                        {
                            if( getSubType() == BlockQBlock.SubType.FiftyFifty )
                            {
                                boolean flip = s_random.nextBoolean();
                                if( flip )
                                {
                                    majoraxis = Facing.oppositeSide[ majoraxis ];
                                }
                            }
                            votes[ majoraxis ]++;
                        }
                    }
                }
            }
        }

        return votes;
    }

    private static int[] addVotes( int[] a, int[] b )
    {
        int[] c = new int[ 6 ];
        for( int i = 0; i < 6; ++i )
        {
            c[ i ] = a[ i ] + b[ i ];
        }
        return c;
    }

    private static int tallyVotes( int[] votes )
    {
        // Tally the votes:
        int winner = 0;
        int winnerVotes = 0;
        for( int i = 0; i < 6; ++i )
        {
            int vote = votes[ i ];
            if( vote > winnerVotes )
            {
                winner = i;
                winnerVotes = vote;
            }
        }

        if( winnerVotes > 0 )
        {
            return winner;
        }
        return -1;
    }

    private int getObservationResult( long currentTime )
    {
        // Get observer votes from entangled twins
        int[] votes = new int[ 6 ];
        //[copied to the readFromNBT method]
        if( m_entanglementFrequency >= 0 )
        {
            List<TileEntityQBlock> twins = getEntanglementRegistry().getEntangledObjects( m_entanglementFrequency );
            if( twins != null )
            {
                Iterator<TileEntityQBlock> it = twins.iterator();
                while( it.hasNext() )
                {
                    TileEntityQBlock twin = it.next();
                    if( twin != this )
                    {
                        //[/copied]
                        if( twin.m_currentlyObserved && twin.m_timeLastUpdated == currentTime )
                        {
                            // If an entangled twin is already up to date, use its result
                            if( twin.m_currentlyForcedSide >= 0 )
                            {
                                return twin.m_currentlyForcedSide + 6;
                            }
                            else
                            {
                                return twin.m_currentDisplayedSide;
                            }
                        }
                        else
                        {
                            // Otherwise, add its votes to the pile
                            if( twin.m_currentlyForcedSide >= 0 && twin.m_forceObserved[ m_currentlyForcedSide ] )
                            {
                                return twin.m_currentlyForcedSide + 6;
                            }
                            else
                            {
                                for( int i=0; i<6; ++i )
                                {
                                    if( twin.m_forceObserved[ i ] )
                                    {
                                        return i + 6;
                                    }
                                }
                            }
                            votes = addVotes( votes, twin.collectVotes() );
                        }
                    }
                }
            }
        }

        // Get local observer votes
        if( m_currentlyForcedSide >= 0 && m_forceObserved[ m_currentlyForcedSide ] )
        {
            return m_currentlyForcedSide + 6;
        }
        else
        {
            for( int i=0; i<6; ++i )
            {
                if( m_forceObserved[ i ] )
                {
                    return i + 6;
                }
            }
        }
        votes = addVotes( votes, collectVotes() );

        // Tally the votes
        return tallyVotes( votes );
    }

    private void redetermineObservedSide()
    {
        // Tally the votes, and work out if we need to change appearance.
        long currentTime = worldObj.getWorldInfo().getWorldTotalTime();
        int winner = getObservationResult( currentTime );
        if( winner >= 6 )
        {
            // Force observed
            winner -= 6;
            setDisplayedSide( true, true, winner );
        }
        else if( winner >= 0 )
        {
            // Passively observed
            if( (m_currentlyForcedSide >= 0) || !m_currentlyObserved )
            {
                setDisplayedSide( true, false, winner );
            }
        }
        else
        {
            // Not observed
            if( m_currentlyObserved )
            {
                if( m_currentlyForcedSide >= 0 )
                {
                    setDisplayedSide( false, false, -1 );
                }
                else
                {
                    setDisplayedSide( false, false, m_currentDisplayedSide );
                }
            }
        }
        m_timeLastUpdated = currentTime;
    }

    private boolean isTouchingLiquid()
    {
        for( int i = 1; i < 6; ++i ) // ignore down
        {
            int x = xCoord + Facing.offsetsXForSide[ i ];
            int y = yCoord + Facing.offsetsYForSide[ i ];
            int z = zCoord + Facing.offsetsZForSide[ i ];
            Block block = worldObj.getBlock( x, y, z );
            if( block != null && block instanceof BlockLiquid )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateEntity()
    {
        // Update material
        if( !worldObj.isRemote )
        {
            redetermineObservedSide();
        }

        // Update ticker, goggles and wetness
        m_timeSinceLastChange++;
        boolean goggles = ( worldObj.isRemote && QCraft.isLocalPlayerWearingQuantumGoggles() );
        boolean wet = isTouchingLiquid();
        if( m_goggles != goggles || m_wet != wet || m_timeSinceLastChange == FUZZ_TIME )
        {
            m_wet = wet;
            m_goggles = goggles;
            blockUpdate();
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        // Read properties
        super.readFromNBT( nbttagcompound );
        m_currentlyObserved = nbttagcompound.getBoolean( "o" );
        m_currentDisplayedSide = nbttagcompound.getInteger( "d" );
        m_entanglementFrequency = nbttagcompound.hasKey( "f" ) ? nbttagcompound.getInteger( "f" ) : -1;
        m_currentlyForcedSide = nbttagcompound.hasKey( "c" ) ? nbttagcompound.getInteger( "c" ) : -1;
        for( int i = 0; i < 6; ++i )
        {
            m_sideBlockTypes[ i ] = nbttagcompound.getInteger( "s" + i );
            m_forceObserved[ i ] = nbttagcompound.getBoolean( "c" + i );
        }
        
        if (hasJustFallen) {
            validate(); //to re-entangle Quantum blocks that have just solidified from a falling block entity
                        
            //[copied from the getObservationResult method]
            if( m_entanglementFrequency >= 0 ) // force-updates blocks to the state of the rest of their entanglement group (this is a very crude implementation
            {
                List<TileEntityQBlock> twins = getEntanglementRegistry().getEntangledObjects( m_entanglementFrequency );
                if( twins != null )
                {
                 Iterator<TileEntityQBlock> it = twins.iterator();
                    while( it.hasNext() )
                    {
                        TileEntityQBlock twin = it.next();
                        if( twin != this )
                        {
                            //[/copied]
                            setDisplayedSide(false, twin.isForceObserved(1), twin.m_currentlyForcedSide );
                            break;
                        }
                    }
                }
            }
            
            
            hasJustFallen = false; //to prevent all kinds of problems
        }
    }

    @Override
    public void writeToNBT( NBTTagCompound nbttagcompound )
    {
        // Write properties
        super.writeToNBT( nbttagcompound );
        nbttagcompound.setBoolean( "o", m_currentlyObserved );
        nbttagcompound.setInteger( "d", m_currentDisplayedSide );
        nbttagcompound.setInteger( "f", m_entanglementFrequency );
        nbttagcompound.setInteger( "c", m_currentlyForcedSide );
        for( int i = 0; i < 6; ++i )
        {
            nbttagcompound.setInteger( "s" + i, m_sideBlockTypes[ i ] );
            nbttagcompound.setBoolean( "c" + i, m_forceObserved[ i ] );
        }
    }

    @Override
    public Packet getDescriptionPacket()
    {
        // Communicate sides and frequency, changing state is calculated on the fly
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeToNBT( nbttagcompound );
        return new S35PacketUpdateTileEntity( this.xCoord, this.yCoord, this.zCoord, 0, nbttagcompound );
    }

    @Override
    public void onDataPacket( NetworkManager net, S35PacketUpdateTileEntity packet )
    {
        switch( packet.func_148853_f() ) // actionType
        {
            case 0:
            {
                // Receive sides and frequency
                int oldSide = m_currentDisplayedSide;
                int oldType = getObservedType();
                NBTTagCompound nbttagcompound = packet.func_148857_g(); // data
                readFromNBT( nbttagcompound );
                int newType = getObservedType();

                // Update state
                if( newType != oldType || oldSide < 0 )
                {
                    m_timeSinceLastChange = 0;
                    blockUpdate();
                }
                break;
            }
        }
    }
}
