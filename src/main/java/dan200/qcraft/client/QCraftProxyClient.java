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
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dan200.QCraft;
import dan200.qcraft.shared.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

public class QCraftProxyClient extends QCraftProxyCommon
{
    private static final ResourceLocation QUANTUM_GOGGLE_HUD = new ResourceLocation( "qcraft", "textures/gui/goggles.png" );
    private static final ResourceLocation AO_GOGGLE_HUD = new ResourceLocation( "qcraft", "textures/gui/ao_goggles.png" );

    private long m_tickCount;
    private RenderBlocks m_renderBlocks;

    public QCraftProxyClient()
    {
        m_tickCount = 0;
    }

    // IQCraftProxy implementation

    @Override
    public void load()
    {
        ItemQuantumGoggles.s_renderIndex = RenderingRegistry.addNewArmourRendererPrefix( "qcraft:goggles" );

        super.load();

        // Setup renderers
        int gateID = RenderingRegistry.getNextAvailableRenderId();
        QCraft.Blocks.quantumLogic.blockRenderID = gateID;

        m_renderBlocks = new RenderBlocks();
        QCraft.Blocks.qBlock.blockRenderID = RenderingRegistry.getNextAvailableRenderId();

        // Setup client forge handlers
        registerForgeHandlers();
    }

    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    public Object getQuantumComputerGUI( InventoryPlayer inventory, TileEntityQuantumComputer computer )
    {
        return new GuiQuantumComputer( inventory, computer );
    }

    @Override
    public void showItemTransferGUI( EntityPlayer entityPlayer, TileEntityQuantumComputer computer )
    {
        if( Minecraft.getMinecraft().currentScreen == null )
        {
            FMLClientHandler.instance().displayGuiScreen( entityPlayer, new GuiItemTransfer( computer ) );
        }
    }

    @Override
    public void travelToServer( LostLuggage.Address address )
    {
        // Disconnect from current server
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.theWorld.sendQuittingDisconnectingPacket();
        minecraft.loadWorld((WorldClient)null);
        minecraft.displayGuiScreen( new GuiTravelStandby( address ) );
    }

    @Override
    public void spawnQuantumDustFX( World world, double x, double y, double z )
    {
        Minecraft mc = Minecraft.getMinecraft();
        double dx = mc.renderViewEntity.posX - x;
        double dy = mc.renderViewEntity.posY - y;
        double dz = mc.renderViewEntity.posZ - z;
        if( dx * dx + dy * dy + dz * dz < 16.0 * 16.0 )
        {
            EntityFX fx = new EntityQuantumDustFX( world, x, y, z, 1.0f );
            mc.effectRenderer.addEffect( fx );
        }
    }

