package cn.academy.client;

import cn.lambdalib2.LambdaLib2Client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AcademyCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LambdaLib2Client.init();
        LambdaLib2Client.runClientStages();
    }
}
