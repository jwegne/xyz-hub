/*
 * Copyright (C) 2017-2024 HERE Europe B.V.
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

package com.here.xyz.hub.config.dynamo;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.util.CollectionUtils;
import com.here.xyz.hub.Service;
import com.here.xyz.util.ARN;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DynamoClient {

  private static final Logger logger = LogManager.getLogger();
  private static final Long READ_CAPACITY_UNITS = 5L;
  private static final Long WRITE_CAPACITY_UNITS = 5L;
  private static final String INDEX_SUFFIX = "-index";
  private static final String HYPHEN = "-";


  public static final WorkerExecutor dynamoWorkers = Service.vertx.createSharedWorkerExecutor(DynamoClient.class.getName(), 8);
  private static AWSCredentialsProvider customCredentialsProvider;

  final AmazonDynamoDBAsync client;
  public final String tableName;
  public final DynamoDB db;
  private final ARN arn;

  public DynamoClient(String tableArn, String localstackEndpoint) {
    arn = new ARN(tableArn);

    final AmazonDynamoDBAsyncClientBuilder builder = AmazonDynamoDBAsyncClientBuilder.standard();

    if (isLocal()) {
      if(localstackEndpoint == null) {
        builder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")));
        final String endpoint = "http://" + arn.getRegion() + ":" + Integer.parseInt(arn.getAccountId());
        builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "US-WEST-1"));
      }else {
        builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        (localstackEndpoint == null ? Service.configuration.LOCALSTACK_ENDPOINT : localstackEndpoint), "eu-west-1"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack")));
      }
    }
    else {
      builder.setRegion(arn.getRegion());
      if (Service.configuration != null && Service.configuration.USE_AWS_INSTANCE_CREDENTIALS_WITH_REFRESH)
        synchronized(DynamoClient.class) {
          if (customCredentialsProvider == null) {
            customCredentialsProvider = InstanceProfileCredentialsProvider.createAsyncRefreshingProvider(true);
          }
          builder.setCredentials(customCredentialsProvider);
        }
    }

    client = builder.build();
    db = new DynamoDB(client);
    tableName = new ARN(tableArn).getResourceWithoutType();
  }

  Table getTable() {
    return db.getTable(tableName);
  }

  public void createTable(String tableName, String attributes, String keys, List<IndexDefinition> indexes, String ttl) {
    try {
      // required
      final List<AttributeDefinition> attList = new ArrayList<>();
      for (String s : attributes.split(",")) {
        attList.add(new AttributeDefinition(s.split(":")[0], s.split(":")[1]));
      }

      // required
      final List<KeySchemaElement> keyList = new ArrayList<>();
      for (String s : keys.split(",")) {
        keyList.add(new KeySchemaElement(s, keyList.isEmpty() ? KeyType.HASH : KeyType.RANGE));
      }


      CreateTableRequest req = new CreateTableRequest()
          .withTableName(tableName)
          .withAttributeDefinitions(attList)
          .withKeySchema(keyList)
          .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

      // optional
      if (!CollectionUtils.isNullOrEmpty(indexes)) {
        final List<GlobalSecondaryIndex> gsiList = indexes.stream().map(this::createGSI).toList();
        req = req.withGlobalSecondaryIndexes(gsiList);
      }

      db.createTable(req);

      if (ttl != null) {
        client.updateTimeToLive(new UpdateTimeToLiveRequest().withTableName(tableName)
            .withTimeToLiveSpecification(new TimeToLiveSpecification().withAttributeName(ttl).withEnabled(true)));
      }
    } catch (ResourceInUseException e) {
      logger.info("Table {} already exists, skipping creation", tableName);
    }
  }

  public boolean isLocal() {
    return Arrays.stream(Region.values()).noneMatch(r -> r.toAWSRegion().getName().equals(arn.getRegion()));
  }

  Future<List<Map<String, AttributeValue>>> executeStatement(ExecuteStatementRequest request) {
    return DynamoClient.dynamoWorkers.executeBlocking(future -> {
      try {
        ExecuteStatementResult result = client.executeStatement(request);
        List<Map<String, AttributeValue>> items = result.getItems();

        while (result.getNextToken() != null) {
          result = client.executeStatement(request.withNextToken(result.getNextToken()));
          items.addAll(result.getItems());
        }

        future.complete(items);
      } catch (Exception e) {
        future.fail(e);
      }
    });
  }

  @FunctionalInterface
  public interface ThrowingSupplier<R> {
    R supply() throws Exception;
  }

  public <R> Future<R> executeQueryAsync(ThrowingSupplier<R> commandExecution) {
    return DynamoClient.dynamoWorkers.executeBlocking(
        promise -> {
          try {
              promise.complete(commandExecution.supply());
            }
            catch (Exception e) {
              promise.fail(e);
            }
        });
  }

  private GlobalSecondaryIndex createGSI(IndexDefinition indexDefinition) {
    List<KeySchemaElement> keySchema = new ArrayList<>();
    keySchema.add(new KeySchemaElement(indexDefinition.getHashKey(), KeyType.HASH));
    if(indexDefinition.getRangeKey() != null) {
      keySchema.add(new KeySchemaElement(indexDefinition.getRangeKey(), KeyType.RANGE));
    }
    String indexName = indexDefinition.getRangeKey() != null ?
            indexDefinition.getHashKey().concat(HYPHEN).concat(indexDefinition.getRangeKey()).concat(INDEX_SUFFIX) :
            indexDefinition.getHashKey().concat(INDEX_SUFFIX);

    return new GlobalSecondaryIndex()
            .withIndexName(indexName)
            .withKeySchema(keySchema)
            .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
            .withProvisionedThroughput(new ProvisionedThroughput(READ_CAPACITY_UNITS, WRITE_CAPACITY_UNITS));
  }
}
