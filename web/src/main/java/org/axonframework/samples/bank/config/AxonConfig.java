/*
 * Copyright (c) 2016. Axon Framework
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
 */

package org.axonframework.samples.bank.config;

import org.axonframework.commandhandling.AsynchronousCommandBus;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.samples.bank.command.BankAccount;
import org.axonframework.samples.bank.command.BankAccountCommandHandler;
import org.axonframework.samples.bank.command.BankTransferManagementSaga;
import org.axonframework.spring.config.AxonConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.Executors;

@Configuration
public class AxonConfig {

  @Autowired
  private AxonConfiguration axonConfiguration;
  @Autowired
  private EventBus eventBus;

  @Bean
  public BankAccountCommandHandler bankAccountCommandHandler() {
    return new BankAccountCommandHandler(axonConfiguration.repository(BankAccount.class), eventBus);
  }

  @Bean
  public SagaConfiguration bankTransferManagementSagaConfiguration() {
    return SagaConfiguration.trackingSagaManager(BankTransferManagementSaga.class);
  }

  @Autowired
  public void configure(@Qualifier("localSegment") AsynchronousCommandBus commandBus) {
    commandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>());
  }

  @Primary
  @Bean(destroyMethod = "shutdown")
  @Qualifier("localSegment")
  public AsynchronousCommandBus commandBus(
    org.axonframework.config.Configuration config,
    org.axonframework.spring.config.AxonConfiguration configuration) {
    AsynchronousCommandBus commandBus = new AsynchronousCommandBus(
      Executors.newCachedThreadPool(),
      config.getComponent(TransactionManager.class, () -> NoTransactionManager.INSTANCE),
      config.messageMonitor(CommandBus.class, "commandBus")
    );
    commandBus.registerHandlerInterceptor(new CorrelationDataInterceptor<>(configuration.correlationDataProviders()));
    return commandBus;
  }
}
