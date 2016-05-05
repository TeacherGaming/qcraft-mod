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


package dan200.qcraft.server;

import dan200.qcraft.shared.LostLuggage;
import dan200.qcraft.shared.QCraftProxyCommon;
import dan200.qcraft.shared.TileEntityQuantumComputer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class QCraftProxyServer extends QCraftProxyCommon
{
    public QCraftProxyServer()
    {
    }

    // IQCraftProxy implementation

    @Override
    public void load()
    {
        super.load();
        registerForgeHandlers();
    }

    @Override
    public boolean isClient()
    {
        return false;
    }

    @Override
    public Object getQuantumComputerGUI( InventoryPlayer inventory, TileEntityQuantumComputer computer )
    {
        return null;
    }

    @Override
    public void showItemTransferGUI( EntityPlayer entityPlayer, TileEntityQuantumComputer computer )
    {
    }

    @Override
    public void travelToServer( LostLuggage.Address address )
    {
    }

    @Override
    public void spawnQuantumDustFX( World world, double x, double y, double z )
    {
    }

    @Override
    public EntityPlayer getLocalPlayer()
    {
        return null;
    }

    @Override
    public boolean isLocalPlayerWearingGoggles()
    {
        return false;
    }

    @Override
    public boolean isLocalPlayerWearingQuantumGoggles()
    {
        return false;
    }

    @Override
    public void renderQuantumGogglesOverlay( float width, float height )
    {
    }

    @Override
    public void renderAOGogglesOverlay( float width, float height )
    {
    }

    // private stuff

    private void registerForgeHandlers()
    {
    }

    @Override
    public World getDefWorld() {
        return MinecraftServer.getServer().worldServerForDimension(0); //gets the server world dim 0 handler
    }
}
