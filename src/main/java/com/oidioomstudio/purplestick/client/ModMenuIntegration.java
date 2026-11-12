package com.oidioomstudio.purplestick.client;

import com.oidioomstudio.purplestick.PurpleStickMod;
import com.oidioomstudio.olib.client.OModMenuApi;

public class ModMenuIntegration extends OModMenuApi {
    @Override
    protected String getModId() {
        return PurpleStickMod.MOD_ID;
    }
}
