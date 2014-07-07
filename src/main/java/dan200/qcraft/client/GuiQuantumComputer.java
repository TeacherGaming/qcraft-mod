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


package dan200.qcraft.client;

import dan200.QCraft;
import dan200.qcraft.shared.ContainerQuantumComputer;
import dan200.qcraft.shared.TileEntityQuantumComputer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiQuantumComputer extends GuiContainer
{
    private static final ResourceLocation background = new ResourceLocation( "qcraft", "textures/gui/computer.png" );

    private static final int BUTTON_ENERGIZE = 1;
    private static final int BUTTON_CYCLE_SERVERS = 2;
    private static final int BUTTON_ADD_SERVER = 3;
    private static final int BUTTON_REMOVE_SERVER = 4;
    private static final int BUTTON_CONFIRM_ADD_SERVER = 5;

    private ContainerQuantumComputer m_container;
    private TileEntityQuantumComputer m_computer;
    private boolean m_addingServer;
    private boolean m_newServerAddressFieldNeedsFocus;

    private GuiButton m_energizeButton;
    private GuiButton m_energizeButton2;
    private GuiTextField m_localPortalIDField;
    private GuiButton m_changeServerButton;
    private GuiButton m_addServerButton;
    private GuiButton m_removeServerButton;
    private GuiButton m_confirmAddServerButton;
    private GuiTextField m_destinationPortalIDField;
    private GuiTextField m_newServerAddressField;

    public GuiQuantumComputer( InventoryPlayer inventoryplayer, TileEntityQuantumComputer computer )
    {
        this( inventoryplayer, computer, new ContainerQuantumComputer( inventoryplayer, computer ) );
    }

    protected GuiQuantumComputer( InventoryPlayer inventoryplayer, TileEntityQuantumComputer computer, ContainerQuantumComputer container )
    {
        super( container );
        m_container = container;
        m_computer = computer;
        m_addingServer = false;
        m_newServerAddressFieldNeedsFocus = false;

        xSize = 196;
        ySize = 8 + 20 + 8;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        Keyboard.enableRepeatEvents( true );

        int x = ( width - xSize ) / 2;
        int y = ( height - ySize ) / 2;
        m_energizeButton = new GuiButton( BUTTON_ENERGIZE, x + 8, y + 8, xSize - 16, 20, "" );
        m_energizeButton2 = new GuiButton( BUTTON_ENERGIZE, x + 8, y + 8 + 8 + 20 + 6 + 20 + 6, xSize - 16, 20, "" );

        m_localPortalIDField = new GuiTextField( fontRendererObj, x + 9, y - 20, xSize - 18, 20 );
        m_localPortalIDField.setFocused( false );
        m_localPortalIDField.setMaxStringLength( 32 );
        m_localPortalIDField.setText( encodeOptionalText( m_computer.getPortalID() ) );

        m_destinationPortalIDField = new GuiTextField( fontRendererObj, x + 9, y + 8 + 8,  xSize - 18, 20 );
        m_destinationPortalIDField.setFocused( false );
        m_destinationPortalIDField.setMaxStringLength( 32 );
        m_destinationPortalIDField.setText( encodeOptionalText( m_computer.getRemotePortalID() ) );

        m_changeServerButton = new GuiButton( BUTTON_CYCLE_SERVERS, x + 8, y + 8 + 8 + 20 + 6, xSize - 16 - 16 - 3 - 16 - 3, 20, "" );
        m_addServerButton = new GuiButton( BUTTON_ADD_SERVER, x + xSize - 8 - 16 - 3 - 16, y + 8 + 8 + 20 + 6, 16, 20, "+" );
        m_removeServerButton =  new GuiButton( BUTTON_REMOVE_SERVER, x + xSize - 8 - 16, y + 8 + 8 + 20 + 6, 16, 20, "-" );
        m_confirmAddServerButton = new GuiButton( BUTTON_CONFIRM_ADD_SERVER, x + xSize - 8 - 35, y + 8 + 8 + 20 + 6, 35, 20, I18n.format( "gui.qcraft:computer.ok" ) );

        m_newServerAddressField = new GuiTextField( fontRendererObj, x + 9, y + 8 + 8 + 20 + 7, xSize - 18 - 35 - 3, 18 );
        m_newServerAddressField.setFocused( false );
        m_newServerAddressField.setMaxStringLength( 64 );
        m_newServerAddressField.setText( encodeOptionalText( m_computer.getRemoteServerAddress() ) );

        updateStatus();
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents( false );
    }

    private void updateStatus()
    {
        boolean isTeleporter = m_container.isTeleporterPresent();
        boolean canEnergize = m_container.canEnergize();
        boolean canEdit = isTeleporter && m_container.canEdit();
        boolean canEditIP = canEdit && m_container.canEditServerAddress();
        String errorMsg = m_container.getErrorMessage();

        if( errorMsg != null )
        {
            m_energizeButton.enabled = false;
            m_energizeButton.displayString = I18n.format( errorMsg );
        }
        else
        {
            m_energizeButton.enabled = canEnergize && !m_addingServer;
            if( !m_container.isTeleporterEnergized() )
            {
                m_energizeButton.displayString = I18n.format( "gui.qcraft:computer.energize" );
            }
            else
            {
                m_energizeButton.displayString = I18n.format( "gui.qcraft:computer.deenergize" );
            }
        }
        m_energizeButton2.enabled = m_energizeButton.enabled;
        m_energizeButton2.displayString = m_energizeButton.displayString;

        if( isTeleporter )
        {
            if( buttonList.contains( m_energizeButton ) ) buttonList.remove( m_energizeButton );
            if( !buttonList.contains( m_energizeButton2 ) ) buttonList.add( m_energizeButton2 );
            if( m_addingServer )
            {
                if( buttonList.contains( m_changeServerButton ) ) buttonList.remove( m_changeServerButton );
                if( buttonList.contains( m_addServerButton ) ) buttonList.remove( m_addServerButton );
                if( buttonList.contains( m_removeServerButton ) ) buttonList.remove( m_removeServerButton );
                if( !buttonList.contains( m_confirmAddServerButton ) ) buttonList.add( m_confirmAddServerButton );
            }
            else
            {
                if( !buttonList.contains( m_changeServerButton ) ) buttonList.add( m_changeServerButton );
                if( !buttonList.contains( m_addServerButton ) ) buttonList.add( m_addServerButton );
                if( !buttonList.contains( m_removeServerButton ) ) buttonList.add( m_removeServerButton );
                if( buttonList.contains( m_confirmAddServerButton ) ) buttonList.remove( m_confirmAddServerButton );
            }
        }
        else
        {
            if( !buttonList.contains( m_energizeButton ) ) buttonList.add( m_energizeButton );
            if( buttonList.contains( m_energizeButton2 ) ) buttonList.remove( m_energizeButton2 );
            if( buttonList.contains( m_changeServerButton ) ) buttonList.remove( m_changeServerButton );
            if( buttonList.contains( m_addServerButton ) ) buttonList.remove( m_addServerButton );
            if( buttonList.contains( m_removeServerButton ) ) buttonList.remove( m_removeServerButton );
            if( buttonList.contains( m_confirmAddServerButton ) ) buttonList.remove( m_confirmAddServerButton );
        }

        m_localPortalIDField.setVisible( isTeleporter );
        m_localPortalIDField.setEnabled( canEdit );
        if( !m_localPortalIDField.isFocused() )
        {
            m_localPortalIDField.setText( encodeOptionalText( m_computer.getPortalID() ) );
        }

        m_changeServerButton.enabled = canEdit && !m_addingServer;
        String remoteServerName = m_computer.getRemoteServerName();
        if( remoteServerName != null )
        {
            int lengthLimit = 20;
            if( remoteServerName.length() <= lengthLimit )
            {
                m_changeServerButton.displayString = I18n.format( "gui.qcraft:computer.remote_server", remoteServerName );
            }
            else
            {
                m_changeServerButton.displayString = I18n.format( "gui.qcraft:computer.remote_server", remoteServerName.substring( 0, lengthLimit - 2 ) + "..." );
            }
        }
        else
        {
            m_changeServerButton.displayString = I18n.format( "gui.qcraft:computer.local_server" );
        }

        m_addServerButton.enabled = canEditIP && !m_addingServer;
        m_removeServerButton.enabled = canEditIP && !m_addingServer && (m_computer.getRemoteServerAddress() != null);
        m_confirmAddServerButton.enabled = isTeleporter && m_addingServer;

        m_destinationPortalIDField.setVisible( isTeleporter );
        m_destinationPortalIDField.setEnabled( canEdit );
        if( !m_destinationPortalIDField.isFocused() )
        {
            m_destinationPortalIDField.setText( encodeOptionalText( m_computer.getRemotePortalID() ) );
        }

        m_newServerAddressField.setVisible( isTeleporter && m_addingServer );
        m_newServerAddressField.setEnabled( isTeleporter && m_addingServer );

        if( m_newServerAddressFieldNeedsFocus )
        {
            m_localPortalIDField.setFocused( false );
            m_destinationPortalIDField.setFocused( false );
            m_newServerAddressField.setFocused( true );
            m_newServerAddressFieldNeedsFocus = false;
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        m_localPortalIDField.updateCursorCounter();
        m_destinationPortalIDField.updateCursorCounter();
        m_newServerAddressField.updateCursorCounter();
        updateStatus();
    }

    private String encodeOptionalText( String id )
    {
        if( id == null )
        {
            return "";
        }
        else
        {
            return id;
        }
    }

    private String decodeOptionalText( String text )
    {
        text = text.trim();
        if( text.isEmpty() )
        {
            return null;
        }
        return text;
    }

    private void updateRemotePortalID()
    {
        QCraft.requestSetRemotePortalID(
            m_computer,
            decodeOptionalText( m_destinationPortalIDField.getText() )
        );
    }

    private void updatePortalID()
    {
        QCraft.requestSetPortalID(
            m_computer,
            decodeOptionalText( m_localPortalIDField.getText() )
        );
    }

    @Override
    public void keyTyped( char c, int k )
    {
        if( k == 1 ) // escape
        {
            if( m_addingServer )
            {
                m_addingServer = false;
                updateStatus();
            }
            else
            {
                super.keyTyped( c, k );
            }
        }
        else if( m_localPortalIDField.isFocused() )
        {
            m_localPortalIDField.textboxKeyTyped( c, k );
            updatePortalID();
        }
        else if( m_destinationPortalIDField.isFocused() )
        {
            m_destinationPortalIDField.textboxKeyTyped( c, k );
            updateRemotePortalID();
        }
        else if( m_newServerAddressField.isFocused() )
        {
            m_newServerAddressField.textboxKeyTyped( c, k );
        }
        else
        {
            super.keyTyped( c, k );
        }
    }

    @Override
    protected void mouseClicked( int par1, int par2, int par3 )
    {
        super.mouseClicked( par1, par2, par3 );
        m_localPortalIDField.mouseClicked( par1, par2, par3 );
        m_destinationPortalIDField.mouseClicked( par1, par2, par3 );
        m_newServerAddressField.mouseClicked( par1, par2, par3 );
    }

    @Override
    protected void actionPerformed( GuiButton button )
    {
        super.actionPerformed( button );
        switch( button.id )
        {
            case BUTTON_ENERGIZE:
            {
                // Clicked "energize"
                QCraft.requestEnergize( m_computer );
                break;
            }
            case BUTTON_CYCLE_SERVERS:
            {
                // Clicked the server name to cycle between the available servers
                if( m_container.canEdit() )
                {
                    QCraft.requestCycleServerAddress( m_computer );
                }
                break;
            }
            case BUTTON_ADD_SERVER:
            {
                if( m_container.canEditServerAddress() && !m_addingServer )
                {
                    // Clicked "+" to start adding a new server
                    m_newServerAddressField.setText( "" );
                    m_addingServer = true;
                    m_newServerAddressFieldNeedsFocus = true;
                }
                break;
            }
            case BUTTON_REMOVE_SERVER:
            {
                if( m_container.canEditServerAddress() && !m_addingServer )
                {
                    // Clicked "-" to remove a server
                    QCraft.requestRemoveServerAddress(
                        m_computer
                    );
                }
                break;
            }
            case BUTTON_CONFIRM_ADD_SERVER:
            {
                if( m_addingServer )
                {
                    // Clicked "OK" to confirm new server addition
                    // Parse the server name and address in the format "address|name"
                    String name = null;
                    String address = null;
                    String both = decodeOptionalText( m_newServerAddressField.getText() );
                    if( both != null )
                    {
                        int pipeIndex = both.indexOf( "|" );
                        if( pipeIndex >= 0 )
                        {
                            address = decodeOptionalText( both.substring( 0, pipeIndex ) );
                            name = decodeOptionalText( both.substring( pipeIndex + 1 ) );
                            if( name == null )
                            {
                                name = address;
                            }
                            if( address == null )
                            {
                                name = null;
                            }
                        }
                        else
                        {
                            name = both;
                            address = both;
                        }
                    }

                    // Send it to the server
                    QCraft.requestSetNewServerAddress( m_computer, name, address );
                    m_addingServer = false;
                }
                break;
            }
        }
    }

    @Override
    public void drawScreen( int i, int j, float f )
    {
        super.drawScreen( i, j, f );
        m_localPortalIDField.drawTextBox();
        m_destinationPortalIDField.drawTextBox();
        m_newServerAddressField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer( int i, int j )
    {
        super.drawGuiContainerForegroundLayer( i, j );
        if( m_container.isTeleporterPresent() )
        {
            String text = I18n.format( "gui.qcraft:computer.local_portal" );
            fontRendererObj.drawString( text, 9, -20 - 3 - 8, 0x404040 );

            String text2 = I18n.format( "gui.qcraft:computer.remote_portal" );
            fontRendererObj.drawString( text2, 9, 5, 0x404040 );
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
    {
        GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
        this.mc.renderEngine.bindTexture( background );
        int x = ( width - xSize ) / 2;
        int y = ( height - ySize ) / 2;

        if( !m_container.isTeleporterPresent() )
        {
            drawTexturedModalRect( x, y, 0, 0, xSize, ySize );
        }
        else
        {
            drawTexturedModalRect( x, y - 20 - 3 - 8 - 8, 0, 8 + 20 + 8, xSize, 8 + 8 + 3 + 20 + 8 + 8 + 20 + 6 + 20 + 6 + 20 + 8 );
        }
    }
}
