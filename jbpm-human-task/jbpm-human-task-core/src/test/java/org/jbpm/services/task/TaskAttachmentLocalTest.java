/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.services.task;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.kie.internal.task.api.InternalTaskService;

import bitronix.tm.resource.jdbc.PoolingDataSource;


public class TaskAttachmentLocalTest extends TaskAttachmentBaseTest {

	private PoolingDataSource pds;
	private EntityManagerFactory emf;
	
	@Before
	public void setup() {
		pds = setupPoolingDataSource();
		emf = Persistence.createEntityManagerFactory( "org.jbpm.services.task" );

		this.taskService = (InternalTaskService) HumanTaskServiceFactory.newTaskServiceConfigurator()
												.entityManagerFactory(emf)
												.getTaskService();
	}
	
	@After
	public void clean() {
		super.tearDown();
		if (emf != null) {
			emf.close();
		}
		if (pds != null) {
			pds.close();
		}
	}
}
