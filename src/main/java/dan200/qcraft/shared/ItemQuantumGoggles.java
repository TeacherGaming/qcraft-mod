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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.EnumHelper;

import java.util.List;

public class ItemQuantumGoggles extends ItemArmor
{
    public static int s_renderIndex;
    private static IIcon[] s_icons;

    public static class SubTypes
    {
        public static final int Quantum = 0;
        public static final int AntiObservation = 1;
        public static final int Count = 2;
    }

    public ItemQuantumGoggles()
    {
        super(
            EnumHelper.addArmorMaterial( "qgoggles", 0, new int[]{ 0, 0, 0, 0 }, 0 ),
            s_renderIndex,
            0
        );
        setUnlocalizedName( "qcraft:goggles" );
        setCreativeTab( QCraft.getCreativeTab() );
        setHasSubtypes( true );
    }

    @Override
    public void getSubItems( Item itemID, CreativeTabs tabs, List list )
    {
        for( int i = 0; i < SubTypes.Count; ++i )
        {
            list.add( new ItemStack( QCraft.Items.quantumGoggles, 1, i ) );
        }
    }

    @Override
    public String getArmorTexture( ItemStack stack, Entity entity, int slot, String type )
    {
        switch( stack.getItemDamage() )
        {
            case SubTypes.Quantum:
            default:
            {
                return "qcraft:textures/armor/goggles.png";
            }
            case SubTypes.AntiObservation:
            {
                return "qcraft:textures/armor/ao_goggles.png";
            }
        }
    }

    @Override
    public void registerIcons( IIconRegister iconRegister )
    {
        s_icons = new IIcon[ SubTypes.Count ];
        s_icons[ SubTypes.Quantum ] = iconRegister.registerIcon( "qcraft:goggles" );
        s_icons[ SubTypes.AntiObservation ] = iconRegister.registerIcon( "qcraft:ao_goggles" );
    }

    @Override
    public IIcon getIconFromDamage( int damage )
    {
        if( damage >= 0 && damage < SubTypes.Count )
        {
            return s_icons[ damage ];
        }
        return s_icons[ SubTypes.Quantum ];
    }

    @Override
    public String getUnlocalizedName( ItemStack stack )
    {
        switch( stack.getItemDamage() )
        {
            case SubTypes.Quantum:
            default:
            {
                return "item.qcraft:goggles";
            }
            case SubTypes.AntiObservation:
            {
                return "item.qcraft:ao_goggles";
            }
        }
    }

    @Override
    public void renderHelmetOverlay( ItemStack stack, EntityPlayer player, ScaledResolution resolution, float partialTicks, boolean hasScreen, int mouseX, int mouseY )
    {
        switch( stack.getItemDamage() )
        {
            case SubTypes.Quantum:
            default:
            {
                QCraft.renderQuantumGogglesOverlay( resolution.getScaledWidth(), resolution.getScaledHeight() );
                break;
            }
            case SubTypes.AntiObservation:
            {
                QCraft.renderAOGogglesOverlay( resolution.getScaledWidth(), resolution.getScaledHeight() );
                break;
            }
        }
    }
}
