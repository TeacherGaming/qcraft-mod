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
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalRegistry
{
    public static final PortalRegistry PortalRegistry = new PortalRegistry();
    public static final PortalRegistry ClientPortalRegistry = new PortalRegistry();

    public static PortalRegistry getPortalRegistry( World world )
    {
        if( !world.isRemote )
        {
            return PortalRegistry;
        }
        else
        {
            return ClientPortalRegistry;
        }
    }

    private static class Server
    {
        public String m_name;
        public String m_address;

        public Server( String name, String address )
        {
            m_name = name;
            m_address = address;
        }
    }

    // Privates
    private List<Server> m_servers;
    private Map<String, TileEntityQuantumComputer.PortalLocation> m_portals;

    // Methods
    public PortalRegistry()
    {
        m_servers = new ArrayList<Server>();
        m_portals = new HashMap<String, TileEntityQuantumComputer.PortalLocation>();
    }

    public void reset()
    {
        m_servers.clear();
        m_portals.clear();
    }

    public void readFromNBT( NBTTagCompound nbt )
    {
        if( nbt.hasKey( "servers" ) )
        {
            NBTTagList servers = nbt.getTagList( "servers", 10 );
            for( int i=0; i<servers.tagCount(); ++i )
            {
                NBTTagCompound server = servers.getCompoundTagAt( i );
                m_servers.add( new Server( server.getString( "name" ), server.getString( "address" ) ) );
            }
        }

        NBTTagList portals = nbt.getTagList( "portals", 10 );
        for( int i=0; i<portals.tagCount(); ++i )
        {
            NBTTagCompound portal = portals.getCompoundTagAt( i );
            m_portals.put( portal.getString( "id" ), TileEntityQuantumComputer.PortalLocation.decode( portal ) );
        }
    }

    public void writeToNBT( NBTTagCompound nbt )
    {
        NBTTagList servers = new NBTTagList();
        for( Server entry : m_servers )
        {
            NBTTagCompound server = new NBTTagCompound();
            server.setString( "name", entry.m_name );
            server.setString( "address", entry.m_address );
            servers.appendTag( server );
        }
        nbt.setTag( "servers", servers );

        NBTTagList portals = new NBTTagList();
        for( Map.Entry<String, TileEntityQuantumComputer.PortalLocation> entry : m_portals.entrySet() )
        {
            NBTTagCompound portal = entry.getValue().encode();
            portal.setString( "id", entry.getKey() );
            portals.appendTag( portal );
        }
        nbt.setTag( "portals", portals );
    }

    public String getUnusedID()
    {
        int id = 1;
        while( m_portals.containsKey( "Gate " + id ) )
        {
            ++id;
        }
        return "Gate " + id;
    }

    public boolean register( String id, TileEntityQuantumComputer.PortalLocation portal )
    {
        if( !m_portals.containsKey( id ) )
        {
            m_portals.put( id, portal );
            //QCraft.log( "Portal " + id + " registered (now " + m_portals.size() + " total)" );
            return true;
        }
        return false;
    }

    public void unregister( String id )
    {
        if( m_portals.containsKey( id ) )
        {
            m_portals.remove( id );
            //QCraft.log( "Portal " + id + " unregistered (now " + m_portals.size() + " total)" );
        }
    }

    public TileEntityQuantumComputer.PortalLocation getPortal( String id )
    {
        return m_portals.get( id );
    }

    public void registerServer( String name, String address )
    {
        // Try to rename existing first (to avoid dupes)
        for( int i=0; i<m_servers.size(); ++i )
        {
            if( m_servers.get( i ).m_address.equals( address ) )
            {
                m_servers.get( i ).m_name = name;
                return;
            }
        }

        // Otherwise, add new entry
        m_servers.add( new Server( name, address ) );
    }

    public void unregisterServer( String address )
    {
        // Find and remove
        for( int i=0; i<m_servers.size(); ++i )
        {
            if( m_servers.get( i ).m_address.equals( address ) )
            {
                m_servers.remove( i );
                break;
            }
        }
    }

    public String getServerAddressAfter( String address )
    {
        if( address == null )
        {
            // If null is passed in, return the first result
            return m_servers.size() >= 0 ? m_servers.get(0).m_address : null;
        }
        else
        {
            // If an entry is passed in, return the result after it, or null if it's the last in the list
            for( int i=0; i<m_servers.size(); ++i )
            {
                if( m_servers.get( i ).m_address.equals( address ) )
                {
                    int indexAfter = i + 1;
                    if( indexAfter >= m_servers.size() )
                    {
                        return null;
                    }
                    else
                    {
                        return m_servers.get( indexAfter ).m_address;
                    }
                }
            }
            return null;
        }
    }

    public String getServerName( String address )
    {
        if( address != null )
        {
            for( int i=0; i<m_servers.size(); ++i )
            {
                if( m_servers.get( i ).m_address.equals( address ) )
                {
                    return m_servers.get( i ).m_name;
                }
            }
        }
        return address;
    }
}
