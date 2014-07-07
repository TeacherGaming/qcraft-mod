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
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.util.*;

public class LostLuggage
{
    public static final long MAX_LUGGAGE_AGE_HOURS = 24; // Luggage lasts 24 hours
    public static final LostLuggage Instance = new LostLuggage();

    public static class Address
    {
        private final String m_address;

        public Address( String address )
        {
            m_address = address;
        }

        @Override
        public boolean equals( Object o )
        {
            if( o == this )
            {
                return true;
            }
            if( o != null && o instanceof Address )
            {
                Address other = (Address)o;
                return other.m_address.equals( m_address );
            }
            return false;
        }

        public String getAddress()
        {
            return m_address;
        }
    }

    private static class Luggage
    {
        public long m_timeStamp;
        public Address m_origin; // Can be null, if portalling from a single player game
        public Address m_destination;
        public byte[] m_luggage;
    }

    public static class LuggageMatch
    {
        public boolean m_matchedDestination; // If this is false, the origin matched
        public byte[] m_luggage;
        public long m_timeStamp;

        public LuggageMatch( boolean matchedDestination, byte[] luggage, long timeStamp )
        {
            m_matchedDestination = matchedDestination;
            m_luggage = luggage;
            m_timeStamp = timeStamp;
        }
    }

    private Set<Luggage> m_luggage;

    public LostLuggage()
    {
        m_luggage = new HashSet<Luggage>();
    }

    public void reset()
    {
        m_luggage.clear();
    }

    public void load()
    {
        File location = new File( "./qcraft/luggage.bin" );
        NBTTagCompound nbt = QCraftProxyCommon.loadNBTFromPath( location );
        if( nbt != null )
        {
            readFromNBT( nbt );
        }
        else
        {
            reset();
        }
    }

    public void save()
    {
        File location = new File( "./qcraft/luggage.bin" );
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT( nbt );
        QCraftProxyCommon.saveNBTToPath( location, nbt );
    }

    private void readFromNBT( NBTTagCompound nbt )
    {
        m_luggage.clear();
        NBTTagList luggageList = nbt.getTagList( "luggage", 10 );
        for( int i=0; i<luggageList.tagCount(); ++i )
        {
            NBTTagCompound luggageTag = luggageList.getCompoundTagAt( i );
            Luggage luggage = new Luggage();
            luggage.m_timeStamp = luggageTag.getLong( "timeStamp" );
            if( luggageTag.hasKey( "originIP" ) && luggageTag.hasKey( "originPort" ) )
            {
                luggage.m_origin = new Address( luggageTag.getString( "originIP" ) + ":" + luggageTag.getInteger( "originPort" ) );
            }
            else if( luggageTag.hasKey( "originAddress" ) )
            {
                luggage.m_origin = new Address( luggageTag.getString( "originAddress" ) );
            }
            if( luggageTag.hasKey( "destinationIP" ) && luggageTag.hasKey( "destinationPort" ) )
            {
                luggage.m_destination = new Address( luggageTag.getString( "destinationIP" ) + ":" + luggageTag.getInteger( "destinationPort" ) );
            }
            else if( luggageTag.hasKey( "destinationAddress" ) )
            {
                luggage.m_destination = new Address( luggageTag.getString( "destinationAddress" ) );
            }
            luggage.m_luggage = luggageTag.getByteArray( "luggage" );
            m_luggage.add( luggage );
        }
    }

    private void writeToNBT( NBTTagCompound nbt )
    {
        NBTTagList luggageList = new NBTTagList();
        for( Luggage luggage : m_luggage )
        {
            NBTTagCompound luggageTag = new NBTTagCompound();
            luggageTag.setLong( "timeStamp", luggage.m_timeStamp );
            if( luggage.m_origin != null )
            {
                luggageTag.setString( "originAddress", luggage.m_origin.getAddress() );
            }
            if( luggage.m_destination != null )
            {
                luggageTag.setString( "destinationAddress", luggage.m_destination.getAddress() );
            }
            luggageTag.setByteArray( "luggage", luggage.m_luggage );
            luggageList.appendTag( luggageTag );
        }
        nbt.setTag( "luggage", luggageList );
    }

    public void storeLuggage( Address origin, Address destination, byte[] luggageData )
    {
        Luggage luggage = new Luggage();
        luggage.m_timeStamp = System.currentTimeMillis(); // Yep, this is a UTC timestamp
        luggage.m_origin = origin;
        luggage.m_destination = destination;
        luggage.m_luggage = luggageData;
        m_luggage.add( luggage );
    }

    public void removeOldLuggage()
    {
        long timeNow = System.currentTimeMillis();
        Iterator<Luggage> it = m_luggage.iterator();
        while( it.hasNext() )
        {
            Luggage luggage = it.next();
            long ageMillis = timeNow - luggage.m_timeStamp;
            if( ageMillis >= MAX_LUGGAGE_AGE_HOURS * 60 * 60 * 1000 )
            {
                it.remove();
            }
        }
    }

    public Collection<LuggageMatch> getMatchingLuggage( Address server )
    {
        List<LuggageMatch> luggages = new ArrayList<LuggageMatch>();
        for( Luggage luggage : m_luggage )
        {
            if( server.equals( luggage.m_destination ) )
            {
                luggages.add( new LuggageMatch( true, luggage.m_luggage, luggage.m_timeStamp ) );
            }
            else if( server.equals( luggage.m_origin ) )
            {
                luggages.add( new LuggageMatch( false, luggage.m_luggage, luggage.m_timeStamp ) );
            }
        }
        return luggages;
    }

    public void removeLuggage( byte[] luggageData )
    {
        Iterator<Luggage> it = m_luggage.iterator();
        while( it.hasNext() )
        {
            Luggage luggage = it.next();
            if( Arrays.equals( luggageData, luggage.m_luggage ) )
            {
                it.remove();
            }
        }
    }
}
