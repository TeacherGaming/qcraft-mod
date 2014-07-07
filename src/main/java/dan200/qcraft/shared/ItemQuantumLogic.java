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
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

public class ItemQuantumLogic extends ItemBlock
{
    public ItemQuantumLogic( Block block )
    {
        super( block );
        setMaxStackSize( 64 );
        setHasSubtypes( true );
        setUnlocalizedName( "qcraft:automatic_observer" );
        setCreativeTab( QCraft.getCreativeTab() );
    }

    @Override
    public void getSubItems( Item item, CreativeTabs tabs, List list )
    {
        list.add( new ItemStack( item, 1, BlockQuantumLogic.SubType.ObserverOff ) );
    }

    @Override
    public IIcon getIconFromDamage( int damage )
    {
        return field_150939_a.getIcon( 1, damage );
    }

    @Override
    public String getUnlocalizedName( ItemStack itemstack )
    {
        int damage = itemstack.getItemDamage();
        int subType = damage;
        switch( subType )
        {
            case BlockQuantumLogic.SubType.ObserverOff:
            default:
            {
                return "tile.qcraft:automatic_observer";
            }
        }
    }
}
