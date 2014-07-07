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

public class EntangledQBlockRecipe implements IRecipe
{
    public EntangledQBlockRecipe()
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
        return ItemQBlock.create( BlockQBlock.SubType.Standard, new int[ 6 ], 0, 2 );
    }

    @Override
    public boolean matches( InventoryCrafting _inventory, World world )
    {
        return ( getCraftingResult( _inventory ) != null );
    }

    @Override
    public ItemStack getCraftingResult( InventoryCrafting inventory )
    {
        // Find the eos
        int eosPosX = -1;
        int eosPosY = -1;
        for( int y = 0; y < 3; ++y )
        {
            for( int x = 0; x < 3; ++x )
            {
                ItemStack item = inventory.getStackInRowAndColumn( x, y );
                if( item != null &&
                        item.getItem() == QCraft.Items.eos &&
                        item.getItemDamage() == ItemEOS.SubType.Entanglement )
                {
                    eosPosX = x;
                    eosPosY = y;
                    break;
                }
            }
        }

        // Fail if no eos found:
        if( eosPosX < 0 || eosPosX < 0 )
        {
            return null;
        }

        // Find ODB
        int subType = -1;
        int[] types = null;
        int entanglementFrequency = -1;
        int odbsFound = 0;
        for( int x = 0; x < 3; ++x )
        {
            for( int y = 0; y < 3; ++y )
            {
                if( !( x == eosPosX && y == eosPosY ) )
                {
                    if( ( x == eosPosX - 1 || x == eosPosX + 1 ) && y == eosPosY )
                    {
                        // Find ODBs
                        ItemStack odb = inventory.getStackInRowAndColumn( x, y );
                        if( odb != null && odb.getItem() instanceof ItemQBlock )
                        {
                            if( odbsFound == 0 )
                            {
                                // First ODB, treat as template
                                subType = ItemQBlock.getSubType( odb );
                                types = ItemQBlock.getTypes( odb );
                                entanglementFrequency = ItemQBlock.getEntanglementFrequency( odb );
                            }
                            else
                            {
                                // Subsequent ODBs, must match
                                int odbFrequency = ItemQBlock.getEntanglementFrequency( odb );
                                if( ItemQBlock.getSubType( odb ) == subType &&
                                        ItemQBlock.compareTypes( ItemQBlock.getTypes( odb ), types ) &&
                                        ( ( entanglementFrequency < 0 ) || ( odbFrequency < 0 ) ) )
                                {
                                    entanglementFrequency = ( odbFrequency >= 0 ) ? odbFrequency : entanglementFrequency;
                                }
                                else
                                {
                                    return null;
                                }
                            }
                            odbsFound++;
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else
                    {
                        // Ensure empty
                        if( inventory.getStackInRowAndColumn( x, y ) != null )
                        {
                            return null;
                        }
                    }
                }
            }
        }

        // Check the types exist
        if( types == null || odbsFound < 2 )
        {
            return null;
        }

        // Determine frequency
        if( entanglementFrequency < 0 )
        {
            entanglementFrequency = 0; // This will be assigned after crafting
        }

        // Create item
        return ItemQBlock.create( subType, types, entanglementFrequency, odbsFound );
    }
}
