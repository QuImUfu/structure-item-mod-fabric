package quimufu.structure_item;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MyPlacementSettings extends StructurePlacementData {
    private List<Block> blacklist;
    private World world;
    private BlockPos size;
    private boolean replaceEntities = true;

    public void forbidOverwrite(List<Block> blocks) {
        if (blocks.size() == 0) {
            blacklist = null;
            return;
        }
        blacklist = Lists.newArrayList(blocks);
    }

    public MyPlacementSettings setWorld(World w) {
        world = w;
        return this;
    }

    public MyPlacementSettings setSize(BlockPos p) {
        size = p;
        return this;
    }

    @Override
    public Structure.PalettedBlockInfoList getRandomBlockInfos(List<Structure.PalettedBlockInfoList> blocks, BlockPos pos) {
        if (world == null || pos == null || size == null) {
            return super.getRandomBlockInfos(blocks, pos);
        }

        List<Structure.PalettedBlockInfoList> eligibleStructures;
        eligibleStructures = getEligibleStructures(blocks, pos);
        if (eligibleStructures.size() == 0)
            return null;
        Structure.PalettedBlockInfoList randomBlockInfos = super.getRandomBlockInfos(eligibleStructures, pos);
        List<Structure.StructureBlockInfo> locs = randomBlockInfos.getAll();
        if (!locs.isEmpty()) {
            List<Entity> entitiesWithinAABB = world.getNonSpectatingEntities(Entity.class, new Box(pos,pos.add(size)));
            for (Structure.StructureBlockInfo blockInfo : locs) {
                BlockPos posToClean = blockInfo.pos.add(pos);
                for (Entity e : entitiesWithinAABB) {
                    if (!(e instanceof PlayerEntity) && e.getBoundingBox().intersects(new Box(posToClean))) {
                        e.remove();
                    }
                }
            }
        }
        return randomBlockInfos;
    }

    private List<Structure.PalettedBlockInfoList> getEligibleStructures(List<Structure.PalettedBlockInfoList> blocks, BlockPos pos) {
        List<Structure.PalettedBlockInfoList> eligibleStructures = new ArrayList<>();
        if (blacklist == null && shouldReplaceEntities()) {
            eligibleStructures = blocks;
        } else {
            for (Structure.PalettedBlockInfoList struct : blocks) {
                if (isValid(struct, pos)) {
                    eligibleStructures.add(struct);
                }
            }
        }
        return eligibleStructures;
    }

    private boolean isValid(Structure.PalettedBlockInfoList struct, BlockPos pos) {
        List<Entity> entitiesWithinAABB = world.getNonSpectatingEntities(shouldReplaceEntities()?PlayerEntity.class:Entity.class, new Box(pos,pos.add(size)));
        for (Structure.StructureBlockInfo bi : struct.getAll()) {
            BlockPos posToCheck = bi.pos.add(pos);
            if (World.isValid(posToCheck)) {
                for (Entity e : entitiesWithinAABB) {
                    if (e.getBoundingBox().intersects(new Box(posToCheck))) {
                        return false;
                    }
                }
                Block blockToCheck = world.getBlockState(posToCheck).getBlock();
                if(blacklist.contains(blockToCheck))
                    return false;
            }
        }
        return true;
    }

    public boolean shouldReplaceEntities() {
        return replaceEntities;
    }

    public void setReplaceEntities(boolean replaceEntities) {
        this.replaceEntities = replaceEntities;
    }
}
