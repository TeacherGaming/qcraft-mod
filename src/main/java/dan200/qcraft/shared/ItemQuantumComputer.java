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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.List;

public class ItemQuantumComputer extends ItemBlock
{
    public ItemQuantumComputer( Block block )
    {
        super( block );
        setMaxStackSize( 64 );
        setHasSubtypes( true );
        setUnlocalizedName( "qcraft:computer" );
        setCreativeTab( QCraft.getCreativeTab() );
    }

    public static ItemStack create( int entanglementFrequency, int quantity )
    {
        ItemStack result = new ItemStack( QCraft.Blocks.quantumComputer, quantity, 0 );
        setEntanglementFrequency( result, entanglementFrequency );
        return result;
    }

    @Override
    public void getSubItems( Item item, CreativeTabs tabs, List list )
    {
        list.add( create( -1, 1 ) );
    }

    public static void setEntanglementFrequency( ItemStack stack, int entanglementFrequency )
    {
        // Ensure the nbt
        if( !stack.hasTagCompound() )
        {
            stack.setTagCompound( new NBTTagCompound() );
        }

        // Set the tags
        NBTTagCompound nbt = stack.getTagCompound();
        if( entanglementFrequency < 0 )
        {
            // No frequency
            if( nbt.hasKey( "e" ) )
            {
                nbt.removeTag( "e" );
            }
            if( nbt.hasKey( "R" ) )
            {
                nbt.removeTag( "R" );
            }
        }
        else if( entanglementFrequency == 0 )
        {
            // Unknown frequency
            nbt.setInteger( "e", entanglementFrequency );
            nbt.setInteger( "R", TileEntityQBlock.s_random.nextInt( 0xffffff ) );
        }
        else
        {
            // Known frequency
            nbt.setInteger( "e", entanglementFrequency );
            if( nbt.hasKey( "R" ) )
            {
                nbt.removeTag( "R" );
            }
        }
    }

    public static int getEntanglementFrequency( ItemStack stack )
    {
        if( stack.hasTagCompound() )
        {
            NBTTagCompound nbt = stack.getTagCompound();
            if( nbt.hasKey( "e" ) )
            {
                int frequency = nbt.getInteger( "e" );
                return frequency;
            }
        }
        return -1;
    }

    public static void setStoredData( ItemStack stack, TileEntityQuantumComputer.AreaData data )
    {
        // Ensure the nbt
        if( !stack.hasTagCompound() )
        {
            stack.setTagCompound( new NBTTagCompound() );
        }

        // Set the tags
        NBTTagCompound nbt = stack.getTagCompound();
        if( data != null )
        {
            nbt.setTag( "d", data.encode() );
        }
        else
        {
            if( nbt.hasKey( "d" ) )
            {
                nbt.removeTag( "d" );
            }
        }
    }

    public static TileEntityQuantumComputer.AreaData getStoredData( ItemStack stack )
    {
        if( stack.hasTagCompound() )
        {
            NBTTagCompound nbt = stack.getTagCompound();
            if( nbt.hasKey( "d" ) )
            {
                return TileEntityQuantumComputer.AreaData.decode( nbt.getCompoundTag( "d" ) );
            }
        }
        return null;
    }

    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player )
    {
        if( getEntanglementFrequency( stack ) == 0 && !world.isRemote )
        {
            setEntanglementFrequency( stack, TileEntityQuantumComputer.getEntanglementRegistry( world ).getUnusedFrequency() );
            player.inventory.markDirty();
            if( player.openContainer != null )
            {
                player.openContainer.detectAndSendChanges();
            }
        }
    }

    @Override
    public boolean placeBlockAt( ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata )
    {
        if( super.placeBlockAt( stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata ) )
        {
            TileEntity entity = world.getTileEntity( x, y, z );
            if( entity != null && entity instanceof TileEntityQuantumComputer )
            {
                TileEntityQuantumComputer quantum = (TileEntityQuantumComputer) entity;
                quantum.setEntanglementFrequency( getEntanglementFrequency( stack ) );
                quantum.setStoredData( getStoredData( stack ) );
            }
            return true;
        }
        return false;
    }

    @Override
    public String getUnlocalizedName( ItemStack stack )
    {
        if( getEntanglementFrequency( stack ) >= 0 )
        {
            return "tile.qcraft:computer_entangled";
        }
        return "tile.qcraft:computer";
    }

    @Override
    public void addInformation( ItemStack stack, EntityPlayer player, List list, boolean par4 )
    {
        int frequency = getEntanglementFrequency( stack );
        if( frequency > 0 )
        {
            list.add( "Group: " + ItemQBlock.formatFrequency( frequency ) );
        }
        //else if( frequency == 0 )
        //{
        //	list.add( "Group: ???" );
        //}

        TileEntityQuantumComputer.AreaData data = getStoredData( stack );
        if( data != null )
        {
            int w = data.m_shape.m_xMax - data.m_shape.m_xMin + 1;
            int h = data.m_shape.m_yMax - data.m_shape.m_yMin + 1;
            int l = data.m_shape.m_zMax - data.m_shape.m_zMin + 1;
            list.add( "Stored Area: " + w + "m x " + l + "m x " + h + "m" );
        }
    }
}
