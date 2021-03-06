/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicecomb.serviceregistry.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import io.servicecomb.serviceregistry.api.Const;
import io.servicecomb.serviceregistry.api.registry.Microservice;
import io.servicecomb.serviceregistry.api.response.MicroserviceInstanceChangedEvent;
import io.servicecomb.serviceregistry.client.ServiceRegistryClient;
import io.servicecomb.serviceregistry.config.ServiceRegistryConfig;
import io.servicecomb.serviceregistry.task.event.ExceptionEvent;
import io.servicecomb.serviceregistry.task.event.RecoveryEvent;

public class MicroserviceWatchTask extends AbstractTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceWatchTask.class);

  private ServiceRegistryConfig serviceRegistryConfig;

  public MicroserviceWatchTask(EventBus eventBus, ServiceRegistryConfig serviceRegistryConfig,
      ServiceRegistryClient srClient, Microservice microservice) {
    super(eventBus, srClient, microservice);
    this.serviceRegistryConfig = serviceRegistryConfig;
  }

  @Subscribe
  public void onMicroserviceInstanceRegisterTask(MicroserviceInstanceRegisterTask task) {
    if (task.taskStatus == TaskStatus.FINISHED && isSameMicroservice(task.getMicroservice())) {
      this.taskStatus = TaskStatus.READY;
    }
  }

  @Override
  public void doRun() {
    // will always run watch when it is ready
    if (!needToWatch()) {
      return;
    }

    srClient.watch(microservice.getServiceId(),
        (event) -> {
          if (event.failed()) {
            eventBus.post(new ExceptionEvent(event.cause()));
            return;
          }

          MicroserviceInstanceChangedEvent changedEvent = event.result();
          if (isProviderInstancesChanged(changedEvent) && !serviceRegistryConfig.isWatch()) {
            return;
          }
          if (!isProviderInstancesChanged(changedEvent)
              && !serviceRegistryConfig.isRegistryAutoDiscovery()) {
            return;
          }

          onMicroserviceInstanceChanged(changedEvent);
        },
        open -> {
          eventBus.post(new RecoveryEvent());
        },
        close -> {
        });
  }

  private void onMicroserviceInstanceChanged(MicroserviceInstanceChangedEvent changedEvent) {
    switch (changedEvent.getAction()) {
      case CREATE:
        LOGGER.info("microservice {}/{} REGISTERED an instance {}, {}.",
            changedEvent.getKey().getAppId(),
            changedEvent.getKey().getServiceName(),
            changedEvent.getInstance().getInstanceId(),
            changedEvent.getInstance().getEndpoints());
        break;
      case DELETE:
        LOGGER.info("microservice {}/{} UNREGISTERED an instance {}, {}.",
            changedEvent.getKey().getAppId(),
            changedEvent.getKey().getServiceName(),
            changedEvent.getInstance().getInstanceId(),
            changedEvent.getInstance().getEndpoints());
        break;
      case EXPIRE:
        LOGGER.info("microservice {}/{} EXPIRE all instance.",
            changedEvent.getKey().getAppId(),
            changedEvent.getKey().getServiceName());
        break;
      case UPDATE:
        LOGGER.info("microservice {}/{} UPDATE an instance {} status or metadata, {}.",
            changedEvent.getKey().getAppId(),
            changedEvent.getKey().getServiceName(),
            changedEvent.getInstance().getInstanceId(),
            changedEvent.getInstance().getEndpoints());
        break;
      default:
        break;
    }

    eventBus.post(changedEvent);
  }

  private boolean needToWatch() {
    return serviceRegistryConfig.isWatch() || serviceRegistryConfig.isRegistryAutoDiscovery();
  }

  private boolean isProviderInstancesChanged(MicroserviceInstanceChangedEvent changedEvent) {
    return !Const.REGISTRY_APP_ID.equals(changedEvent.getKey().getAppId())
        && !Const.REGISTRY_SERVICE_NAME.equals(changedEvent.getKey().getServiceName());
  }
}