    @Override
    public EntityPlayer getLocalPlayer()
    {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public boolean isLocalPlayerWearingGoggles()
    {
        EntityPlayer player = getLocalPlayer();
        if( player != null )
        {
            return isPlayerWearingGoggles( player );
        }
        return false;
    }

    @Override
    public boolean isLocalPlayerWearingQuantumGoggles()
    {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if( player != null )
        {
            return isPlayerWearingQuantumGoggles( player );
        }
        return false;
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
        FMLCommonHandler.instance().bus().register( handlers );

        // Logic gate rendering
        QuantumLogicBlockRenderingHandler logicHandler = new QuantumLogicBlockRenderingHandler();
        RenderingRegistry.registerBlockHandler( logicHandler );

        // qBlock rendering
        QBlockRenderingHandler qBlockHandler = new QBlockRenderingHandler();
        MinecraftForgeClient.registerItemRenderer( Item.getItemFromBlock( QCraft.Blocks.qBlock ), qBlockHandler );
        RenderingRegistry.registerBlockHandler( qBlockHandler );
    }

    @Override
    public World getDefWorld() {
        return Minecraft.getMinecraft().getIntegratedServer().worldServerForDimension(0); //gets the client world dim 0 handler
    }

    public class ForgeHandlers
    {
        public ForgeHandlers()
        {
        }

        // Forge/FML event responses

        @SubscribeEvent
        public void handleTick( TickEvent.ClientTickEvent clientTickEvent )
        {
            if( clientTickEvent.phase == TickEvent.Phase.START )
            {
                m_tickCount++;
            }

            if( QCraft.travelNextTick != null )
            {
                travelToServer( QCraft.travelNextTick );
                QCraft.travelNextTick = null;
            }
        }
    }

    private class QuantumLogicBlockRenderingHandler implements
            ISimpleBlockRenderingHandler
    {
        public QuantumLogicBlockRenderingHandler()
        {
        }

        // ISimpleBlockRenderingHandler implementation

        @Override
        public boolean shouldRender3DInInventory( int modelID )
        {
            return false;
        }

        @Override
        public int getRenderId()
        {
            return QCraft.Blocks.quantumLogic.blockRenderID;
        }

        @Override
        public boolean renderWorldBlock( IBlockAccess world, int i, int j, int k, Block block, int modelID, RenderBlocks renderblocks )
        {
            if( modelID == QCraft.Blocks.quantumLogic.blockRenderID )
            {
                int metadata = world.getBlockMetadata( i, j, k );
                int direction = BlockDirectional.getDirection( metadata );
                int subType = ( (BlockQuantumLogic) block ).getSubType( metadata );

                // Draw Base
                switch( direction )
                {
                    case 0:
                        renderblocks.uvRotateTop = 0;
                        break;
                    case 1:
                        renderblocks.uvRotateTop = 1;
                        break;
                    case 2:
                        renderblocks.uvRotateTop = 3;
                        break;
                    case 3:
                        renderblocks.uvRotateTop = 2;
                        break;
                }
                renderblocks.setRenderBoundsFromBlock( block );
                renderblocks.renderStandardBlock( block, i, j, k );
                renderblocks.uvRotateTop = 0;

                return true;
            }
            return false;
        }

        @Override
        public void renderInventoryBlock( Block block, int metadata, int modelID, RenderBlocks renderblocks )
        {
        }
    }

    private class QBlockRenderingHandler implements
            IItemRenderer, ISimpleBlockRenderingHandler
    {
        public QBlockRenderingHandler()
        {
        }

        // IItemRenderer implementation

        @Override
        public boolean handleRenderType( ItemStack item, IItemRenderer.ItemRenderType type )
        {
            switch( type )
            {
                case ENTITY:
                case EQUIPPED:
                case EQUIPPED_FIRST_PERSON:
                case INVENTORY:
                {
                    return true;
                }
                case FIRST_PERSON_MAP:
                default:
                {
                    return false;
                }
            }
        }

        @Override
        public boolean shouldUseRenderHelper( IItemRenderer.ItemRenderType type, ItemStack item, IItemRenderer.ItemRendererHelper helper )
        {
            switch( helper )
            {
                case ENTITY_ROTATION:
                case ENTITY_BOBBING:
                case EQUIPPED_BLOCK:
                case BLOCK_3D:
                case INVENTORY_BLOCK:
                {
                    return true;
                }
                default:
                {
                    return false;
                }
            }
        }

        @Override
        public void renderItem( ItemRenderType type, ItemStack item, Object[] data )
        {
            switch( type )
            {
                case INVENTORY:
                case ENTITY:
                {
                    GL11.glPushMatrix();
                    GL11.glTranslatef( -0.5f, -0.5f, -0.5f );
                    QCraft.Blocks.qBlock.setBlockBounds( 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f );
                    m_renderBlocks.setRenderBoundsFromBlock( QCraft.Blocks.qBlock );
                    renderInventoryQBlock( m_renderBlocks, QCraft.Blocks.qBlock, item );
                    GL11.glPopMatrix();
                    break;
                }
                case EQUIPPED_FIRST_PERSON:
                case EQUIPPED:
                {
                    GL11.glPushMatrix();
                    QCraft.Blocks.qBlock.setBlockBounds( 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f );
                    m_renderBlocks.setRenderBoundsFromBlock( QCraft.Blocks.qBlock );
                    renderInventoryQBlock( m_renderBlocks, QCraft.Blocks.qBlock, item );
                    GL11.glPopMatrix();
                    break;
                }
                default:
                {
                    break;
                }
            }
        }

        // ISimpleBlockRenderingHandler implementation

        @Override
        public boolean shouldRender3DInInventory( int modelID )
        {
            return true;
        }

        @Override
        public int getRenderId()
        {
            return QCraft.Blocks.qBlock.blockRenderID;
        }

        @Override
        public boolean renderWorldBlock( IBlockAccess world, int i, int j, int k, Block block, int modelID, RenderBlocks renderblocks )
        {
            if( modelID == getRenderId() && block == QCraft.Blocks.qBlock )
            {
                QCraft.Blocks.qBlock.s_forceGrass = ( QCraft.Blocks.qBlock.getImpostorBlock( world, i, j, k ) == Blocks.grass );
                QCraft.Blocks.qBlock.setBlockBounds( 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f );
                renderblocks.setRenderBoundsFromBlock( QCraft.Blocks.qBlock );
                renderblocks.renderStandardBlock( QCraft.Blocks.qBlock, i, j, k );
                QCraft.Blocks.qBlock.s_forceGrass = false;
                return true;
            }
            return false;
        }

        @Override
        public void renderInventoryBlock( Block block, int metadata, int modelID, RenderBlocks renderblocks )
        {
            // IItemRenderer handles this
        }
    }

    private void renderInventoryQBlock( RenderBlocks renderblocks, BlockQBlock block, ItemStack item )
    {
        int[] types = ItemQBlock.getTypes( item );
        int type = cycleType( types );
        if( type < 0 )
        {
            renderInventoryQBlock( renderblocks, block, 0, BlockQBlock.Appearance.Fuzz );
        }
        else
        {
            renderInventoryQBlock( renderblocks, block, type, BlockQBlock.Appearance.Block );
        }
    }

    private int cycleType( int[] types )
    {
        int type = -99;
        int cycle = (int) ( m_tickCount % ( 6 * 20 ) );
        int subcycle = ( cycle % 20 );
        if( subcycle > 5 )
        {
            type = types[ cycle / 20 ];
        }
        return type;
    }

    private void bindColor( int c )
    {
        float r = (float) ( c >> 16 & 255 ) / 255.0F;
        float g = (float) ( c >> 8 & 255 ) / 255.0F;
        float b = (float) ( c & 255 ) / 255.0F;
        GL11.glColor4f( r, g, b, 1.0f );
    }

    private void renderInventoryQBlock( RenderBlocks renderblocks, BlockQBlock block, int type, BlockQBlock.Appearance appearance )
    {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 0, type ) );
        tessellator.setNormal( 0.0F, -1F, 0.0F );
        renderblocks.renderFaceYNeg( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 0, type, appearance ) );
        tessellator.draw();

        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 1, type ) );
        tessellator.setNormal( 0.0F, 1.0F, 0.0F );
        renderblocks.renderFaceYPos( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 1, type, appearance ) );
        tessellator.draw();

        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 2, type ) );
        tessellator.setNormal( 0.0F, 0.0F, -1F );
        renderblocks.renderFaceZNeg( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 2, type, appearance ) );
        tessellator.draw();

        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 3, type ) );
        tessellator.setNormal( 0.0F, 0.0F, 1.0F );
        renderblocks.renderFaceZPos( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 3, type, appearance ) );
        tessellator.draw();

        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 4, type ) );
        tessellator.setNormal( -1F, 0.0F, 0.0F );
        renderblocks.renderFaceXNeg( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 4, type, appearance ) );
        tessellator.draw();

        tessellator.startDrawingQuads();
        bindColor( block.getColorForType( 5, type ) );
        tessellator.setNormal( 1.0F, 0.0F, 0.0F );
        renderblocks.renderFaceXPos( block, 0.0D, 0.0D, 0.0D, block.getIconForType( 5, type, appearance ) );
        tessellator.draw();
    }

    @Override
    public void renderQuantumGogglesOverlay( float width, float height )
    {
        renderOverlay( QUANTUM_GOGGLE_HUD, width, height );
    }

    @Override
    public void renderAOGogglesOverlay( float width, float height )
    {
        renderOverlay( AO_GOGGLE_HUD, width, height );
    }

    private void renderOverlay( ResourceLocation texture, float width, float height )
    {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glDisable( GL11.GL_DEPTH_TEST );
        GL11.glDepthMask( false );
        GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
        GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
        GL11.glDisable( GL11.GL_ALPHA_TEST );
        mc.renderEngine.bindTexture( texture );
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV( 0.0D, (double) height, -90.0D, 0.0D, 1.0D );
        tessellator.addVertexWithUV( (double) width, (double) height, -90.0D, 1.0D, 1.0D );
        tessellator.addVertexWithUV( (double) width, 0.0D, -90.0D, 1.0D, 0.0D );
        tessellator.addVertexWithUV( 0.0D, 0.0D, -90.0D, 0.0D, 0.0D );
        tessellator.draw();
        GL11.glDepthMask( true );
        GL11.glEnable( GL11.GL_DEPTH_TEST );
        GL11.glEnable( GL11.GL_ALPHA_TEST );
        GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
    }
}
