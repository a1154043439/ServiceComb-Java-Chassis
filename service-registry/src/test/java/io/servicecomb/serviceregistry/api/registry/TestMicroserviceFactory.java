/*
    * Copyright 2017 Huawei Technologies Co., Ltd
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

package io.servicecomb.serviceregistry.api.registry;

import static io.servicecomb.serviceregistry.definition.DefinitionConst.CONFIG_ALLOW_CROSS_APP_KEY;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.servicecomb.config.archaius.sources.MicroserviceConfigLoader;
import io.servicecomb.serviceregistry.definition.MicroserviceDefinition;
import mockit.Deencapsulation;

public class TestMicroserviceFactory {
  @Test
  public void testAllowCrossApp() {
    MicroserviceFactory factory = new MicroserviceFactory();
    Map<String, String> propertiesMap = new HashMap<>();
    Assert.assertFalse(Deencapsulation.invoke(factory, "allowCrossApp", propertiesMap));

    propertiesMap.put(CONFIG_ALLOW_CROSS_APP_KEY, "true");
    Assert.assertTrue(Deencapsulation.invoke(factory, "allowCrossApp", propertiesMap));

    propertiesMap.put(CONFIG_ALLOW_CROSS_APP_KEY, "false");
    Assert.assertFalse(Deencapsulation.invoke(factory, "allowCrossApp", propertiesMap));

    propertiesMap.put(CONFIG_ALLOW_CROSS_APP_KEY, "asfas");
    Assert.assertFalse(Deencapsulation.invoke(factory, "allowCrossApp", propertiesMap));
  }

  @Test
  public void testInit() {
    MicroserviceConfigLoader loader = new MicroserviceConfigLoader();
    loader.loadAndSort();

    MicroserviceDefinition microserviceDefinition = new MicroserviceDefinition(loader.getConfigModels());
    MicroserviceFactory factory = new MicroserviceFactory();
    Microservice microservice = factory.create(microserviceDefinition);

    String microserviceName = "default";

    Assert.assertEquals(microserviceName, microservice.getServiceName());
  }
}
