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
import net.minecraft.block.*;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockQBlock extends BlockSand
     implements ITileEntityProvider, IQuantumObservable
{
    public int blockRenderID;
    private static IIcon s_transparentIcon;
    private static IIcon s_swirlIcon;
    private static IIcon s_fuzzIcon;
    private static ItemStack[] s_impostorBlocks;

    public static enum Appearance
    {
        Block,
        Fuzz,
        Swirl
    }

    public static ItemStack[] getImpostorBlockList()
    {
        if( s_impostorBlocks == null )
        {
            s_impostorBlocks = new ItemStack[]{
                    null,
                    new ItemStack( Blocks.stone, 1, 0 ),
                    new ItemStack( Blocks.grass, 1, 0 ),
                    new ItemStack( Blocks.dirt, 1, 0 ),
                    new ItemStack( Blocks.bedrock, 1, 0 ),
                    new ItemStack( Blocks.sand, 1, 0 ),
                    new ItemStack( Blocks.gravel, 1, 0 ),
                    new ItemStack( Blocks.gold_ore, 1, 0 ),
                    new ItemStack( Blocks.iron_ore, 1, 0 ),
                    new ItemStack( Blocks.coal_ore, 1, 0 ),
                    new ItemStack( Blocks.log, 1, 0 ),
                    new ItemStack( Blocks.lapis_ore, 1, 0 ),
                    new ItemStack( Blocks.sandstone, 1, 0 ),
                    new ItemStack( Blocks.diamond_ore, 1, 0 ),
                    new ItemStack( Blocks.redstone_ore, 1, 0 ),
                    new ItemStack( Blocks.emerald_ore, 1, 0 ),
                    new ItemStack( Blocks.ice, 1, 0 ),
                    new ItemStack( Blocks.clay, 1, 0 ),
                    new ItemStack( Blocks.pumpkin, 1, 0 ),
                    new ItemStack( Blocks.melon_block, 1, 0 ),
                    new ItemStack( Blocks.mycelium, 1, 0 ),
                    new ItemStack( Blocks.obsidian, 1, 0 ), // 21
                    new ItemStack( Blocks.cobblestone, 1, 0 ),
                    new ItemStack( Blocks.planks, 1, 0 ),
                    new ItemStack( Blocks.bookshelf, 1, 0 ),
                    new ItemStack( Blocks.mossy_cobblestone, 1, 0 ),
                    new ItemStack( Blocks.netherrack, 1, 0 ),
                    new ItemStack( Blocks.soul_sand, 1, 0 ),
                    new ItemStack( Blocks.glowstone, 1, 0 ),
                    new ItemStack( Blocks.end_stone, 1, 0 ),
                    new ItemStack( Blocks.iron_block, 1, 0 ),
                    new ItemStack( Blocks.gold_block, 1, 0 ), // 31
                    new ItemStack( Blocks.diamond_block, 1, 0 ),
                    new ItemStack( Blocks.lapis_block, 1, 0 ),
                    new ItemStack( Blocks.wool, 1, 0 ),
                    new ItemStack( Blocks.glass, 1, 0 ),
                    new ItemStack( Blocks.wool, 1, 1 ),
                    new ItemStack( Blocks.wool, 1, 2 ),
                    new ItemStack( Blocks.wool, 1, 3 ),
                    new ItemStack( Blocks.wool, 1, 4 ),
                    new ItemStack( Blocks.wool, 1, 5 ),
                    new ItemStack( Blocks.wool, 1, 6 ),
                    new ItemStack( Blocks.wool, 1, 7 ),
                    new ItemStack( Blocks.wool, 1, 8 ),
                    new ItemStack( Blocks.wool, 1, 9 ),
                    new ItemStack( Blocks.wool, 1, 10 ),
                    new ItemStack( Blocks.wool, 1, 11 ),
                    new ItemStack( Blocks.wool, 1, 12 ),
                    new ItemStack( Blocks.wool, 1, 13 ),
                    new ItemStack( Blocks.wool, 1, 14 ),
                    new ItemStack( Blocks.wool, 1, 15 ),
                    new ItemStack( Blocks.log, 1, 1 ),
                    new ItemStack( Blocks.log, 1, 2 ),
                    new ItemStack( Blocks.log, 1, 3 ),
                    new ItemStack( Blocks.planks, 1, 1 ),
                    new ItemStack( Blocks.planks, 1, 2 ),
                    new ItemStack( Blocks.planks, 1, 3 ),
                    new ItemStack( Blocks.sandstone, 1, 1 ),
                    new ItemStack( Blocks.sandstone, 1, 2 ),
                    new ItemStack( Blocks.stonebrick, 1, 0 ),
                    new ItemStack( Blocks.stonebrick, 1, 1 ),
                    new ItemStack( Blocks.stonebrick, 1, 2 ),
                    new ItemStack( Blocks.stonebrick, 1, 3 ),
                    new ItemStack( Blocks.nether_brick, 1, 0 ),
                    new ItemStack( Blocks.brick_block, 1, 0 ),
                    new ItemStack( Blocks.redstone_block, 1, 0 ),
                    new ItemStack( Blocks.quartz_ore, 1, 0 ),
                    new ItemStack( Blocks.quartz_block, 1, 0 ),
                    new ItemStack( Blocks.quartz_block, 1, 1 ),
                    new ItemStack( Blocks.quartz_block, 1, 2 ),

                    // New in 1.6.4!
                    new ItemStack( Blocks.stained_hardened_clay, 1, 0 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 1 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 2 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 3 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 4 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 5 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 6 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 7 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 8 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 9 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 10 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 11 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 12 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 13 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 14 ),
                    new ItemStack( Blocks.stained_hardened_clay, 1, 15 ),
                    new ItemStack( Blocks.hay_block, 1, 0 ),
                    new ItemStack( Blocks.hardened_clay, 1, 0 ),
                    new ItemStack( Blocks.coal_block, 1, 0 ),

                    // New in 1.7.2!
                    new ItemStack( Blocks.log2, 1, 0 ),
                    new ItemStack( Blocks.log2, 1, 1 ),
                    new ItemStack( Blocks.dirt, 1, 2 ), // Podzol
                    new ItemStack( Blocks.planks, 1, 4 ),
                    new ItemStack( Blocks.planks, 1, 5 ),
                    new ItemStack( Blocks.sand, 1, 1 ), // Red sand
                    new ItemStack( Blocks.packed_ice, 1, 0 ),
                    new ItemStack( Blocks.stained_glass, 1, 0 ),
                    new ItemStack( Blocks.stained_glass, 1, 1 ),
                    new ItemStack( Blocks.stained_glass, 1, 2 ),
                    new ItemStack( Blocks.stained_glass, 1, 3 ),
                    new ItemStack( Blocks.stained_glass, 1, 4 ),
                    new ItemStack( Blocks.stained_glass, 1, 5 ),
                    new ItemStack( Blocks.stained_glass, 1, 6 ),
                    new ItemStack( Blocks.stained_glass, 1, 7 ),
                    new ItemStack( Blocks.stained_glass, 1, 8 ),
                    new ItemStack( Blocks.stained_glass, 1, 9 ),
                    new ItemStack( Blocks.stained_glass, 1, 10 ),
                    new ItemStack( Blocks.stained_glass, 1, 11 ),
                    new ItemStack( Blocks.stained_glass, 1, 12 ),
                    new ItemStack( Blocks.stained_glass, 1, 13 ),
                    new ItemStack( Blocks.stained_glass, 1, 14 ),
                    new ItemStack( Blocks.stained_glass, 1, 15 ),
            };
        }
        return s_impostorBlocks;
    }

    public static class SubType
    {
        public static final int Standard = 0;
        public static final int FiftyFifty = 1;
        public static final int Count = 2;
    }

    public BlockQBlock()
    {
        setCreativeTab( QCraft.getCreativeTab() );
        setHardness( 5.0f );
        setResistance( 10.0f );
        setStepSound( Block.soundTypeMetal );
        setBlockName( "qcraft:qblock" );
    }

    @Override
    public boolean getUseNeighborBrightness()
    {
        return true;
    }

    public int getSubType( IBlockAccess world, int x, int y, int z )
    {
        return world.getBlockMetadata( x, y, z );
    }

    // IQuantumObservable implementation

    @Override
    public boolean isObserved( World world, int x, int y, int z, int side )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock qBlock = (TileEntityQBlock) entity;
            if( qBlock.isForceObserved( side ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void observe( World world, int x, int y, int z, int side )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock qBlock = (TileEntityQBlock) entity;
            qBlock.setForceObserved( side, true );
        }
    }

    @Override
    public void reset( World world, int x, int y, int z, int side )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock qBlock = (TileEntityQBlock) entity;
            qBlock.setForceObserved( side, false );
        }
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean shouldSideBeRendered( IBlockAccess iblockaccess, int i, int j, int k, int l )
    {
        return true;
    }

    @Override
    public int getRenderType()
    {
        return blockRenderID;
    }

    @Override
    public boolean isNormalCube( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null && !( block instanceof BlockCompressedPowered ) && block != Blocks.ice && block != Blocks.packed_ice && block != Blocks.glass && block != Blocks.stained_glass )
        {
            return true;
        }
        return false;
    }

    @Override
    public int colorMultiplier( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block == Blocks.grass )
        {
            return block.colorMultiplier( world, x, y, z );
        }
        return 0xffffff;
    }

    @Override
    public void addCollisionBoxesToList( World world, int x, int y, int z, AxisAlignedBB bigBox, List list, Entity entity )
    {
        // Determine if solid
        boolean solid = false;
        int type = getImpostorType( world, x, y, z );
        if( type > 0 )
        {
            // Solid blocks are solid to everyone
            solid = true;
        }
        else if( entity instanceof EntityPlayer )
        {
            // Air blocks are solid to people with goggles on
            EntityPlayer player = (EntityPlayer) entity;
            if( QCraft.isPlayerWearingQuantumGoggles( player ) )
            {
                solid = true;
            }
        }

        // Add AABB if so
        if( solid )
        {
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                    (double) x, (double) y, (double) z,
                    (double) x + 1.0, (double) y + 1.0, (double) z + 1.0
            );
            if( aabb != null && aabb.intersectsWith( bigBox ) )
            {
                list.add( aabb );
            }
        }
    }

    @Override
    public boolean isReplaceable( IBlockAccess world, int x, int y, int z )
    {
        /*
		Appearance appearance = getAppearance( world, x, y, z );
		int type = getImpostorType( world, x, y, z );
		if( appearance == Appearance.Block && type == 0 )
		{
			return true;
		}
		*/
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState( IBlockAccess world, int x, int y, int z )
    {
        Appearance appearance = getAppearance( world, x, y, z );
        int type = getImpostorType( world, x, y, z );
        if( appearance != Appearance.Block || type > 0 )
        {
            super.setBlockBounds( 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f );
        }
        else
        {
            super.setBlockBounds( 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f );
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool( World world, int x, int y, int z )
    {
        setBlockBoundsBasedOnState( world, x, y, z );
        return super.getCollisionBoundingBoxFromPool( world, x, y, z );
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool( World world, int x, int y, int z )
    {
        setBlockBoundsBasedOnState( world, x, y, z );
        return super.getSelectedBoundingBoxFromPool( world, x, y, z );
    }

    @Override
    public float getBlockHardness( World world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getBlockHardness( world, x, y, z );
        }
        return 0.0f;
    }

    @Override
    public float getExplosionResistance( Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getExplosionResistance( entity, world, x, y, z, explosionX, explosionY, explosionZ );
        }
        return 0.0f;
    }

    @Override
    public boolean isSideSolid( IBlockAccess world, int x, int y, int z, ForgeDirection side )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isAir( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean canSustainLeaves( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.canSustainLeaves( world, x, y, z );
        }
        return false;
    }

    @Override
    public boolean canBeReplacedByLeaves( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean isWood( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.isWood( world, x, y, z );
        }
        return true;
    }

    @Override
    public int getFlammability( IBlockAccess world, int x, int y, int z, ForgeDirection face )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getFlammability( world, x, y, z, face );
        }
        return 0;
    }

    @Override
    public boolean isFlammable( IBlockAccess world, int x, int y, int z, ForgeDirection face )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.isFlammable( world, x, y, z, face );
        }
        return false;
    }

    @Override
    public int getFireSpreadSpeed( IBlockAccess world, int x, int y, int z, ForgeDirection face )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getFireSpreadSpeed( world, x, y, z, face );
        }
        return 0;
    }

    @Override
    public boolean isFireSource( World world, int x, int y, int z, ForgeDirection side )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.isFireSource( world, x, y, z, side );
        }
        return false;
    }

    @Override
    public int getLightOpacity( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getLightOpacity( world, x, y, z );
        }
        return 0;
    }

    @Override
    public boolean isBeaconBase( IBlockAccess world, int x, int y, int z, int beaconX, int beaconY, int beaconZ )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.isBeaconBase( world, x, y, z, beaconX, beaconY, beaconZ );
        }
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops( World world, int x, int y, int z, int metadata, int fortune )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getDrops( world, x, y, z, getImpostorDamage( world, x, y, z ), fortune );
        }
        return new ArrayList<ItemStack>();
    }

    @Override
    public void dropBlockAsItemWithChance( World world, int i, int j, int k, int l, float f, int unknown )
    {
        // removeBlockByPlayer handles this instead
    }

    @Override
    public boolean removedByPlayer( World world, EntityPlayer player, int x, int y, int z )
    {
        if( world.isRemote )
        {
            return false;
        }

        if( !player.capabilities.isCreativeMode )
        {
            if( EnchantmentHelper.getSilkTouchModifier( player ) )
            {
                // Silk harvest (get qblock back)
                TileEntity entity = world.getTileEntity( x, y, z );
                if( entity != null && entity instanceof TileEntityQBlock )
                {
                    TileEntityQBlock qblock = (TileEntityQBlock) entity;
                    ItemStack item = ItemQBlock.create( qblock.getSubType(), qblock.getTypes(), qblock.getEntanglementFrequency(), 1 );
                    dropBlockAsItem( world, x, y, z, item );
                }
            }
            else
            {
                // Regular harvest (get impostor)
                Block block = getImpostorBlock( world, x, y, z );
                if( block != null )
                {
                    int metadata = getImpostorDamage( world, x, y, z );
                    if( block.canHarvestBlock( player, metadata ) )
                    {
                        int fortune = EnchantmentHelper.getFortuneModifier( player );
                        ArrayList<ItemStack> items = getDrops( world, x, y, z, metadata, fortune );
                        Iterator<ItemStack> it = items.iterator();
                        while( it.hasNext() )
                        {
                            ItemStack item = it.next();
                            dropBlockAsItem( world, x, y, z, item );
                        }
                    }
                }
            }
        }
        return super.removedByPlayer( world, player, x, y, z );
    }

    @Override
    public ItemStack getPickBlock( MovingObjectPosition target, World world, int x, int y, int z )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock qblock = (TileEntityQBlock) entity;
            return ItemQBlock.create( qblock.getSubType(), qblock.getTypes(), qblock.getEntanglementFrequency(), 1 );
        }
        return null;
    }

    @Override
    public boolean canHarvestBlock( EntityPlayer player, int metadata )
    {
        return true;
    }

    @Override
    public void harvestBlock( World world, EntityPlayer player, int x, int y, int z, int metadata )
    {
    }

    @Override
    public boolean canSilkHarvest( World world, EntityPlayer player, int x, int y, int z, int metadata )
    {
        return false;
    }

    @Override
    public void onBlockPlacedBy( World world, int x, int y, int z, EntityLivingBase player, ItemStack stack )
    {
        int subType = stack.getItemDamage();
        int metadata = subType;
        world.setBlockMetadataWithNotify( x, y, z, metadata, 3 );
    }

    @Override
    public void updateTick( World world, int x, int y, int z, Random r )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null && block instanceof BlockSand )
        {
            super.updateTick( world, x, y, z, r );
        }
    }

    @Override
    protected void func_149829_a( EntityFallingBlock entityFallingSand ) // onStartFalling
    {
        // Setup NBT for block to place
        World world = entityFallingSand.worldObj;
        int x = (int) ( entityFallingSand.posX - 0.5f );
        int y = (int) ( entityFallingSand.posY - 0.5f );
        int z = (int) ( entityFallingSand.posZ - 0.5f );
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            entity.writeToNBT( nbttagcompound );
            entityFallingSand.field_145810_d = nbttagcompound; // data
        }

        // Prevent the falling qBlock from dropping items
        entityFallingSand.field_145813_c = false; // dropItems
    }
    
    @Override
    public void func_149828_a(World world, int x, int y, int z, int p) // onStopFalling
    {        
        TileEntity entity = world.getTileEntity(x, y, z);        
        if (entity != null && entity instanceof TileEntityQBlock) {
            TileEntityQBlock qBlock = (TileEntityQBlock) entity;
            qBlock.hasJustFallen = true;
        }
    }

    @Override
    public boolean canProvidePower()
    {
        return true;
    }

    @Override
    public boolean canConnectRedstone( IBlockAccess world, int x, int y, int z, int side )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null && block instanceof BlockCompressedPowered )
        {
            return true;
        }
        return false;
    }

    @Override
    public int isProvidingWeakPower( IBlockAccess world, int x, int y, int z, int side )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null && block instanceof BlockCompressedPowered )
        {
            return 15;
        }
        return 0;
    }

    @Override
    public int getLightValue( IBlockAccess world, int x, int y, int z )
    {
        Block block = getImpostorBlock( world, x, y, z );
        if( block != null )
        {
            return block.getLightValue();
        }
        return 0;
    }

    public int getColorForType( int side, int type )
    {
        if( type == 2 ) // grass
        {
            return ( side == 1 ) ? Blocks.grass.getRenderColor( 0 ) : 0xffffff;
        }
        return 0xffffff;
    }

    public IIcon getIconForType( int side, int type, Appearance appearance )
    {
        if( appearance == Appearance.Swirl )
        {
            return s_swirlIcon;
        }
        else if( appearance == Appearance.Fuzz )
        {
            return s_fuzzIcon;
        }
        else //if( appearance == Appearance.Block )
        {
            ItemStack[] blockList = getImpostorBlockList();
            if( type >= 0 && type < blockList.length )
            {
                ItemStack item = blockList[ type ];
                if( item != null )
                {
                    Block block = ((ItemBlock)item.getItem()).field_150939_a;
                    int damage = item.getItemDamage();
                    return block.getIcon( side, damage );
                }
            }
            return s_transparentIcon;
        }
    }

    @Override
    public IIcon getIcon( IBlockAccess world, int x, int y, int z, int side )
    {
        int type = getImpostorType( world, x, y, z );
        Appearance appearance = getAppearance( world, x, y, z );
        return getIconForType( side, type, appearance );
    }

    public static boolean s_forceGrass = false;

    @Override
    public IIcon getIcon( int side, int damage )
    {
        if( s_forceGrass )
        {
            return Blocks.grass.getIcon( side, damage );
        }
        else
        {
            return s_swirlIcon;
        }
    }

    @Override
    public void registerBlockIcons( IIconRegister iconRegister )
    {
        s_transparentIcon = iconRegister.registerIcon( "qcraft:transparent" );
        s_swirlIcon = iconRegister.registerIcon( "qcraft:qblock_swirl" );
        s_fuzzIcon = iconRegister.registerIcon( "qcraft:qblock_fuzz" );
    }

    @Override
    public TileEntity createNewTileEntity( World world, int metadata )
    {
        return new TileEntityQBlock();
    }

    @Override
    public TileEntity createTileEntity( World world, int metadata )
    {
        return createNewTileEntity( world, metadata );
    }

    private Appearance getAppearance( IBlockAccess world, int x, int y, int z )
    {
        TileEntity entity = world.getTileEntity( x, y, z );
        if( entity != null && entity instanceof TileEntityQBlock )
        {
            TileEntityQBlock quantum = (TileEntityQBlock) entity;
            return quantum.getAppearance();
        }
        return Appearance.Fuzz;
    }

    private int getImpostorType( IBlockAccess world, int x, int y, int z )
    {
        int type = 0;
        if( y >= 0 )
        {
            TileEntity entity = world.getTileEntity( x, y, z );
            if( entity != null && entity instanceof TileEntityQBlock )
            {
                TileEntityQBlock quantum = (TileEntityQBlock) entity;
                type = quantum.getObservedType();
            }
        }
        return type;
    }

    public Block getImpostorBlock( IBlockAccess world, int x, int y, int z )
    {
        // Return block
        int type = getImpostorType( world, x, y, z );
        ItemStack[] blockList = getImpostorBlockList();
        if( type < blockList.length )
        {
            ItemStack item = blockList[ type ];
            if( item != null )
            {
                return Block.getBlockFromItem( item.getItem() );
            }
        }
        return null;
    }

    private int getImpostorDamage( IBlockAccess world, int x, int y, int z )
    {
        // Return damage
        int type = getImpostorType( world, x, y, z );
        ItemStack[] blockList = getImpostorBlockList();
        if( type < blockList.length )
        {
            ItemStack item = blockList[ type ];
            if( item != null )
            {
                return item.getItemDamage();
            }
        }
        return 0;
    }
}
