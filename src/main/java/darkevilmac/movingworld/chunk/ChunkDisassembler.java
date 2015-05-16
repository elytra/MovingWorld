package darkevilmac.movingworld.chunk;

import darkevilmac.movingworld.MovingWorld;
import darkevilmac.movingworld.entity.EntityMovingWorld;
import darkevilmac.movingworld.tile.IMovingWorldTileEntity;
import darkevilmac.movingworld.util.MathHelperMod;
import darkevilmac.movingworld.util.Vec3Mod;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ChunkDisassembler {
    public boolean overwrite;
    private EntityMovingWorld movingWorld;

    public ChunkDisassembler(EntityMovingWorld EntityMovingWorld) {
        movingWorld = EntityMovingWorld;
        overwrite = false;
    }

    public boolean canDisassemble(MovingWorldAssemblyInteractor assemblyInteractor) {
        if (overwrite) {
            return true;
        }
        World world = movingWorld.worldObj;
        MobileChunk chunk = movingWorld.getMovingWorldChunk();
        float yaw = Math.round(movingWorld.rotationYaw / 90F) * 90F;
        yaw = (float) Math.toRadians(yaw);

        float ox = -chunk.getCenterX();
        float oy = -chunk.minY(); //Created the normal way, through a VehicleFiller, this value will always be 0.
        float oz = -chunk.getCenterZ();

        Vec3Mod vec;
        IBlockState state;
        Block block;
        BlockPos pos;
        for (int i = chunk.minX(); i < chunk.maxX(); i++) {
            for (int j = chunk.minY(); j < chunk.maxY(); j++) {
                for (int k = chunk.minZ(); k < chunk.maxZ(); k++) {
                    if (chunk.isAirBlock(new BlockPos(i, j, k))) continue;
                    Vec3Mod vecB = new Vec3Mod(i + ox, j + oy, k + oz);

                    vec = vecB;
                    vec.rotateYaw(yaw);

                    pos = new BlockPos(MathHelperMod.round_double(vec.xCoord + movingWorld.posX),
                            MathHelperMod.round_double(vec.yCoord + movingWorld.posY),
                            MathHelperMod.round_double(vec.zCoord + movingWorld.posZ));

                    state = world.getBlockState(pos);
                    block = state.getBlock();
                    if (block != null && !block.isAir(world, pos) && !block.getMaterial().isLiquid() && !assemblyInteractor.canOverwriteBlock(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public AssembleResult doDisassemble(MovingWorldAssemblyInteractor assemblyInteractor) {
        World world = movingWorld.worldObj;
        MobileChunk chunk = movingWorld.getMovingWorldChunk();
        AssembleResult result = new AssembleResult();
        result.xOffset = Integer.MAX_VALUE;
        result.yOffset = Integer.MAX_VALUE;
        result.zOffset = Integer.MAX_VALUE;

        int currentRot = Math.round(movingWorld.rotationYaw / 90F) & 3;
        int deltarot = (-currentRot) & 3;
        movingWorld.rotationYaw = currentRot * 90F;
        movingWorld.rotationPitch = 0F;
        float yaw = currentRot * MathHelperMod.PI_HALF;

        boolean flag = world.getGameRules().getGameRuleBooleanValue("doTileDrops");
        world.getGameRules().setOrCreateGameRule("doTileDrops", "false");

        List<LocatedBlock> postlist = new ArrayList<LocatedBlock>(4);

        float ox = -chunk.getCenterX();
        float oy = -chunk.minY(); //Created the normal way, through a ChunkAssembler, this value will always be 0.
        float oz = -chunk.getCenterZ();

        Vec3Mod vec;
        TileEntity tileentity;
        IBlockState blockState;
        Block block;
        IBlockState owBlockState;
        Block owBlock;
        BlockPos pos;
        for (int i = chunk.minX(); i < chunk.maxX(); i++) {
            for (int j = chunk.minY(); j < chunk.maxY(); j++) {
                for (int k = chunk.minZ(); k < chunk.maxZ(); k++) {
                    blockState = chunk.getBlockState(new BlockPos(i, j, k));
                    block = blockState.getBlock();
                    if (block == Blocks.air) {
                        if (block.getMetaFromState(blockState) == 1) continue;
                    } else if (block.isAir(world, new BlockPos(i, j, k))) continue;
                    tileentity = chunk.getTileEntity(new BlockPos(i, j, k));

                    //meta = MovingWorld.instance.metaRotations.getRotatedMeta(block, block.getMetaFromState(blockState), deltarot);

                    vec = new Vec3Mod(i + ox, j + oy, k + oz);
                    vec.rotateYaw(yaw);

                    pos = new BlockPos(MathHelperMod.round_double(vec.xCoord + movingWorld.posX),
                            MathHelperMod.round_double(vec.yCoord + movingWorld.posY),
                            MathHelperMod.round_double(vec.zCoord + movingWorld.posZ));

                    owBlockState = world.getBlockState(pos);
                    owBlock = owBlockState.getBlock();
                    if (owBlock != null)
                        assemblyInteractor.blockOverwritten(owBlock);

                    if (!world.setBlockState(pos, blockState, 2) || block != world.getBlockState(pos).getBlock()) {
                        postlist.add(new LocatedBlock(blockState, tileentity, pos));
                        continue;
                    }
                    if (blockState != world.getBlockState(pos)) {
                        world.setBlockState(pos, blockState, 2);
                    }
                    if (tileentity != null) {
                        if (tileentity instanceof IMovingWorldTileEntity) {
                            ((IMovingWorldTileEntity) tileentity).setParentMovingWorld(new BlockPos(i, j, k), null);
                        }
                        tileentity.validate();
                        world.setTileEntity(pos, tileentity);
                    }

                    if (!MovingWorld.instance.metaRotations.hasBlock(block)) {
                        assemblyInteractor.blockRotated(block, world, pos, currentRot);
                        rotateBlock(block, world, pos, currentRot);
                        blockState = world.getBlockState(pos);
                        block = blockState.getBlock();
                        tileentity = world.getTileEntity(pos);
                    }

                    LocatedBlock lb = new LocatedBlock(blockState, tileentity, pos);
                    assemblyInteractor.blockDisassembled(lb);
                    result.assembleBlock(lb);
                }
            }
        }

        world.getGameRules().setOrCreateGameRule("doTileDrops", String.valueOf(flag));

        for (LocatedBlock ilb : postlist) {
            pos = ilb.blockPos;
            MovingWorld.logger.debug("Post-rejoining block: " + ilb.toString());
            world.setBlockState(pos, ilb.blockState, 0);
            assemblyInteractor.blockDisassembled(ilb);
            result.assembleBlock(ilb);
        }

        movingWorld.setDead();

        if (result.movingWorldMarkingBlock == null || !assemblyInteractor.isTileMovingWorldMarker(result.movingWorldMarkingBlock.tileEntity)) {
            result.resultCode = AssembleResult.RESULT_MISSING_MARKER;
        } else {
            result.checkConsistent(world);
        }
        assemblyInteractor.chunkDissasembled(result);
        result.assemblyInteractor = assemblyInteractor;
        return result;
    }

    private void rotateBlock(Block block, World world, BlockPos pos, int deltarot) {
        deltarot &= 3;
        if (deltarot != 0) {
            if (deltarot == 3) {
                block.rotateBlock(world, pos, EnumFacing.UP);
            } else {
                for (int r = 0; r < deltarot; r++) {
                    block.rotateBlock(world, pos, EnumFacing.DOWN);
                }
            }
        }
    }
}
