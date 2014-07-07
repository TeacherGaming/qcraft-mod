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

import cpw.mods.fml.client.FMLClientHandler;
import dan200.qcraft.shared.LostLuggage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;

public class GuiTravelStandby extends GuiScreen
{
    private final LostLuggage.Address m_destination;
    private int m_ticks;

    public GuiTravelStandby( LostLuggage.Address destination )
    {
        m_destination = destination;
        m_ticks = 0;
    }

    @Override
    public void initGui()
    {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + 12, I18n.format("gui.cancel", new Object[0])));
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();

        m_ticks++;
        if( m_ticks == 30 )
        {
            ServerData serverData = new ServerData( "qCraft Transfer", m_destination.getAddress() );
            FMLClientHandler.instance().setupServerList();
            FMLClientHandler.instance().connectToServer( new GuiMainMenu(), serverData );
        }
    }

    @Override
    protected void actionPerformed( GuiButton button )
    {
        if( button.id == 0 )
        {
            this.mc.displayGuiScreen( new GuiMainMenu() );
        }
    }

    @Override
    public void drawScreen( int par1, int par2, float par3 )
    {
        this.drawDefaultBackground();
        this.drawCenteredString( this.fontRendererObj, I18n.format( "gui.qcraft:standby.line1" ), this.width / 2, this.height / 2 - 50, 16777215 );
        super.drawScreen( par1, par2, par3 );
    }
}
