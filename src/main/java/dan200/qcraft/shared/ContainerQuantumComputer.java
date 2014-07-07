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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;

public class ContainerQuantumComputer extends Container
{
    public static final int PROGRESS_ERRORMSG = 0;
    public static final int PROGRESS_IS_TELEPORTER = 1;
    public static final int PROGRESS_IS_TELEPORTER_ENERGIZED = 2;
    public static final int PROGRESS_CAN_EDIT = 3;
    public static final int PROGRESS_CAN_EDIT_IP = 4;

    private TileEntityQuantumComputer m_computer;
    private TileEntityQuantumComputer.TeleportError m_errorMessage;
    private boolean m_isTeleporter;
    private boolean m_isTeleporterEnergized;
    private boolean m_canEdit;
    private boolean m_canEditIPAddress;

    public ContainerQuantumComputer( InventoryPlayer inventory, TileEntityQuantumComputer computer )
    {
        m_computer = computer;
        m_errorMessage = TileEntityQuantumComputer.TeleportError.Ok;
        m_isTeleporter = false;
        m_isTeleporterEnergized = false;
        m_canEdit = false;
        m_canEditIPAddress = false;
    }

    @Override
    public boolean canInteractWith( EntityPlayer entityplayer )
    {
        return m_computer.getDistanceFrom( entityplayer.posX, entityplayer.posY, entityplayer.posZ ) <= (8.0 * 8.0);
    }

    @Override
    public void addCraftingToCrafters( ICrafting icrafting )
    {
        super.addCraftingToCrafters( icrafting );
        icrafting.sendProgressBarUpdate( this, PROGRESS_ERRORMSG, m_errorMessage.ordinal() );

        boolean canEdit = false;
        boolean canEditIPAddress = false;
        if( icrafting instanceof EntityPlayer )
        {
            EntityPlayer player = (EntityPlayer)icrafting;
            canEdit = QCraft.canPlayerCreatePortals( player );
            canEditIPAddress = QCraft.canPlayerEditPortalServers( player );
        }
        icrafting.sendProgressBarUpdate( this, PROGRESS_IS_TELEPORTER, m_isTeleporter ? 1 : 0 );
        icrafting.sendProgressBarUpdate( this, PROGRESS_IS_TELEPORTER_ENERGIZED, m_isTeleporterEnergized ? 1 : 0 );
        icrafting.sendProgressBarUpdate( this, PROGRESS_CAN_EDIT, canEdit ? 1 : 0 );
        icrafting.sendProgressBarUpdate( this, PROGRESS_CAN_EDIT_IP, canEditIPAddress ? 1 : 0 );
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        TileEntityQuantumComputer.TeleportError error = m_computer.canEnergize();
        boolean isTeleporter = m_computer.isTeleporter();
        boolean isTeleporterEnergized = m_computer.isTeleporterEnergized();
        for( int i = 0; i < crafters.size(); ++i )
        {
            ICrafting icrafting = (ICrafting) crafters.get( i );
            if( error != m_errorMessage )
            {
                icrafting.sendProgressBarUpdate( this, PROGRESS_ERRORMSG, error.ordinal() );
            }
            if( isTeleporter != m_isTeleporter )
            {
                icrafting.sendProgressBarUpdate( this, PROGRESS_IS_TELEPORTER, isTeleporter ? 1 : 0 );
            }
            if( isTeleporterEnergized != m_isTeleporterEnergized )
            {
                icrafting.sendProgressBarUpdate( this, PROGRESS_IS_TELEPORTER_ENERGIZED, isTeleporterEnergized ? 1 : 0 );
            }
        }
        m_errorMessage = error;
        m_isTeleporter = isTeleporter;
        m_isTeleporterEnergized = isTeleporterEnergized;
    }

    @Override
    public void updateProgressBar( int i, int j )
    {
        switch( i )
        {
            case PROGRESS_ERRORMSG:
            {
                m_errorMessage = TileEntityQuantumComputer.TeleportError.values()[ j ];
                break;
            }
            case PROGRESS_IS_TELEPORTER:
            {
                m_isTeleporter = (j > 0);
                break;
            }
            case PROGRESS_IS_TELEPORTER_ENERGIZED:
            {
                m_isTeleporterEnergized = (j > 0);
                break;
            }
            case PROGRESS_CAN_EDIT:
            {
                m_canEdit = (j > 0);
                break;
            }
            case PROGRESS_CAN_EDIT_IP:
            {
                m_canEditIPAddress = (j > 0);
                break;
            }
        }
    }

    public String getErrorMessage()
    {
        return TileEntityQuantumComputer.TeleportError.decode( m_errorMessage );
    }

    public boolean isTeleporterPresent()
    {
        return m_isTeleporter;
    }

    public boolean isTeleporterEnergized()
    {
        return m_isTeleporterEnergized;
    }

    public boolean canEdit()
    {
        return m_canEdit && !m_isTeleporterEnergized;
    }

    public boolean canEditServerAddress()
    {
        return m_canEditIPAddress && !m_isTeleporterEnergized;
    }

    public boolean canEnergize()
    {
        return m_canEdit || !m_isTeleporter;
    }
}
