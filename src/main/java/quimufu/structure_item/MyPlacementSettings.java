package quimufu.structure_item;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MyPlacementSettings extends StructurePlacementData {
    private List<Block> blacklist;
    private World world;
    private Vec3i size;
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

    public MyPlacementSettings setSize(Vec3i p) {
        size = p;
        return this;
    }

    @Override
    public StructureTemplate.PalettedBlockInfoList getRandomBlockInfos(List<StructureTemplate.PalettedBlockInfoList> blocks, BlockPos pos) {
        if (world == null || pos == null || size == null) {
            return super.getRandomBlockInfos(blocks, pos);
        }

        List<StructureTemplate.PalettedBlockInfoList> eligibleStructures;
        eligibleStructures = getEligibleStructures(blocks, pos);
        if (eligibleStructures.size() == 0)
            return null;
        StructureTemplate.PalettedBlockInfoList randomBlockInfos = super.getRandomBlockInfos(eligibleStructures, pos);
        List<StructureTemplate.StructureBlockInfo> locs = randomBlockInfos.getAll();
        if (!locs.isEmpty()) {
            List<Entity> entitiesWithinAABB = world.getNonSpectatingEntities(Entity.class, new Box(pos,pos.add(size)));
            for (StructureTemplate.StructureBlockInfo blockInfo : locs) {
                BlockPos posToClean = blockInfo.pos().add(pos);
                for (Entity e : entitiesWithinAABB) {
                    if (!(e instanceof PlayerEntity) && e.getBoundingBox().intersects(new Box(posToClean))) {
                        e.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        }
        return randomBlockInfos;
    }

    private List<StructureTemplate.PalettedBlockInfoList> getEligibleStructures(List<StructureTemplate.PalettedBlockInfoList> blocks, BlockPos pos) {
        List<StructureTemplate.PalettedBlockInfoList> eligibleStructures = new ArrayList<>();
        if (blacklist == null && shouldReplaceEntities()) {
            eligibleStructures = blocks;
        } else {
            for (StructureTemplate.PalettedBlockInfoList struct : blocks) {
                if (isValid(struct, pos)) {
                    eligibleStructures.add(struct);
                }
            }
        }
        return eligibleStructures;
    }

    private boolean isValid(StructureTemplate.PalettedBlockInfoList struct, BlockPos pos) {
        List<? extends Entity> entitiesWithinAABB;
        if (shouldReplaceEntities()) {
            entitiesWithinAABB = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos, pos.add(size)));
        } else {
            entitiesWithinAABB = world.getNonSpectatingEntities(Entity.class, new Box(pos, pos.add(size)));
        }
        for (StructureTemplate.StructureBlockInfo bi : struct.getAll()) {
            BlockPos posToCheck = bi.pos().add(pos);
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
