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
import dan200.qcraft.shared.TileEntityQuantumComputer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiItemTransfer extends GuiScreen
{
    public TileEntityQuantumComputer m_computer;
    public String m_destinationServer;
    public String m_destinationServerName;

    public GuiItemTransfer( TileEntityQuantumComputer computer )
    {
        m_computer = computer;
        m_destinationServer = m_computer.getRemoteServerAddress();
        m_destinationServerName = m_computer.getRemoteServerName();
    }

    @Override
    public void initGui()
    {
        this.buttonList.add( new GuiOptionButton(0, this.width / 2 - 155, this.height / 6 + 96, I18n.format( "gui.yes" )) );
        this.buttonList.add( new GuiOptionButton(1, this.width / 2 - 155 + 160, this.height / 6 + 96, I18n.format( "gui.no" )) );
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();

        // Close GUI if the remote server address changes while we're using it
        String destinationServer = m_computer.getRemoteServerAddress();
        if( m_destinationServer == null || destinationServer == null || !destinationServer.equals( m_destinationServer ) )
        {
            this.mc.displayGuiScreen( null );
        }
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        QCraft.requestConfirmGoToServer( m_computer, m_destinationServer, (par1GuiButton.id == 0) );
        this.mc.displayGuiScreen( null );
    }

    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
        this.drawDefaultBackground();
        this.drawCenteredString( this.fontRendererObj, I18n.format( "gui.qcraft:item_transfer.line1", m_destinationServerName ), this.width / 2, 70, 16777215 );
        this.drawCenteredString( this.fontRendererObj, I18n.format( "gui.qcraft:item_transfer.line2", m_destinationServerName ), this.width / 2, 90, 16777215 );
        super.drawScreen( par1, par2, par3 );
    }
}
