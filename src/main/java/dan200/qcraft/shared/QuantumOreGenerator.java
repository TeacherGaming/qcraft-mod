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

import cpw.mods.fml.common.IWorldGenerator;
import dan200.QCraft;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.Random;

public class QuantumOreGenerator implements IWorldGenerator
{
    private WorldGenMinable m_oreGen;

    public QuantumOreGenerator()
    {
        m_oreGen = new WorldGenMinable( QCraft.Blocks.quantumOre, 5 );
    }

    @Override
    public void generate( Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider )
    {
        if( !world.provider.isHellWorld && world.provider.terrainType != WorldType.FLAT )
        {
            generateSurface( world, random, chunkX * 16, chunkZ * 16 );
        }
    }

    private void generateSurface( World world, Random rand, int chunkX, int chunkZ )
    {
        for( int k = 0; k < 4; ++k )
        {
            int firstBlockXCoord = chunkX + rand.nextInt( 16 );
            int firstBlockYCoord = rand.nextInt( 24 );
            int firstBlockZCoord = chunkZ + rand.nextInt( 16 );
            m_oreGen.generate( world, rand, firstBlockXCoord, firstBlockYCoord, firstBlockZCoord );
        }
    }
}
