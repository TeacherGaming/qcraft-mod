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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EncryptionRegistry
{
    public static final EncryptionRegistry Instance = new EncryptionRegistry();

    // Privates
    private KeyPair m_localKeyPair;
    private Set<PublicKey> m_verifiedPublicKeys; // The public keys of all the servers we trust players from
    private Set<UUID> m_receivedLuggageIDs; // The UUID of every message we've ever received, to prevent repeat attacks

    // Methods
    public EncryptionRegistry()
    {
        reset();
    }

    public void reset()
    {
        m_localKeyPair = generateKeyPair();
        m_verifiedPublicKeys = new HashSet<PublicKey>();
        m_receivedLuggageIDs = new HashSet<UUID>();
    }

    public void readFromNBT( NBTTagCompound nbt )
    {
        PublicKey localPublicKey = decodePublicKey( nbt.getByteArray( "localPublicKey" ) );
        PrivateKey localPrivateKey = decodePrivateKey( nbt.getByteArray( "localPrivateKey" ) );
        m_localKeyPair = new KeyPair( localPublicKey, localPrivateKey );

        if( nbt.hasKey( "verifiedPublicKeys" ) )
        {
            NBTTagList verifiedPublicKeys = nbt.getTagList( "verifiedPublicKeys", 10 );
            for( int i=0; i<verifiedPublicKeys.tagCount(); ++i )
            {
                NBTTagCompound key = verifiedPublicKeys.getCompoundTagAt( i );
                m_verifiedPublicKeys.add( decodePublicKey( key.getByteArray( "publicKey" ) ) );
            }
        }

        if( nbt.hasKey( "receivedLuggageIDs" ) )
        {
            NBTTagList receivedLuggageIDs = nbt.getTagList( "receivedLuggageIDs", 10 );
            for( int i=0; i<receivedLuggageIDs.tagCount(); ++i )
            {
                NBTTagCompound key = receivedLuggageIDs.getCompoundTagAt( i );
                m_receivedLuggageIDs.add( UUID.fromString( key.getString( "uuid" ) ) );
            }
        }
    }

    public void writeToNBT( NBTTagCompound nbt )
    {
        nbt.setByteArray( "localPublicKey", encodePublicKey( m_localKeyPair.getPublic() ) );
        nbt.setByteArray( "localPrivateKey", encodePrivateKey( m_localKeyPair.getPrivate() ) );

        NBTTagList knownPublicKeys = new NBTTagList();
        for( PublicKey publicKey : m_verifiedPublicKeys )
        {
            NBTTagCompound key = new NBTTagCompound();
            key.setByteArray( "publicKey", encodePublicKey( publicKey ) );
            knownPublicKeys.appendTag( key );
        }
        nbt.setTag( "verifiedPublicKeys", knownPublicKeys );

        NBTTagList receivedLuggageIDs = new NBTTagList();
        for( UUID uuid : m_receivedLuggageIDs )
        {
            NBTTagCompound key = new NBTTagCompound();
            key.setString( "uuid", uuid.toString() );
            receivedLuggageIDs.appendTag( key );
        }
        nbt.setTag( "receivedLuggageIDs", receivedLuggageIDs );
    }

    public KeyPair getLocalKeyPair()
    {
        return m_localKeyPair;
    }

    public Set<PublicKey> getVerifiedPublicKeys()
    {
        return m_verifiedPublicKeys;
    }

    public Set<UUID> getReceivedLuggageIDs()
    {
        return m_receivedLuggageIDs;
    }

    public byte[] encodePublicKey( PublicKey key )
    {
        return new X509EncodedKeySpec( key.getEncoded() ).getEncoded();
    }

    public PublicKey decodePublicKey( byte[] encodedKey )
    {
        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance( "DSA" );
            return keyFactory.generatePublic( new X509EncodedKeySpec( encodedKey ) );
        }
        catch( Exception e )
        {
            System.out.println( "QCraft: decoding key failed with exception: " + e.toString() );
            return null;
        }
    }

    public byte[] encodePrivateKey( PrivateKey key )
    {
        return new PKCS8EncodedKeySpec( key.getEncoded() ).getEncoded();
    }

    public PrivateKey decodePrivateKey( byte[] encodedKey )
    {
        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance( "DSA" );
            return keyFactory.generatePrivate( new PKCS8EncodedKeySpec( encodedKey ) );
        }
        catch( Exception e )
        {
            QCraft.log( "QCraft: Decoding key failed with exception: " + e.toString() );
            return null;
        }
    }

    public byte[] signData( byte[] message )
    {
        try
        {
            Signature signature = Signature.getInstance( "SHA1withDSA", "SUN" );        // generate a signature
            signature.initSign( m_localKeyPair.getPrivate() );
            signature.update( message );
            return signature.sign();
        }
        catch( Exception ex )
        {
            QCraft.log( "QCraft: Signing data failed with exception: " + ex.toString() );
        }
        return null;
    }

    public boolean verifyData( PublicKey verifyKey, byte[] digest, byte[] message )
    {
        try
        {
            // verify a signature
            Signature signature = Signature.getInstance( "SHA1withDSA", "SUN" );
            signature.initVerify( verifyKey );
            signature.update( message );

            if( signature.verify( digest ) )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch( Exception e )
        {
            QCraft.log( "QCraft: Verifying data failed with exception: " + e.toString() );
        }
        return false;
    }

    private static KeyPair generateKeyPair()
    {
        try
        {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "DSA", "SUN" );
            SecureRandom random = SecureRandom.getInstance( "SHA1PRNG", "SUN" );
            keyGen.initialize( 1024, random );
            return keyGen.generateKeyPair();
        }
        catch( Exception ex )
        {
            QCraft.log( "QCraft: Generating keypair failed with exception: " + ex.toString() );
        }
        return null;
    }
}
