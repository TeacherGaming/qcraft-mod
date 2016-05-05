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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.World;

public interface IQCraftProxy
{
    public boolean isClient();
    public void preLoad();
    public void load();

    public Object getQuantumComputerGUI( InventoryPlayer inventory, TileEntityQuantumComputer computer );
    public void showItemTransferGUI( EntityPlayer player, TileEntityQuantumComputer computer );

    public void travelToServer( LostLuggage.Address address );

    public void spawnQuantumDustFX( World world, double x, double y, double z );

    public EntityPlayer getLocalPlayer();

    public boolean isPlayerWearingGoggles( EntityPlayer player );
    public boolean isPlayerWearingQuantumGoggles( EntityPlayer player );
    public boolean isLocalPlayerWearingGoggles();
    public boolean isLocalPlayerWearingQuantumGoggles();

    public void renderQuantumGogglesOverlay( float width, float height );
    public void renderAOGogglesOverlay( float width, float height );

    public World getDefWorld();
}
