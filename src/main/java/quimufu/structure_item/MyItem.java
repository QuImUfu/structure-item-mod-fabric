package quimufu.structure_item;

import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.datafixer.NbtOps;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;

import static quimufu.structure_item.StructureItemMod.LOGGER;

public class MyItem extends Item {
    static Item.Settings p = (new Item.Settings()).group(ItemGroup.REDSTONE).maxCount(1);

    public MyItem() {
        super(p);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> texts, TooltipContext tooltipFlag) {
        if (!itemStack.hasTag() || !itemStack.getTag().contains("structure", 8)) {
            texts.add((new TranslatableText("item.structure_item.item.tooltip.tag.invalid")).formatted(Formatting.RED));
        } else {
            CompoundTag tag = itemStack.getTag();
            if (tooltipFlag.isAdvanced()) {
                texts.add(new TranslatableText("item.structure_item.item.tooltip.structure"));
                texts.add(new LiteralText("  " + tag.getString("structure")));
                if (tag.contains("allowedOn", 8)) {
                    texts.add(new TranslatableText("item.structure_item.item.tooltip.allowed.on"));
                    texts.add(new LiteralText("  " + tag.getString("allowedOn")));
                }
                if (tag.contains("offset")) {
                    BlockPos offset = BlockPos.deserialize(new Dynamic<Tag>(NbtOps.INSTANCE, tag.get("offset")));
                    texts.add(new TranslatableText("item.structure_item.item.tooltip.fixed.offset"));
                    Text c = new TranslatableText("item.structure_item.item.tooltip.xyz",
                            new LiteralText(String.valueOf(offset.getX())),
                            new LiteralText(String.valueOf(offset.getY())),
                            new LiteralText(String.valueOf(offset.getZ())));
                    texts.add(c);
                } else {
                    texts.add(new TranslatableText("item.structure_item.item.tooltip.dynamic.offset"));
                }
                if (tag.contains("blacklist", 9)) {
                    texts.add(new TranslatableText("item.structure_item.item.tooltip.blacklist"));
                    ListTag bl = tag.getList("blacklist", 8);
                    int i = 0;
                    for ( Tag entry : bl) {
                        texts.add(new LiteralText("  " + entry.asString()));
                        i++;
                        if (i == 4) {
                            texts.add(new TranslatableText("item.structure_item.item.tooltip.blacklist.more",
                                    new LiteralText(String.valueOf(bl.size() - i))));
                        }
                    }
                }
            }
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext c) {
        if (!c.getWorld().isClient) {
            ServerPlayerEntity player;
            if (c.getPlayer() instanceof ServerPlayerEntity) {
                player = ((ServerPlayerEntity) c.getPlayer());
            } else {
                player = null;
            }
            CompoundTag tag = c.getStack().getTag();
            if (tag == null) {
                TranslatableText message =
                        new TranslatableText("items.structure.spawner.no.tag");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            Block allowed = null;
            if (tag.contains("allowedOn", 8)) {
                String allowedOn = tag.getString("allowedOn");
                allowed = getBlock(allowedOn);
                if (allowed == null) {
                    TranslatableText message =
                            new TranslatableText("items.structure.spawner.invalid.block",
                                    new LiteralText(allowedOn));
                    sendPlayerChat(player, message);
                    return ActionResult.FAIL;
                }
            }
            Block current = c.getWorld().getBlockState(c.getBlockPos()).getBlock();
            if (allowed != null && !current.equals(allowed)) {
                TranslatableText message =
                        new TranslatableText("items.structure.spawner.invalid.block.clicked",
                                current.getName(), allowed.getName());
                sendPlayer(player, message);
                return ActionResult.FAIL;
            }
            if (!tag.contains("structure", 8)) {
                LOGGER.info("No structure name set");
                TranslatableText message =
                        new TranslatableText("items.structure.spawner.no.structure");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            String structureName = tag.getString("structure");
            Identifier structureResourceID = Identifier.tryParse(structureName);
            if (structureResourceID == null) {
                TranslatableText message =
                        new TranslatableText("items.structure.spawner.invalid.structure.name");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            Structure x = ((ServerWorld) c.getWorld()).getStructureManager().getStructure(structureResourceID);
            if (x == null) {
                TranslatableText message =
                        new TranslatableText("items.structure.spawner.structure.nonexistent",
                                new LiteralText(structureResourceID.toString()));
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }

            BlockPos loc = c.getBlockPos().offset(c.getSide());
            if (tag.contains("offset")) {
                BlockPos offset = BlockPos.deserialize(new Dynamic<Tag>(NbtOps.INSTANCE, tag.get("offset")));
                loc = loc.add(offset);
            } else if (c.getPlayer() != null) {
                Direction direction = Direction.getEntityFacingOrder(c.getPlayer())[0];
                BlockPos size = x.getSize();
                loc = loc.add(getDirectionalOffset(direction, size));
            } else {
                LOGGER.info("No player & no offset");
            }

            MyPlacementSettings ps = (new MyPlacementSettings());
            if (tag.contains("blacklist", 9)) {
                ListTag bl = tag.getList("blacklist", 8);
                List<Block> blacklist = Lists.newArrayList();
                for (Tag b : bl) {
                    Block block = getBlock(b.asString());
                    if (block != null) {
                        blacklist.add(block);
                    } else {
                        TranslatableText message =
                                new TranslatableText("items.structure.spawner.invalid.block",
                                        new LiteralText(b.asString()));
                        sendPlayerChat(player, message);
                    }

                }
                ps.forbidOverwrite(blacklist);
            }
            ps.setWorld(c.getWorld())
                    .setSize(x.getSize())
                    .setMirrored(BlockMirror.NONE)
                    .setRotation(BlockRotation.NONE)
                    .setChunkPosition(null);
            boolean succes = x.method_15172(c.getWorld(), loc, ps, 2);
            if (succes) {
                c.getStack().decrement(1);
                return ActionResult.SUCCESS;
            }
            TranslatableText message =
                    new TranslatableText("items.structure.spawner.invalid.location");
            sendPlayer(player, message);
            return ActionResult.FAIL;
        }
        return ActionResult.FAIL;
    }

    private static void sendPlayer(ServerPlayerEntity player, Text message) {
        if (player == null)
            return;
        ServerPlayNetworkHandler connection = player.networkHandler;
        TitleS2CPacket packet = new TitleS2CPacket(TitleS2CPacket.Action.SUBTITLE, message);
        connection.sendPacket(packet);
        packet = new TitleS2CPacket(TitleS2CPacket.Action.TITLE, new LiteralText(""));
        connection.sendPacket(packet);
    }

    private static void sendPlayerChat(ServerPlayerEntity player, Text message) {
        if (player != null)
            player.sendChatMessage(message, MessageType.SYSTEM);
        LOGGER.info(message.asFormattedString());
    }

    private BlockPos getDirectionalOffset(Direction direction, BlockPos size) {
        BlockPos loc = new BlockPos(0, 0, 0);
        switch (direction) {
            case WEST:
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() - 1);
                break;
            case EAST: //positive x
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                break;
            case NORTH:
                loc = loc.offset(Direction.NORTH, size.getZ() - 1);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                break;
            case SOUTH: //positive z
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                break;
            case UP:    //positive y
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.UP);
                break;
            case DOWN:
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.DOWN, size.getY());
                break;
        }
        return loc;
    }

    private Block getBlock(String loc) {
        Identifier location = Identifier.tryParse(loc);
        DefaultedRegistry<Block> blocks = Registry.BLOCK;
        if (location == null || !blocks.containsId(location)) {
            return null;
        }
        return blocks.get(location);
    }
}
