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
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class BlockQuantumComputer extends BlockDirectional
        implements ITileEntityProvider
{
    private static class Icons
    {
        public static IIcon Front;
        public static IIcon Top;
        public static IIcon Side;
    }

    public BlockQuantumComputer()
    {
        super( Material.iron );
        setCreativeTab( QCraft.getCreativeTab() );
        setHardness( 5.0f );
        setResistance( 10.0f );
        setStepSound( Block.soundTypeMetal );
        setBlockName( "qcraft:computer" );
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public Item getItemDropped( int i, Random random, int j )
    {
        return Item.getItemFromBlock( this );
    }

    @Override
    public int damageDropped( int i )
    {
        return 0;
    }

    @Override
    public void dropBlockAsItemWithChance( World world, int x, int y, int z, int side, float f, int unknown )
    {
        // RemoveBlockByPlayer handles this instead
    }

    @Override
    public ArrayList<ItemStack> getDrops( World world, int x, int y, int z, int metadata, int fortune )
    {
        ArrayList<ItemStack> blocks = new ArrayList<ItemStack>();
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQuantumComputer )
        {
            // Get the computer back
            TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
            ItemStack stack = ItemQuantumComputer.create( computer.getEntanglementFrequency(), 1 );
            ItemQuantumComputer.setStoredData( stack, computer.getStoredData() );
            blocks.add( stack );
        }
        return blocks;
    }

    protected boolean shouldDropItemsInCreative( World world, int x, int y, int z )
    {
        return false;
    }

    @Override
    public boolean removedByPlayer( World world, EntityPlayer player, int x, int y, int z )
    {
        if( world.isRemote )
        {
            return false;
        }

        if( !player.capabilities.isCreativeMode || shouldDropItemsInCreative( world, x, y, z ) )
        {
            // Regular and silk touch block (identical)
            int metadata = world.getBlockMetadata( x, y, z );
            ArrayList<ItemStack> items = getDrops( world, x, y, z, metadata, 0 );
            Iterator<ItemStack> it = items.iterator();
            while( it.hasNext() )
            {
                ItemStack item = it.next();
                dropBlockAsItem( world, x, y, z, item );
            }
        }

        return super.removedByPlayer( world, player, x, y, z );
    }

    @Override
    public ItemStack getPickBlock( MovingObjectPosition target, World world, int x, int y, int z )
    {
        int metadata = world.getBlockMetadata( x, y, z );
        ArrayList<ItemStack> items = getDrops( world, x, y, z, metadata, 0 );
        if( items.size() > 0 )
        {
            return items.get( 0 );
        }
        return null;
    }

    @Override
    public boolean onBlockActivated( World world, int x, int y, int z, EntityPlayer player, int l, float m, float n, float o )
    {
        if( player.isSneaking() )
        {
            return false;
        }

        if( !world.isRemote )
        {
            // Show GUI
            TileEntity entity = world.getTileEntity( x, y, z );
            if( entity != null && entity instanceof TileEntityQuantumComputer )
            {
                TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
                QCraft.openQuantumComputerGUI( player, computer );
            }
        }
        return true;
    }

    @Override
    public void breakBlock( World world, int x, int y, int z, Block par5, int par6 )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQuantumComputer )
        {
            TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
            computer.onDestroy();
        }
        super.breakBlock( world, x, y, z, par5, par6 );
    }

    @Override
    public void onBlockPlacedBy( World world, int x, int y, int z, EntityLivingBase player, ItemStack stack )
    {
        int direction = ( ( MathHelper.floor_double( (double) ( player.rotationYaw * 4.0F / 360.0F ) + 0.5D ) & 0x3 ) + 2 ) % 4;
        int metadata = ( direction & 0x3 );
        world.setBlockMetadataWithNotify( x, y, z, metadata, 3 );
    }

    @Override
    public void onNeighborBlockChange( World world, int x, int y, int z, Block id )
    {
        super.onNeighborBlockChange( world, x, y, z, id );

        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQuantumComputer )
        {
            TileEntityQuantumComputer computer = (TileEntityQuantumComputer) entity;
            computer.setRedstonePowered( world.isBlockIndirectlyGettingPowered( x, y, z ) );
        }
    }

    @Override
    public boolean canConnectRedstone( IBlockAccess world, int x, int y, int z, int side )
    {
        return true;
    }

    @Override
    public void registerBlockIcons( IIconRegister iconRegister )
    {
        Icons.Front = iconRegister.registerIcon( "qcraft:computer" );
        Icons.Top = iconRegister.registerIcon( "qcraft:computer_top" );
        Icons.Side = iconRegister.registerIcon( "qcraft:computer_side" );
    }

    @Override
    public IIcon getIcon( IBlockAccess world, int i, int j, int k, int side )
    {
        if( side == 0 || side == 1 )
        {
            return Icons.Top;
        }

        int metadata = world.getBlockMetadata( i, j, k );
        int direction = Direction.directionToFacing[ getDirection( metadata ) ];
        if( side == direction )
        {
            return Icons.Front;
        }

        return Icons.Side;
    }

    @Override
    public IIcon getIcon( int side, int damage )
    {
        switch( side )
        {
            case 0:
            case 1:
            {
                return Icons.Top;
            }
            case 4:
            {
                return Icons.Front;
            }
            default:
            {
                return Icons.Side;
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity( World world, int metadata )
    {
        return new TileEntityQuantumComputer();
    }

    @Override
    public TileEntity createTileEntity( World world, int metadata )
    {
        return createNewTileEntity( world, metadata );
    }
}
