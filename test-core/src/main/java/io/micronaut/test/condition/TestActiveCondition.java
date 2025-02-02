/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.test.condition;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.test.annotation.MockBean;

import java.util.Optional;

/**
 * A custom {@link Condition} that enables inner classes and {@link MockBean} instances only for the scope of the test.
 *
 * @author graemerocher
 * @since 1.0
 */
public class TestActiveCondition implements Condition {

    public static final String ACTIVE_MOCKS = "micronaut.test.spock.active.mocks";
    /**
     * Deprecated please use {@link #ACTIVE_SPEC_CLAZZ} instead.
     */
    @Deprecated
    public static final String ACTIVE_SPEC_NAME = "micronaut.test.active.spec";

    public static final String ACTIVE_SPEC_CLAZZ = "micronaut.test.active.spec.clazz";

    @Override
    public boolean matches(ConditionContext context) {
        if (context.getComponent() instanceof BeanDefinition) {
            BeanDefinition<?> definition = (BeanDefinition<?>) context.getComponent();
            final BeanContext beanContext = context.getBeanContext();
            if (beanContext instanceof ApplicationContext) {
                final Optional<Class<?>> declaringType = definition.getDeclaringType();
                ApplicationContext applicationContext = (ApplicationContext) beanContext;
                final Class activeSpecClazz = applicationContext.get(ACTIVE_SPEC_CLAZZ, Class.class).orElse(null);
                final String activeSpecName = Optional.ofNullable(activeSpecClazz).map(clazz-> clazz.getPackage().getName() + "." + clazz.getSimpleName()).orElse(null);
                if (definition.isAnnotationPresent(MockBean.class) && declaringType.isPresent()) {
                    final Class<?> declaringTypeClass = declaringType.get();
                    String declaringTypeName = declaringTypeClass.getName();
                    if (activeSpecClazz != null) {
                        if (definition.isProxy()) {
                            final String packageName = NameUtils.getPackageName(activeSpecName);
                            final String simpleName = NameUtils.getSimpleName(activeSpecName);
                            final String rootName = packageName + ".$" + simpleName;
                            return declaringTypeClass.isAssignableFrom(activeSpecClazz) || declaringTypeName.equals(rootName) || declaringTypeName.startsWith(rootName + "$");
                        } else {
                            return declaringTypeClass.isAssignableFrom(activeSpecClazz) || activeSpecName.equals(declaringTypeName) || declaringTypeName.startsWith(activeSpecName + "$");
                        }
                    } else {
                        context.fail(
                                "@MockBean of type " + definition.getBeanType() + " not within scope of parent test."
                        );
                        return false;
                    }
                } else {
                    return activeSpecName != null && activeSpecName.equals(definition.getBeanType().getName());
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
