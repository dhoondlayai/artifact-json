package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Defines a naming strategy for a class to automatically apply during mapping.
 * Examples strategies: SNAKE_CASE, KEBAB_CASE, CAMEL_CASE.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonNaming {
    
    public enum NamingStrategy {
        CAMEL_CASE, // myProp
        SNAKE_CASE, // my_prop
        KEBAB_CASE, // my-prop
        PASCAL_CASE // MyProp
    }

    /**
     * The strategy to apply to all fields inside this class.
     */
    NamingStrategy value() default NamingStrategy.CAMEL_CASE;
}
