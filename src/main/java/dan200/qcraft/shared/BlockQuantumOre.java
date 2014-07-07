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
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockQuantumOre extends Block
{
    private static IIcon s_icon;
    private boolean m_glowing;

    public BlockQuantumOre( boolean glowing )
    {
        super( Material.rock );
        setHardness( 3.0f );
        setResistance( 5.0f );
        setBlockName( "qcraft:ore" );

        m_glowing = glowing;
        if( m_glowing )
        {
            setCreativeTab( QCraft.getCreativeTab() );
            setLightLevel( 0.625f );
            setTickRandomly( true );
        }
    }

    @Override
    public int tickRate( World par1World )
    {
        return 30;
    }

    @Override
    public void onBlockClicked( World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer )
    {
        this.glow( par1World, par2, par3, par4 );
        super.onBlockClicked( par1World, par2, par3, par4, par5EntityPlayer );
    }

    @Override
    public void onEntityWalking( World par1World, int par2, int par3, int par4, Entity par5Entity )
    {
        this.glow( par1World, par2, par3, par4 );
        super.onEntityWalking( par1World, par2, par3, par4, par5Entity );
    }

    @Override
    public boolean onBlockActivated( World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9 )
    {
        this.glow( par1World, par2, par3, par4 );
        return super.onBlockActivated( par1World, par2, par3, par4, par5EntityPlayer, par6, par7, par8, par9 );
    }

    @Override
    public void updateTick( World world, int i, int j, int k, Random r )
    {
        if( this == QCraft.Blocks.quantumOreGlowing )
        {
            world.setBlock( i, j, k, QCraft.Blocks.quantumOre );
        }
    }

    @Override
    public Item getItemDropped( int i, Random r, int j )
    {
        return QCraft.Items.quantumDust;
    }

    @Override
    public int quantityDroppedWithBonus( int par1, Random par2Random )
    {
        return this.quantityDropped( par2Random ) + par2Random.nextInt( par1 + 1 );
    }

    @Override
    public int quantityDropped( Random par1Random )
    {
        return 1 + par1Random.nextInt( 2 );
    }

    @Override
    public void dropBlockAsItemWithChance( World par1World, int par2, int par3, int par4, int par5, float par6, int par7 )
    {
        super.dropBlockAsItemWithChance( par1World, par2, par3, par4, par5, par6, par7 );

        if( this.getItemDropped( par5, par1World.rand, par7 ) != Item.getItemFromBlock( this ) )
        {
            int j1 = 1 + par1World.rand.nextInt( 5 );
            this.dropXpOnBlockBreak( par1World, par2, par3, par4, j1 );
        }
    }

    @Override
    public void randomDisplayTick( World par1World, int par2, int par3, int par4, Random par5Random )
    {
        if( m_glowing )
        {
            this.sparkle( par1World, par2, par3, par4 );
        }
    }

    private void sparkle( World par1World, int par2, int par3, int par4 )
    {
        if( !par1World.isRemote )
        {
            return;
        }

        Random random = par1World.rand;
        double d0 = 0.0625D;

        for( int l = 0; l < 6; ++l )
        {
            double d1 = (double) ( (float) par2 + random.nextFloat() );
            double d2 = (double) ( (float) par3 + random.nextFloat() );
            double d3 = (double) ( (float) par4 + random.nextFloat() );

            if( l == 0 && !par1World.getBlock( par2, par3 + 1, par4 ).isOpaqueCube() )
            {
                d2 = (double) ( par3 + 1 ) + d0;
            }

            if( l == 1 && !par1World.getBlock( par2, par3 - 1, par4 ).isOpaqueCube() )
            {
                d2 = (double) ( par3 + 0 ) - d0;
            }

            if( l == 2 && !par1World.getBlock( par2, par3, par4 + 1 ).isOpaqueCube() )
            {
                d3 = (double) ( par4 + 1 ) + d0;
            }

            if( l == 3 && !par1World.getBlock( par2, par3, par4 - 1 ).isOpaqueCube() )
            {
                d3 = (double) ( par4 + 0 ) - d0;
            }

            if( l == 4 && !par1World.getBlock( par2 + 1, par3, par4 ).isOpaqueCube() )
            {
                d1 = (double) ( par2 + 1 ) + d0;
            }

            if( l == 5 && !par1World.getBlock( par2 - 1, par3, par4 ).isOpaqueCube() )
            {
                d1 = (double) ( par2 + 0 ) - d0;
            }

            if( d1 < (double) par2 || d1 > (double) ( par2 + 1 ) || d2 < 0.0D || d2 > (double) ( par3 + 1 ) || d3 < (double) par4 || d3 > (double) ( par4 + 1 ) )
            {
                QCraft.spawnQuantumDustFX( par1World, d1, d2, d3 );
            }
        }
    }

    private void glow( World world, int i, int j, int k )
    {
        this.sparkle( world, i, j, k );
        if( this == QCraft.Blocks.quantumOre )
        {
            world.setBlock( i, j, k, QCraft.Blocks.quantumOreGlowing );
        }
    }

    @Override
    protected ItemStack createStackedBlock( int i )
    {
        return new ItemStack( QCraft.Blocks.quantumOre );
    }

    @Override
    public void registerBlockIcons( IIconRegister iconRegister )
    {
        s_icon = iconRegister.registerIcon( "qcraft:ore" );
    }

    @Override
    public IIcon getIcon( IBlockAccess world, int i, int j, int k, int side )
    {
        return s_icon;
    }

    @Override
    public IIcon getIcon( int side, int damage )
    {
        return s_icon;
    }
}
