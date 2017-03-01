package com.elytradev.movingworld.common.experiments.network.messages.client;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.field.MarshalledAs;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.movingworld.common.experiments.entity.EntityMobileRegion;
import com.elytradev.movingworld.common.experiments.network.MovingWorldExperimentsNetworking;
import com.elytradev.movingworld.common.network.marshallers.EntityMarshaller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;


@ReceivedOn(Side.SERVER)
public class MessageRequestData extends Message {

    @MarshalledAs(EntityMarshaller.MARSHALLER_NAME)
    public EntityMobileRegion regionEntity;

    public MessageRequestData(NetworkContext ctx) {
        super(ctx);
    }

    public MessageRequestData(EntityMobileRegion regionEntity) {
        super(MovingWorldExperimentsNetworking.networkContext);
        this.regionEntity = regionEntity;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (regionEntity != null) {
            // return to sender, address unknown.
            for (int cX = regionEntity.region.regionMin.chunkXPos; cX < regionEntity.region.regionMax.chunkXPos; cX++) {
                for (int cZ = regionEntity.region.regionMin.chunkZPos; cZ < regionEntity.region.regionMax.chunkZPos; cZ++) {
                    WorldServer worldServer = ((WorldServer) regionEntity.getParentWorld());
                    worldServer.playerChunkMap.getOrCreateEntry(cX, cZ).addPlayer((EntityPlayerMP) sender);
                }
            }
        }
    }
}