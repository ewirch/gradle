/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.tasks.properties.annotations;

import org.gradle.api.internal.tasks.DeclaredTaskInputFileProperty;
import org.gradle.api.internal.tasks.PropertySpecFactory;
import org.gradle.api.internal.tasks.ValidationActions;
import org.gradle.api.internal.tasks.properties.BeanPropertyContext;
import org.gradle.api.internal.tasks.properties.PropertyValue;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.CompileClasspathNormalizer;
import org.gradle.api.tasks.InputFiles;
import org.gradle.internal.fingerprint.CompileClasspathFingerprinter;
import org.gradle.internal.fingerprint.FileCollectionFingerprinter;

import java.lang.annotation.Annotation;

public class CompileClasspathPropertyAnnotationHandler implements OverridingPropertyAnnotationHandler, FileFingerprintingPropertyAnnotationHandler {
    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return CompileClasspath.class;
    }

    @Override
    public Class<? extends Annotation> getOverriddenAnnotationType() {
        return InputFiles.class;
    }

    @Override
    public Class<? extends FileCollectionFingerprinter> getFingerprinterImplementationType() {
        return CompileClasspathFingerprinter.class;
    }

    @Override
    public void visitPropertyValue(PropertyValue propertyValue, PropertyVisitor visitor, PropertySpecFactory specFactory, BeanPropertyContext context) {
        DeclaredTaskInputFileProperty fileSpec = specFactory.createInputFileSpec(propertyValue, ValidationActions.NO_OP);
        fileSpec
            .withPropertyName(propertyValue.getPropertyName())
            .withNormalizer(CompileClasspathNormalizer.class)
            .optional(propertyValue.isOptional());
        visitor.visitInputFileProperty(fileSpec);
    }
}
