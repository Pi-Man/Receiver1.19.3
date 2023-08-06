package pi_man.receiver.world.level.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;
import pi_man.receiver.Receiver;

public class ModBlocks {
    // Creates a new Block with the id "receiver:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = Receiver.BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));

}
