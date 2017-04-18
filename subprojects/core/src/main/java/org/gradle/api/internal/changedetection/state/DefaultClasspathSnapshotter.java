/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.changedetection.state;

import com.google.common.hash.HashCode;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.changedetection.resources.AbstractSnapshotter;
import org.gradle.api.internal.changedetection.resources.CachingResourceSnapshotter;
import org.gradle.api.internal.changedetection.resources.ClasspathResourceSnapshotter;
import org.gradle.api.internal.changedetection.resources.ResourceSnapshotter;
import org.gradle.api.internal.changedetection.resources.SnapshottableReadableResource;
import org.gradle.api.internal.changedetection.resources.SnapshottableResource;
import org.gradle.api.internal.changedetection.resources.recorders.SnapshottingResultRecorder;
import org.gradle.cache.PersistentIndexedCache;

public class DefaultClasspathSnapshotter extends AbstractFileCollectionSnapshotter implements ClasspathSnapshotter {
    private final StringInterner stringInterner;
    private final ResourceSnapshotter resourceSnapshotter;

    public DefaultClasspathSnapshotter(FileSystemSnapshotter fileSystemSnapshotter, StringInterner stringInterner, PersistentIndexedCache<HashCode, HashCode> jarSignatureCache) {
        super(fileSystemSnapshotter, stringInterner);
        this.stringInterner = stringInterner;
        this.resourceSnapshotter = new CachingResourceSnapshotter(
            new ClasspathResourceSnapshotter(new ClasspathEntrySnapshotter(), stringInterner),
            jarSignatureCache
        );
    }

    @Override
    public Class<? extends FileCollectionSnapshotter> getRegisteredType() {
        return ClasspathSnapshotter.class;
    }

    @Override
    protected FileCollectionSnapshotBuilder createFileCollectionSnapshotBuilder(SnapshotNormalizationStrategy normalizationStrategy, TaskFilePropertyCompareStrategy compareStrategy) {
        return new FileCollectionSnapshotBuilder(TaskFilePropertySnapshotNormalizationStrategy.NONE, TaskFilePropertyCompareStrategy.ORDERED, stringInterner);
    }

    @Override
    protected ResourceSnapshotter createSnapshotter(SnapshotNormalizationStrategy normalizationStrategy, TaskFilePropertyCompareStrategy compareStrategy) {
        return resourceSnapshotter;
    }

    private class ClasspathEntrySnapshotter extends AbstractSnapshotter {
        @Override
        protected void snapshotResource(SnapshottableResource resource, SnapshottingResultRecorder recorder) {
            if (resource instanceof SnapshottableReadableResource) {
                HashCode signatureForClass = resource.getContent().getContentMd5();
                recorder.recordResult(resource, signatureForClass);
            }
        }

        @Override
        protected void snapshotTree(SnapshottableResourceTree snapshottable, SnapshottingResultRecorder recorder) {
            throw new UnsupportedOperationException("Trees cannot be classpath entries");
        }
    }
}
