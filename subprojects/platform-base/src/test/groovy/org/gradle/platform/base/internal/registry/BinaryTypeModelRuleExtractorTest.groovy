/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal.registry

import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.InvalidModelRuleDeclarationException
import org.gradle.model.Managed
import org.gradle.model.internal.core.ModelAction
import org.gradle.model.internal.core.ModelActionRole
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.manage.schema.extract.DefaultModelSchemaExtractor
import org.gradle.model.internal.manage.schema.extract.DefaultModelSchemaStore
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.platform.base.BinarySpec
import org.gradle.platform.base.BinaryType
import org.gradle.platform.base.BinaryTypeBuilder
import org.gradle.platform.base.InvalidModelException
import org.gradle.platform.base.binary.BaseBinarySpec
import org.gradle.platform.base.binary.internal.BinarySpecFactory
import spock.lang.Unroll

import java.lang.annotation.Annotation

class BinaryTypeModelRuleExtractorTest extends AbstractAnnotationModelRuleExtractorTest {
    BinaryTypeModelRuleExtractor ruleHandler = new BinaryTypeModelRuleExtractor(new DefaultModelSchemaStore(DefaultModelSchemaExtractor.withDefaultStrategies()))

    @Override
    Class<? extends Annotation> getAnnotation() {
        return BinaryType
    }

    Class<?> ruleClass = Rules

    def "applies ComponentModelBasePlugin and creates binary type rule"() {
        def mockRegistry = Mock(ModelRegistry)

        when:
        def registration = extract(ruleDefinitionForMethod("validTypeRule"))

        then:
        registration.ruleDependencies == [ComponentModelBasePlugin]

        when:
        apply(registration, mockRegistry)

        then:
        1 * mockRegistry.configure(_, _, _) >> { ModelActionRole role, ModelAction<?> action, ModelPath scope ->
            assert role == ModelActionRole.Defaults
            assert action.subject == ModelReference.of(BinarySpecFactory)
        }
        0 * _
    }

    @Unroll
    def "decent error message for rule declaration problem - #descr"() {
        def ruleMethod = ruleDefinitionForMethod(methodName)
        def ruleDescription = getStringDescription(ruleMethod.method)

        when:
        extract(ruleMethod)

        then:
        def ex = thrown(InvalidModelRuleDeclarationException)
        ex.message == """Type ${ruleClass.name} is not a valid rule source:
- Method ${ruleDescription} is not a valid rule method: ${expectedMessage}"""

        where:
        methodName       | expectedMessage                                                                                                        | descr
        "extraParameter" | "A method annotated with @BinaryType must have a single parameter of type ${BinaryTypeBuilder.name}."                  | "additional rule parameter"
        "returnValue"    | "A method annotated with @BinaryType must have void return type."                                                      | "method with return type"
        "noTypeParam"    | "Parameter of type ${BinaryTypeBuilder.name} must declare a type parameter."                                           | "missing type parameter"
        "notBinarySpec"  | "Binary type '${NotBinarySpec.name}' is not a subtype of '${BinarySpec.name}'."                                        | "type not extending BinarySpec"
        "wildcardType"   | "Binary type '?' cannot be a wildcard type (i.e. cannot use ? super, ? extends etc.)."                                 | "wildcard type parameter"
        "extendsType"    | "Binary type '? extends ${BinarySpec.getName()}' cannot be a wildcard type (i.e. cannot use ? super, ? extends etc.)." | "extends type parameter"
        "superType"      | "Binary type '? super ${BinarySpec.getName()}' cannot be a wildcard type (i.e. cannot use ? super, ? extends etc.)."   | "super type parameter"
    }

