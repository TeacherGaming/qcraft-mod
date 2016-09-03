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

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author Mathijs Riezebos
 */
public class ItemMissing extends Item 
{
    private String wrappedItemName;
    private int wrappedItemId;
    private int wrappedItemCount;
    private int wrappedItemDamage;
    private NBTTagCompound wrappedItemTag;
    
    public ItemMissing() 
    {
        setMaxStackSize( 1 );
        setUnlocalizedName( "qcraft:itemMissing" );
        setCreativeTab( null );
    }
    
    public ItemMissing(NBTTagCompound itemNBT) {
        this();
        
        this.wrappedItemName = itemNBT.getString("Name");
        this.wrappedItemId = itemNBT.getShort("id");
        this.wrappedItemCount = itemNBT.getByte("Count");
        this.wrappedItemDamage = itemNBT.getShort("Damage");
        if (itemNBT.hasKey("tag", 10))
        {
            this.wrappedItemTag = itemNBT.getCompoundTag("tag");
        } 
    }
    
    public NBTTagCompound missingToNBT() {
        NBTTagCompound itemTag = new NBTTagCompound();
        
        //[copied from net.minecraft.item.Item]
        itemTag.setString("Name", this.wrappedItemName);
        itemTag.setShort("id", (short) this.wrappedItemId);
        itemTag.setByte("Count", (byte) this.wrappedItemCount);
        itemTag.setShort("Damage", (short) this.wrappedItemDamage);

        if (this.wrappedItemTag != null)
        {
            itemTag.setTag("tag", this.wrappedItemTag);
        }
        return itemTag;
        //[/copied]
    }
}
