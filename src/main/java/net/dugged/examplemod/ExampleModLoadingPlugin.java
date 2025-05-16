package net.dugged.examplemod;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class ExampleModLoadingPlugin implements IFMLLoadingPlugin {
    public ExampleModLoadingPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.examplemod.json");
    }

    // @formatter:off
    @Override public String getAccessTransformerClass() { return null; }
    @Override public String[] getASMTransformerClass() { return null; }
    @Override public void injectData(final Map<String, Object> data) {}
    @Nullable @Override public String getSetupClass() { return null; }
    @Override public String getModContainerClass() { return null; }
    // @formatter:on
}