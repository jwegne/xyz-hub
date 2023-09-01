/*
 * Copyright (C) 2017-2023 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.here.xyz.httpconnector.util.jobs;

import static com.here.xyz.httpconnector.util.scheduler.JobQueue.setJobAborted;
import static com.here.xyz.httpconnector.util.scheduler.JobQueue.setJobFailed;
import static com.here.xyz.httpconnector.util.scheduler.JobQueue.updateJobStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.here.xyz.httpconnector.CService;
import com.here.xyz.httpconnector.config.JDBCExporter;
import com.here.xyz.httpconnector.config.JDBCImporter;
import com.here.xyz.httpconnector.rest.HApiParam;
import com.here.xyz.httpconnector.util.jobs.DatasetDescription.Files;
import com.here.xyz.hub.Core;
import com.here.xyz.hub.rest.ApiParam;
import com.here.xyz.models.geojson.implementation.Geometry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Export extends Job<Export> {
    private static final Logger logger = LogManager.getLogger();
    public static String ERROR_TYPE_HTTP_TRIGGER_FAILED = "http_trigger_failed";
    public static String ERROR_TYPE_TARGET_ID_INVALID = "targetId_invalid";
    public static String ERROR_TYPE_HTTP_TRIGGER_STATUS_FAILED = "http_get_trigger_status_failed";

    @JsonInclude
    private Type type = Type.Export;

    @JsonView({Public.class})
    private Map<String,ExportObject> exportObjects;

    @JsonView({Public.class})
    private Map<String,ExportObject> superExportObjects;

    @JsonView({Public.class})
    private ExportStatistic statistic;

    @JsonView({Public.class})
    private ExportTarget exportTarget;

    @JsonView({Internal.class})
    private long estimatedFeatureCount;

    @JsonView({Internal.class})
    private Map<String,Long> searchableProperties;

    @JsonView({Internal.class})
    private List<String> processingList;

    /** Only used by type VML */
    @JsonView({Public.class})
    private int maxTilesPerFile;

    /** Only used by type VML */
    @JsonView({Public.class})
    private Boolean clipped;

    /** Only used by type VML */
    @JsonView({Public.class})
    private Integer targetLevel;

    /** Only used by type VML */
    @JsonView({Public.class})
    private String partitionKey;

    @JsonView({Public.class})
    private String targetVersion;

    @JsonView({Public.class})
    private Filters filters;

    @JsonView({Public.class})
    private String triggerId;

    public Export(){ }

    public Export(String description, String targetSpaceId, String targetTable, Strategy strategy) {
        this.description = description;
        this.targetSpaceId = targetSpaceId;
        this.targetTable = targetTable;
        this.strategy = strategy;
        this.clipped = false;
    }

    public Type getType() {
        return type;
    }

    public List<String> getProcessingList() {
        return processingList;
    }

    public void setProcessingList(List<String> processingList) {
        this.processingList = processingList;
    }

    public Export withProcessingList(List<String> processingList) {
        setProcessingList(processingList);
        return this;
    }

    public long getEstimatedFeatureCount() {
        return estimatedFeatureCount;
    }

    public void setEstimatedFeatureCount(long estimatedFeatureCount) {
        this.estimatedFeatureCount = estimatedFeatureCount;
    }

    public Export withEstimatedFeatureCount(long estimatedFeatureCount){
        setEstimatedFeatureCount(estimatedFeatureCount);
        return this;
    }

    public Map<String,Long> getSearchableProperties() {
        return searchableProperties;
    }

    public void setSearchableProperties(Map<String,Long> searchableProperties) {
        this.searchableProperties = searchableProperties;
    }

    public Export withSearchableProperties(Map<String,Long> searchableProperties) {
        setSearchableProperties(searchableProperties);
        return this;
    }

    public ExportStatistic getStatistic(){
        return this.statistic;
    }

    public void setStatistic(ExportStatistic statistic) {
        this.statistic = statistic;
    }

    public void addStatistic(ExportStatistic statistic) {
        if(this.statistic == null)
            this.statistic = statistic;
        else {
            this.statistic.setBytesUploaded(this.statistic.getBytesUploaded() + statistic.getBytesUploaded());
            this.statistic.setFilesUploaded(this.statistic.getFilesUploaded() + statistic.getFilesUploaded());
            this.statistic.setRowsUploaded(this.statistic.getRowsUploaded() + statistic.getRowsUploaded());
        }
    }

    public Map<String,ExportObject> getSuperExportObjects() {
        if(superExportObjects == null)
            superExportObjects = new HashMap<>();
        return superExportObjects;
    }

    public void setSuperExportObjects(Map<String, ExportObject> superExportObjects) {
        this.superExportObjects = superExportObjects;
    }

    public Map<String,ExportObject> getExportObjects() {
        if(exportObjects == null)
            exportObjects = new HashMap<>();
        return exportObjects;
    }

    public void setExportObjects(Map<String, ExportObject> exportObjects) {
        this.exportObjects = exportObjects;
    }

    public Export withExportObjects(Map<String, ExportObject> exportObjects) {
        setExportObjects(exportObjects);
        return this;
    }

    /**
     * @deprecated Please use method {@link #getTarget()} instead.
     */
    @Deprecated
    public ExportTarget getExportTarget() {
        return exportTarget;
    }

    /**
     * @deprecated Please use method {@link #setTarget(DatasetDescription)} instead.
     * @param exportTarget
     */
    @Deprecated
    public void setExportTarget(ExportTarget exportTarget) {
        this.exportTarget = exportTarget;
        //Keep BWC
        if (getTarget() == null && (exportTarget.getType() == ExportTarget.Type.S3 || exportTarget.getType() == ExportTarget.Type.DOWNLOAD))
            setTarget(new Files());
    }

    /**
     * @deprecated Please use method {@link #withTarget(DatasetDescription)} instead.
     * @param exportTarget
     */
    @Deprecated
    public Export withExportTarget(final ExportTarget exportTarget) {
        setExportTarget(exportTarget);
        return this;
    }

    public Integer getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(Integer targetLevel) {
        this.targetLevel = targetLevel;
    }

    public Export withTargetLevel(final Integer targetLevel) {
        setTargetLevel(targetLevel);
        return this;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public Export withPartitionKey(final String partitionKey) {
        setPartitionKey(partitionKey);
        return this;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public Export withTargetVersion(final String targetVersion) {
        setTargetVersion(targetVersion);
        return this;
    }

    public int getMaxTilesPerFile() {
        return maxTilesPerFile;
    }

    public void setMaxTilesPerFile(int maxTilesPerFile) {
        this.maxTilesPerFile = maxTilesPerFile;
    }

    public Export withMaxTilesPerFile(final int maxTilesPerFile) {
        setMaxTilesPerFile(maxTilesPerFile);
        return this;
    }

    public Boolean getClipped() {
        return clipped;
    }

    public void setClipped(boolean clipped) {
        this.clipped = clipped;
    }

    public Export withClipped(final boolean clipped) {
        setClipped(clipped);
        return this;
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public Export withFilters(final Filters filters) {
        setFilters(filters);
        return this;
    }

    public String getTriggerId() { return triggerId; }

    public void setTriggerId(String triggerId) { this.triggerId = triggerId; }

    public Export withTriggerId(String triggerId) {
        setTriggerId(triggerId);
        return this;
    }

    @JsonIgnore
    public String readParamSuperExportPath() {
        return this.params.containsKey("superExportPath") ? (String) this.getParam("superExportPath") : null;
    }

    @JsonIgnore
    public boolean readParamPersistExport(){
        return this.params.containsKey("persistExport") ? (boolean) this.getParam("persistExport") : false;
    }

    @JsonIgnore
    public boolean includesSecondLevelExtension() {
        if(this.params == null)
            return false;

        Map extension = (Map) this.params.get("extends");

        if(extension != null && extension.get("extends") != null)
            return true;
        return false;
    }

    @JsonIgnore
    public boolean isSuperSpacePersistent() {
        Map extension = (Map) this.params.get("extends");
        if(this.params == null && extension == null)
            return false;

        Map recursiveExtension = (Map) extension.get("extends");
        if(recursiveExtension != null) {
            return (boolean) recursiveExtension.get("persistExport");
        }
        return (boolean) extension.get("persistExport");
    }

    @JsonIgnore
    public String extractSuperSpaceId() {
        Map extension = (Map) this.params.get("extends");
        if(this.params == null && extension == null)
            return null;

        Map recursiveExtension = (Map) extension.get("extends");
        if(recursiveExtension != null) {
            return (String) recursiveExtension.get("spaceId");
        }
        return (String) extension.get("spaceId");
    }

    @JsonIgnore
    public ApiParam.Query.Incremental readParamIncremental() {
        return this.params.containsKey("incremental") ?
                ApiParam.Query.Incremental.valueOf((String)this.params.get(HApiParam.HQuery.INCREMENTAL)) :
                ApiParam.Query.Incremental.DEACTIVATED;
    }

    public void resetToPreviousState() throws Exception {
        switch (getStatus()){
            case failed:
            case aborted:
                setErrorType(null);
                setErrorDescription(null);
                if(getLastStatus() != null) {
                    /** set to last valid state */
                    resetStatus(getLastStatus());
                    setLastStatus(null);
                    resetToPreviousState();
                }else
                    throw new Exception("No last Status found!");
                break;
            case executing:
                resetStatus(Status.waiting);
                break;
            case executing_trigger:
                resetStatus(Status.executed);
                break;
            case collecting_trigger_status:
                resetStatus(Status.trigger_executed);
                break;
        }
    }

    public static class ExportStatistic {
        private long rowsUploaded;
        private long filesUploaded;
        private long bytesUploaded;

        public long getRowsUploaded() {
            return rowsUploaded;
        }

        public void setRowsUploaded(long rowsUploaded) {
            this.rowsUploaded = rowsUploaded;
        }

        public ExportStatistic withRowsUploaded(long rowsUploaded){
            this.setRowsUploaded(rowsUploaded);
            return this;
        }

        public long getFilesUploaded() {
            return filesUploaded;
        }

        public void setFilesUploaded(long filesUploaded) {
            this.filesUploaded = filesUploaded;
        }

        public ExportStatistic withFilesUploaded(long filesUploaded){
            this.setFilesUploaded(filesUploaded);
            return this;
        }

        public long getBytesUploaded() {
            return bytesUploaded;
        }

        public void setBytesUploaded(long bytesUploaded) {
            this.bytesUploaded = bytesUploaded;
        }

        public ExportStatistic withBytesUploaded(long bytesUploaded){
            this.setBytesUploaded(bytesUploaded);
            return this;
        }

        public void addRows(long rows){
            rowsUploaded += rows;
        }
        public void addBytes(long bytes){
            bytesUploaded += bytes;
        }
        public void addFiles(long files){
            filesUploaded += files;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class ExportTarget {
        @JsonView({Public.class})
        public enum Type {
            VML, DOWNLOAD, S3 /** same as download */;
            public static Type of(String value) {
                if (value == null) {
                    return null;
                }
                try {
                    return valueOf(value.toLowerCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        @JsonView({Public.class})
        private String targetId;

        @JsonView({Public.class})
        private Type type;

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public ExportTarget withTargetId(String targetId){
            this.setTargetId(targetId);
            return this;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public ExportTarget withType(final Type type) {
            setType(type);
            return this;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class SpatialFilter {

        @JsonView({Public.class})
        private Geometry geometry;

        @JsonView({Public.class})
        private int radius;

        @JsonView({Public.class})
        private boolean clipped;

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }

        public SpatialFilter withGeometry(Geometry geometry){
            this.setGeometry(geometry);
            return this;
        }

        public int getRadius() {
            return radius;
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }

        public SpatialFilter withRadius(final int radius) {
            setRadius(radius);
            return this;
        }

        public boolean isClipped() {
            return clipped;
        }

        public void setClipped(boolean clipped) {
            this.clipped = clipped;
        }

        public SpatialFilter withClipped(final boolean clipped) {
            setClipped(clipped);
            return this;
        }
    }

    public static class Filters {
        @JsonView({Public.class})
        private String propertyFilter;

        @JsonView({Public.class})
        private SpatialFilter spatialFilter;

        public String getPropertyFilter() {
            return propertyFilter;
        }

        public void setPropertyFilter(String propertyFilter) {
            this.propertyFilter = propertyFilter;
        }

        public Filters withPropertyFilter(String propertyFilter) {
            setPropertyFilter(propertyFilter);
            return this;
        }

        public SpatialFilter getSpatialFilter() {
            return spatialFilter;
        }

        public void setSpatialFilter(SpatialFilter spatialFilter) {
            this.spatialFilter = spatialFilter;
        }

        public Filters withSpatialFilter(SpatialFilter spatialFilter) {
            setSpatialFilter(spatialFilter);
            return this;
        }
    }

    @Override
    public String getQueryIdentifier() {
        return "export_hint";
    }

    @Override
    public void execute() {
        setExecutedAt(Core.currentTimeMillis() / 1000L);
        String defaultSchema = JDBCImporter.getDefaultSchema(getTargetConnector());

        String s3Path = CService.jobS3Client.getS3Path(this);

        if (readParamPersistExport()) {
            Export existingJob = CService.jobS3Client.readMetaFileFromJob(this);
            if (existingJob != null) {
                if (existingJob.getExportObjects() == null || existingJob.getExportObjects().isEmpty()) {
                    String message = String.format("Another job already started for %s and targetLevel %s with status %s",
                        existingJob.getTargetSpaceId(), existingJob.getTargetLevel(), existingJob.getStatus());
                    setJobFailed(this, message, Job.ERROR_TYPE_EXECUTION_FAILED);
                }
                else {
                    addDownloadLinksAndWriteMetaFile(existingJob);
                    setExportObjects(existingJob.getExportObjects());
                    updateJobStatus(this, Job.Status.executed);
                }
                return;
            }
            else {
                addDownloadLinksAndWriteMetaFile(this);
            }
        }

        JDBCExporter.executeExport(this, defaultSchema, CService.configuration.JOBS_S3_BUCKET, s3Path,
                CService.configuration.JOBS_REGION)
            .onSuccess(statistic -> {
                    /** Everything is processed */
                    logger.info("job[{}] Export of '{}' completely succeeded!", getId(), getTargetSpaceId());
                    addStatistic(statistic);
                    addDownloadLinksAndWriteMetaFile(this);
                    updateJobStatus(this, Job.Status.executed);
                }
            )
            .onFailure(e -> {
                logger.warn("job[{}] export of '{}' failed! ", getId(), getTargetSpaceId(), e);

                if (e.getMessage() != null && e.getMessage().equalsIgnoreCase("Fail to read any response from the server, the underlying connection might get lost unexpectedly."))
                    setJobAborted(this);
                else {
                    setJobFailed(this, null, Job.ERROR_TYPE_EXECUTION_FAILED);
                }}
            );
    }

    protected void addDownloadLinksAndWriteMetaFile(Job j){
        /** Add file statistics and downloadLinks */
        Map<String, ExportObject> exportObjects = CService.jobS3Client.scanExportPath((Export)j, false, true);
        ((Export) j).setExportObjects(exportObjects);

        if(((Export)j).readParamSuperExportPath() != null) {
            /** Add exportObjects including fresh download links for persistent base exports */
            Map<String, ExportObject> superExportObjects = CService.jobS3Client.scanExportPath((Export) j, true, true);
            ((Export) j).setSuperExportObjects(superExportObjects);
        }

        /** Write MetaFile to S3 */
        CService.jobS3Client.writeMetaFile((Export) j);
    }

    @Override
    public void finalizeJob() {
        setFinalizedAt(Core.currentTimeMillis() / 1000L);
        updateJobStatus(this, Job.Status.finalized);
    }
}
