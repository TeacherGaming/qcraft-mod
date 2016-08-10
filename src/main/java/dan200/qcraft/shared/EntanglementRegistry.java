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

import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.World;

public class EntanglementRegistry<T>
{
    // Privates
    private int m_nextUnusedFrequency;
    private final Map<Integer, List<T>> m_entanglements;

    // Methods
    public EntanglementRegistry()
    {
        m_nextUnusedFrequency = 1;
        m_entanglements = new HashMap<Integer, List<T>>();
    }

    public void reset()
    {
        m_nextUnusedFrequency = 1;
        m_entanglements.clear();
    }

    public void readFromNBT( NBTTagCompound nbt )
    {
        m_nextUnusedFrequency = nbt.getInteger( "nextUnusedFrequency" );
    }

    public void writeToNBT( NBTTagCompound nbt )
    {
        nbt.setInteger( "nextUnusedFrequency", m_nextUnusedFrequency );
    }

    public int getUnusedFrequency()
    {
        int freq = m_nextUnusedFrequency;
        m_nextUnusedFrequency++;
        return freq;
    }

    public void register( int frequency, T entangledObject, World world )
    {
        if( !m_entanglements.containsKey( frequency ) )
        {
            m_entanglements.put( frequency, new ArrayList<T>() );
            if( frequency >= m_nextUnusedFrequency )
            {
                m_nextUnusedFrequency = frequency + 1;
            }
        }
        m_entanglements.get( frequency ).add( entangledObject );
        EntanglementSavedData.get(world).markDirty(); //Notify that this needs to be saved on world save
    }

    public void unregister( int frequency, T entangledObject, World world )
    {
        if( m_entanglements.containsKey( frequency ) )
        {
            m_entanglements.get( frequency ).remove( entangledObject );
            EntanglementSavedData.get(world).markDirty(); //Notify that this needs to be saved on world save
        }
    }

    public List<T> getEntangledObjects( int frequency )
    {
        return m_entanglements.get( frequency );
    }
}
