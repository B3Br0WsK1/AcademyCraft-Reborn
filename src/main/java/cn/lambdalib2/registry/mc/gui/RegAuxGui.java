/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.registry.mc.gui;

import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ReflectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;

/**
 * AuxGui register annotation. Applies on a static AuxGui field, or on an AuxGui class with a public no-arg ctor.
 * @author WeathFolD
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegAuxGui {}

@Environment(EnvType.CLIENT)
class RegAuxGuiImpl {

    @StateEventCallback
    @SuppressWarnings("unchecked")
    private static void init(StateEvents.Init ev) {
        ReflectionUtils.getFields(RegAuxGui.class).forEach(it -> {
            try {
                AuxGui instance = (AuxGui) it.get(null);
                AuxGui.register(instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        ReflectionUtils.getClasses(RegAuxGui.class).forEach(type -> {
            try {
                Constructor<AuxGui> ctor = (Constructor<AuxGui>) type.getConstructor();
                AuxGui.register(ctor.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
