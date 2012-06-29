package com.netflix.asgard.plugin

import com.netflix.asgard.Task

/**
 * Observer interface to execute code when a task is finished. Listeners are registered under
 * plugins/taskFinishedListeners in Config.groovy.
 */
interface TaskFinishedListener {

    /**
     * Method to call when a task is finished.
     *
     * @param task The finished task (can be completed or failed)
     */
    void taskFinished(Task task)

}
