/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Queues
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.plugin.TaskFinishedListener
import java.rmi.RemoteException
import java.rmi.ServerException
import java.util.concurrent.CountDownLatch
import org.apache.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class TaskServiceSpec extends Specification {

    private UserContext userContext = UserContext.auto()

    private Task task1 = new Task(id: '4', env: 'prod', name: 'Create ASG helloworld-v001', status: 'running',
            userContext: new UserContext('CMC-123', 'marge', 'iphone1', '1.2.3.4', Region.US_EAST_1, false),
            startTime: new Date(1394486251000), updateTime: new Date(1394486252000),
            email: 'yo@example.com', operation: 'Go!', objectType: EntityType.cluster,
            objectId: 'helloworld', log: ['Get ready', 'Go!'])

    private Task task2 = new Task(id: '6', env: 'prod', name: 'Disable ASG helloworld-v000', status: 'running',
            userContext: new UserContext('CMC-456', 'homer', 'mrplow', '4.5.6.7', Region.US_WEST_2, false),
            startTime: new Date(1394486261000), updateTime: new Date(1394486262000),
            email: 'hi@example.com', operation: 'Drinking', objectType: EntityType.cluster,
            objectId: 'helloworld', log: ['Preparing to work', 'Doing work', 'Work is done', 'Drinking'])

    private Task task3 = new Task(id: '77', env: 'prod', name: 'Mess with people', status: 'running',
            userContext: new UserContext('blah', 'bart', 'ipad3', '6.3.4.2', Region.EU_WEST_1, false),
            startTime: new Date(1394486351000), updateTime: new Date(1394486358000),
            email: 'bart@example.com', operation: 'Do it', objectType: EntityType.security,
            objectId: 'eureka', log: ['Plan', 'Do it'])

    private String task1Json = '{"id":"4","userContext":{"ticket":"CMC-123","username":"marge",' +
            '"clientHostName":"iphone1","clientIpAddress":"1.2.3.4","region":"us-east-1",' +
            '"internalAutomation":false},"env":"prod","name":"Create ASG helloworld-v001","status":"running",' +
            '"startTime":1394486251000,"updateTime":1394486252000,"email":"yo@example.com","operation":"Go!",' +
            '"objectType":{"name":"cluster"},"objectId":"helloworld",' +
            '"log":["Get ready","Go!"]}'

    private String task2Json = '{"id":"6","userContext":{"ticket":"CMC-456","username":"homer",' +
            '"clientHostName":"mrplow","clientIpAddress":"4.5.6.7","region":"us-west-2",' +
            '"internalAutomation":false},"env":"prod",' +
            '"name":"Disable ASG helloworld-v000","status":"running","startTime":1394486261000,' +
            '"updateTime":1394486262000,"email":"hi@example.com","operation":"Drinking",' +
            '"objectType":{"name":"cluster"},"objectId":"helloworld",' +
            '"log":["Preparing to work","Doing work","Work is done","Drinking"]}'

    private String task3Json = '{"id":"77","userContext":{"ticket":"blah","username":"bart",' +
            '"clientHostName":"ipad3","clientIpAddress":"6.3.4.2","region":"eu-west-1",' +
            '"internalAutomation":false},"env":"prod","name":"Mess with people","status":"running",' +
            '"startTime":1394486351000,"updateTime":1394486358000,"email":"bart@example.com","operation":"Do it",' +
            '"objectType":{"name":"security"},"objectId":"eureka","log":["Plan","Do it"]}'

    private List<String> tagList = [
            '{"id":"42"}',
            '{"desc":"Give it away give it away give it away give it away now"}',
            '{"user":{"region":"US_WEST_2","internalAutomation":true}}',
            '{"link":{"type":{"name":"cluster"},"id":"123"}}'
    ]

    private WorkflowExecution execution = new WorkflowExecution(runId: 'abc', workflowId: 'def')
    private WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo(
            execution: execution,
            tagList: tagList,
            startTimestamp: new Date(1372230630000)
    )

    private Task workflowTask = new Task(id: '42', workflowExecution: execution,
            name: 'Give it away give it away give it away give it away now',
            userContext: new UserContext(region: Region.US_WEST_2, internalAutomation: true),
            startTime: new Date(1372230630000), status: 'running', objectType: EntityType.cluster, objectId: '123')

    def setup() {
        Retriable.mixin(NoDelayRetriableMixin)
    }

    AwsSimpleWorkflowService awsSimpleWorkflowService = Mock(AwsSimpleWorkflowService)
    RestClientService restClientService = Mock(RestClientService)
    ServerService serverService = Mock(ServerService)
    FlowService flowService = Mock(FlowService)
    EmailerService emailerService = Mock(EmailerService)
    EnvironmentService environmentService = Mock(EnvironmentService)
    PluginService pluginService = Mock(PluginService)
    TaskFinishedListener taskFinishedListener = Mock(TaskFinishedListener)
    Queue<Task> runningTaskQueue = Queues.newConcurrentLinkedQueue()
    Queue<Task> completedTaskQueue = Queues.newConcurrentLinkedQueue()
    TaskService service = new TaskService(awsSimpleWorkflowService: awsSimpleWorkflowService,
            running: runningTaskQueue, completed: completedTaskQueue, emailerService: emailerService,
            environmentService: environmentService, flowService: flowService, objectMapper: new ObjectMapper(),
            pluginService: pluginService, restClientService: restClientService, serverService: serverService)

    private <T> void addToQueue(Queue<T> queue, T... items) {
        items.each { queue.add(it) }
    }

    void 'should get task by ID from workflow execution'() {
        when:
        Task task = service.getTaskById('123')

        then:
        task.name == 'clean your room'
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >>
                Mock(WorkflowExecutionBeanOptions) {
                    asTask() >> new Task(name: 'clean your room')
                }
    }

    void 'should retry get task by ID'() {
        when:
        Task task = service.getTaskById('123')

        then:
        task.name == 'clean your room'
        2 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >> null
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >>
                Mock(WorkflowExecutionBeanOptions) {
                    asTask() >> new Task(name: 'clean your room')
                }
    }

    void 'should get running task by ID from local memory'() {

        Task task = new Task(id: '999')
        addToQueue(runningTaskQueue, task)

        when:
        Task result = service.getTaskById('999')

        then:
        result == task

        0 * _
    }

    void 'should get completed task by ID from local memory'() {

        Task task = new Task(id: '999')
        addToQueue(completedTaskQueue, task)

        when:
        Task result = service.getTaskById('999')

        then:
        result == task

        0 * _
    }

    void 'getTaskById should get task by ID from remote server if not found in SWF nor in local memory'() {

        task1.server = 'asgard100'

        when:
        Task result = service.getTaskById('4')

        then:
        result == task1
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('4') >> null
        1 * serverService.listRemoteServerNamesAndPorts() >> ['asgard100']
        1 * restClientService.getJsonAsText('http://asgard100/task/runningInMemory/4.json') >> task1Json
        0 * _
    }

    void 'should fail to get a task by returning null'() {

        when:
        Task task = service.getTaskById('123')

        then:
        task == null
        3 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >> null
    }

    void 'getLocalTaskById should explicitly get a locally running in-memory task'() {

        Task task = new Task(id: '123')
        addToQueue(runningTaskQueue, task)

        expect:
        service.getLocalTaskById('123') == task
    }

    void 'getLocalTaskById should explicitly get a locally completed in-memory task'() {

        Task task = new Task(id: '123')
        addToQueue(completedTaskQueue, task)

        expect:
        service.getLocalTaskById('123') == task
    }

    void 'getLocalTaskById should return null if task ID is missing'() {

        expect:
        service.getLocalTaskById(id) == null

        where:
        id << [null, '']
    }

    void 'getWorkflowTaskById should return null if task ID is missing'() {

        expect:
        service.getWorkflowTaskById(id) == null

        where:
        id << [null, '']
    }

    void 'getWorkflowTaskById should return null if no matching workflow execution exists'() {

        when:
        Task task = service.getWorkflowTaskById('789')

        then:
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('789') >> null
        task == null
    }

    void 'getWorkflowTaskById should return matching task for workflow executions'() {

        when:
        Task task = service.getWorkflowTaskById('789')

        then:
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('789') >>
                new WorkflowExecutionBeanOptions(executionInfo, [])
        task == workflowTask
    }

    void 'getRemoteTaskById should return null if task ID is missing'() {

        expect:
        service.getRemoteTaskById(id) == null

        where:
        id << [null, '']
    }

    void 'getRemoteTaskById should return null if there are no remote servers'() {

        when:
        Task task = service.getRemoteTaskById('789')

        then:
        1 * serverService.listRemoteServerNamesAndPorts() >> []
        task == null
    }

    void 'getRemoteTaskById should return null if no remote servers have the task'() {

        when:
        Task task = service.getRemoteTaskById('789')

        then:
        1 * serverService.listRemoteServerNamesAndPorts() >> ['asgard100', 'asgard101']
        1 * restClientService.getJsonAsText('http://asgard100/task/runningInMemory/789.json') >> null
        1 * restClientService.getJsonAsText('http://asgard101/task/runningInMemory/789.json') >> null
        0 * _
        task == null
    }

    void 'getRemoteTaskById should return first task found from remote servers'() {

        task1.server = 'asgard100'

        when:
        Task task = service.getRemoteTaskById('789')

        then:
        1 * serverService.listRemoteServerNamesAndPorts() >> ['asgard100', 'asgard101']
        1 * restClientService.getJsonAsText('http://asgard100/task/runningInMemory/789.json') >> task1Json
        0 * _
        task == task1
    }

    void 'should run an asynchronous task thread'() {

        CountDownLatch workHasStarted = new CountDownLatch(1)

        List<String> thingToChange = []
        service.idService = Mock(IdService)
        service.grailsApplication = [config: [cloud: [accountName: 'test']]]

        when:
        Task task = service.startTask(UserContext.auto(), 'test task', {
            thingToChange << 'hello'
            workHasStarted.countDown()
        })

        then:
        workHasStarted.await()
        task.thread.join()
        thingToChange == ['hello']
    }

    void 'should list local in-memory running tasks'() {

        Task taskA = new Task(name: 'a')
        Task taskB = new Task(name: 'b')
        addToQueue(runningTaskQueue, taskA, taskB)

        expect:
        service.localRunningInMemory == [taskA, taskB]
    }

    void 'should list remote in-memory running tasks'() {

        String jsonA = "[${task1Json},${task2Json}]"
        String jsonB = "[${task3Json}]"

        when:
        List<Task> remoteInMemoryTasks = service.getRemoteRunningInMemory()

        then:
        remoteInMemoryTasks == [task1, task2, task3]
        1 * serverService.listRemoteServerNamesAndPorts() >> ['asgard1:8080', 'asgard3:8080', 'asgard4:8080']
        1 * restClientService.getJsonAsText("http://asgard1:8080/task/runningInMemory.json") >> jsonA
        1 * restClientService.getJsonAsText("http://asgard3:8080/task/runningInMemory.json") >> '[]'
        1 * restClientService.getJsonAsText("http://asgard4:8080/task/runningInMemory.json") >> jsonB
        0 * _
    }

    void 'should list zero remote in-memory running tasks when the response from the remote server is not OK'() {
        when:
        List<Task> remoteInMemoryTasks = service.getRemoteRunningInMemory()

        then:
        remoteInMemoryTasks == []
        1 * serverService.listRemoteServerNamesAndPorts() >> ['asgard1:8080']
        1 * restClientService.getJsonAsText("http://asgard1:8080/task/runningInMemory.json") >> null
        0 * _
    }

    void 'should list zero remote in-memory running tasks when there are no remote servers'() {
        when:
        List<Task> remoteInMemoryTasks = service.getRemoteRunningInMemory()

        then:
        remoteInMemoryTasks == []
        1 * serverService.listRemoteServerNamesAndPorts() >> []
        0 * _
    }

    void 'should list all in-memory running tasks both local and remote together'() {

        Task task1 = new Task(id: '1')
        Task task2 = new Task(id: '2')
        Task task3 = new Task(id: '3')
        Task task4 = new Task(id: '4')
        TaskService taskService = Spy(TaskService) {
            getLocalRunningInMemory() >> [task1, task2]
            getRemoteRunningInMemory() >> [task3, task4]
        }

        expect:
        taskService.allRunningInMemory == [task1, task2, task3, task4]
    }

    void 'should list all running tasks including local in-memory, remote in-memory, and in SWF'() {
        Task task1 = new Task(id: '1')
        Task task2 = new Task(id: '2')
        TaskService taskService = Spy(TaskService) {
            getAllRunningInMemory() >> [task1, task2]
        }
        taskService.awsSimpleWorkflowService = awsSimpleWorkflowService

        when:
        Collection<Task> allRunning = taskService.allRunning

        then:
        1 * awsSimpleWorkflowService.openWorkflowExecutions >> [executionInfo]
        allRunning == [task1, task2, workflowTask]
    }

    void 'should list completed tasks including local in-memory completed and SWF completed'() {
        Task task1 = new Task(id: '1')
        Task task2 = new Task(id: '2')
        addToQueue(completedTaskQueue, task1, task2)
        executionInfo.closeTimestamp = new Date(1372230639000)
        workflowTask.updateTime = new Date(1372230639000)

        when:
        Collection<Task> allCompleted = service.allCompleted

        then:
        1 * awsSimpleWorkflowService.closedWorkflowExecutions >> [executionInfo]
        allCompleted == [task1, task2, workflowTask]
    }

    void 'should get all tasks for a specific object'() {

        UserContext east = UserContext.auto(Region.US_EAST_1)
        UserContext west = UserContext.auto(Region.US_WEST_1)
        Task wrongType = new Task(id: '1', objectId: 'hello', objectType: EntityType.alarm, userContext: east)
        Task task2 = new Task(id: '2', objectId: 'hello', objectType: EntityType.cluster, userContext: east)
        Task wrongId = new Task(id: '3', objectId: 'buffy', objectType: EntityType.cluster, userContext: east)
        Task task4 = new Task(id: '4', objectId: 'hello', objectType: EntityType.cluster, userContext: east)
        Task wrongRegion = new Task(id: '5', objectId: 'hello', objectType: EntityType.cluster, userContext: west)
        addToQueue(runningTaskQueue, wrongType, task2, wrongId, task4, wrongRegion)
        Link link = new Link(EntityType.cluster, 'hello')

        when:
        Collection<Task> tasks = service.getRunningTasksByObject(link, Region.US_EAST_1)

        then:
        tasks == [task2, task4]
    }

    void 'should throw an exception if trying to cancel a task without a workflowExecution, thread, or server'() {

        when:
        service.cancelTask(userContext, task1)

        then:
        thrown(IllegalStateException)
        0 * _
    }

    void 'should cancel a workflow execution task'() {

        WorkflowClientExternal workflowClientExternal = Mock(WorkflowClientExternal)

        when:
        service.cancelTask(userContext, workflowTask)

        then:
        1 * flowService.getWorkflowClient(execution) >> workflowClientExternal
        1 * workflowClientExternal.terminateWorkflowExecution(_, _, _)
        0 * _
    }

    void 'should cancel a local in-memory running task'() {

        Date date = new Date(1394486461000)
        UserContext userContext1 = new UserContext(username: 'homer', clientHostName: 'commodore64')
        CountDownLatch workFinished = new CountDownLatch(1)
        Thread thread = Thread.start {
            try {
                workFinished.await()
            } catch (InterruptedException ignore) { }
        }
        task1.thread = thread

        expect:
        thread.isAlive()
        task1.log == ['Get ready', 'Go!']
        task1.status == 'running'
        task1.operation == 'Go!'
        completedTaskQueue.size() == 0

        when:
        service.cancelTask(userContext1, task1)

        then:
        thread.join(10)
        !thread.isAlive()
        task1.log[0] == 'Get ready'
        task1.log[1] == 'Go!'
        task1.log[2] ==~ /2014-03-.._..:21:01 Cancelled by homer@commodore64/
        task1.status == 'failed'
        task1.operation == ''
        completedTaskQueue.size() == 1
        1 * environmentService.currentDate >> date
        1 * pluginService.taskFinishedListeners >> []
        0 * _
    }

    void 'should cancel a remote in-memory running task'() {

        UserContext userContext1 = new UserContext(username: 'homer', clientHostName: 'commodore64')
        task1.server = 'asgard100'

        when:
        service.cancelTask(userContext1, task1)

        then:
        1 * restClientService.post('http://asgard100/task/cancel', ['id':'4', format: 'json']) >> HttpStatus.SC_OK
        0 * _
    }

    void 'should throw an exception if failing to cancel a remote in-memory running task'() {

        UserContext userContext1 = new UserContext(username: 'homer', clientHostName: 'commodore64')
        task1.server = 'asgard100'

        when:
        service.cancelTask(userContext1, task1)

        then:
        1 * restClientService.post('http://asgard100/task/cancel', ['id':'4', format: 'json'])
        thrown(RemoteException)
        0 * _
    }

    @SuppressWarnings("GroovyAccessibility")
    void "should call task finished listeners after a task finishes"() {

        when:
        service.finish(task1)

        then:
        1 * pluginService.taskFinishedListeners >> [taskFinishedListener]
        1 * taskFinishedListener.taskFinished(task1)
        0 * _
    }

    @SuppressWarnings("GroovyAccessibility")
    void "should send a system alert email if task finished listener call throws an exception"() {

        when:
        service.finish(task1)

        then:
        1 * pluginService.taskFinishedListeners >> [taskFinishedListener]
        1 * taskFinishedListener.taskFinished(task1) >> { throw new ServerException('Busted') }
        1 * emailerService.sendExceptionEmail(_, _)
        0 * _
    }
}
