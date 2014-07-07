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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class QuantumTeleporter extends Teleporter
{
    private double m_xPos;
    private double m_yPos;
    private double m_zPos;

    public QuantumTeleporter( WorldServer server, double xPos, double yPos, double zPos )
    {
        super( server );
        m_xPos = xPos;
        m_yPos = yPos;
        m_zPos = zPos;
    }

    @Override
    public void placeInPortal( Entity entity, double par2, double par4, double par6, float par8 )
    {
        if( entity instanceof EntityPlayer )
        {
            ((EntityPlayer)entity).setPositionAndUpdate( m_xPos, m_yPos, m_zPos );
            entity.motionX = entity.motionY = entity.motionZ = 0.0;
        }
    }

    @Override
    public boolean placeInExistingPortal(Entity par1Entity, double par2, double par4, double par6, float par8)
    {
        return false;
    }

    @Override
    public boolean makePortal(Entity par1Entity)
    {
        return false;
    }

    @Override
    public void removeStalePortalLocations(long par1)
    {
    }
}
