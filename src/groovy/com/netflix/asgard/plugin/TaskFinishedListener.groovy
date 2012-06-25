package com.netflix.asgard.plugin

import com.netflix.asgard.Task

interface TaskFinishedListener {

    void taskFinished(Task task)

}
