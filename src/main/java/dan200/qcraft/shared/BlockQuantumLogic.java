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
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockQuantumLogic extends BlockDirectional
{
    public int blockRenderID;
    private IIcon[] m_icons;

    public class SubType
    {
        public static final int ObserverOff = 0;
        public static final int ObserverOn = 1;
        public static final int Count = 2;
    }

    public int getSubType( int metadata )
    {
        return ( ( metadata >> 2 ) & 0x3 );
    }

    protected BlockQuantumLogic()
    {
        super( Material.circuits );
        setBlockBounds( 0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F );
        setHardness( 0.0F );
        setStepSound( Block.soundTypeWood );
        setBlockName( "qcraft:automatic_observer" );
        setCreativeTab( QCraft.getCreativeTab() );
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return blockRenderID;
    }

    @Override
    public boolean canPlaceBlockAt( World world, int x, int y, int z )
    {
        if( World.doesBlockHaveSolidTopSurface( world, x, y - 1, z ) )
        {
            return super.canPlaceBlockAt( world, x, y, z );
        }
        return false;
    }

    @Override
    public boolean canBlockStay( World world, int x, int y, int z )
    {
        if( World.doesBlockHaveSolidTopSurface( world, x, y - 1, z ) )
        {
            return super.canBlockStay( world, x, y, z );
        }
        return false;
    }

    @Override
    public IIcon getIcon( IBlockAccess world, int i, int j, int k, int side )
    {
        int metadata = world.getBlockMetadata( i, j, k );
        int damage = getSubType( metadata );
        return getIcon( side, damage );
    }

    @Override
    public IIcon getIcon( int side, int damage )
    {
        int subType = damage;
        if( side == 1 && damage >= 0 && damage < m_icons.length )
        {
            return m_icons[ damage ];
        }
        return Blocks.double_stone_slab.getBlockTextureFromSide( side );
    }

    @Override
    public int isProvidingStrongPower( IBlockAccess world, int x, int y, int z, int side )
    {
        return 0;
    }

    @Override
    public int isProvidingWeakPower( IBlockAccess world, int x, int y, int z, int side )
    {
        return 0;
    }

    @Override
    public boolean canConnectRedstone( IBlockAccess world, int x, int y, int z, int side )
    {
        int metadata = world.getBlockMetadata( x, y, z );
        int direction = Direction.rotateOpposite[ getDirection( metadata ) ];
        return ( side == direction );
    }

    @Override
    public boolean canProvidePower()
    {
        return true;
    }

    @Override
    public void onNeighborBlockChange( World world, int x, int y, int z, Block block )
    {
        int metadata = world.getBlockMetadata( x, y, z );
        if( !this.canBlockStay( world, x, y, z ) )
        {
            if( !world.isRemote )
            {
                // Destroy
                this.dropBlockAsItem( world, x, y, z, metadata, 0 );
                world.setBlockToAir( x, y, z );
            }
        }
        else
        {
            // Redetermine subtype
            updateOutput( world, x, y, z );
        }
    }

    private void updateOutput( World world, int x, int y, int z )
    {
        if( world.isRemote )
        {
            return;
        }

        // Redetermine subtype
        int metadata = world.getBlockMetadata( x, y, z );
        int direction = getDirection( metadata );
        int subType = getSubType( metadata );
        int newSubType = evaluateInput( world, x, y, z ) ? SubType.ObserverOn : SubType.ObserverOff;
        if( newSubType != subType )
        {
            // Set new subtype
            setDirectionAndSubType( world, x, y, z, direction, newSubType );
            subType = newSubType;

            // Notify
            world.markBlockForUpdate( x, y, z );
            world.notifyBlocksOfNeighborChange( x, y, z, this );
        }

        // Observe
        int facing = Facing.oppositeSide[ Direction.directionToFacing[ direction ] ];
        observe( world, x, y, z, facing, subType == SubType.ObserverOn );
    }

    private void setDirectionAndSubType( World world, int x, int y, int z, int direction, int subType )
    {
        int metadata = ( direction & 0x3 ) + ( ( subType & 0x3 ) << 2 );
        world.setBlockMetadataWithNotify( x, y, z, metadata, 3 );
    }

    @Override
    public void onBlockPlacedBy( World world, int x, int y, int z, EntityLivingBase player, ItemStack stack )
    {
        int direction = ( ( MathHelper.floor_double( (double) ( player.rotationYaw * 4.0F / 360.0F ) + 0.5D ) & 3 ) + 2 ) % 4;
        int subType = stack.getItemDamage();
        setDirectionAndSubType( world, x, y, z, direction, subType );
    }

    @Override
    public void onBlockAdded( World world, int x, int y, int z )
    {
        updateOutput( world, x, y, z );
    }

    @Override
    public void onBlockDestroyedByPlayer( World par1World, int par2, int par3, int par4, int par5 )
    {
        super.onBlockDestroyedByPlayer( par1World, par2, par3, par4, par5 );
    }

    @Override
    public void randomDisplayTick( World world, int i, int j, int k, Random r )
    {
        if( !world.isRemote )
        {
            return;
        }
    }

    @Override
    public void registerBlockIcons( IIconRegister iconRegister )
    {
        m_icons = new IIcon[ SubType.Count ];
        m_icons[ SubType.ObserverOff ] = iconRegister.registerIcon( "qcraft:automatic_observer" );
        m_icons[ SubType.ObserverOn ] = iconRegister.registerIcon( "qcraft:automatic_observer_on" );
    }

    private boolean evaluateInput( World world, int i, int j, int k )
    {
        int metadata = world.getBlockMetadata( i, j, k );
        int direction = Facing.oppositeSide[ Direction.directionToFacing[ getDirection( metadata ) ] ];
        int backDir = Facing.oppositeSide[ direction ];
        return getRedstoneSignal( world, i, j, k, backDir );
    }

    private boolean getRedstoneSignal( World world, int i, int j, int k, int dir )
    {
        i += Facing.offsetsXForSide[ dir ];
        j += Facing.offsetsYForSide[ dir ];
        k += Facing.offsetsZForSide[ dir ];
        int side = Facing.oppositeSide[ dir ];
        return QuantumUtil.getRedstoneSignal( world, i, j, k, side );
    }

    private void observe( World world, int i, int j, int k, int dir, boolean observe )
    {
        i += Facing.offsetsXForSide[ dir ];
        j += Facing.offsetsYForSide[ dir ];
        k += Facing.offsetsZForSide[ dir ];
        Block block = world.getBlock( i, j, k );
        if( block != null && block instanceof IQuantumObservable )
        {
            int side = Facing.oppositeSide[ dir ];
            IQuantumObservable observable = (IQuantumObservable) block;
            if( observable.isObserved( world, i, j, k, side ) != observe )
            {
                if( observe )
                {
                    observable.observe( world, i, j, k, side );
                }
                else
                {
                    observable.reset( world, i, j, k, side );
                }
            }
        }
    }
}
