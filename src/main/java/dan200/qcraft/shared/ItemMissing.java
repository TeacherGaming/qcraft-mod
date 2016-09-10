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

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;

/**
 *
 * @author Mathijs Riezebos
 */
public class ItemMissing extends Item 
{    
    private static IIcon s_icon;
    
    public ItemMissing() 
    {
        super();
        setUnlocalizedName( "qcraft:itemMissing" );
    }
    
    @Override
    public void registerIcons( IIconRegister iconRegister )
    {
        s_icon = iconRegister.registerIcon( "qcraft:missing" );
    }

    @Override
    public IIcon getIconFromDamage( int damage )
    {
        return s_icon;
    }
}
