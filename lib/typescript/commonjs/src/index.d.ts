import EventEmitter from 'eventemitter3';
import { NativeEventEmitter } from 'react-native';
declare const UpsellBackgroundActions: any;
declare const nativeEventEmitter: NativeEventEmitter;
export { UpsellBackgroundActions, nativeEventEmitter };
declare class BackgroundServer extends EventEmitter {
    constructor();
    /**
     * @private
     */
    _addListeners(): void;
    /**
     * **ANDROID ONLY**
     *
     * Updates the task notification.
     *
     * *On iOS this method will return immediately*
     *
     * @param {{taskTitle?: string,
     *          taskDesc?: string,
     *          taskIcon?: {name: string, type: string, package?: string},
     *          color?: string,
     *          linkingURI?: string,
     *          progressBar?: {max: number, value: number, indeterminate?: boolean}}} taskData
     */
    updateNotification(taskData: any): Promise<void>;
    /**
     * Returns if the current background task is running.
     *
     * It returns `true` if `start()` has been called and the task has not finished.
     *
     * It returns `false` if `stop()` has been called, **even if the task has not finished**.
     */
    isRunning(): any;
    /**
     * @argument {()=>{}} callback
     * @returns {Promise<void>}
     */
    setCallBack(callback: any): Promise<void>;
    /**
     * @template T
     *
     * @param {(taskData?: T) => Promise<void>} task
     * @param {BackgroundTaskOptions & {parameters?: T}} options
     * @param  {number} triggerTime when will the alarm fire to trigger the restart callback
     * @returns {Promise<void>}
     */
    start(task: any, options: any, triggerTime: any): Promise<void>;
    /**
     * @private
     * @template T
     * @param {(taskData?: T) => Promise<void>} task
     * @param {T} [parameters]
     */
    _generateTask(task: any, parameters: any): () => Promise<void>;
    /**
     * @private
     * @param {BackgroundTaskOptions} options
     */
    _normalizeOptions(options: any): {
        taskName: any;
        taskTitle: any;
        taskDesc: any;
        taskIcon: any;
        color: any;
        linkingURI: any;
        progressBar: any;
    };
    /**
     * Stops the background task.
     *
     * @returns {Promise<void>}
     */
    stop(): Promise<void>;
    sendStopBroadcast(): Promise<void>;
    isBackgroundServiceRunning(): Promise<any>;
    checkScheduleExactAlarmPermission(): Promise<any>;
    requestExactAlarmPermission(): Promise<any>;
    listRunningServices(): Promise<any>;
    stopAlarm(): Promise<any>;
}
declare const BackgroundService: BackgroundServer;
export default BackgroundService;
//# sourceMappingURL=index.d.ts.map