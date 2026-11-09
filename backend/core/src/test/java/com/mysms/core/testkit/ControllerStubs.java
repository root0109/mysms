package com.mysms.core.testkit;

import com.mysms.core.error.NotImplementedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Counts the contract operations a controller answers with 501 NOT_IMPLEMENTED: every method of its contract
 * interfaces is invoked with null arguments on an instance whose collaborators are null. A stub throws
 * {@link NotImplementedException} before touching anything; a served operation fails on its first collaborator call
 * instead (or on its arguments), so the count tells the two apart without a running service.
 */
public final class ControllerStubs {

    private ControllerStubs() {
    }

    public static int count(Class<?> controller) {
        Object instance = instantiate(controller);
        int stubs = 0;
        for (Class<?> api : controller.getInterfaces()) {
            for (Method method : api.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (isStub(instance, controller, method)) {
                    stubs++;
                }
            }
        }
        return stubs;
    }

    private static boolean isStub(Object instance, Class<?> controller, Method method) {
        try {
            Method implementation = controller.getMethod(method.getName(), method.getParameterTypes());
            implementation.invoke(instance, new Object[method.getParameterCount()]);
            return false;
        } catch (InvocationTargetException e) {
            return e.getCause() instanceof NotImplementedException;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method.getName(), e);
        }
    }

    private static Object instantiate(Class<?> controller) {
        Constructor<?> constructor = controller.getConstructors()[0];
        try {
            return constructor.newInstance(new Object[constructor.getParameterCount()]);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(controller.getName(), e);
        }
    }
}
