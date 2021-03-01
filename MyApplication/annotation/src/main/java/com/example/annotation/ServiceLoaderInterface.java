package com.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface ServiceLoaderInterface {
    String key();
    String interfaceName() default "";
    Class<?> interfaceClass() default Void.class;

    String[] keys() default {};
    Class<?>[] interfaceClasses() default {};
}
