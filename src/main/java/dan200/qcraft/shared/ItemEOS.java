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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

public class ItemEOS extends Item
{
    private static IIcon[] s_icons;

    public static class SubType
    {
        public static final int Superposition = 0;
        public static final int Observation = 1;
        public static final int Entanglement = 2;
        public static final int Count = 3;
    }

    public ItemEOS()
    {
        setMaxStackSize( 64 );
        setHasSubtypes( true );
        setUnlocalizedName( "qcraft:eos" );
        setCreativeTab( QCraft.getCreativeTab() );
    }

    @Override
    public void getSubItems( Item itemID, CreativeTabs tabs, List list )
    {
        for( int i = 0; i < SubType.Count; ++i )
        {
            list.add( new ItemStack( itemID, 1, i ) );
        }
    }

    @Override
    public void registerIcons( IIconRegister iconRegister )
    {
        s_icons = new IIcon[ SubType.Count ];
        s_icons[ SubType.Superposition ] = iconRegister.registerIcon( "qcraft:eos" );
        s_icons[ SubType.Observation ] = iconRegister.registerIcon( "qcraft:eoo" );
        s_icons[ SubType.Entanglement ] = iconRegister.registerIcon( "qcraft:eoe" );
    }

    @Override
    public IIcon getIconFromDamage( int damage )
    {
        if( damage >= 0 && damage < SubType.Count )
        {
            return s_icons[ damage ];
        }
        return s_icons[ SubType.Superposition ];
    }

    @Override
    public String getUnlocalizedName( ItemStack stack )
    {
        int damage = stack.getItemDamage();
        switch( damage )
        {
            case SubType.Superposition:
            default:
            {
                return "item.qcraft:eos";
            }
            case SubType.Observation:
            {
                return "item.qcraft:eoo";
            }
            case SubType.Entanglement:
            {
                return "item.qcraft:eoe";
            }
        }
    }
}
