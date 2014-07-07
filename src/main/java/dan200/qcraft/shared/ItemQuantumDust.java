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
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

public class ItemQuantumDust extends Item
{
    private static IIcon s_icon;

    public ItemQuantumDust()
    {
        super();
        setMaxStackSize( 64 );
        setHasSubtypes( false );
        setUnlocalizedName( "qcraft:dust" );
        setCreativeTab( QCraft.getCreativeTab() );
    }

    @Override
    public void getSubItems( Item item, CreativeTabs tabs, List list )
    {
        list.add( new ItemStack( QCraft.Items.quantumDust, 1, 0 ) );
    }

    @Override
    public boolean onItemUse( ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fx, float fy, float fz )
    {
        return false;
    }

    @Override
    public void registerIcons( IIconRegister iconRegister )
    {
        s_icon = iconRegister.registerIcon( "qcraft:dust" );
    }

    @Override
    public IIcon getIconFromDamage( int damage )
    {
        return s_icon;
    }
}
