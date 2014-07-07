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
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class QBlockRecipe implements IRecipe
{
    public QBlockRecipe()
    {
    }

    @Override
    public int getRecipeSize()
    {
        return 9;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return ItemQBlock.create( BlockQBlock.SubType.Standard, new int[ 6 ], -1, 1 );
    }

    @Override
    public boolean matches( InventoryCrafting _inventory, World world )
    {
        return ( getCraftingResult( _inventory ) != null );
    }

    private int getImpostorType( ItemStack stack )
    {
        if( stack == null )
        {
            return 0;
        }

        ItemStack[] blocks = BlockQBlock.getImpostorBlockList();
        for( int i = 1; i < blocks.length; ++i )
        {
            ItemStack block = blocks[ i ];
            if( block.getItem() == stack.getItem() && block.getItemDamage() == stack.getItemDamage() )
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ItemStack getCraftingResult( InventoryCrafting inventory )
    {
        // Find the stone
        int stonePosX = -1;
        int stonePosY = -1;
        int stoneType = -1;
        for( int y = 0; y < 3; ++y )
        {
            for( int x = 0; x < 3; ++x )
            {
                ItemStack item = inventory.getStackInRowAndColumn( x, y );
                if( item != null &&
                        item.getItem() == QCraft.Items.eos &&
                        ( item.getItemDamage() == ItemEOS.SubType.Observation || item.getItemDamage() == ItemEOS.SubType.Superposition ) )
                {
                    stonePosX = x;
                    stonePosY = y;
                    stoneType = item.getItemDamage();
                    break;
                }
            }
        }

        // Fail if no stone found:
        if( stonePosX < 0 || stonePosY < 0 )
        {
            return null;
        }

        // Find the types of the things around the stone
        int numTypes = 0;
        int[] types = new int[ 6 ];
        for( int y = 0; y < 3; ++y )
        {
            for( int x = 0; x < 3; ++x )
            {
                if( !( x == stonePosX && y == stonePosY ) )
                {
                    ItemStack item = inventory.getStackInRowAndColumn( x, y );
                    int type = getImpostorType( item );

                    if( type < 0 )
                    {
                        return null;
                    }

                    int lx = x - stonePosX;
                    int ly = y - stonePosY;
                    if( lx == 0 && ly == -1 )
                    {
                        // North
                        types[ 2 ] = type;
                    }
                    else if( lx == 0 && ly == 1 )
                    {
                        // South
                        types[ 3 ] = type;
                    }
                    else if( lx == -1 && ly == 0 )
                    {
                        // West
                        types[ 4 ] = type;
                    }
                    else if( lx == 1 && ly == 0 )
                    {
                        // East
                        types[ 5 ] = type;
                    }
                    else if( lx == -1 && ly == 1 )
                    {
                        // Up
                        types[ 0 ] = type;
                    }
                    else if( lx == -1 && ly == -1 )
                    {
                        // Down
                        types[ 1 ] = type;
                    }
                    else if( type != 0 )
                    {
                        return null;
                    }

                    if( type > 0 )
                    {
                        numTypes++;
                    }
                }
            }
        }

        if( numTypes > 0 )
        {
            // Create the item
            if( stoneType == ItemEOS.SubType.Observation )
            {
                return ItemQBlock.create( BlockQBlock.SubType.Standard, types, -1, 1 );
            }
            else if( stoneType == ItemEOS.SubType.Superposition )
            {
                return ItemQBlock.create( BlockQBlock.SubType.FiftyFifty, types, -1, 1 );
            }
        }
        return null;
    }
}
