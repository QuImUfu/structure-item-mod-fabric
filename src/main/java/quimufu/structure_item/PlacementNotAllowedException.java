package quimufu.structure_item;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PlacementNotAllowedException extends RuntimeException {
    private final Text name;
    private final BlockPos pos;

    public PlacementNotAllowedException(Text name, BlockPos pos) {
        this.name = name;
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Text getName() {
        return name;
    }
}
