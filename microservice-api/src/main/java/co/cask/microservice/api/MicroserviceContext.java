/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.microservice.api;

import co.cask.cdap.api.Admin;
import co.cask.cdap.api.TxRunnable;
import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.data.DatasetInstantiationException;
import co.cask.cdap.api.data.stream.StreamBatchWriter;
import co.cask.cdap.api.dataset.Dataset;
import co.cask.cdap.api.metrics.Metrics;
import co.cask.cdap.api.stream.StreamEventData;
import co.cask.cdap.api.worker.WorkerContext;
import co.cask.microservice.annotation.PublicEvolving;
import org.apache.tephra.TransactionFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Class description here.
 */
@PublicEvolving
public final class MicroserviceContext {
  private final WorkerContext delegate;
  private final MicroserviceDefinition definition;
  private final Metrics metrics;

  public MicroserviceContext(WorkerContext delegate, Metrics metrics, MicroserviceDefinition definition) {
    this.delegate = delegate;
    this.metrics = metrics;
    this.definition = definition;
  }

  /**
   * Writes a string to a stream
   *
   * @param stream stream id
   * @param data data to be written
   *
   * @throws IOException if an error occurred during write
   */
  public void write(String stream, String data) throws IOException {
    delegate.write(stream, data);
  }

  /**
   * Writes a string to a stream with headers
   *
   * @param stream stream id
   * @param data data to be written
   * @param headers headers for the data
   *
   * @throws IOException if an error occurred during write
   */
  void write(String stream, String data, Map<String, String> headers) throws IOException {
    delegate.write(stream, data, headers);
  }

  /**
   * Writes a {@link ByteBuffer} to a stream
   *
   * @param stream stream id
   * @param data {@link ByteBuffer} data to be written
   *
   * @throws IOException if an error occurred during write
   */
  void write(String stream, ByteBuffer data) throws IOException {
    delegate.write(stream, data);
  }

  /**
   * Writes a {@link StreamEventData} to a stream
   *
   * @param stream stream id
   * @param data {@link StreamEventData} data to be written
   *
   * @throws IOException if an error occurred during write
   */
  void write(String stream, StreamEventData data) throws IOException {
    delegate.write(stream,data);
  }

  /**
   * Writes a File to a stream in batch
   * @param stream stream id
   * @param file File
   * @param contentType content type
   *
   * @throws IOException if an error occurred during write
   */
  void writeFile(String stream, File file, String contentType) throws IOException {
    delegate.writeFile(stream, file, contentType);
  }

  /**
   * Writes in batch using {@link StreamBatchWriter} to a stream
   * @param stream stream id
   * @param contentType content type
   * @return {@link StreamBatchWriter} provides a batch writer
   *
   * @throws IOException if an error occurred during write
   */
  public StreamBatchWriter createBatchWriter(String stream, String contentType) throws IOException {
    return delegate.createBatchWriter(stream, contentType);
  }

