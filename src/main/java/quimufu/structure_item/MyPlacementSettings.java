package quimufu.structure_item;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MyPlacementSettings extends StructurePlacementData {
    private List<Block> blacklist;
    private World world;
    private Vec3i size;
    private boolean replaceEntities = true;

    public MyPlacementSettings() {
        addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
        addProcessor(new CheckingStructureProcess());
    }

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

    public boolean shouldReplaceEntities() {
        return replaceEntities;
    }

    public void setReplaceEntities(boolean replaceEntities) {
        this.replaceEntities = replaceEntities;
    }

    public class CheckingStructureProcess extends StructureProcessor {

        List<? extends Entity> entitiesWithinAABB;

        @Nullable
        @Override
        public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlacementData data) {
            if (entitiesWithinAABB == null) {
                if (shouldReplaceEntities()) {
                    entitiesWithinAABB = ((ServerWorld) world).getNonSpectatingEntities(PlayerEntity.class, new Box(pos.subtract(size), pos.add(size)));
                } else {
                    entitiesWithinAABB = ((ServerWorld) world).getNonSpectatingEntities(Entity.class, new Box(pos.subtract(size), pos.add(size)));
                }
            }
            BlockPos posToCheck;
            if (currentBlockInfo != null && World.isValid(posToCheck = currentBlockInfo.pos())) {
                for (Entity e : entitiesWithinAABB) {
                    if (e.getBoundingBox().intersects(new Box(posToCheck))) {
                        throw new PlacementNotAllowedException(e.getName(), posToCheck);
                    }
                }
                Block blockToCheck = world.getBlockState(posToCheck).getBlock();
                if (blacklist.contains(blockToCheck)) {
                    throw new PlacementNotAllowedException(blockToCheck.getName(), posToCheck);
                }
            }
            return currentBlockInfo;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return null;
        }
    }
}
