package darkevilmac.movingworld.common.entity;

import darkevilmac.movingworld.common.chunk.ChunkIO;
import darkevilmac.movingworld.common.chunk.mobilechunk.MobileChunkServer;
import darkevilmac.movingworld.common.network.MovingWorldNetworking;
import darkevilmac.movingworld.common.tile.TileMovingWorldMarkingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public abstract class MovingWorldHandlerServer extends MovingWorldHandlerCommon {
    protected boolean firstChunkUpdate;

    public MovingWorldHandlerServer(EntityMovingWorld entitymovingWorld) {
        super(entitymovingWorld);
        firstChunkUpdate = true;
    }

    @Override
    public boolean interact(EntityPlayer player, ItemStack stack, EnumHand hand) {
        if (getMovingWorld().getControllingPassenger() == null) {
            player.startRiding(getMovingWorld());
            return true;
        } else if (player.getControllingPassenger() == null) {
            return getMovingWorld().getCapabilities().mountEntity(player);
        }

        return false;
    }

    private MobileChunkServer getMobileChunkServer() {
        if (this.getMovingWorld() != null && this.getMovingWorld().getMobileChunk() != null && this.getMovingWorld().getMobileChunk().side().isServer())
            return (MobileChunkServer) this.getMovingWorld().getMobileChunk();
        else
            return null;
    }

    @Override
    public void onChunkUpdate() {
        super.onChunkUpdate();
        if (getMobileChunkServer() != null) {
            if (!firstChunkUpdate) {
                if (!getMobileChunkServer().getBlockQueue().isEmpty())
                    MovingWorldNetworking.NETWORK.send().packet("ChunkBlockUpdateMessage")
                            .with("dimID", getMovingWorld().worldObj.provider.getDimension())
                            .with("entityID", getMovingWorld().getEntityId())
                            .with("chunk", ChunkIO.writeCompressed(getMovingWorld().getMobileChunk(), getMobileChunkServer().getBlockQueue()))
                            .toAllAround(getMovingWorld().worldObj, getMovingWorld(), 64D);

                if(!getMobileChunkServer().getTileQueue().isEmpty()){
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    NBTTagList list = new NBTTagList();
                    for (BlockPos tilePosition : getMobileChunkServer().getTileQueue()) {
                        NBTTagCompound nbt = new NBTTagCompound();
                        if(getMobileChunkServer().getTileEntity(tilePosition) == null)
                            continue;

                        TileEntity te = getMobileChunkServer().getTileEntity(tilePosition);
                        if (te instanceof TileMovingWorldMarkingBlock) {
                            ((TileMovingWorldMarkingBlock) te).writeNBTForSending(nbt);
                        } else {
                            te.writeToNBT(nbt);
                        }
                        list.appendTag(nbt);
                    }
                    tagCompound.setTag("list", list);

                    MovingWorldNetworking.NETWORK.send().packet("TileEntitiesMessage")
                            .with("dimID", getMovingWorld().dimension)
                            .with("entityID", getMovingWorld().getEntityId())
                            .with("tagCompound", tagCompound)
                            .toAllAround(getMovingWorld().worldObj, getMovingWorld(), 64D);
                }
            }
            getMobileChunkServer().getTileQueue().clear();
            getMobileChunkServer().getBlockQueue().clear();
        }
        firstChunkUpdate = false;
    }
}

