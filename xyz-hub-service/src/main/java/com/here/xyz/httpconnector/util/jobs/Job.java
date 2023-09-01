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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.here.xyz.models.hub.Space;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Import.class, name = "Import"),
        @JsonSubTypes.Type(value = Export.class, name = "Export")
})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public abstract class Job<T extends Job> {
    public static String ERROR_TYPE_VALIDATION_FAILED = "validation_failed";
    public static String ERROR_TYPE_PREPARATION_FAILED = "preparation_failed";
    public static String ERROR_TYPE_EXECUTION_FAILED = "execution_failed";
    public static String ERROR_TYPE_FINALIZATION_FAILED = "finalization_failed";
    public static String ERROR_TYPE_FAILED_DUE_RESTART = "failed_due_node_restart";
    public static String ERROR_TYPE_ABORTED = "aborted";

    @JsonView({Public.class})
    public enum Type {
        Import, Export;
        public static Type of(String value) {
            if (value == null) {
                return null;
            }
            try {
                return valueOf(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @JsonView({Public.class})
    public enum Status {
        waiting, queued, validating, validated, preparing, prepared, executing, executed,
            executing_trigger, trigger_executed, collecting_trigger_status, trigger_status_collected,
            finalizing, finalized(true), aborted(true), failed(true);

        private final boolean isFinal;


        Status() {
            this(false);
        }

        Status(boolean isFinal) {
            this.isFinal = isFinal;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public static Status of(String value) {
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
    public enum CSVFormat {
        GEOJSON, JSON_WKT, JSON_WKB, TILEID_FC_B64, PARTITIONID_FC_B64;

        public static CSVFormat of(String value) {
            if (value == null) {
                return null;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @JsonView({Public.class})
    public enum Strategy {
        LASTWINS, SKIPEXISTING, ERROR;

        public static Strategy of(String value) {
            if (value == null) {
                return null;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Beta release date: 2018-10-01T00:00Z[UTC]
     */
    @JsonIgnore
    private final long DEFAULT_TIMESTAMP = 1538352000000L;

    /**
     * The creation timestamp.
     */
    @JsonView({Public.class})
    private long createdAt = DEFAULT_TIMESTAMP;

    /**
     * The last update timestamp.
     */
    @JsonView({Public.class})
    private long updatedAt = DEFAULT_TIMESTAMP;

    /**
     * The timestamp which indicates when the execution began.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Public.class})
    private Long executedAt;

    /**
     * The timestamp at which time the finalization is completed.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Public.class})
    private Long finalizedAt;

    /**
     * The expiration timestamp.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Public.class})
    private Long exp;

    /**
     * The job ID
     */
    @JsonView({Public.class})
    private String id;

    @JsonView({Public.class})
    protected String description;

    @JsonView({Internal.class})
    protected String targetSpaceId;

    @JsonView({Internal.class})
    protected String targetTable;

    @JsonView({Public.class})
    private String errorDescription;

    @JsonView({Public.class})
    private String errorType;

    @JsonView({Public.class})
    private Status status;

    @JsonView({Public.class})
    private Status lastStatus;

    @JsonView({Public.class})
    protected CSVFormat csvFormat;

    @JsonView({Public.class})
    protected Strategy strategy;

    @JsonView({Internal.class})
    private String targetConnector;

    @JsonView({Internal.class})
    private Long spaceVersion;

    @JsonView({Internal.class})
    private String author;

    @JsonView({Public.class})
    protected Boolean clipped;

    @JsonView({Public.class})
    protected Boolean omitOnNull;

    /**
     * Arbitrary parameters to be provided from hub
     */
    @JsonView({Internal.class})
    protected Map<String, Object> params;

    private DatasetDescription source;
    private DatasetDescription target;

    public String getId(){
        return id;
    }

    public void setId(final String id){
        this.id = id;
    }

    public T withId(final String id) {
        setId(id);
        return (T) this;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public T withErrorDescription(final String errorDescription) {
        setErrorDescription(errorDescription);
        return (T) this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public T withDescription(final String description) {
        setDescription(description);
        return (T) this;
    }

    /**
     * @deprecated Please use methods {@link #getTarget()} or {@link #getSource()} instead.
     * @return
     */
    @Deprecated
    public String getTargetSpaceId() {
        return targetSpaceId;
    }

    /**
     * @deprecated Please use methods {@link #setTarget(DatasetDescription)} or {@link #setSource(DatasetDescription)} instead.
     * @param targetSpaceId
     */
    @Deprecated
    public void setTargetSpaceId(String targetSpaceId) {
        this.targetSpaceId = targetSpaceId;
        //Keep BWC
        if (this instanceof Import && target == null)
            setTarget(new DatasetDescription.Space().withId(targetSpaceId));
        else if (this instanceof Export && source == null)
            setSource(new DatasetDescription.Space().withId(targetSpaceId));
    }

    /**
     * @deprecated Please use methods {@link #withTarget(DatasetDescription)} or {@link #withSource(DatasetDescription)} instead.
     * @param targetSpaceId
     * @return
     */
    @Deprecated
    public T withTargetSpaceId(final String targetSpaceId) {
        setTargetSpaceId(targetSpaceId);
        return (T) this;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public T withTargetTable(final String targetTable) {
        setTargetTable(targetTable);
        return (T) this;
    }

    public Status getStatus() { return status; }

    public void setStatus(Job.Status status) {
        if(this.status == Status.failed || this.status == Status.finalized || this.status == Status.aborted)
            return;
        if((status.equals(Status.aborted) || status.equals(Status.failed)) && lastStatus == null)
            lastStatus = this.status;
        this.status = status;
    }

    public T withStatus(final Job.Status status) {
        setStatus(status);
        return (T) this;
    }

    public void resetStatus(Job.Status status) {
        this.status = status;
    }

    public Status getLastStatus() { return lastStatus; }

    public void setLastStatus(Job.Status lastStatus) {
        this.lastStatus = lastStatus;
    }

    public CSVFormat getCsvFormat() {
        return csvFormat;
    }

    public void setCsvFormat(CSVFormat csvFormat) {
        this.csvFormat = csvFormat;
    }

    public T withCsvFormat(CSVFormat csvFormat) {
        setCsvFormat(csvFormat);
        return (T) this;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public T withCsvFormat(Strategy strategy) {
        setStrategy(strategy);
        return (T) this;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    public T withCreatedAt(final long createdAt) {
        setCreatedAt(createdAt);
        return (T) this;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public T withUpdatedAt(final long updatedAt) {
        setUpdatedAt(updatedAt);
        return (T) this;
    }

    public Long getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(final Long executedAt) {
        this.executedAt = executedAt;
    }

    public T withExecutedAt(final Long startedAt) {
        setExecutedAt(startedAt);
        return (T) this;
    }

    public Long getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(final Long finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public T withFinalizedAt(final Long finalizedAt) {
        setFinalizedAt(finalizedAt);
        return (T) this;
    }

    public abstract void finalizeJob();

    public Long getExp() {
        return exp;
    }

    public void setExp(final Long exp) {
        this.exp = exp;
    }

    public T withExp(final Long exp) {
        setExp(exp);
        return (T) this;
    }

    public String getTargetConnector() {
        return targetConnector;
    }

    public void setTargetConnector(String targetConnector) {
        this.targetConnector = targetConnector;
    }

    public T withTargetConnector(String targetConnector) {
        setTargetConnector(targetConnector);
        return (T) this;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType){
        this.errorType = errorType;
    }

    public T withErrorType(String errorType) {
        setErrorType(errorType);
        return (T) this;
    }

    public Long getSpaceVersion() {
        return spaceVersion;
    }

    public void setSpaceVersion(final Long spaceVersion) {
        this.spaceVersion = spaceVersion;
    }

    public T withSpaceVersion(final long spaceVersion) {
        setSpaceVersion(spaceVersion);
        return (T) this;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public T withAuthor(String author) {
        setAuthor(author);
        return (T) this;
    }

    public Boolean getClipped() {
        return clipped;
    }

    public void setClipped(Boolean clipped) {
        this.clipped = clipped;
    }

    public Boolean getOmitOnNull() {
        return omitOnNull;
    }

    public void setOmitOnNull(Boolean omitOnNull) {
        this.omitOnNull = omitOnNull;
    }

    public T withOmitOnNull(final boolean omitOnNull) {
        setOmitOnNull(omitOnNull);
        return (T) this;
    }

    public Object getParam(String key) {
        if(params == null)
            return null;
        return params.get(key);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public T withParams(Map params) {
        setParams(params);
        return (T) this;
    }

    public DatasetDescription getSource() {
        return source;
    }

    public void setSource(DatasetDescription source) {
        this.source = source;
        //Keep BWC
//        if (source instanceof DatasetDescription.Space)
//            setTargetSpaceId(((DatasetDescription.Space) source).getId());
    }

    public T withSource(DatasetDescription source) {
        setSource(source);
        return (T) this;
    }

    public DatasetDescription getTarget() {
        return target;
    }

    public void setTarget(DatasetDescription target) {
        this.target = target;
        //Keep BWC
//        if (target instanceof DatasetDescription.Space)
//            setTargetSpaceId(((DatasetDescription.Space) source).getId());
    }

    public T withTarget(DatasetDescription target) {
        setTarget(target);
        return (T) this;
    }

    public void addParam(String key, Object value){
        if(this.params == null){
            this.params = new HashMap<>();
        }
        this.params.put(key,value);
    }
    @JsonIgnore
    public abstract void resetToPreviousState() throws Exception;

    @JsonIgnore
    public abstract String getQueryIdentifier();

    public abstract void execute();

    public static class Public {
    }

    public static class Internal extends Space.Internal {
    }

    public Job(){ }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