  public String getId() {
    return definition.getId();
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public String getDescription() {
    return definition.getDescription();
  }

  public Plugin getPlugin() {
    return definition.getPlugin();
  }

  public MicroserviceConfiguration getConfiguration() {
    return definition.getConfiguration();
  }

  /**
   * @return The application namespace
   */
  public String getNamespace() {
    return delegate.getNamespace();
  }

  /**
   * @return The application namespace
   */
  public String getClusterName() {
    return delegate.getClusterName();
  }

  /**
   * @return The application namespace
   */
  public int getInstanceCount() {
    return delegate.getInstanceCount();
  }

  /**
   * @return The application namespace
   */
  public int getInstanceId() {
    return delegate.getInstanceId();
  }

  /**
   * @return an {@link Admin} to perform admin operations
   */
  public Admin getAdmin() {
    return delegate.getAdmin();
  }

  /**
   * Executes a set of operations via a {@link TxRunnable} that are committed as a single transaction.
   * The {@link TxRunnable} can gain access to a {@link Dataset} through the provided {@link DatasetContext}.
   *
   * @param runnable the runnable to be executed in the transaction
   * @throws TransactionFailureException if failed to execute the given {@link TxRunnable} in a transaction
   */
  public void execute(TxRunnable runnable) throws TransactionFailureException {
    delegate.execute(runnable);
  }

  /**
   * Executes a set of operations via a {@link TxRunnable} that are committed as a single transaction with a given
   * timeout. The {@link TxRunnable} can gain access to a {@link Dataset} through the provided {@link DatasetContext}.
   *
   * @param timeoutInSeconds the transaction timeout for the transaction, in seconds
   * @param runnable the runnable to be executed in the transaction
   *
   * @throws TransactionFailureException if failed to execute the given {@link TxRunnable} in a transaction
   */
  public void execute(int timeoutInSeconds, TxRunnable runnable) throws TransactionFailureException {
    delegate.execute(timeoutInSeconds, runnable);
  }

  /**
   * Get an instance of the specified Dataset.
   *
   * @param name The name of the Dataset
   * @param <T> The type of the Dataset
   * @return An instance of the specified Dataset, never null.
   * @throws DatasetInstantiationException If the Dataset cannot be instantiated: its class
   *         cannot be loaded; the default constructor throws an exception; or the Dataset
   *         cannot be opened (for example, one of the underlying tables in the DataFabric
   *         cannot be accessed).
   */
  public <T extends Dataset> T getDataset(String name) throws DatasetInstantiationException {
    return delegate.getDataset(name);
  }

  /**
   * Get an instance of the specified Dataset.
   *
   * @param namespace The namespace of the Dataset
   * @param name The name of the Dataset
   * @param <T> The type of the Dataset
   * @return An instance of the specified Dataset, never null.
   * @throws DatasetInstantiationException If the Dataset cannot be instantiated: its class
   *         cannot be loaded; the default constructor throws an exception; or the Dataset
   *         cannot be opened (for example, one of the underlying tables in the DataFabric
   *         cannot be accessed).
   */
  public <T extends Dataset> T getDataset(String namespace, String name) throws DatasetInstantiationException {
    return delegate.getDataset(namespace, name);
  }

  /**
   * Get an instance of the specified Dataset.
   *
   * @param name The name of the Dataset
   * @param arguments the arguments for this dataset instance
   * @param <T> The type of the Dataset
   * @return An instance of the specified Dataset, never null.
   * @throws DatasetInstantiationException If the Dataset cannot be instantiated: its class
   *         cannot be loaded; the default constructor throws an exception; or the Dataset
   *         cannot be opened (for example, one of the underlying tables in the DataFabric
   *         cannot be accessed).
   */
  public <T extends Dataset> T getDataset(String name, Map<String, String> arguments) throws DatasetInstantiationException {
    return delegate.getDataset(name, arguments);
  }

  /**
   * Get an instance of the specified Dataset.
   *
   * @param namespace The namespace of Dataset
   * @param name The name of the Dataset
   * @param arguments the arguments for this dataset instance
   * @param <T> The type of the Dataset
   * @return An instance of the specified Dataset, never null.
   * @throws DatasetInstantiationException If the Dataset cannot be instantiated: its class
   *         cannot be loaded; the default constructor throws an exception; or the Dataset
   *         cannot be opened (for example, one of the underlying tables in the DataFabric
   *         cannot be accessed).
   */
  public <T extends Dataset> T getDataset(String namespace, String name, Map<String, String> arguments)
    throws DatasetInstantiationException {
    return delegate.getDataset(namespace, name, arguments);
  }

  /**
   * Calling this means that the dataset is not used by the caller any more,
   * and the DatasetContext is free to dismiss or reuse it. The dataset must
   * have been acquired through this context using {@link #getDataset(String)}
   * or {@link #getDataset(String, Map)}. It is up to the implementation of the
   * context whether this dataset is discarded, or whether it is reused as the
   * result for subsequent {@link #getDataset} calls with the same arguments.
   * This may be influenced by resource constraints, expiration policy, or
   * advanced configuration.
   *
   * @param dataset The dataset to be released.
   */
  public void releaseDataset(Dataset dataset) {
    delegate.releaseDataset(dataset);
  }

  /**
   * Calling this means that the dataset is not used by the caller any more,
   * and the DatasetContext must close and discard it as soon as possible.
   * The dataset must have been acquired through this context using
   * {@link #getDataset(String)} or {@link #getDataset(String, Map)}.
   * <p>
   * It is guaranteed that no subsequent
   * invocation of {@link #getDataset} will return the same object. The only
   * exception is if getDataset() is called again before the dataset could be
   * effectively discarded. For example, if the dataset participates in a
   * transaction, then the system can only discard it after that transaction
   * has completed. If getDataset() is called again during the same transaction,
   * then the DatasetContext will return the same object, and this effectively
   * cancels the discardDataset(), because the dataset was reacquired before it
   * could be discarded.
   * </p>
   *
   * @param dataset The dataset to be dismissed.
   */
  public void discardDataset(Dataset dataset) {
    delegate.discardDataset(dataset);
  }
}
