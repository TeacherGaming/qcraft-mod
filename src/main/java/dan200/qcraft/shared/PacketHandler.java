/**
 * This file is part of qCraft - http://www.qcraft.org Copyright Daniel Ratcliffe and
 * TeacherGaming LLC, 2013. Do not distribute without permission. Send enquiries
 * to dratcliffe@gmail.com
 */
package dan200.qcraft.shared;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import dan200.QCraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

public class PacketHandler
{
    @SubscribeEvent
    public void onClientPacket( FMLNetworkEvent.ClientCustomPacketEvent event )
    {
        try
        {
            QCraftPacket packet = new QCraftPacket();
            packet.fromBytes( event.packet.payload() );
            QCraft.handleClientPacket( packet );
        }
        catch( Exception e )
        {
            // Something failed, ignore it
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onServerPacket( FMLNetworkEvent.ServerCustomPacketEvent event )
    {
        try
        {
            QCraftPacket packet = new QCraftPacket();
            packet.fromBytes( event.packet.payload() );
            QCraft.handleServerPacket( packet, ((NetHandlerPlayServer)event.handler).playerEntity );
        }
        catch( Exception e )
        {
            // Something failed, ignore it
            e.printStackTrace();
        }
    }
}
