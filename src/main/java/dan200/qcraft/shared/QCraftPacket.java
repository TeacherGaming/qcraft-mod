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

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;

public class QCraftPacket implements IMessage
{
    // Static Packet types
    public static final byte EnergizeComputer = 1;
    public static final byte SetComputerRemotePortalID = 2;
    public static final byte SetComputerPortalID = 3;
    public static final byte GoToServer = 4;
    public static final byte RequestLuggage = 5;
    public static final byte UnpackLuggage = 6;
    public static final byte CycleServerAddress = 7;
    public static final byte SetNewServerAddress = 8;
    public static final byte RemoveServerAddress = 9;
    public static final byte QueryGoToServer = 10;
    public static final byte ConfirmGoToServer = 11;
    public static final byte DiscardLuggage = 12;

    // Packet class
    public byte packetType;
    public String[] dataString;
    public int[] dataInt;
    public byte[][] dataByte;

    public QCraftPacket()
    {
        packetType = 0;
        dataString = null;
        dataInt = null;
        dataByte = null;
    }

    @Override
    public void toBytes( ByteBuf buffer )
    {
        buffer.writeByte( packetType );
        if( dataString != null )
        {
            buffer.writeByte( dataString.length );
        }
        else
        {
            buffer.writeByte( 0 );
        }
        if( dataInt != null )
        {
            buffer.writeByte( dataInt.length );
        }
        else
        {
            buffer.writeByte( 0 );
        }
        if( dataByte != null )
        {
            buffer.writeInt( dataByte.length );
        }
        else
        {
            buffer.writeInt( 0 );
        }
        if( dataString != null )
        {
            for( String s : dataString )
            {
                if( s != null )
                {
                    try
                    {
                        byte[] b = s.getBytes( "UTF-8" );
                        buffer.writeBoolean( true );
                        buffer.writeInt( b.length );
                        buffer.writeBytes( b );
                    }
                    catch( UnsupportedEncodingException e )
                    {
                        buffer.writeBoolean( false );
                    }
                }
                else
                {
                    buffer.writeBoolean( false );
                }
            }
        }
        if( dataInt != null )
        {
            for( int i : dataInt )
            {
                buffer.writeInt( i );
            }
        }
        if( dataByte != null )
        {
            for( byte[] bytes : dataByte )
            {
                if( bytes != null )
                {
                    buffer.writeInt( bytes.length );
                    buffer.writeBytes( bytes );
                }
                else
                {
                    buffer.writeInt( 0 );
                }
            }
        }
    }

    @Override
    public void fromBytes( ByteBuf buffer )
    {
        packetType = buffer.readByte();
        byte nString = buffer.readByte();
        byte nInt = buffer.readByte();
        int nByte = buffer.readInt();
        if( nString == 0 )
        {
            dataString = null;
        }
        else
        {
            dataString = new String[ nString ];
            for( int k = 0; k < nString; k++ )
            {
                if( buffer.readBoolean() )
                {
                    int len = buffer.readInt();
                    byte[] b = new byte[len];
                    buffer.readBytes( b );
                    try
                    {
                        dataString[ k ] = new String( b, "UTF-8" );
                    }
                    catch( UnsupportedEncodingException e )
                    {
                        dataString[ k ] = null;
                    }
                }
            }
        }
        if( nInt == 0 )
        {
            dataInt = null;
        }
        else
        {
            dataInt = new int[ nInt ];
            for( int k = 0; k < nInt; k++ )
            {
                dataInt[ k ] = buffer.readInt();
            }
        }
        if( nByte == 0 )
        {
            dataByte = null;
        }
        else
        {
            dataByte = new byte[ nByte ][];
            for( int k = 0; k < nByte; k++ )
            {
                int length = buffer.readInt();
                if( length > 0 )
                {
                    dataByte[ k ] = new byte[ length ];
                    buffer.getBytes( buffer.readerIndex(), dataByte[ k ] );
                }
            }
        }
    }
}
