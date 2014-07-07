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


package dan200.qcraft.client;

import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.world.World;

public class EntityQuantumDustFX extends EntityReddustFX
{
    public EntityQuantumDustFX( World world, double par2, double par4, double par6, float par8 )
    {
        super( world, par2, par4, par6, par8, 0.0f, 0.0f, 0.0f );

        float f4 = (float) Math.random() * 0.4F + 0.6F;
        this.particleRed = ( (float) ( Math.random() * 0.2f ) + 0.8F ) * 0.0f * f4;
        this.particleGreen = ( (float) ( Math.random() * 0.2f ) + 0.8F ) * 1.0f * f4;
        this.particleBlue = ( (float) ( Math.random() * 0.2f ) + 0.8F ) * 0.0f * f4;
    }
}