    @Unroll
    def "decent error message for rule behaviour problem - #descr"() {
        def ruleMethod = ruleDefinitionForMethod(methodName)
        def ruleDescription = getStringDescription(ruleMethod)

        when:
        apply(ruleMethod)

        then:
        def ex = thrown(InvalidModelRuleDeclarationException)
        ex.message == "${ruleDescription} is not a valid binary model rule method."
        ex.cause instanceof InvalidModelException
        ex.cause.message == expectedMessage

        where:
        methodName                         | expectedMessage                                                                                                               | descr
        "implementationSetMultipleTimes"   | "Method annotated with @BinaryType cannot set default implementation multiple times."                                         | "implementation set multiple times"
        "implementationSetForManagedType"  | "Method annotated with @BinaryType cannot set default implementation for managed type ${ManagedBinarySpec.name}."             | "implementation set for managed type"
        "notImplementingBinaryType"        | "Binary implementation ${NotImplementingCustomBinary.name} must implement ${SomeBinarySpec.name}."                            | "implementation not implementing type class"
        "notExtendingDefaultSampleLibrary" | "Binary implementation ${NotExtendingBaseBinarySpec.name} must extend ${BaseBinarySpec.name}."                                | "implementation not extending BaseBinarySpec"
        "noDefaultConstructor"             | "Binary implementation ${NoDefaultConstructor.name} must have public default constructor."                                    | "implementation with no public default constructor"
        "internalViewNotInterface"         | "Internal view ${NonInterfaceInternalView.name} must be an interface."                                                        | "non-interface internal view"
        "notExtendingInternalView"         | "Binary implementation ${SomeBinarySpecImpl.name} must implement internal view ${NotImplementedBinarySpecInternalView.name}." | "implementation not extending internal view"
        "repeatedInternalView"             | "Internal view '${BinarySpecInternalView.name}' must not be specified multiple times."                                        | "internal view specified multiple times"
    }

    static interface SomeBinarySpec extends BinarySpec {}

    static class SomeBinarySpecImpl extends BaseBinarySpec implements SomeBinarySpec, BinarySpecInternalView, BareInternalView {}

    static class SomeBinarySpecOtherImpl extends SomeBinarySpecImpl {}

    static class NotImplementingCustomBinary extends BaseBinarySpec implements BinarySpec {}

    abstract static class NotExtendingBaseBinarySpec implements SomeBinarySpec {}

    static interface BinarySpecInternalView extends BinarySpec {}

    static interface BareInternalView {}

    abstract static class NonInterfaceInternalView implements BinarySpec {}

    static interface NotImplementedBinarySpecInternalView extends BinarySpec {}

    static interface NotBinarySpec {}

    static class NoDefaultConstructor extends BaseBinarySpec implements SomeBinarySpec {
        NoDefaultConstructor(String arg) {
        }
    }

    @Managed
    static abstract class ManagedBinarySpec implements BinarySpec {}

    static class Rules {
        @BinaryType
        static void validTypeRule(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(SomeBinarySpecImpl)
            builder.internalView(BinarySpecInternalView)
            builder.internalView(BareInternalView)
        }

        @BinaryType
        static void extraParameter(BinaryTypeBuilder<SomeBinarySpec> builder, String otherParam) {
        }

        @BinaryType
        static String returnValue(BinaryTypeBuilder<SomeBinarySpec> builder) {
        }

        @BinaryType
        static void noImplementationSetForUnmanagedBinarySpec(BinaryTypeBuilder<SomeBinarySpec> builder) {
        }

        @BinaryType
        static void implementationSetMultipleTimes(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(SomeBinarySpecImpl)
            builder.defaultImplementation(SomeBinarySpecOtherImpl)
        }

        @BinaryType
        static void implementationSetForManagedType(BinaryTypeBuilder<ManagedBinarySpec> builder) {
            builder.defaultImplementation(ManagedBinarySpec)
        }

        @BinaryType
        static void noTypeParam(BinaryTypeBuilder builder) {
        }

        @BinaryType
        static void wildcardType(BinaryTypeBuilder<?> builder) {
        }

        @BinaryType
        static void extendsType(BinaryTypeBuilder<? extends BinarySpec> builder) {
        }

        @BinaryType
        static void superType(BinaryTypeBuilder<? super BinarySpec> builder) {
        }

        @BinaryType
        static void notBinarySpec(BinaryTypeBuilder<NotBinarySpec> builder) {
        }

        @BinaryType
        static void notImplementingBinaryType(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(NotImplementingCustomBinary)
        }

        @BinaryType
        static void notExtendingDefaultSampleLibrary(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(NotExtendingBaseBinarySpec)
        }

        @BinaryType
        static void noDefaultConstructor(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(NoDefaultConstructor)
        }

        @BinaryType
        static void internalViewNotInterface(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(SomeBinarySpecImpl)
            builder.internalView(NonInterfaceInternalView)
        }

        @BinaryType
        static void notExtendingInternalView(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(SomeBinarySpecImpl)
            builder.internalView(NotImplementedBinarySpecInternalView)
        }

        @BinaryType
        static void repeatedInternalView(BinaryTypeBuilder<SomeBinarySpec> builder) {
            builder.defaultImplementation(SomeBinarySpecImpl)
            builder.internalView(BinarySpecInternalView)
            builder.internalView(BinarySpecInternalView)
        }
    }
}
